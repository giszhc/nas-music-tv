package com.yunying.music;

import android.content.Context;
import android.content.SharedPreferences;

/** 配置与登录态持久化（SharedPreferences，单文件）。 */
public class Config {

    public static final String MODE_NONE = "none";
    public static final String MODE_OPEN = "open";   // 开放平台 client_id + refresh_token
    public static final String MODE_WEB = "web";     // 网页扫码登录态

    /* 群晖 NAS */
    public static String nasHost = "192.168.2.77";
    public static int    nasPort = 5000;
    public static boolean nasHttps = false;
    public static String nasUser = "";
    public static String nasPass = "";
    public static String nasSid = "";
    public static String nasToken = "";
    public static String nasName = "";

    /* 阿里云盘 */
    public static String aliHost = "https://openapi.alipan.com";
    public static String aliMode = MODE_NONE;
    public static String aliClientId = "";
    public static String aliSecret = "";
    public static String aliRefresh = "";
    public static String aliAccess = "";
    public static long   aliExpire = 0L;
    public static String aliDriveId = "";
    public static String aliDriveName = "";
    public static String aliAvatar="";

    /* 在线元数据 */
    public static boolean autoLyrics = true;
    public static boolean autoCover = true;
    public static int lyricOffset = 0;
    public static String audioEffect = "off";
    public static boolean basicService=false,recommendations=true;
    public static String aliManualRefresh = "";
    public static String aliUserId = "", aliDeviceId = "", aliPrivateKey = "";
    private static Context application;
    private static final java.util.concurrent.atomic.AtomicLong AUTH_EPOCH=new java.util.concurrent.atomic.AtomicLong();
    static long authVersion(){return AUTH_EPOCH.get();}
    static void invalidateAuth(){AUTH_EPOCH.incrementAndGet();}
    public static void persist(){if(application!=null)save(application);}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("yy_music", Context.MODE_PRIVATE);
    }

    public static void load(Context c) {
        application=c.getApplicationContext();
        SharedPreferences p = sp(c);
        nasHost    = p.getString("nasHost", nasHost);
        nasPort    = p.getInt("nasPort", nasPort);
        nasHttps   = p.getBoolean("nasHttps", nasHttps);
        nasUser    = p.getString("nasUser", "");
        nasPass    = p.getString("nasPass", "");
        nasSid     = p.getString("nasSid", "");
        nasToken   = p.getString("nasToken", "");
        nasName    = p.getString("nasName", "");
        aliHost    = p.getString("aliHost", aliHost);
        aliMode    = p.getString("aliMode", MODE_NONE);
        aliClientId= p.getString("aliClientId", "");
        aliSecret  = p.getString("aliSecret", "");
        aliRefresh = p.getString("aliRefresh", "");
        aliAccess  = p.getString("aliAccess", "");
        aliExpire  = p.getLong("aliExpire", 0L);
        aliDriveId = p.getString("aliDriveId", "");
        aliDriveName = p.getString("aliDriveName", "");
        aliAvatar=p.getString("aliAvatar", "");
        autoLyrics = p.getBoolean("autoLyrics", true);
        autoCover  = p.getBoolean("autoCover", true);
        lyricOffset = p.getInt("lyricOffset", 0);
        audioEffect = p.getString("audioEffect", "off");
        basicService=p.getBoolean("basicService",false);recommendations=p.getBoolean("recommendations",true);
        aliManualRefresh = p.getString("aliManualRefresh",MODE_OPEN.equals(aliMode)?aliRefresh:"");
        aliUserId = p.getString("aliUserId", "");aliDeviceId=p.getString("aliDeviceId", "");aliPrivateKey=p.getString("aliPrivateKey", "");
    }

    public static void save(Context c) {
        sp(c).edit()
                .putBoolean("basicService",basicService).putBoolean("recommendations",recommendations).putString("audioEffect",audioEffect).putString("aliManualRefresh",aliManualRefresh)
                .putString("aliUserId",aliUserId).putString("aliDeviceId",aliDeviceId).putString("aliPrivateKey",aliPrivateKey)
                .putString("nasHost", nasHost).putInt("nasPort", nasPort)
                .putBoolean("nasHttps", nasHttps)
                .putString("nasUser", nasUser).putString("nasPass", nasPass)
                .putString("nasSid", nasSid).putString("nasToken", nasToken)
                .putString("nasName", nasName)
                .putString("aliHost", aliHost).putString("aliMode", aliMode)
                .putString("aliClientId", aliClientId).putString("aliSecret", aliSecret)
                .putString("aliRefresh", aliRefresh).putString("aliAccess", aliAccess)
                .putLong("aliExpire", aliExpire)
                .putString("aliDriveId", aliDriveId).putString("aliDriveName", aliDriveName).putString("aliAvatar",aliAvatar)
                .putBoolean("autoLyrics", autoLyrics).putBoolean("autoCover", autoCover).putInt("lyricOffset", lyricOffset)
                .apply();
    }

    public static boolean nasReady() {
        return nasHost != null && nasHost.trim().length() > 0
                && nasUser != null && nasUser.trim().length() > 0;
    }

    public static boolean aliReady() {
        if (MODE_WEB.equals(aliMode)) {
            return (aliRefresh != null && aliRefresh.length() > 0)
                    || (aliAccess != null && aliAccess.length() > 0);
        }
        if (MODE_OPEN.equals(aliMode)) {
            return (aliRefresh != null && aliRefresh.length() > 0)
                    && (aliClientId != null && aliClientId.length() > 0);
        }
        return false;
    }

    public static String aliModeText() {
        if (MODE_WEB.equals(aliMode)) return "扫码登录";
        if (MODE_OPEN.equals(aliMode)) return "开放平台 token";
        return "未登录";
    }
}
