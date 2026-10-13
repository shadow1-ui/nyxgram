package org.telegram.ui;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.*;
import java.util.ArrayList;
import static org.telegram.messenger.LocaleController.getString;
public class NyxProfilesActivity extends BaseFragment{
    private UniversalRecyclerView list;private ArrayList<NyxProfiles.Item> profiles;
    @Override public View createView(Context context){actionBar.setTitle(getString(R.string.NyxSavedProfiles));actionBar.setBackButtonImage(R.drawable.ic_ab_back);actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});FrameLayout f=new FrameLayout(context);f.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));fragmentView=f;list=new UniversalRecyclerView(context,currentAccount,classGuid,this::fill,this::click,this::longClick,getResourceProvider());f.addView(list,LayoutHelper.createFrame(-1,-1));list.adapter.update(false);return f;}
    private void fill(ArrayList<UItem> items,UniversalAdapter adapter){profiles=NyxProfiles.list(currentAccount);items.add(UItem.asShadow(getString(R.string.NyxProfileLinkHint)));if(profiles.isEmpty())items.add(UItem.asShadow(getString(R.string.NyxArchiveEmpty)));for(int i=0;i<profiles.size();i++){NyxProfiles.Item p=profiles.get(i);items.add(UItem.asButton(i+1,p.name,"ID "+p.id));}}
    private void click(UItem item,View v,int position,float x,float y){int i=item.id-1;if(i>=0&&i<profiles.size())NyxProfiles.open(this,profiles.get(i).id);}
    private boolean longClick(UItem item,View v,int position,float x,float y){int i=item.id-1;if(i<0||i>=profiles.size())return false;long id=profiles.get(i).id;showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxRemoveBookmark)).setPositiveButton(getString(R.string.NyxResetAction),(d,w)->{NyxProfiles.remove(currentAccount,id);list.adapter.update(false);}).setNegativeButton(getString(R.string.NyxCancel),null).create());return true;}
    @Override public void onResume(){super.onResume();if(list!=null)list.adapter.update(false);}
}
