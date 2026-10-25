package com.yunying.music.test;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.*;
import android.graphics.Bitmap;
import android.view.KeyEvent;
/** 对相邻列表项注入真实下键，测量中间帧中白色卡面的宽度。 */
public final class FocusMotionProbe extends Instrumentation {
    private Bundle args;
    public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    public void onStart(){Bundle result=new Bundle();try{
        UiAutomation a=getUiAutomation();int x1=Integer.parseInt(args.getString("x1")),x2=Integer.parseInt(args.getString("x2")),y=Integer.parseInt(args.getString("y"));
        int baseline=width(a.takeScreenshot(),x1,x2,y);long now=SystemClock.uptimeMillis();KeyEvent down=new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN,0),up=new KeyEvent(now,now+1,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_DOWN,0);down.setSource(android.view.InputDevice.SOURCE_DPAD);up.setSource(android.view.InputDevice.SOURCE_DPAD);if(!a.injectInputEvent(down,false)||!a.injectInputEvent(up,false))throw new AssertionError("Key injection failed");
        SystemClock.sleep(12);int first=width(a.takeScreenshot(),x1,x2,y);SystemClock.sleep(45);int middle=width(a.takeScreenshot(),x1,x2,y);SystemClock.sleep(230);int last=width(a.takeScreenshot(),x1,x2,y);
        if(last<400||baseline>=last)throw new AssertionError("Card did not grow: "+baseline+","+first+","+middle+","+last);
        result.putString("focusMotion","PASS: growing card widths "+baseline+","+first+","+middle+","+last);finish(0,result);
    }catch(Throwable error){result.putString("error",error.toString());finish(1,result);}}
    private int width(Bitmap b,int left,int right,int y){if(b==null)throw new AssertionError("No screenshot");int run=0,max=0;for(int x=left;x<right;x++){int p=b.getPixel(x,y);if(((p>>16)&255)>70&&((p>>8)&255)>70&&(p&255)>70)run++;else run=0;max=Math.max(max,run);}b.recycle();return max;}
}
