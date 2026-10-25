package com.yunying.music;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.ScrollView;

/** 独立裁剪滚动视口，焦点缩放、阴影和滚动内容均不能绘制到标题栏。 */
final class ViewportScrollView extends ScrollView {
    ViewportScrollView(Context context) {
        super(context);
        setClipChildren(false);setClipToPadding(false);setOverScrollMode(OVER_SCROLL_NEVER);
        setVerticalScrollBarEnabled(false);setSmoothScrollingEnabled(true);
    }
    @Override public void requestChildFocus(android.view.View child,android.view.View focused){
        super.requestChildFocus(child,focused);
        if(focused!=null)post(()->{if(focused.hasFocus()){int inset=Ui.dp(getContext(),8);android.graphics.Rect rect=new android.graphics.Rect(-inset,-inset,focused.getWidth()+inset,focused.getHeight()+inset);focused.requestRectangleOnScreen(rect,false);}});
    }
    @Override public void draw(Canvas canvas) {
        int save=canvas.save();canvas.clipRect(getScrollX(),getScrollY(),getScrollX()+getWidth(),getScrollY()+getHeight());
        super.draw(canvas);canvas.restoreToCount(save);
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        int save=canvas.save();canvas.clipRect(getScrollX(),getScrollY(),getScrollX()+getWidth(),getScrollY()+getHeight());
        super.dispatchDraw(canvas);canvas.restoreToCount(save);
    }
}
