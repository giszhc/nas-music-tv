package com.yunying.music;

import android.os.Bundle;

/** 启动后进入音乐首页；连接和目录管理统一收在设置中。 */
public class MainActivity extends MusicHomeActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state==null&&android.content.Intent.ACTION_MAIN.equals(getIntent().getAction())){
            final android.view.ViewGroup content=findViewById(android.R.id.content);
            final SplashView splash=new SplashView(this);content.addView(splash,new android.view.ViewGroup.LayoutParams(-1,-1));
            splash.postDelayed(()->{if(isFinishing()||isDestroyed())return;if(Ui.motionEnabled(this))splash.animate().alpha(0).setDuration(240).withEndAction(()->content.removeView(splash)).start();else content.removeView(splash);},1100);
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7);
        } catch (Throwable ignored) { }
    }
}
