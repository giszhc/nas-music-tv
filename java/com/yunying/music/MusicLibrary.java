package com.yunying.music;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** 只保存媒体标识，不保存下载地址、令牌或账号。 */
public final class MusicLibrary {
    private static final java.util.Map<String,String> META_KEYS=new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile String favoriteSnapshot="";
    private static volatile java.util.Set<String> favoriteKeys=java.util.Collections.emptySet();
    private static android.content.SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("music_library", 0);
    }
    public static String key(Entry e) { return e.source + ":" + e.path + ":" + e.id; }
    private static JSONArray encode(List<Entry> entries) {
        JSONArray a = new JSONArray();
        for (Entry e : entries) try {
            JSONObject j = new JSONObject();
            j.put("name", e.name); j.put("path", e.path); j.put("id", e.id);
            j.put("audiobook",e.audiobook); j.put("source", e.source); j.put("size", e.size); a.put(j);
        } catch (Exception ignored) { }
        return a;
    }
    public static void write(Context c,String name,List<Entry> entries){prefs(c).edit().putString(name,encode(entries).toString()).apply();}
    public static List<Entry> read(Context c, String name) {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(name, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject j = a.getJSONObject(i);
                Entry e = new Entry(j.optString("name"), false, j.optString("source", Entry.SRC_NAS));
                e.audiobook=j.optBoolean("audiobook",false)||"books".equals(name)||"bookRecent".equals(name);
                e.path = j.optString("path"); e.id = j.optString("id"); e.size = j.optLong("size");
                if (e.isAudio()) out.add(e);
            }
        } catch (Exception ignored) { }
        return out;
    }
    public static boolean favorite(Context c, Entry e) {
        if(e==null)return false;
        String raw=prefs(c).getString("favorites","[]");
        if(!raw.equals(favoriteSnapshot)) {
            java.util.Set<String> keys=new java.util.HashSet<>();
            for(Entry item:read(c,"favorites"))keys.add(key(item));
            favoriteKeys=keys;favoriteSnapshot=raw;
        }
        return favoriteKeys.contains(key(e));
    }
    public static void toggleFavorite(Context c, Entry e) {
        if (e == null) return;
        List<Entry> list = read(c, "favorites");
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) if (key(list.get(i)).equals(key(e))) {
            list.remove(i); removed = true;
        }
        if (!removed) list.add(0, e);
        prefs(c).edit().putString("favorites", encode(list).toString()).apply();
    }
    public static boolean isBook(Context c,Entry e){return e!=null&&e.audiobook;}
    public static int addToBooks(Context c,List<Entry> entries){List<Entry> all=read(c,"books");java.util.Set<String> keys=new java.util.HashSet<>();for(Entry e:all)keys.add(key(e));int count=0;for(Entry e:entries)if(e.isAudio()&&keys.add(key(e))){all.add(e);count++;}prefs(c).edit().putString("books",encode(all).toString()).apply();return count;}
    public static void remember(Context c, Entry e) {
        if(isBook(c,e)){List<Entry> books=read(c,"bookRecent");for(int i=books.size()-1;i>=0;i--)if(key(books.get(i)).equals(key(e)))books.remove(i);books.add(0,e);if(books.size()>100)books=new ArrayList<>(books.subList(0,100));prefs(c).edit().putString("bookRecent",encode(books).toString()).apply();return;}
        List<Entry> list = read(c, "recent");
        for (int i = list.size() - 1; i >= 0; i--) if (key(list.get(i)).equals(key(e))) list.remove(i);
        list.add(0, e);
        if (list.size() > 100) list = new ArrayList<>(list.subList(0, 100));
        prefs(c).edit().putString("recent", encode(list).toString()).apply();
    }
    public static List<Entry> queue(Context c) { return read(c, "queue"); }
    public static void saveQueue(Context c, List<Entry> entries, int index, int mode, int position) {
        if (entries.isEmpty()) return;
        prefs(c).edit().putString("queue", encode(entries).toString()).putInt("index", index)
            .putInt("mode", mode).putInt("position", position).apply();
    }

    public static List<Entry> playlist(Context c) {
        if (!prefs(c).getBoolean("playlistInitialized",false)) {
            List<Entry> initial=queue(c);if(initial.isEmpty())initial=read(c,"catalog");
            setPlaylist(c,initial);
        }
        return read(c,"playlist");
    }
    public static void setPlaylist(Context c,List<Entry> entries) {
        prefs(c).edit().putString("playlist",encode(entries).toString()).putBoolean("playlistInitialized",true).apply();
    }
    public static int addToPlaylist(Context c,List<Entry> entries) {
        List<Entry> all=playlist(c);java.util.Set<String> keys=new java.util.HashSet<>();for(Entry e:all)keys.add(key(e));
        int count=0;for(Entry e:entries)if(e.isAudio()&&keys.add(key(e))){all.add(e);count++;}
        setPlaylist(c,all);catalog(c,entries);return count;
    }

    public static void catalog(Context c, List<Entry> entries) {
        List<Entry> all = read(c, "catalog");
        java.util.LinkedHashMap<String, Entry> unique = new java.util.LinkedHashMap<>();
        for (Entry e : all) unique.put(key(e), e);
        for (Entry e : entries) if (e.isAudio()) unique.put(key(e), e);
        prefs(c).edit().putString("catalog", encode(new ArrayList<>(unique.values())).toString()).apply();
    }
    private static String metaKey(Entry e) {
        String identity=key(e);String cached=META_KEYS.get(identity);if(cached!=null)return cached;
        try {
            byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(identity.getBytes("UTF-8"));
            char[] hex="0123456789abcdef".toCharArray();char[] out=new char[bytes.length*2];
            for(int i=0;i<bytes.length;i++){out[i*2]=hex[(bytes[i]>>4)&15];out[i*2+1]=hex[bytes[i]&15];}
            String result=new String(out);META_KEYS.put(identity,result);return result;
        } catch(Exception ex){return Integer.toHexString(identity.hashCode());}
    }
    public static void metadata(Context c, Entry e, OnlineMeta.Meta m) {
        if (m == null) return;
        String k = metaKey(e);
        android.content.SharedPreferences.Editor editor = prefs(c).edit();
        if (!m.title.isEmpty()) editor.putString(k + "title", m.title);
        if (!m.artist.isEmpty()) editor.putString(k + "artist", m.artist);
        if (!m.album.isEmpty()) editor.putString(k + "album", m.album);
        editor.apply();
        if (m.cover != null) try {
            java.io.FileOutputStream stream = new java.io.FileOutputStream(new java.io.File(c.getFilesDir(), k + ".jpg"));
            stream.write(m.cover); stream.close();
        } catch (Exception ignored) { }
    }
    public static String songTitle(Context c, Entry e) { return prefs(c).getString(metaKey(e) + "title", OnlineMeta.splitName(e.name)[0]); }
    public static String artist(Context c, Entry e) {
        String artist = prefs(c).getString(metaKey(e) + "artist", OnlineMeta.splitName(e.name)[1]);
        return artist.isEmpty() ? "未知歌手" : artist;
    }
    public static String album(Context c, Entry e) {
        String album = prefs(c).getString(metaKey(e) + "album", "");
        if (!album.isEmpty()) return album;
        int end = e.path.lastIndexOf('/');
        if (end > 0) { String parent = e.path.substring(0, end); return parent.substring(parent.lastIndexOf('/') + 1); }
        return "未分类专辑";
    }
    public static android.graphics.Bitmap artwork(Context c, Entry e) {
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options(); options.inSampleSize = 1;
        return android.graphics.BitmapFactory.decodeFile(new java.io.File(c.getFilesDir(), metaKey(e) + ".jpg").getAbsolutePath(), options);
    }
}
