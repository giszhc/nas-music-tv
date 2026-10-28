package com.yunying.music;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** 静态漫射光场；无需逐帧模糊，兼容旧电视。 */
final class ImmersiveBackground extends Drawable {
 private final Paint paint=new Paint(3);
 private Shader base,left,right;
 @Override protected void onBoundsChange(Rect b){float w=b.width(),h=b.height();base=new LinearGradient(0,0,w,h,Ui.light?new int[]{0xFFDFEAF5,0xFFF2F5FA,0xFFE9E5F3}:new int[]{0xFF111B2A,0xFF11151F,0xFF171C29},null,Shader.TileMode.CLAMP);left=new RadialGradient(w*.12f,h*.15f,w*.65f,new int[]{0x55427397,0x00244466},null,Shader.TileMode.CLAMP);right=new RadialGradient(w*.92f,h*.8f,w*.6f,new int[]{0x454F486F,0x002C2541},null,Shader.TileMode.CLAMP);}
 @Override public void draw(Canvas c){paint.setShader(base);c.drawRect(getBounds(),paint);paint.setShader(left);c.drawRect(getBounds(),paint);paint.setShader(right);c.drawRect(getBounds(),paint);paint.setShader(null);}
 public void setAlpha(int a){paint.setAlpha(a);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.OPAQUE;}
}
