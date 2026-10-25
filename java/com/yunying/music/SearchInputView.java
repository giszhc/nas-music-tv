package com.yunying.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

/** 遥控器搜索输入框：输入框或其屏幕键盘获焦时显示闪烁光标。 */
final class SearchInputView extends TextView {
    private final Paint cursor=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean blinking,lit;
    private final Runnable blink=new Runnable(){public void run(){
        if(!blinking)return;
        lit=!lit;invalidate();postDelayed(this,500);
    }};
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener=(oldFocus,newFocus)->updateCursor();
    SearchInputView(Context context){super(context);}
    private void updateCursor(){
        boolean active=isShown()&&hasWindowFocus()&&(hasFocus()||(getParent() instanceof View&&((View)getParent()).hasFocus()));
        removeCallbacks(blink);blinking=active;lit=active;invalidate();
        if(active)postDelayed(blink,500);
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);updateCursor();}
    @Override protected void onDetachedFromWindow(){getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);blinking=false;removeCallbacks(blink);super.onDetachedFromWindow();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);updateCursor();}
    @Override protected void onVisibilityChanged(View changed,int visibility){super.onVisibilityChanged(changed,visibility);if(cursor!=null)updateCursor();}
    @Override protected void onTextChanged(CharSequence text,int start,int before,int count){super.onTextChanged(text,start,before,count);if(cursor!=null)updateCursor();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(!blinking||!lit)return;
        float x=getPaddingLeft();
        if(length()>0&&getLayout()!=null)x+=getLayout().getLineRight(0)-getScrollX();
        x=Math.min(x,getWidth()-getPaddingRight()-2);
        float height=getTextSize()*1.15f,top=(getHeight()-height)/2f;
        cursor.setColor(Ui.TEXT);cursor.setStrokeWidth(Math.max(1,Ui.dp(getContext(),1)));
        canvas.drawLine(x,top,x,top+height,cursor);
    }
}
