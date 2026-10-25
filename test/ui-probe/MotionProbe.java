package com.yunying.music.test;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.*;
import android.graphics.Bitmap;
import android.view.MotionEvent;
/** 从真实合成屏幕取三帧，确认模块切换不是直接跳帧。 */
public final class MotionProbe extends Instrumentation {
    private Bundle args;
    public void onCreate(Bundle values){super.onCreate(values);args=values;start();}
    public void onStart(){Bundle result=new Bundle();try{
        UiAutomation automation=getUiAutomation();float x=Float.parseFloat(args.getString("x")),y=Float.parseFloat(args.getString("y"));long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0),up=MotionEvent.obtain(now,now+30,MotionEvent.ACTION_UP,x,y,0);down.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);automation.injectInputEvent(down,true);automation.injectInputEvent(up,true);down.recycle();up.recycle();
        SystemClock.sleep(30);Bitmap a=automation.takeScreenshot();SystemClock.sleep(90);Bitmap b=automation.takeScreenshot();SystemClock.sleep(300);Bitmap c=automation.takeScreenshot();int first=difference(a,b),second=difference(b,c);a.recycle();b.recycle();c.recycle();
        if(first<20||second<20)throw new AssertionError("No intermediate rendered frames: "+first+","+second);
        result.putString("motion","PASS: rendered intermediate frames; changed samples "+first+","+second);finish(0,result);
    }catch(Throwable error){result.putString("error",error.toString());finish(1,result);}}
    private int difference(Bitmap a,Bitmap b){if(a==null||b==null)throw new AssertionError("No compositor screenshot");int count=0;for(int y=0;y<Math.min(a.getHeight(),b.getHeight());y+=6)for(int x=0;x<Math.min(a.getWidth(),b.getWidth());x+=6)if(a.getPixel(x,y)!=b.getPixel(x,y))count++;return count;}
}
