package com.yunying.music;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群晖 DSM FileStation 接口（对标参考项目的 lib/synology.js）
 * - SYNO.API.Auth 登录取 sid + synotoken
 * - SYNO.FileStation.List 列共享目录 / 列目录
 * - SYNO.FileStation.Download 取播放直链（播放器带 X-SYNO-TOKEN 头请求）
 */
public class Synology {

    private static final String[] AUTH_VERSIONS = {"6", "3", "1"};

    public static String base() {
        String h = Config.nasHost == null ? "" : Config.nasHost.trim();
        h = h.replaceAll("^https?://", "").replaceAll("/+$", "");
        if (!h.contains(":")) h = h + ":" + Config.nasPort;
        return (Config.nasHttps ? "https://" : "http://") + h;
    }

    public static String baseNoPort() {
        return base();
    }

    private static String buildQuery(Map<String, String> p) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : p.entrySet()) {
            if (e.getValue() == null || e.getValue().length() == 0) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=').append(Http.enc(e.getValue()));
        }
        return sb.toString();
    }

    private static String errText(int code) {
        switch (code) {
            case 400: return "账号或密码不正确";
            case 401: return "账号不存在";
            case 402: return "账号权限不足";
            case 403: return "该账号启用了两步验证，请使用专用账号或关闭两步验证";
            case 407: return "IP 被封禁";
            case 105: return "当前账号没有访问该目录的权限";
            case 106: return "登录已超时，请重新登录";
            case 107: return "同一账号重复登录被拒绝";
            case 119: return "登录状态失效，请重新登录";
            case 408: return "文件或目录不存在";
            case 900: return "目录不存在";
            case 902: return "没有权限访问该目录";
            default:  return "群晖返回错误码 " + code;
        }
    }

    /** 登录；成功写入 Config 的 sid / token / 主机名 */
    public static void login() throws Exception {
        if (!Config.nasReady()) throw new Exception("请先填写 NAS 地址与账号");
        String last = "登录失败";
        for (String ver : AUTH_VERSIONS) {
            Map<String, String> q = new HashMap<>();
            q.put("api", "SYNO.API.Auth");
            q.put("version", ver);
            q.put("method", "login");
            q.put("account", Config.nasUser);
            q.put("passwd", Config.nasPass);
            q.put("session", "FileStation");
            q.put("format", "sid");
            String url = base() + "/webapi/entry.cgi?" + buildQuery(q);
            try {
                JSONObject r = new JSONObject(Http.get(url, null, 20000));
                if (r.optBoolean("success")) {
                    JSONObject d = r.optJSONObject("data");
                    Config.nasSid = d == null ? "" : d.optString("sid", "");
                    Config.nasToken = d == null ? "" : d.optString("synotoken", "");
                    if (Config.nasSid.length() == 0) {
                        last = "登录响应异常（无 sid）";
                        continue;
                    }
                    detectName();
                    return;
                }
                int code = r.optJSONObject("error") == null ? 0
                        : r.optJSONObject("error").optInt("code", 0);
                last = errText(code);
                // 密码错误 / 账号被停用 这类错误换版本也没用，直接抛出
                if (code == 400 || code == 401 || code == 405 || code == 403) break;
            } catch (Exception e) {
                last = friendly(e);
            }
        }
        throw new Exception(last);
    }

    private static String friendly(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.contains("ECONNREFUSED") || m.contains("refused")) {
            return "端口未开放：请确认端口号与协议（HTTP 5000 / HTTPS 5001）";
        }
        if (m.contains("ETIMEDOUT") || m.contains("timed out") || m.contains("timeout")) {
            return "连接超时：IP 可能不正确，或被防火墙拦截";
        }
        if (m.contains("Unable to resolve host") || m.contains("UnknownHost")) {
            return "找不到该地址，请检查 IP 或域名";
        }
        if (m.contains("SSL") || m.contains("Cert")) {
            return "HTTPS 证书异常：请改用 HTTP 端口（5000）";
        }
        return "网络错误：" + m;
    }

    /** 抓首页标题作为设备名（失败不影响登录） */
    private static void detectName() {
        if (Config.nasName != null && Config.nasName.length() > 0) return;
        try {
            String html = Http.get(base() + "/", null, 6000);
            int a = html.indexOf("<title>");
            int b = html.indexOf("</title>", a < 0 ? 0 : a);
            if (a > 0 && b > a) {
                String t = html.substring(a + 7, b).replace("&nbsp;", " ").trim();
                int dash = t.indexOf('-');
                if (dash > 0) t = t.substring(0, dash).trim();
                Config.nasName = t;
            }
        } catch (Throwable ignored) { }
    }

    /** 通用接口调用：返回 data 对象 */
    public static JSONObject call(String api, String version, String method,
                                 Map<String, String> params) throws Exception {
        if (Config.nasSid == null || Config.nasSid.length() == 0) {
            login();
        }
        Map<String, String> q = new HashMap<>();
        q.put("api", api);
        q.put("version", version);
        q.put("method", method);
        q.put("_sid", Config.nasSid);
        if (params != null) q.putAll(params);
        String url = base() + "/webapi/entry.cgi?" + buildQuery(q);
        Map<String, String> h = new HashMap<>();
        if (Config.nasToken != null && Config.nasToken.length() > 0) {
            h.put("X-SYNO-TOKEN", Config.nasToken);
        }
        String body = Http.get(url, h, 30000);
        JSONObject r = new JSONObject(body);
        if (!r.optBoolean("success")) {
            int code = r.optJSONObject("error") == null ? 0
                    : r.optJSONObject("error").optInt("code", 0);
            if (code == 106 || code == 107 || code == 119) {
                login();
                q.put("_sid", Config.nasSid);
                if (Config.nasToken != null && Config.nasToken.length() > 0) {
                    h.put("X-SYNO-TOKEN", Config.nasToken);
                }
                url = base() + "/webapi/entry.cgi?" + buildQuery(q);
                r = new JSONObject(Http.get(url, h, 30000));
                if (r.optBoolean("success")) return r.optJSONObject("data");
            }
            throw new Exception(errText(code));
        }
        JSONObject d = r.optJSONObject("data");
        return d == null ? new JSONObject() : d;
    }

    /** 共享文件夹列表 */
    public static List<Entry> listShares() throws Exception {
        JSONObject d = call("SYNO.FileStation.List", "2", "list_share",
                single("additional", "[\"real_path\"]"));
        List<Entry> out = new ArrayList<>();
        JSONArray arr = d.optJSONArray("shares");
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Entry e = new Entry(o.optString("name"), true, Entry.SRC_NAS);
            e.path = o.optString("path");
            out.add(e);
        }
        return out;
    }

    /** 目录内容（自动翻页） */
    public static List<Entry> list(String folder) throws Exception {
        List<Entry> out = new ArrayList<>();
        int offset = 0;
        final int limit = 1000;
        for (int guard = 0; guard < 30; guard++) {
            Map<String, String> p = new HashMap<>();
            p.put("folder_path", folder);
            p.put("additional", "[\"size\",\"time\"]");
            p.put("sort_by", "name");
            p.put("sort_direction", "asc");
            p.put("limit", String.valueOf(limit));
            p.put("offset", String.valueOf(offset));
            JSONObject d = call("SYNO.FileStation.List", "2", "list", p);
            JSONArray arr = d.optJSONArray("files");
            if (arr == null) break;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Entry e = new Entry(o.optString("name"), o.optBoolean("isdir"), Entry.SRC_NAS);
                e.path = o.optString("path");
                JSONObject ad = o.optJSONObject("additional");
                if (ad != null) e.size = ad.optLong("size", 0L);
                out.add(e);
            }
            int got = arr.length();
            offset += got;
            int total = d.optInt("total", got);
            if (got < limit || offset >= total) break;
        }
        return out;
    }

    /** 播放直链：entry.cgi?api=SYNO.FileStation.Download…，需带 X-SYNO-TOKEN */
    public static String downloadUrl(String filePath) {
        Map<String, String> q = new HashMap<>();
        q.put("api", "SYNO.FileStation.Download");
        q.put("version", "2");
        q.put("method", "download");
        q.put("path", "[\"" + filePath.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]");
        q.put("mode", "open");
        q.put("_sid", Config.nasSid);
        return base() + "/webapi/entry.cgi?" + buildQuery(q);
    }

    public static Map<String, String> playHeaders() {
        Map<String, String> h = new HashMap<>();
        if (Config.nasToken != null && Config.nasToken.length() > 0) {
            h.put("X-SYNO-TOKEN", Config.nasToken);
        }
        return h;
    }

    private static Map<String, String> single(String k, String v) {
        Map<String, String> m = new HashMap<>();
        m.put(k, v);
        return m;
    }
}
