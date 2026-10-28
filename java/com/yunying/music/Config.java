package com.yunying.music;

import android.content.Context;
import android.content.SharedPreferences;

/** 配置与登录态持久化（SharedPreferences，单文件）。 */
public class Config {

    /* 群晖 NAS */
    public static String nasHost = "192.168.2.77";
    public static int    nasPort = 5000;
    public static boolean nasHttps = false;
    public static String nasUser = "";
    public static String nasPass = "";
    public static String nasSid = "";
    public static String nasToken = "";
    public static String nasName = "";

    public static boolean lightTheme=true;

    /* 在线元数据 */
    public static boolean autoLyrics = true;
    public static boolean autoCover = true;
    public static int lyricOffset = 0;
    public static String audioEffect = "off";
    public static boolean basicService=false,recommendations=true;

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("yy_music", Context.MODE_PRIVATE);
    }

    public static void load(Context c) {
        SharedPreferences p = sp(c);
        SharedPreferences.Editor cleanup=p.edit();
        for(String key:p.getAll().keySet())if(key.startsWith("ali"))cleanup.remove(key);
        cleanup.apply();
        lightTheme=p.getBoolean("lightTheme",true);
        nasHost    = p.getString("nasHost", nasHost);
        nasPort    = p.getInt("nasPort", nasPort);
        nasHttps   = p.getBoolean("nasHttps", nasHttps);
        nasUser    = p.getString("nasUser", "");
        nasPass    = p.getString("nasPass", "");
        nasSid     = p.getString("nasSid", "");
        nasToken   = p.getString("nasToken", "");
        nasName    = p.getString("nasName", "");
        autoLyrics = p.getBoolean("autoLyrics", true);
        autoCover  = p.getBoolean("autoCover", true);
        lyricOffset = p.getInt("lyricOffset", 0);
        audioEffect = p.getString("audioEffect", "off");
        basicService=p.getBoolean("basicService",false);recommendations=p.getBoolean("recommendations",true);
    }

    public static void save(Context c) {
        sp(c).edit().putBoolean("lightTheme",lightTheme)
                .putBoolean("basicService",basicService).putBoolean("recommendations",recommendations).putString("audioEffect",audioEffect)
                .putString("nasHost", nasHost).putInt("nasPort", nasPort)
                .putBoolean("nasHttps", nasHttps)
                .putString("nasUser", nasUser).putString("nasPass", nasPass)
                .putString("nasSid", nasSid).putString("nasToken", nasToken)
                .putString("nasName", nasName)
                .putBoolean("autoLyrics", autoLyrics).putBoolean("autoCover", autoCover).putInt("lyricOffset", lyricOffset)
                .apply();
    }

    public static boolean nasReady() {
        return nasHost != null && nasHost.trim().length() > 0
                && nasUser != null && nasUser.trim().length() > 0;
    }

}
