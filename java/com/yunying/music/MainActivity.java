package com.yunying.music;

import android.os.Bundle;

/** 首次启动展示品牌页，首页已在下层完成布局。 */
public class MainActivity extends MusicHomeActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state==null&&android.content.Intent.ACTION_MAIN.equals(getIntent().getAction())){
            final android.view.ViewGroup content=findViewById(android.R.id.content);
            final SplashView splash=new SplashView(this);splash.setClickable(true);
            content.addView(splash,new android.view.ViewGroup.LayoutParams(-1,-1));
            splash.postDelayed(()->{
                if(isFinishing()||isDestroyed())return;
                Runnable finish=()->{content.removeView(splash);requestNotificationPermission();};
                if(Ui.motionEnabled(this))splash.animate().alpha(0).scaleX(1.015f).scaleY(1.015f).setDuration(400).setInterpolator(Ui.EASE).withEndAction(finish).start();
                else finish.run();
            },3000);
        }else requestNotificationPermission();
    }
    private void requestNotificationPermission(){
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7);
        } catch (Throwable ignored) { }
    }
}
