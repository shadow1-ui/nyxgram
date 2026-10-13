package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.*;
import java.text.DateFormat;
import java.util.*;
import static org.telegram.messenger.LocaleController.getString;

public class NyxArchiveActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private final long dialogId;private final int kind;
    private UniversalRecyclerView list;private String query="";private int generation;private boolean busy;
    private boolean addedSecureFlag;private final ArrayList<NyxArchive.Entry> entries=new ArrayList<>();
    public NyxArchiveActivity(long dialogId,int kind){this.dialogId=dialogId;this.kind=kind;}
    @Override public View createView(Context context){
        actionBar.setTitle(getString(kind==NyxArchive.DELETED?R.string.NyxDeletedArchive:R.string.NyxEditsArchive));actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
        actionBar.createMenu().addItem(0,R.drawable.outline_header_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener(){
            @Override public void onTextChanged(EditText edit){query=edit.getText().toString();load(false);}
        }).setSearchFieldHint(getString(R.string.NyxArchiveSearch));
        FrameLayout frame=new FrameLayout(context);frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));fragmentView=frame;
        list=new UniversalRecyclerView(context,currentAccount,classGuid,this::fill,this::click,null,getResourceProvider());frame.addView(list,LayoutHelper.createFrame(-1,-1));list.adapter.update(false);return frame;
    }
    private void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        if(!NyxArchiveGate.canShow(currentAccount)){items.add(UItem.asButton(-1,getString(R.string.NyxUnlock)));items.add(UItem.asShadow(getString(R.string.NyxArchiveLocked)));return;}
        if(entries.isEmpty())items.add(UItem.asShadow(getString(busy?R.string.NyxArchiveBusy:R.string.NyxArchiveEmpty)));
        DateFormat date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
        for(int i=0;i<entries.size();i++){
            NyxArchive.Entry e=entries.get(i);String text=e.message.message==null?"":e.message.message;
            if(text.isEmpty())text=getString(R.string.NyxArchiveLocalFile);
            if(text.length()>70)text=text.substring(0,70)+"…";
            items.add(UItem.asButton(i+100,text,date.format(new Date(e.changed))+" · ID "+e.dialog));
        }
        if(entries.size()>0 && entries.size()%100==0)items.add(UItem.asButton(-2,getString(R.string.NyxArchiveMore)));
    }
    private void load(boolean more){
        if(!NyxArchiveGate.canShow(currentAccount)){entries.clear();if(list!=null)list.adapter.update(false);return;}
        int token=++generation;busy=true;long before=more&&!entries.isEmpty()?entries.get(entries.size()-1).id:0;
        if(!more)entries.clear();if(list!=null)list.adapter.update(false);
        NyxArchive.list(currentAccount,dialogId,kind,query,before,result->{if(token!=generation || list==null)return;busy=false;if(NyxArchiveGate.canShow(currentAccount))entries.addAll(result);else entries.clear();list.adapter.update(false);});
    }
    private void click(UItem item,View view,int position,float x,float y){
        if(!NyxArchiveGate.canShow(currentAccount)){NyxArchiveGate.require(currentAccount,this,()->load(false));return;}
        if(item.id==-1){NyxArchiveGate.require(currentAccount,this,()->load(false));return;}
        if(item.id==-2){load(true);return;}
        int index=item.id-100;if(index<0||index>=entries.size())return;
        NyxArchive.Entry entry=entries.get(index);NyxArchiveGate.require(currentAccount,this,()->showEntry(this,entry));
    }
    public static void showEntry(BaseFragment fragment,NyxArchive.Entry e){
        NyxArchiveGate.require(fragment.getCurrentAccount(),fragment,()->{
            String text=e.message.message==null?"":e.message.message;
            if(text.isEmpty())text=getString(R.string.NyxArchiveLocalFile);
            final String copy=text;
            AlertDialog protectedDialog=new AlertDialog.Builder(fragment.getParentActivity()).setTitle(getString(e.kind==NyxArchive.DELETED?R.string.NyxArchiveDeleted:R.string.NyxArchiveEdited))
                .setMessage(text+"\n\nID "+e.mid+" · "+DateFormat.getDateTimeInstance().format(new Date(e.changed)))
                .setPositiveButton(getString(R.string.NyxArchiveCopy),(d,w)->AndroidUtilities.addToClipboard(copy))
                .setNeutralButton(getString(R.string.NyxArchiveOpen),(d,w)->fragment.presentFragment(ChatActivity.of(e.dialog)))
                .setNegativeButton(getString(R.string.NyxCancel),null).create();NyxArchiveGate.track(protectedDialog);fragment.showDialog(protectedDialog);
        });
    }
    @Override public boolean onFragmentCreate(){getNotificationCenter().addObserver(this,NotificationCenter.updateInterfaces);return super.onFragmentCreate();}
    @Override public void onFragmentDestroy(){getNotificationCenter().removeObserver(this,NotificationCenter.updateInterfaces);if(getParentActivity()!=null&&addedSecureFlag)getParentActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);super.onFragmentDestroy();}
    @Override public void didReceivedNotification(int id,int account,Object... args){if(!NyxArchiveGate.canShow(currentAccount)){generation++;entries.clear();if(list!=null)list.adapter.update(false);}}
    @Override public void onResume(){
        super.onResume();if(getParentActivity()!=null){if(NyxConfig.get("protectArchive",currentAccount,false)&&(getParentActivity().getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)==0){getParentActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);addedSecureFlag=true;}}
        load(false);if(!NyxArchiveGate.canShow(currentAccount))NyxArchiveGate.require(currentAccount,this,()->load(false));
    }
    @Override public void onPause(){super.onPause();generation++;entries.clear();if(list!=null)list.adapter.update(false);}
}
