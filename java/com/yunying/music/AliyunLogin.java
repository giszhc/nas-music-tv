package com.yunying.music;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云盘网页登录态（扫码登录）。
 * 走 passport.aliyundrive.com 的二维码接口，拿到的是「个人账号」登录态，
 * 之后用 auth.aliyundrive.com/v2/account/token 刷新 access_token，接口仍是 /adrive/v1.0/*。
 *
 * 注意：这是逆向出来的第三方登录协议，阿里随时可能改；官方短信登录还要过滑块验证码，
 * 原生 App 里无法完成，所以只提供扫码 + 手动 token 两条路。
 */
public class AliyunLogin {

    private static final String PASSPORT = "https://passport.aliyundrive.com";
    private static final String AUTH = "https://auth.alipan.com";
    private static final String COMMON =
            "appName=aliyun_drive&fromSite=52&appEntrance=web&_bx-v=2.5.3&isMobile=false"
                    + "&lang=zh_CN&return_url=&bizParams=&_csrf_token=&umidToken=&hsiz=";

    public static final String STATE_NEW = "NEW";
    public static final String STATE_SCANED = "SCANED";
    public static final String STATE_CONFIRMED = "CONFIRMED";
    public static final String STATE_EXPIRED = "EXPIRED";

    private static String hsiz() {
        StringBuilder sb = new StringBuilder();
        String hex = "0123456789abcdef";
        for (int i = 0; i < 32; i++) {
            sb.append(hex.charAt((int) (Math.random() * 16)));
        }
        return sb.toString();
    }

    /** 一次扫码会话 */
    public static class QrSession {
        public String codeContent = "";
        public String ck = "";
        public String t = "";
        public String hsiz = "";
        public long at = System.currentTimeMillis();
    }

    /** 轮询结果 */
    public static class PollResult {
        public String state = "";
        public String message = "";
        public boolean loggedIn = false;
        public boolean terminal = false;
        public JSONObject credentials;
    }

    /** 生成二维码字符串 */
    public static QrSession start() throws Exception {
        QrSession s = new QrSession();
        s.hsiz = hsiz();
        String url = PASSPORT + "/newlogin/qrcode/generate.do?" + COMMON + s.hsiz;
        Map<String, String> h = new HashMap<>();
        h.put("Referer", "https://www.aliyundrive.com/");
        h.put("Accept", "application/json");
        String body = Http.get(url, h, 15000);
        JSONObject o = new JSONObject(body);
        JSONObject content = o.optJSONObject("content");
        JSONObject data = content == null ? null : content.optJSONObject("data");
        if (data == null) throw new Exception("二维码服务响应异常，请稍后重试");
        s.codeContent = data.optString("codeContent", "");
        s.ck = data.optString("ck", "");
        s.t = String.valueOf(data.opt("t"));
        if (s.codeContent.length() == 0) throw new Exception("二维码内容为空");
        return s;
    }

    /** 轮询扫码状态；CONFIRMED 时自动写入 Config */
    public static PollResult poll(QrSession s) throws Exception {
        String url = PASSPORT + "/newlogin/qrcode/query.do?" + COMMON + s.hsiz;
        StringBuilder form = new StringBuilder();
        form.append("t=").append(Http.enc(s.t));
        form.append("&ck=").append(Http.enc(s.ck));
        form.append("&appName=aliyun_drive&fromSite=52&appEntrance=web&isMobile=false&lang=zh_CN");
        Map<String, String> h = new HashMap<>();
        h.put("Referer", "https://www.aliyundrive.com/");
        String body = Http.postForm(url, form.toString(), h);
        return parsePoll(body);
    }

    static PollResult parsePoll(String body) throws Exception {
        PollResult r = new PollResult();
        JSONObject o = new JSONObject(body);
        JSONObject content = o.optJSONObject("content");
        JSONObject data = content == null ? null : content.optJSONObject("data");
        if (data == null) {
            r.state = "ERROR";r.terminal=true;r.message="扫码服务未返回有效状态，请刷新二维码";
            return r;
        }
        String status = data.optString("qrCodeStatus", STATE_NEW);
        r.state = status;
        if (STATE_CONFIRMED.equals(status)) {
            Object biz=data.opt("bizExt");
            JSONObject pds=null;
            if(biz instanceof JSONObject)pds=(JSONObject)biz;
            else if(biz instanceof String){String text=((String)biz).trim();pds=extract(text.startsWith("{")?text:utf8(base64(text)));}
            if(pds==null){r.terminal=true;r.message="手机已确认，但未返回可解析的登录凭据，请重新生成二维码";return r;}
            String at=find(pds,"access_token","accessToken"),rt=find(pds,"refresh_token","refreshToken");
            if((at==null||at.isEmpty())&&(rt==null||rt.isEmpty())){r.terminal=true;r.message="手机已确认，但凭据中缺少令牌，请重新扫码";return r;}
            r.credentials=pds;r.loggedIn=true;r.terminal=true;r.message="手机已确认，正在验证云盘连接…";
        } else if (STATE_EXPIRED.equals(status)) {
            r.terminal=true;r.message = "二维码已过期，请刷新";
        } else if (STATE_SCANED.equals(status)) {
            r.message = "已扫码，请在手机上确认";
        } else if("CANCELED".equals(status)||"CANCELLED".equals(status)){r.terminal=true;r.message="手机已取消授权，请重新生成二维码";
        } else {
            r.message = "等待扫码…";
        }
        return r;
    }

    static void accept(PollResult result)throws Exception {
        if(result.credentials==null)throw new Exception("未收到扫码凭据");
        Config.invalidateAuth();
        JSONObject pds=result.credentials;
        String access=find(pds,"access_token","accessToken"),refresh=find(pds,"refresh_token","refreshToken");
        Config.aliMode=Config.MODE_WEB;Config.aliAccess=access==null?"":access;Config.aliRefresh=refresh==null?"":refresh;
        Config.aliDriveId="";Config.aliUserId="";Config.aliDeviceId="";Config.aliPrivateKey="";
        Config.aliExpire=System.currentTimeMillis()+Math.max(60,pds.optLong("expires_in",7200))*1000L;
        String name=find(pds,"nick_name","nickName","user_name","userName");Config.aliDriveName=name==null?"阿里云盘":name;
        String avatar=find(pds,"avatar","avatar_url","avatarUrl");Config.aliAvatar=avatar==null?"":avatar;
        if(Config.aliAccess.isEmpty())refreshWebToken();
        Config.persist();
    }

    /* ------------------------------- 工具 ------------------------------- */

    /** 在嵌套 JSON 里按候选 key 递归找字符串值 */
    private static String find(Object node, String... keys) {
        if (node instanceof JSONObject) {
            JSONObject o = (JSONObject) node;
            for (String k : keys) {
                if (o.has(k)) {
                    Object v = o.opt(k);
                    if (v instanceof String && ((String) v).length() > 0) return (String) v;
                }
            }
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String r = find(o.opt(it.next()), keys);
                if (r != null) return r;
            }
            // 有些字段本身是 JSON 字符串
            for (String k : keys) {
                if (o.has(k) && o.opt(k) instanceof String) {
                    String inner = (String) o.opt(k);
                    try {
                        return find(new JSONObject(inner), keys);
                    } catch (Exception ignored) { }
                }
            }
        } else if (node instanceof org.json.JSONArray) {
            org.json.JSONArray arr = (org.json.JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                String r = find(arr.opt(i), keys);
                if (r != null) return r;
            }
        } else if (node instanceof String) {
            String s = (String) node;
            if (s.startsWith("{")) {
                try {
                    return find(new JSONObject(s), keys);
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    /** bizExt → JSON（可能套一层字符串） */
    private static JSONObject extract(String text) {
        try {
            JSONObject o = new JSONObject(text);
            Object pds = o.opt("pds_login_result");
            if (pds instanceof String) return new JSONObject((String) pds);
            if (pds instanceof JSONObject) return (JSONObject) pds;
            return o;
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] base64(String s) {
        return B64.decode(s);
    }

    private static String utf8(byte[] b) {
        try {
            return new String(b, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    /** 网页登录态的刷新接口 */
    public static void refreshWebToken() throws Exception {
        if (Config.aliRefresh == null || Config.aliRefresh.length() == 0) {
            throw new Exception("登录已失效，请重新扫码");
        }
        final long epoch=Config.authVersion();
        JSONObject body = new JSONObject();
        body.put("refresh_token", Config.aliRefresh);
        body.put("grant_type", "refresh_token");
        Map<String, String> h = new HashMap<>();
        h.put("Content-Type", "application/json;charset=utf-8");
        h.put("Referer", "https://www.aliyundrive.com/");
        String r = Http.postJson(AUTH + "/v2/account/token", body.toString(), h);
        JSONObject o = new JSONObject(r);
        String at = o.optString("access_token", "");
        if (at.length() == 0) {
            String msg = o.optString("message", "登录已失效，请重新扫码");
            throw new Exception(msg);
        }
        if(epoch!=Config.authVersion())throw new Exception("登录操作已取消");
        Config.aliAccess = at;
        String rt = o.optString("refresh_token", "");
        if (rt.length() > 0) Config.aliRefresh = rt;
        Config.aliExpire = System.currentTimeMillis()
                + Math.max(600, o.optLong("expires_in", 7200)) * 1000L;
        Config.aliMode = Config.MODE_WEB;
        String user=o.optString("user_id","");if(!user.isEmpty())Config.aliUserId=user;
        Config.persist();
    }
}
