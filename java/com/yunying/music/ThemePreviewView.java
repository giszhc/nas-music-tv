package com.yunying.music;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** 独立配色的主题缩略预览，不修改运行中的全局主题。 */
final class ThemePreviewView extends View {
 private final boolean light;private final Paint p=new Paint(3);
 ThemePreviewView(Context c,boolean light){super(c);this.light=light;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 protected void onDraw(Canvas c){
  float w=getWidth(),h=getHeight();int save=c.save();Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),Ui.dp(getContext(),12),Ui.dp(getContext(),12),Path.Direction.CW);c.clipPath(clip);
  p.setShader(new LinearGradient(0,0,w,h,light?new int[]{0xFFB9D4E5,0xFFF0E9F5}:new int[]{0xFF19364B,0xFF221D38},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
  float unit=Math.min(w/300,h/210);c.translate((w-300*unit)/2,(h-210*unit)/2);c.scale(unit,unit);float bottom=210;
  p.setColor(light?0xFF21334B:0xFFF3F6FC);p.setTextSize(16);p.setTypeface(Typeface.DEFAULT_BOLD);c.drawText("音乐",18,31,p);p.setTypeface(Typeface.DEFAULT);
  for(int i=0;i<3;i++){float y=51+i*42;p.setColor(light?0xB8FFFFFF:0xAA35475F);c.drawRoundRect(16,y,170,y+33,7,7,p);p.setColor(light?0xFF53677D:0xFFCEDBED);p.setStrokeWidth(3);c.drawLine(28,y+12,106-i*10,y+12,p);p.setAlpha(160);p.setStrokeWidth(2);c.drawLine(28,y+23,132,y+23,p);p.setAlpha(255);}
  p.setShader(new LinearGradient(185,56,276,145,new int[]{0xFF638FA8,0xFFC2ACC9},null,Shader.TileMode.CLAMP));c.drawRoundRect(185,56,280,151,10,10,p);p.setShader(null);
  p.setColor(light?0xFF334F6C:0xFFE4ECF8);p.setStrokeWidth(3);c.drawLine(20,bottom-18,280,bottom-18,p);p.setColor(light?0xFF3267A2:0xFF91C8EE);c.drawCircle(116,bottom-18,4,p);c.restoreToCount(save);
 }
}
