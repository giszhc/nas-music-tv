package com.yunying.music;

/** 群晖 NAS 媒体条目。 */
public class Entry {

    public static final String SRC_NAS = "nas";

    public String name = "";
    public String path = "";     // NAS 绝对路径
    public String id = "";       // 保留旧版条目标识以兼容本地元数据键
    public boolean dir = false;
    public boolean audiobook = false;
    public long size = 0L;
    public String source = SRC_NAS;

    public Entry() { }

    public Entry(String name, boolean dir, String source) {
        this.name = name;
        this.dir = dir;
        this.source = source;
    }

    private static final String[] AUDIO_EXT = {
            "mp3", "flac", "m4a", "aac", "wav", "ogg", "oga", "opus", "ape", "aiff", "aif", "mp4"
    };

    public boolean isAudio() {
        if (dir) return false;
        String n = name.toLowerCase();
        int dot = n.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = n.substring(dot + 1);
        for (String a : AUDIO_EXT) if (a.equals(ext)) return true;
        return false;
    }

    public String sizeText() {
        if (dir) return "文件夹";
        if (size <= 0) return "";
        double k = size / 1024.0;
        if (k < 1024) return String.format("%.0f KB", k);
        double m = k / 1024.0;
        if (m < 1024) return String.format("%.1f MB", m);
        return String.format("%.2f GB", m / 1024.0);
    }

    public String title() {
        String n = name;
        int dot = n.lastIndexOf('.');
        if (!dir && dot > 0) n = n.substring(0, dot);
        return n;
    }
}
