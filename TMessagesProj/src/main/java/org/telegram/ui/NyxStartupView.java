package org.telegram.ui;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.messenger.*;
import org.telegram.ui.Components.LayoutHelper;
/** Brief, skippable visual overlay; never waits for network or blocks initialization. */
public final class NyxStartupView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private Bitmap background,logo;private ValueAnimator animator;private float progress;private final boolean decor;
    private NyxStartupView(Context c,int account){super(c);decor=NyxConfig.get("localPremium",account,false);background=BitmapFactory.decodeResource(getResources(),R.drawable.nyx_startup_background);logo=BitmapFactory.decodeResource(getResources(),R.drawable.nyx_preview_original);setContentDescription("Nyxgram. "+LocaleController.getString(R.string.NyxStartupCaption));setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(true);}
    public static void attach(Activity activity,FrameLayout parent,int account){
        if(!NyxConfig.prefs().getBoolean("nyxStartup",true)||!SharedConfig.animationsEnabled()||(Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled())||SharedConfig.appLocked)return;
        NyxStartupView view=new NyxStartupView(activity,account);parent.addView(view,LayoutHelper.createFrame(-1,-1));view.animator=ValueAnimator.ofFloat(0,1);view.animator.setDuration(680);view.animator.addUpdateListener(a->{view.progress=(float)a.getAnimatedValue();view.invalidate();if(view.progress>=1)view.dismiss();});view.animator.start();view.postDelayed(view::dismiss,900);
    }
    private void dismiss(){if(animator!=null)animator.cancel();if(getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)getParent()).removeView(this);}
    @Override public boolean onTouchEvent(MotionEvent event){if(event.getAction()==MotionEvent.ACTION_UP)dismiss();return true;}
    private float dp(float x){return AndroidUtilities.dpf2(x);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float w=getWidth(),h=getHeight(),cx=w/2,cy=h*.38f;float exit=progress>.8f?(1-progress)/.2f:1;setAlpha(exit);
        c.drawColor(0xFF080D1A);if(background!=null){float k=Math.max(w/background.getWidth(),h/background.getHeight());float bw=background.getWidth()*k,bh=background.getHeight()*k;c.drawBitmap(background,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);}
        float intro=Math.min(1,progress/.45f),radius=Math.min(dp(72),Math.min(w*.22f,h*.15f))*(0.88f+0.12f*intro);paint.setShader(new RadialGradient(cx,cy,radius*1.9f,new int[]{decor?0x559C74EB:0x449C74EB,0x0070D9E8},null,Shader.TileMode.CLAMP));c.drawCircle(cx,cy,radius*1.9f,paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(0x9970D9E8);RectF orbit=new RectF(cx-radius-dp(14),cy-radius-dp(14),cx+radius+dp(14),cy+radius+dp(14));c.drawArc(orbit,progress*35-40,225,false,paint);paint.setStyle(Paint.Style.FILL);
        if(logo!=null){Path clip=new Path();clip.addCircle(cx,cy,radius,Path.Direction.CW);c.save();c.clipPath(clip);c.drawBitmap(logo,null,new RectF(cx-radius,cy-radius,cx+radius,cy+radius),paint);c.restore();}
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(dp(27));paint.setColor(0xFFF1F3FA);String title="NYXGRAM";float gap=dp(4),total=paint.measureText(title)+gap*(title.length()-1),x=cx-total/2;for(int i=0;i<title.length();i++){String letter=title.substring(i,i+1);c.drawText(letter,x,cy+radius+Math.min(dp(62),h*.12f),paint);x+=paint.measureText(letter)+gap;}
        paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextSize(dp(14));paint.setColor(0xFFB7BED2);String caption=LocaleController.getString(R.string.NyxStartupCaption);c.drawText(caption,cx-paint.measureText(caption)/2,cy+radius+Math.min(dp(62),h*.12f)+dp(29),paint);
    }
    @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();if(animator!=null)animator.cancel();if(background!=null){background.recycle();background=null;}if(logo!=null){logo.recycle();logo=null;}}
}
