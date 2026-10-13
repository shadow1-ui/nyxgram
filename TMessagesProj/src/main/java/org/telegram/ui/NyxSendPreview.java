package org.telegram.ui;

import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.telegram.messenger.LocaleController.getString;

/** Explicit selection preview. Scoped approval never changes persisted settings. */
public final class NyxSendPreview {
    private NyxSendPreview() {}
    private static final ThreadLocal<Approval> approval = new ThreadLocal<>();
    private static class Approval {
        int account; long peer; Object media;
        Approval(int account,long peer,Object media){this.account=account;this.peer=peer;this.media=identity(media);}
    }
    private static Object identity(Object media){
        return media instanceof TLRPC.BotInlineResult && ((TLRPC.BotInlineResult)media).document != null ? ((TLRPC.BotInlineResult)media).document : media;
    }
    public static boolean approved(int account,long peer,Object media){
        Approval a=approval.get();return a!=null && a.account==account && a.peer==peer && a.media==identity(media);
    }
    public static void withApproval(int account,long peer,Object media,Runnable send){
        Approval previous=approval.get();approval.set(new Approval(account,peer,media));
        try{send.run();}finally{if(previous==null)approval.remove();else approval.set(previous);}
    }
    public static boolean gif(Object media){
        if(media instanceof TLRPC.Document)return MessageObject.isGifDocument((TLRPC.Document)media);
        if(media instanceof TLRPC.BotInlineResult){TLRPC.BotInlineResult result=(TLRPC.BotInlineResult)media;return "gif".equals(result.type) || result.document!=null && MessageObject.isGifDocument(result.document);}
        return false;
    }
    public static boolean enabled(int account,Object media){
        if(gif(media))return NyxConfig.get("confirmGifs",account,true);
        Object item=identity(media);
        return item instanceof TLRPC.Document && (MessageObject.isStickerDocument((TLRPC.Document)item) || MessageObject.isAnimatedStickerDocument((TLRPC.Document)item,true) || MessageObject.isVideoStickerDocument((TLRPC.Document)item)) && NyxConfig.get("confirmStickers",account,true);
    }
    public static void show(BaseFragment fragment,int account,long peer,Object media,Object parent,Runnable send){
        if(fragment==null || fragment.getParentActivity()==null || fragment.getCurrentAccount()!=account || ApplicationLoader.mainInterfacePaused)return;
        if(fragment instanceof ChatActivity && ((ChatActivity)fragment).getDialogId()!=peer)return;
        long owner=UserConfig.getInstance(account).getClientUserId();
        TLRPC.Document document=identity(media) instanceof TLRPC.Document?(TLRPC.Document)identity(media):null;
        TLRPC.BotInlineResult result=media instanceof TLRPC.BotInlineResult?(TLRPC.BotInlineResult)media:null;
        if(document==null && (result==null || result.content==null))return;
        if(ContentPreviewViewer.hasInstance() && ContentPreviewViewer.getInstance().isVisible())ContentPreviewViewer.getInstance().closeWithMenu();
        LinearLayout box=new LinearLayout(fragment.getParentActivity());box.setOrientation(LinearLayout.VERTICAL);
        BackupImageView image=new BackupImageView(fragment.getParentActivity());ImageReceiver receiver=image.getImageReceiver();
        receiver.setCurrentAccount(account);receiver.setAspectFit(true);receiver.setAllowStartAnimation(true);receiver.setAllowStartLottieAnimation(true);receiver.setAutoRepeat(1);
        image.setContentDescription(getString(gif(media)?R.string.NyxGifPreview:R.string.NyxStickerPreview));
        box.addView(image,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,240,Gravity.CENTER,16,8,16,0));
        TextView status=new TextView(fragment.getParentActivity());status.setGravity(Gravity.CENTER);status.setTextSize(13);status.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText,fragment.getResourceProvider()));status.setText(getString(R.string.NyxPreviewLoading));
        box.addView(status,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,32,Gravity.CENTER,16,0,16,8));
        receiver.setDelegate((loaded,set,thumb,cache)->{if(set&&!thumb){status.setText(getString(R.string.NyxSendPreviewHint));loaded.startAnimation();}});
        if(document!=null){
            TLRPC.PhotoSize thumb=FileLoader.getClosestPhotoSizeWithSize(document.thumbs,90);
            ImageLocation location=ImageLocation.getForDocument(document);
            if(gif(media)){
                location.imageType=FileLoader.IMAGE_TYPE_ANIMATION;
                TLRPC.VideoSize video=MessageObject.getDocumentVideoThumb(document);
                if(video!=null)receiver.setImage(location,null,ImageLocation.getForDocument(video,document),null,ImageLocation.getForDocument(thumb,document),"90_90_b",null,document.size,null,parent,0);
                else receiver.setImage(location,null,ImageLocation.getForDocument(thumb,document),"90_90_b",document.size,null,parent,0);
            }else if(MessageObject.isVideoStickerDocument(document))receiver.setImage(location,null,ImageLocation.getForDocument(thumb,document),null,null,0,"webp",parent,1);
            else receiver.setImage(location,null,ImageLocation.getForDocument(thumb,document),null,"webp",parent,1);
        }else{
            ImageLocation content=ImageLocation.getForWebFile(WebFile.createWithWebDocument(result.content));
            content.imageType=FileLoader.IMAGE_TYPE_ANIMATION;
            ImageLocation thumb=result.thumb==null?null:ImageLocation.getForWebFile(WebFile.createWithWebDocument(result.thumb));
            receiver.setImage(content,null,thumb,"90_90_b",result.content.size,null,parent,1);
        }
        AtomicBoolean sent=new AtomicBoolean();
        AlertDialog dialog=new AlertDialog.Builder(fragment.getParentActivity(),fragment.getResourceProvider())
            .setTitle(getString(gif(media)?R.string.NyxGifPreview:R.string.NyxStickerPreview)).setView(box)
            .setPositiveButton(getString(R.string.NyxSend),(d,w)->{
                if(!sent.compareAndSet(false,true) || ApplicationLoader.mainInterfacePaused || fragment.getParentActivity()==null || LaunchActivity.getLastFragmentIncludeMainTabs()!=fragment || fragment.getCurrentAccount()!=account || owner!=UserConfig.getInstance(account).getClientUserId())return;
                if(fragment instanceof ChatActivity && ((ChatActivity)fragment).getDialogId()!=peer)return;
                withApproval(account,peer,media,send);
            }).setNegativeButton(getString(R.string.NyxCancel),null).create();
        if(fragment.showDialog(dialog,d->{receiver.stopAnimation();receiver.clearImage();})==null){receiver.stopAnimation();receiver.clearImage();}
    }
}
