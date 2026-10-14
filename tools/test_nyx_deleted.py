"""Execute the actual production lifecycle/page rules; check native integration wiring."""
from pathlib import Path
import tempfile,subprocess
r=Path(__file__).resolve().parents[1]
helper=r/'TMessagesProj/src/main/java/org/telegram/messenger/NyxDeletedMessages.java'
java_test='''import org.telegram.messenger.NyxDeletedMessages;
public class DeletedTest {
 static int checks;static void check(boolean value){checks++;if(!value)throw new AssertionError("check "+checks);}
 static void range(int min,int max,int anchor,boolean latest,boolean oldest,int lo,int hi){int[] b=NyxDeletedMessages.pageBounds(min,max,anchor,latest,oldest);check(b[0]==lo&&b[1]==hi);}
 public static void main(String[] args){
  check(NyxDeletedMessages.visible(false,false));check(!NyxDeletedMessages.visible(true,false));check(NyxDeletedMessages.visible(true,true));check(NyxDeletedMessages.visible(false,true));
  // Every combination: local copy only for eligible ordinary deletions, never scheduled moves.
  for(int bits=0;bits<256;bits++){boolean[] f=new boolean[8];for(int i=0;i<8;i++)f[i]=(bits&(1<<i))!=0;check(NyxDeletedMessages.retain(f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7])==(f[0]&&f[1]&&f[2]&&!f[3]&&!f[4]&&!f[5]&&!f[6]&&f[7]));}
  range(90,100,0,true,false,90,Integer.MAX_VALUE); // deleted newest 101 must return after reopening
  range(90,100,0,true,true,1,Integer.MAX_VALUE); // deleted oldest 89 in complete history
  range(50,60,70,false,false,50,70); // deleted page-boundary 61..70
  range(Integer.MAX_VALUE,0,70,false,false,1,70); // an entirely deleted older page
  range(Integer.MAX_VALUE,0,0,true,true,1,Integer.MAX_VALUE); // entire dialog deleted remotely
  range(50,60,0,false,false,50,60); // around-message loading must not pull unrelated newest history
  range(Integer.MAX_VALUE,0,0,false,false,1,0); // unknown empty window must not pull whole archive
  check(NyxDeletedMessages.inThread(10,4,0,0,4,true));check(!NyxDeletedMessages.inThread(10,5,0,0,4,true));
  check(NyxDeletedMessages.inThread(10,0,4,0,4,false));check(NyxDeletedMessages.inThread(10,0,7,4,4,false));check(NyxDeletedMessages.inThread(4,0,0,0,4,false));check(!NyxDeletedMessages.inThread(10,0,7,8,4,false));check(NyxDeletedMessages.inThread(10,4,7,8,0,false));
  System.out.println("PASS: "+checks+" actual deleted-message lifecycle, paging and thread assertions");
 }
}'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d);(p/'DeletedTest.java').write_text(java_test)
 (p/'Compile.java').write_text('import javax.tools.*;public class Compile{public static void main(String[] a){int n=ToolProvider.getSystemJavaCompiler().run(null,null,null,a);if(n!=0)System.exit(n);}}')
 subprocess.run(['java',str(p/'Compile.java'),'-d',d,str(helper),str(p/'DeletedTest.java')],check=True)
 subprocess.run(['java','-cp',d,'DeletedTest'],check=True)
s=(r/'TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java').read_text()
assert s.count('NyxDeletedMessages.visible(')>=3
assert 'NyxDeletedMessages.retain(' in s and 'old.nyxDeleted' in s and 'nyx_archive_style' in s
s=(r/'TMessagesProj/src/main/java/org/telegram/messenger/NyxArchive.java').read_text()
assert 'thread!=0' not in s and 'NyxDeletedMessages.pageBounds' in s and 'NyxDeletedMessages.inThread' in s
assert 'UNION ALL SELECT uid,mid,data,ttl FROM messages_topics' in s
assert 'm.id = messageId; m.dialog_id = dialog; m.ttl = Math.max' in s
assert 'if(ids.contains(mid)){deletedIds.add(mid);continue;}' in s
print('PASS: native parent rendering, refresh, pagination and topic archive hooks')
