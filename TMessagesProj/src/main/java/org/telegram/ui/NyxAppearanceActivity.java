package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.*;
import java.util.ArrayList;
import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

/** Schematic native preview. Draft never touches live settings before Apply. */
public class NyxAppearanceActivity extends BaseFragment {
    private final NyxAppearance.Draft draft=new NyxAppearance.Draft();private UniversalRecyclerView list;private Preview preview;private org.telegram.ui.Cells.AppIconsSelectorCell icons;
    @Override public View createView(Context context){
        actionBar.setTitle(getString(R.string.NyxAppearance));actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
        FrameLayout frame=new FrameLayout(context);frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));fragmentView=frame;
        icons=new org.telegram.ui.Cells.AppIconsSelectorCell(context,this,currentAccount);
        preview=new Preview(context);list=new UniversalRecyclerView(context,currentAccount,classGuid,this::fill,this::click,null,getResourceProvider());frame.addView(list,LayoutHelper.createFrame(-1,-1));list.adapter.update(false);return frame;
    }
    private void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        items.add(UItem.asHeader(getString(R.string.NyxPreview)));items.add(UItem.asCustom(preview,300));items.add(UItem.asShadow(getString(R.string.NyxAppearanceHint)));
        items.add(UItem.asHeader(getString(R.string.NyxFreeIcons)));items.add(UItem.asCustom(icons,112));items.add(UItem.asShadow(getString(R.string.NyxFreeIconsHint)));
        String[] names={getString(R.string.NyxStyleStandard),getString(R.string.NyxStyleNyx),getString(R.string.NyxStyleCompact),getString(R.string.NyxStyleMinimal)};
        items.add(UItem.asButton(1,getString(R.string.NyxStylePreset),names[Math.max(0,Math.min(3,draft.preset))]));
        items.add(UItem.asButton(2,getString(R.string.NyxBubbleRadius),""+draft.radius));items.add(UItem.asButton(3,getString(R.string.NyxFontSize),""+draft.font));
        items.add(UItem.asCheck(4,getString(R.string.NyxHideStickerTime)).setChecked(draft.stickers));items.add(UItem.asCheck(5,getString(R.string.NyxHideGifTime)).setChecked(draft.gifs));items.add(UItem.asCheck(6,getString(R.string.NyxHideRoundTime)).setChecked(draft.round));
        items.add(UItem.asCheck(7,getString(R.string.NyxHideAvatars)).setChecked(draft.avatars));items.add(UItem.asCheck(8,getString(R.string.NyxHideDates)).setChecked(draft.dates));items.add(UItem.asShadow(null));
        items.add(UItem.asButton(9,getString(R.string.NyxApply)));items.add(UItem.asButton(10,getString(R.string.NyxAppearanceRestore)));
    }
    private void refresh(){preview.invalidate();list.adapter.update(false);}
    private void click(UItem item,View view,int position,float x,float y){
        if(item.id==1){String[] a={getString(R.string.NyxStyleStandard),getString(R.string.NyxStyleNyx),getString(R.string.NyxStyleCompact),getString(R.string.NyxStyleMinimal)};showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(R.string.NyxStylePreset)).setItems(a,(d,n)->{draft.preset(n);refresh();}).create());return;}
        if(item.id==2||item.id==3){int[] a=item.id==2?new int[]{0,4,8,10,12,17,18,20,24}:new int[]{12,14,15,16,18,20,22,24};String[] text=new String[a.length];for(int i=0;i<a.length;i++)text[i]=""+a[i];showDialog(new AlertDialog.Builder(getParentActivity()).setTitle(getString(item.id==2?R.string.NyxBubbleRadius:R.string.NyxFontSize)).setItems(text,(d,n)->{if(item.id==2)draft.radius=a[n];else draft.font=a[n];refresh();}).create());return;}
        switch(item.id){case 4:draft.stickers=!draft.stickers;break;case 5:draft.gifs=!draft.gifs;break;case 6:draft.round=!draft.round;break;case 7:draft.avatars=!draft.avatars;break;case 8:draft.dates=!draft.dates;break;case 9:NyxAppearance.apply(draft);finishFragment();return;case 10:NyxAppearance.restore();finishFragment();return;default:return;}refresh();
    }
    private class Preview extends View{
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Preview(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setContentDescription(getString(R.string.NyxPreview));}
        void text(Canvas c,String s,float x,float y,int size,int color){paint.setColor(color);paint.setTextSize(dp(size));c.drawText(s,dp(x),dp(y),paint);}
        void bubble(Canvas c,float x,float y,float w,float h,int color){paint.setColor(color);c.drawRoundRect(dp(x),dp(y),dp(x+w),dp(y+h),dp(draft.radius),dp(draft.radius),paint);}
        @Override protected void onDraw(Canvas c){
            float width=getWidth()/(float)dp(1);boolean night=draft.preset==1;int bg=night?0xFF10131D:Theme.getColor(Theme.key_chat_wallpaper);c.drawColor(bg);
            int ink=night?0xFFE9EAF0:Theme.getColor(Theme.key_chat_messageTextIn);int in=night?0xFF25293A:Theme.getColor(Theme.key_chat_inBubble),out=night?0xFF551C83:Theme.getColor(Theme.key_chat_outBubble);
            if(!draft.dates){bubble(c,width/2-36,12,72,22,0x884E5262);text(c,"13 октября",width/2-31,28,11,Color.WHITE);}
            float left=draft.avatars?12:44;if(!draft.avatars){paint.setColor(0xFF855ADD);c.drawCircle(dp(23),dp(63),dp(14),paint);}
            bubble(c,left,44,width-left-36,60,in);text(c,getString(R.string.NyxSampleIncoming),left+10,70,Math.min(16,draft.font),ink);
            bubble(c,58,116,width-70,65,out);text(c,"Nyxgram",68,142,draft.font,night?Color.WHITE:Theme.getColor(Theme.key_chat_messageTextOut));text(c,"Предпросмотр изменений",68,163,12,night?0xFFDAE1F2:Theme.getColor(Theme.key_chat_messageTextOut));
            paint.setColor(0xFF845BDD);c.drawCircle(dp(53),dp(215),dp(23),paint);text(c,"✦",43,224,24,Color.WHITE);if(!draft.stickers)text(c,"12:34 ✓",82,230,11,ink);
            bubble(c,left,254,width-left-36,36,in);text(c,NyxAppearance.symbol(currentAccount)+" "+getString(R.string.NyxArchiveDeleted),left+10,277,12,NyxAppearance.color(currentAccount));
        }
    }
}
