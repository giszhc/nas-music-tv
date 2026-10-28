package com.yunying.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;

/** 参考图的深灰页面底色。 */
final class AmbientView extends View {
    AmbientView(Context c){super(c);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas canvas){ImmersiveBackground background=new ImmersiveBackground();background.setBounds(0,0,getWidth(),getHeight());background.draw(canvas);}
}
