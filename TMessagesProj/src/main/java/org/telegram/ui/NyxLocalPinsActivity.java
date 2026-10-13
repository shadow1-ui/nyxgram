package org.telegram.ui;
import android.content.Context;import android.view.View;import android.widget.FrameLayout;
import org.telegram.messenger.*;import org.telegram.ui.ActionBar.*;import org.telegram.ui.Components.*;import java.util.*;
import static org.telegram.messenger.LocaleController.getString;
public class NyxLocalPinsActivity extends BaseFragment {
 private UniversalRecyclerView list;private ArrayList<Long> pins;
 @Override public View createView(Context c){actionBar.setTitle(getString(R.string.NyxLocalPins));actionBar.setBackButtonImage(R.drawable.ic_ab_back);actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});FrameLayout frame=new FrameLayout(c);frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));fragmentView=frame;list=new UniversalRecyclerView(c,currentAccount,classGuid,this::fill,this::click,this::hold,getResourceProvider());frame.addView(list,LayoutHelper.createFrame(-1,-1));list.adapter.update(false);return frame;}
 private void fill(ArrayList<UItem> items,UniversalAdapter adapter){pins=NyxLocalPins.list(currentAccount);items.add(UItem.asShadow(getString(R.string.NyxLocalPinsHint)));if(pins.isEmpty())items.add(UItem.asShadow(getString(R.string.NyxPinsEmpty)));for(int i=0;i<pins.size();i++)items.add(UItem.asButton(i+1,NyxLocalPins.title(currentAccount,pins.get(i)),"ID "+pins.get(i)));}
 private void click(UItem item,View v,int pos,float x,float y){int i=item.id-1;if(i>=0&&i<pins.size())presentFragment(ChatActivity.of(pins.get(i)));}
 private boolean hold(UItem item,View v,int pos,float x,float y){int i=item.id-1;if(i<0||i>=pins.size())return false;long did=pins.get(i);showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxLocalUnpin)).setPositiveButton(getString(R.string.NyxDone),(d,w)->{NyxLocalPins.toggle(currentAccount,did);list.adapter.update(false);}).setNegativeButton(getString(R.string.NyxCancel),null).create());return true;}
 @Override public void onResume(){super.onResume();if(list!=null)list.adapter.update(false);}
}
