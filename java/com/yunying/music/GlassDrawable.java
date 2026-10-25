package com.yunying.music;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** 电视卡片材质：深灰底、白色聚焦与双层边框。 */
final class GlassDrawable extends Drawable {
    private final Paint paint=new Paint(3);
    private final RectF bounds=new RectF(), surface=new RectF();
    private final float radius,stroke;
    private final boolean brightFocus;
    private final Context context;
    private Shader sheen,edge;
    private float focus,targetFocus;
    private int alpha=255;
    private ValueAnimator animator;
    private final Runnable frame=()->invalidateSelf();
    private final Matrix rotation=new Matrix();
    private boolean forced,outlineOnly;
    GlassDrawable outlineOnly(){outlineOnly=true;return this;}
    GlassDrawable(Context c,float radius,boolean brightFocus){this.context=c;this.radius=Ui.dp(c,radius);this.stroke=Math.max(1,Ui.dp(c,.7f));this.brightFocus=brightFocus;}
    GlassDrawable focused(){forced=true;focus=1;targetFocus=1;return this;}
    @Override public boolean isStateful(){return true;}
    @Override protected void onBoundsChange(Rect b){
        bounds.set(b);bounds.inset(Ui.dp(context,2),Ui.dp(context,2));
        sheen=new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{0x00FFFFFF,0x00FFFFFF,0x00FFFFFF,0x00FFFFFF},new float[]{0,.35f,.7f,1},Shader.TileMode.CLAMP);
        edge=new SweepGradient(bounds.centerX(),bounds.centerY(),new int[]{0x80FFFFFF,0xB3FFFFFF,0xFFFFFFFF,0x80FFFFFF},new float[]{0,.45f,.82f,1});
    }
    @Override protected boolean onStateChange(int[] states){
        boolean active=forced;for(int state:states)if(state==android.R.attr.state_focused||state==android.R.attr.state_pressed)active=true;
        return animateFocus(active?1:0,true);
    }
    void setFocus(boolean active,boolean animated){forced=active;animateFocus(active?1:0,animated);}
    private boolean animateFocus(float target,boolean animated){
        if(target==targetFocus&&animated)return false;targetFocus=target;if(animator!=null)animator.cancel();
        if(!animated||!Ui.motionEnabled(context)||getCallback()==null){focus=target;invalidateSelf();return true;}
        animator=ValueAnimator.ofFloat(focus,target);animator.setDuration(190);animator.setInterpolator(Ui.EASE);
        animator.addUpdateListener(a->{focus=(Float)a.getAnimatedValue();invalidateSelf();});animator.start();return true;
    }
    @Override public void draw(Canvas canvas){
        float amount=brightFocus?focus:0;
        surface.set(bounds);surface.inset(Ui.dp(context,3)*focus,Ui.dp(context,3)*focus);
        float surfaceRadius=Math.max(0,radius-Ui.dp(context,3)*focus);
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);
        if(!outlineOnly){
        paint.setColor(blend(brightFocus?0xFF58595B:0xF23A3B3D,0xFFF6F6F6,amount));paint.setAlpha((int)(Color.alpha(paint.getColor())*alpha/255f));canvas.drawRoundRect(surface,surfaceRadius,surfaceRadius,paint);
        paint.setShader(sheen);paint.setAlpha(alpha);canvas.drawRoundRect(surface,surfaceRadius,surfaceRadius,paint);
        }
        if(edge!=null){rotation.setRotate(Ui.motionEnabled(context)?(android.os.SystemClock.uptimeMillis()%2400)*360f/2400:0,bounds.centerX(),bounds.centerY());edge.setLocalMatrix(rotation);}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke+Ui.dp(context,2f)*focus);paint.setShader(edge);paint.setAlpha((int)(alpha*(outlineOnly?focus:(.05f+.95f*focus))));canvas.drawRoundRect(bounds,radius,radius,paint);
        if(focus>.01f && isVisible() && Ui.motionEnabled(context))scheduleSelf(frame,android.os.SystemClock.uptimeMillis()+16);
        else unscheduleSelf(frame);
        paint.setShader(null);paint.setAlpha(255);
    }
    private int blend(int a,int b,float t){return Color.argb((int)(Color.alpha(a)+(Color.alpha(b)-Color.alpha(a))*t),(int)(Color.red(a)+(Color.red(b)-Color.red(a))*t),(int)(Color.green(a)+(Color.green(b)-Color.green(a))*t),(int)(Color.blue(a)+(Color.blue(b)-Color.blue(a))*t));}
    @Override public void getOutline(Outline outline){outline.setRoundRect(getBounds(),radius);outline.setAlpha(.16f);}
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public void jumpToCurrentState(){if(animator!=null)animator.end();}
}
