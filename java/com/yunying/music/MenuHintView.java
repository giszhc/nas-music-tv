package com.yunying.music;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.TextView;
/** 菜单键提示：三条横线与标签作为一组居中，跟随主题与焦点文字颜色。 */
final class MenuHintView extends TextView {
    private final Paint iconPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    MenuHintView(Context context){super(context);setIncludeFontPadding(false);}
    @Override protected void onDraw(Canvas canvas){
        float size=Ui.dp(getContext(),15),gap=Ui.dp(getContext(),8);
        float textWidth=getPaint().measureText(getText().toString());
        float start=(getWidth()-textWidth-size-gap)/2;
        iconPaint.setColor(getCurrentTextColor());iconPaint.setStrokeWidth(Ui.dp(getContext(),1.5f));iconPaint.setStrokeCap(Paint.Cap.ROUND);
        float cy=getHeight()/2f;
        for(int i=-1;i<=1;i++)canvas.drawLine(start,cy+i*size*.3f,start+size,cy+i*size*.3f,iconPaint);
        int save=canvas.save();canvas.translate((size+gap)/2,0);super.onDraw(canvas);canvas.restoreToCount(save);
    }
}
