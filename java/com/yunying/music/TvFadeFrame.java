package com.yunying.music;

import android.content.Context;
import android.graphics.*;
import android.widget.FrameLayout;

/** 参考图列表底部渐隐；遮罩只作用于独立滚动区域。 */
final class TvFadeFrame extends FrameLayout {
    private final Paint paint=new Paint(3);
    private Shader fade;
    TvFadeFrame(Context context){super(context);setClipChildren(true);setClipToPadding(true);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){fade=new LinearGradient(0,Math.max(0,h-Ui.dp(getContext(),32)),0,h,new int[]{0x00FFFFFF,0xFFFFFFFF},null,Shader.TileMode.CLAMP);}
    @Override protected void dispatchDraw(Canvas canvas){
        int layer=canvas.saveLayer(0,0,getWidth(),getHeight(),null);super.dispatchDraw(canvas);
        paint.setShader(fade);paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));canvas.drawRect(0,Math.max(0,getHeight()-Ui.dp(getContext(),32)),getWidth(),getHeight(),paint);paint.setXfermode(null);paint.setShader(null);canvas.restoreToCount(layer);
    }
}
