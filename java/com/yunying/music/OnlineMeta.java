package com.yunying.music;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * 联网抓取歌词与封面（纯 Java，无 Android 依赖，可桌面单测）。
 * 歌词：酷狗歌词库优先，网易云兜底；封面：网易云专辑图（500×500）。
 * 结果按「歌名|歌手」哈希缓存在本地目录，避免重复请求。
 */
public class OnlineMeta {

    private static final String NETEAST_SEARCH = "https://music.163.com/api/cloudsearch/pc";
    private static final String NETEAST_LYRIC = "https://music.163.com/api/song/lyric";
    private static final String KUGOU_SEARCH = "https://lyrics.kugou.com/search";
    private static final String KUGOU_DOWNLOAD = "https://lyrics.kugou.com/download";

    public static class Meta {
        public String title = "";
        public String artist = "";
        public String album = "";
        public String lrc = "";
        public String lyricSource = "";
        public byte[] cover = null;
        public String error = "";

        public boolean hasLrc() {
            return lrc != null && lrc.trim().length() > 0;
        }
    }

    /** 主入口：返回歌词 + 封面（任一失败不影响另一个） */
    public static Meta fetch(String cacheDir, String rawName, int durationMs) {
        return fetch(cacheDir, rawName, durationMs, false);
    }

    /** 手动重新匹配跳过旧缓存，失败时保留磁盘上的已有资源。 */
    public static Meta fetch(String cacheDir, String rawName, int durationMs, boolean refresh) {
        Meta m = new Meta();
        String[] ta = splitName(rawName);
        m.title = ta[0];
        m.artist = ta[1];
        String query = cleanForQuery(rawName);
        if (query.length() == 0) query = m.title;
        if (query.trim().length() == 0) return m;

        String key = md5(query + "|" + (durationMs / 1000));
        File lrcCache = cacheDir == null ? null : new File(cacheDir, key + ".lrc");
        File imgCache = cacheDir == null ? null : new File(cacheDir, key + ".jpg");

        if (!refresh && lrcCache != null && lrcCache.exists()) {
            m.lrc = readText(lrcCache);
            m.lyricSource = "cache";
        }
        if (!refresh && imgCache != null && imgCache.exists()) {
            m.cover = readBytes(imgCache);
        }
        File infoCache = cacheDir == null ? null : new File(cacheDir, key + ".json");
        if (!refresh && infoCache != null && infoCache.exists()) try {
            JSONObject info = new JSONObject(readText(infoCache));
            m.title = info.optString("title", m.title); m.artist = info.optString("artist", m.artist); m.album = info.optString("album", "");
        } catch (Exception ignored) { }
        if (m.hasLrc() && m.cover != null && infoCache != null && infoCache.exists()) return m;

        NetEaseSong hit = null;
        try {
            hit = searchNetease(query, m.title, m.artist, durationMs);
        } catch (Exception e) {
            m.error = "网易云搜索失败：" + msg(e);
        }

        if (hit != null) {
            m.title = hit.name; m.artist = hit.artist; m.album = hit.album;
            if (infoCache != null) try {
                JSONObject info = new JSONObject(); info.put("title", m.title); info.put("artist", m.artist); info.put("album", m.album); writeText(infoCache, info.toString());
            } catch (Exception ignored) { }
        }

        if (!m.hasLrc()) {
            try {
                String lrc = kugouLyric(query, durationMs);
                if (lrc != null && lrc.trim().length() > 0) {
                    m.lrc = lrc;
                    m.lyricSource = "kugou";
                    if (lrcCache != null) writeText(lrcCache, lrc);
                }
            } catch (Exception e) {
                if (m.error.length() == 0) m.error = "酷狗歌词失败：" + msg(e);
            }
        }
        if (!m.hasLrc() && hit != null) {
            try {
                String lrc = neteaseLyric(hit.id);
                if (lrc != null && lrc.trim().length() > 0) {
                    m.lrc = lrc;
                    m.lyricSource = "netease";
                    if (lrcCache != null) writeText(lrcCache, lrc);
                }
            } catch (Exception e) {
                if (m.error.length() == 0) m.error = "网易云歌词失败：" + msg(e);
            }
        }
        if (m.cover == null && hit != null && hit.picUrl.length() > 0) {
            String p = hit.picUrl;
            String https = p.replaceFirst("^http://", "https://");
            String[] tries = {p + "?param=500y500", https + "?param=500y500", https, p};
            for (String url : tries) {
                try {
                    Map<String, String> h = new HashMap<>();
                    h.put("Referer", "https://music.163.com/");
                    h.put("Accept", "image/*");
                    byte[] img = Http.bytes(url, h);
                    if (img != null && img.length > 1024) {
                        m.cover = img;
                        if (imgCache != null) writeBytes(imgCache, img);
                        m.error = "";
                        break;
                    }
                } catch (Exception e) {
                    if (m.error.length() == 0) m.error = "封面下载失败：" + msg(e);
                }
            }
        }
        return m;
    }

    /** 歌手头像按准确姓名查询，失败返回空，避免用其他歌手图片填充。 */
    public static byte[] artistPicture(String cacheDir, String name) {
        if (name == null || name.isEmpty() || "未知歌手".equals(name)) return null;
        File directory = new File(cacheDir); directory.mkdirs();
        File cached = new File(directory,"artist-"+md5(name)+".jpg");
        if (cached.exists()) return readBytes(cached);
        try {
            Map<String,String> headers = Http.headers("Referer","https://music.163.com/");
            JSONObject result = new JSONObject(Http.postForm(NETEAST_SEARCH,"s="+Http.enc(name)+"&type=100&limit=5",headers)).optJSONObject("result");
            JSONArray artists = result == null ? null : result.optJSONArray("artists");
            if (artists == null) return null;
            for (int i=0;i<artists.length();i++) {
                JSONObject artist=artists.getJSONObject(i);
                if (!name.equalsIgnoreCase(artist.optString("name"))) continue;
                String url=artist.optString("img1v1Url",artist.optString("picUrl"));
                if (!url.startsWith("http")) return null;
                byte[] image=Http.bytes(url.replaceFirst("^http://","https://")+"?param=300y300",headers);
                if (image!=null && image.length>1024) {writeBytes(cached,image);return image;}
            }
        } catch(Exception ignored) { }
        return null;
    }

    /* ----------------------------- 文件名解析 ----------------------------- */

    /** "Artist - Title" / "Title - Artist" / "01. Title" → {title, artist} */
    public static String[] splitName(String raw) {
        String s = raw == null ? "" : raw.trim();
        int dot = s.lastIndexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        s = s.replaceAll("\\[[^\\]]*\\]", " ")      // [320K] [Hi-Res]
                .replaceAll("【[^】]*】", " ")
                .replaceAll("\\([^)]*(live|remix|伴奏|翻自)[^)]*\\)", " ")
                .replaceAll("\\s+", " ").trim();
        s = s.replaceFirst("^\\d{1,3}[\\s.\\-_、]+", "");  // 去掉序号
        String title = s, artist = "";
        String[] sep = {" - ", " – ", " — ", "_", "-"};
        for (String p : sep) {
            int i = s.indexOf(p);
            if (i > 0 && i < s.length() - p.length()) {
                String a = s.substring(0, i).trim();
                String b = s.substring(i + p.length()).trim();
                if (a.length() == 0 || b.length() == 0) continue;
                // 音乐库里「歌手 - 歌名」最常见：A 当歌手、B 当歌名
                artist = a;
                title = b;
                break;
            }
        }
        return new String[]{title.trim(), artist.trim()};
    }

    /** 搜索用关键词：去掉扩展名/序号/括号备注，分隔符换空格 */
    public static String cleanForQuery(String raw) {
        String s = raw == null ? "" : raw.trim();
        int dot = s.lastIndexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        s = s.replaceAll("\\[[^\\]]*\\]", " ")
                .replaceAll("【[^】]*】", " ")
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("^\\d{1,3}[\\s.\\-_、]+", " ")
                .replaceAll("[-_–—]+", " ")
                .replaceAll("\\s+", " ").trim();
        return s;
    }

    /* ------------------------------ 网易云 ------------------------------ */

    private static class NetEaseSong {
        String id = "";
        String name = "";
        String artist = "";
        String picUrl = "";
        String album = "";
        long duration = 0;
    }

    private static NetEaseSong searchNetease(String query, String title, String artist,
                                             int durationMs) throws Exception {
        String form = "s=" + Http.enc(query) + "&type=1&limit=8&offset=0";
        Map<String, String> h = new HashMap<>();
        h.put("Referer", "https://music.163.com/");
        h.put("Cookie", "appver=2.0.2");
        String body = Http.postForm(NETEAST_SEARCH, form, h);
        JSONObject o = new JSONObject(body);
        JSONObject result = o.optJSONObject("result");
        JSONArray songs = result == null ? null : result.optJSONArray("songs");
        if (songs == null || songs.length() == 0) return null;

        NetEaseSong best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < songs.length(); i++) {
            JSONObject s = songs.getJSONObject(i);
            NetEaseSong c = new NetEaseSong();
            c.id = s.optString("id");
            c.name = s.optString("name");
            c.duration = s.optLong("dt", 0);
            JSONObject al = s.optJSONObject("al");
            if (al != null) { c.picUrl = al.optString("picUrl", ""); c.album = al.optString("name", ""); }
            JSONArray ar = s.optJSONArray("ar");
            if (ar != null && ar.length() > 0) {
                JSONObject a0 = ar.optJSONObject(0);
                if (a0 != null) c.artist = a0.optString("name", "");
            }
            int score = 0;
            if (c.name.equalsIgnoreCase(title)) score += 100;
            else if (c.name.contains(title) || title.contains(c.name)) score += 55;
            if (artist.length() > 0 && c.artist.contains(artist)) score += 40;
            if (durationMs > 0 && c.duration > 0) {
                long d = Math.abs(c.duration - durationMs);
                if (d <= 3000) score += 40;
                else if (d <= 10000) score += 18;
                else if (d > 60000) score -= 25;
            }
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return bestScore >= 40 ? best : null;
    }

    private static String neteaseLyric(String id) throws Exception {
        String url = NETEAST_LYRIC + "?id=" + id + "&lv=-1&kv=-1&tv=-1";
        Map<String, String> h = new HashMap<>();
        h.put("Referer", "https://music.163.com/");
        JSONObject o = new JSONObject(Http.get(url, h, 15000));
        JSONObject lrc = o.optJSONObject("lrc");
        String text = lrc == null ? "" : lrc.optString("lyric", "");
        return "暂无歌词".equals(text.trim()) ? null : text;
    }

    /* ------------------------------- 酷狗 ------------------------------- */

    private static String kugouLyric(String query, int durationMs) throws Exception {
        String url = KUGOU_SEARCH + "?ver=1&man=yes&client=pc&keyword=" + Http.enc(query)
                + "&duration=" + Math.max(0, durationMs);
        Map<String, String> h = new HashMap<>();
        h.put("Referer", "https://www.kugou.com/");
        JSONObject o = new JSONObject(Http.get(url, h, 15000));
        JSONArray cand = o.optJSONArray("candidates");
        if (cand == null || cand.length() == 0) return null;

        String id = null, ak = null;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < cand.length(); i++) {
            JSONObject c = cand.getJSONObject(i);
            long d = durationMs > 0 ? Math.abs(c.optLong("duration", 0) - durationMs) : 0;
            if (id == null || d < bestDiff) {
                bestDiff = d;
                id = c.optString("id");
                ak = c.optString("accesskey");
            }
        }
        if (id == null) return null;
        String dl = KUGOU_DOWNLOAD + "?ver=1&client=pc&id=" + Http.enc(id)
                + "&accesskey=" + Http.enc(ak) + "&fmt=lrc&charset=utf8";
        JSONObject d = new JSONObject(Http.get(dl, h, 15000));
        String content = d.optString("content", "");
        if (content.length() == 0) return null;
        byte[] raw = B64.decode(content);
        return new String(raw, "UTF-8");
    }

    /* ------------------------------- 工具 ------------------------------- */

    static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    private static String msg(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return m.length() > 120 ? m.substring(0, 120) : m;
    }

    private static String readText(File f) {
        try {
            byte[] b = readBytes(f);
            return b == null ? "" : new String(b, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static void writeText(File f, String s) {
        try {
            writeBytes(f, s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ignored) { }
    }

    private static byte[] readBytes(File f) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int off = 0, n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
            return buf;
        } catch (IOException e) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (IOException ignored) { }
        }
    }

    private static void writeBytes(File f, byte[] data) {
        try {
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileOutputStream out = new FileOutputStream(f);
            out.write(data);
            out.close();
        } catch (IOException ignored) { }
    }
}
