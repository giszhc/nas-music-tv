package com.yunying.music;

import android.os.Bundle;

/** 启动后进入音乐首页；连接和目录管理统一收在设置中。 */
public class MainActivity extends MusicHomeActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7);
        } catch (Throwable ignored) { }
    }
}
