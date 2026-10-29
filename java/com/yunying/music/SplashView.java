package com.yunying.music;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** 白色底与品牌蓝紫色漫射融合，文字独立绘制以保证清晰。 */
final class SplashView extends View {
 private final Paint paint=new Paint(3);private final Bitmap logo;
 SplashView(Context c){super(c);logo=BitmapFactory.decodeResource(c.getResources(),R.drawable.brand_logo);setContentDescription("聆境音乐，让喜欢的音乐，陪伴每一刻");}
 protected void onDraw(Canvas canvas){float w=getWidth(),h=getHeight(),scale=Math.min(w/960f,h/540f),cx=w/2,cy=h*.40f;
 canvas.drawColor(0xFFF8FBFF);paint.setShader(new RadialGradient(cx,cy,Math.max(w,h)*.53f,new int[]{0x75C0EAFB,0x30D9CFF5,0x00FFFFFF},new float[]{0,.5f,1},Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
 float size=220*scale;canvas.drawBitmap(logo,null,new RectF(cx-size/2,cy-size/2,cx+size/2,cy+size/2),paint);
 paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextSize(27*scale);paint.setColor(0xFF203D5A);canvas.drawText("聆境音乐",cx,cy+143*scale,paint);paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(15*scale);paint.setColor(0xFF526E87);canvas.drawText("让喜欢的音乐，陪伴每一刻",cx,cy+177*scale,paint);
 }
}
