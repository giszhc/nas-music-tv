package com.yunying.music;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/** 极简 HTTP 客户端（HttpURLConnection），够用且无第三方依赖。 */
public class Http {
    interface Transport {String call(String url,String method,Map<String,String> headers,String body)throws IOException;}
    static Transport testTransport;

    public static final String UA =
            "Mozilla/5.0 (Linux; Android 12) YunYingMusic/1.1";

    public static Map<String, String> headers(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static HttpURLConnection open(String url, String method,
                                         Map<String, String> h, int timeout) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(Math.min(timeout, 15000));
        c.setReadTimeout(timeout);
        c.setRequestMethod(method);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("Accept-Encoding", "identity");
        if (h != null) {
            for (Map.Entry<String, String> e : h.entrySet()) {
                if (e.getValue() != null) c.setRequestProperty(e.getKey(), e.getValue());
            }
        }
        return c;
    }

    public static String get(String url, Map<String, String> h, int timeout) throws IOException {
        return call(url, "GET", h, null, null, timeout);
    }

    public static String postJson(String url, String body, Map<String, String> h) throws IOException {
        return call(url, "POST", h, body, "application/json;charset=utf-8", 20000);
    }

    public static String postForm(String url, String body, Map<String, String> h) throws IOException {
        return call(url, "POST", h, body, "application/x-www-form-urlencoded;charset=utf-8", 20000);
    }

    public static String call(String url, String method, Map<String, String> h,
                              String body, String contentType, int timeout) throws IOException {
        if(testTransport!=null)return testTransport.call(url,method,h,body);
        HttpURLConnection c = open(url, method, h, timeout);
        try {
            if (body != null) {
                c.setDoOutput(true);
                if (contentType != null) c.setRequestProperty("Content-Type", contentType);
                OutputStream os = c.getOutputStream();
                os.write(body.getBytes("UTF-8"));
                os.flush();
                os.close();
            }
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            String text = read(in);
            if (code >= 400 && (text == null || text.length() == 0)) {
                throw new IOException("HTTP " + code + " " + c.getResponseMessage());
            }
            return text == null ? "" : text;
        } finally {
            try { c.disconnect(); } catch (Throwable ignored) { }
        }
    }

    /** 取二进制内容（图片等） */
    public static byte[] bytes(String url, Map<String, String> h) throws IOException {
        HttpURLConnection c = open(url, "GET", h, 20000);
        try {
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            if (in == null) throw new IOException("HTTP " + code);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            if (code >= 400) throw new IOException("HTTP " + code);
            return bos.toByteArray();
        } finally {
            try { c.disconnect(); } catch (Throwable ignored) { }
        }
    }

    public static String read(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

    public static String enc(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
