"""Executable queue regression tests and source/resource checks, NOT device QA."""
from pathlib import Path
import re, subprocess, tempfile, xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
j=r/'TMessagesProj/src/main/java/org/telegram'
s=(j/'messenger/MediaController.java').read_text()
start=s.index('    public ArrayList<MessageObject> getVoiceMessagesPlaylist()')
end=s.index('    public void setVoiceMessagesPlaylist(',start)
# Compile and execute the exact production methods with a minimal playback boundary.
methods=s[start:end]
stubs='''import java.util.*;
class UserConfig { static int selectedAccount; }
class MessageObject {
 int currentAccount; long dialog; boolean once,roundOnce,deleted,voice=true,round; boolean nyxDeleted;
 MessageObject(int account,long dialog){currentAccount=account;this.dialog=dialog;}
 boolean isVoice(){return voice;} boolean isRoundVideo(){return round;}
 boolean isVoiceOnce(){return once;} boolean isRoundOnce(){return roundOnce;}
 long getDialogId(){return dialog;}
}
public class QueueTest {
 MessageObject playingMessageObject; ArrayList<MessageObject> voiceMessagesPlaylist;
 boolean voiceMessagesPlaylistUnread; boolean playbackSucceeds=true;
 boolean playMessage(MessageObject m){if(playbackSucceeds)playingMessageObject=m;return playbackSucceeds;}
 void setVoiceMessagesPlaylist(ArrayList<MessageObject> p,boolean u){voiceMessagesPlaylist=p;voiceMessagesPlaylistUnread=u;}
 static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}
'''
tests='''
 public static void main(String[] args){
 QueueTest c=new QueueTest();
 check(c.getVoiceMessagesPlaylist().isEmpty());
 MessageObject a=new MessageObject(0,10),b=new MessageObject(0,10),d=new MessageObject(0,10);
 MessageObject foreign=new MessageObject(1,10),other=new MessageObject(0,20),once=new MessageObject(0,10),deleted=new MessageObject(0,10);
 once.once=true;deleted.nyxDeleted=true;c.playingMessageObject=a;
 check(c.getVoiceMessagesPlaylist().equals(Arrays.asList(a)));
 c.voiceMessagesPlaylist=new ArrayList<>(Arrays.asList(a,b,d,foreign,other,once,deleted));
 check(c.getVoiceMessagesPlaylist().equals(Arrays.asList(a,b,d)));
 c.getVoiceMessagesPlaylist().clear();check(c.voiceMessagesPlaylist.size()==7);
 check(!c.playVoiceMessageFromQueue(foreign));check(!c.playVoiceMessageFromQueue(other));
 check(!c.playVoiceMessageFromQueue(once));check(!c.playVoiceMessageFromQueue(deleted));
 check(!c.playVoiceMessageFromQueue(new MessageObject(0,10)));check(!c.playVoiceMessageFromQueue(null));
 UserConfig.selectedAccount=1;check(!c.playVoiceMessageFromQueue(b));UserConfig.selectedAccount=0;
 c.playbackSucceeds=false;check(!c.playVoiceMessageFromQueue(b));check(c.voiceMessagesPlaylist.size()==7);
 c.playbackSucceeds=true;c.voiceMessagesPlaylistUnread=true;
 check(c.playVoiceMessageFromQueue(b));check(c.playingMessageObject==b);
 check(c.voiceMessagesPlaylist.equals(Arrays.asList(b,d)));check(c.voiceMessagesPlaylistUnread);
 c.voiceMessagesPlaylist=new ArrayList<>(Arrays.asList(b,d));check(c.playVoiceMessageFromQueue(d));
 check(c.voiceMessagesPlaylist.equals(Arrays.asList(d)));
 d.round=true;d.voice=false;check(c.getVoiceMessagesPlaylist().equals(Arrays.asList(d)));
 d.roundOnce=true;check(c.getVoiceMessagesPlaylist().isEmpty());check(!c.playVoiceMessageFromQueue(d));
 d.roundOnce=false;d.nyxDeleted=true;check(c.getVoiceMessagesPlaylist().isEmpty());
 System.out.println("PASS: "+checks+" executable production queue assertions (stub playback boundary)");
 }
}'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'QueueTest.java';p.write_text(stubs+methods+tests)
 subprocess.run(['java',str(p)],check=True)
# Parse changed Java with javac, without falsely claiming an Android build.
parser='''import javax.tools.*;import com.sun.source.util.*;import java.util.*;
public class ParseJava{public static void main(String[]a)throws Exception{
 var c=ToolProvider.getSystemJavaCompiler();var ds=new DiagnosticCollector<JavaFileObject>();
 try(var f=c.getStandardFileManager(ds,null,null)){
 var t=(JavacTask)c.getTask(null,f,ds,List.of("-proc:none"),null,f.getJavaFileObjects(a));t.parse();
 for(var d:ds.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)throw new AssertionError(d.toString());
 }System.out.println("PASS: javac syntax parsing (no Android type checking)");}}'''
files=['messenger/MediaController.java','messenger/NyxAppearance.java','ui/Components/FragmentContextView.java','ui/ProfileActivity.java','ui/NyxgramActivity.java','ui/NyxVoiceQueue.java']
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'ParseJava.java';p.write_text(parser)
 subprocess.run(['java',str(p),*[str(j/f) for f in files]],check=True)
resources=r/'TMessagesProj/src/main/res'
names={el.attrib['name'] for el in ET.parse(resources/'values/strings.xml').getroot() if 'name' in el.attrib}
for f in ['ui/NyxVoiceQueue.java','ui/NyxgramActivity.java']:
 text=(j/f).read_text()
 for name in re.findall(r'R.string.(\w+)',text):assert name in names,name
 for name in re.findall(r'R.drawable.(\w+)',text):assert list(resources.glob('drawable*/'+name+'.*')),name
sheet=(j/'ui/NyxVoiceQueue.java').read_text()
for guard in ['owner == UserConfig.getInstance(account).getClientUserId()', 'cancelRunOnUIThread(ticker)', '!message.isVoiceOnce()', '!message.isRoundOnce()', '!message.nyxDeleted']:
 assert guard in sheet,guard
profile=(j/'ui/ProfileActivity.java').read_text()
assert 'setTextAndValue(idText, LocaleController.getString(R.string.NyxInfo)' in profile
cell=(j/'ui/Cells/ChatMessageCell.java').read_text()
assert 'saveLayerAlpha(0,0,getWidth(),getHeight(),225)' in cell
assert 'label.setSpan(new android.text.style.ForegroundColorSpan' in cell
print('PASS: resources, account/lifecycle guards, compact labels and existing faded rendering wiring')
