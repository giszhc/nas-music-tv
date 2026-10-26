package com.yunying.music;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云盘接口（openapi.alipan.com /adrive/v1.0/*）。
 * 两种凭据：
 *  - MODE_WEB：App 内扫码登录（推荐，电视上最省事）
 *  - MODE_OPEN：开放平台 client_id + refresh_token（手动填）
 */
public class Aliyun {

    public static String host() {
        if(Config.MODE_WEB.equals(Config.aliMode))return "https://api.alipan.com";
        String h = Config.aliHost == null ? "https://openapi.alipan.com" : Config.aliHost.trim();
        return h.replaceAll("/+$", "");
    }

    private static Map<String, String> auth() {
        Map<String, String> h = new HashMap<>();
        h.put("Authorization", "Bearer " + Config.aliAccess);
        h.put("Content-Type", "application/json;charset=utf-8");
        h.put("Referer", "https://www.alipan.com/");
        h.put("Origin","https://www.alipan.com");
        if(Config.MODE_WEB.equals(Config.aliMode)) {
            h.put("X-Canary","client=Android,app=adrive,version=v4.1.0");
            if(!Config.aliUserId.isEmpty())try{ensureIdentity();h.put("X-Device-Id",Config.aliDeviceId);h.put("X-Signature",deviceSignature());}catch(Exception ignored){}
        }
        return h;
    }

    /** 刷新 access_token（按登录方式分流） */
    public static void refresh() throws Exception {
        if (Config.MODE_WEB.equals(Config.aliMode)) {
            AliyunLogin.refreshWebToken();
            return;
        }
        if (Config.aliRefresh == null || Config.aliRefresh.trim().length() == 0) {
            throw new Exception("未登录阿里云盘");
        }
        if (Config.aliClientId == null || Config.aliClientId.trim().length() == 0) {
            throw new Exception("未填写 client_id（开放平台凭据）");
        }
        final long epoch=Config.authVersion();
        JSONObject payload=new JSONObject();payload.put("grant_type","refresh_token");payload.put("refresh_token",Config.aliRefresh.trim());payload.put("client_id",Config.aliClientId.trim());payload.put("client_secret",Config.aliSecret.trim());
        String body=Http.postJson(host()+"/oauth/access_token",payload.toString(),null);
        JSONObject o;
        try {
            o = new JSONObject(body);
        } catch (Exception e) {
            throw new Exception("token 响应无法解析");
        }
        String at = o.optString("access_token", "");
        if (at.length() == 0) {
            String err = o.optString("message", o.optString("error_description", "未返回访问令牌"));
            throw new Exception("token 获取失败：" + err);
        }
        if(epoch!=Config.authVersion())throw new Exception("登录操作已取消");
        Config.aliDriveId = "";
        Config.aliAccess = at;
        String rt = o.optString("refresh_token", "");
        if (rt.length() > 0) Config.aliRefresh = rt;
        Config.aliExpire = System.currentTimeMillis() + o.optLong("expires_in", 7200) * 1000L;
        Config.aliMode = Config.MODE_OPEN;Config.aliManualRefresh=Config.aliRefresh;Config.persist();
    }

    private static String shortMsg(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        return m.length() > 160 ? m.substring(0, 160) : m;
    }

    private static boolean accessValid() {
        return Config.aliAccess != null && Config.aliAccess.length() > 0
                && Config.aliExpire > System.currentTimeMillis() + 60000L;
    }

    public static void ensureToken() throws Exception {
        if (!accessValid()) refresh();
    }

    private static String signatureIdentity="",signatureValue="";
    private static void ensureIdentity(){
        if(Config.aliDeviceId.isEmpty())Config.aliDeviceId=java.util.UUID.randomUUID().toString();
        if(Config.aliPrivateKey.isEmpty())Config.aliPrivateKey=AliyunDevice.privateKey();
        Config.persist();
    }
    private static String deviceSignature()throws Exception {
        String identity=Config.aliUserId+":"+Config.aliDeviceId+":"+Config.aliPrivateKey;
        if(!identity.equals(signatureIdentity)){signatureValue=AliyunDevice.signature(Config.aliPrivateKey,Config.aliDeviceId,Config.aliUserId);signatureIdentity=identity;}
        return signatureValue;
    }
    private static void createDeviceSession()throws Exception {
        if(Config.aliUserId.isEmpty())throw new Exception("无法初始化设备：缺少用户标识，请重新验证登录");
        ensureIdentity();JSONObject body=new JSONObject();body.put("deviceName","云影音乐");body.put("modelName","Android TV");body.put("nonce",0);body.put("pubKey",AliyunDevice.publicKey(Config.aliPrivateKey));body.put("refreshToken",Config.aliRefresh);
        JSONObject response=new JSONObject(Http.postJson(host()+"/users/v1/users/device/create_session",body.toString(),auth()));
        if(!response.optString("code").isEmpty())throw new Exception("设备验证失败（"+response.optString("code")+"），请重新扫码或使用开放平台凭据");
    }
    static String route(String path){
        if(!Config.MODE_WEB.equals(Config.aliMode))return path;
        if(path.equals("/adrive/v1.0/user/getDriveInfo"))return "/v2/user/get";
        if(path.equals("/adrive/v1.0/openFile/list"))return "/v2/file/list";
        if(path.equals("/adrive/v1.0/openFile/getDownloadUrl"))return "/v2/file/get_download_url";
        return path;
    }
    /** 网页与开放平台凭据严格分流；仅明确的令牌/设备会话错误触发一次重试。 */
    public static synchronized JSONObject api(String path,JSONObject body)throws Exception {
        ensureToken();boolean refreshed=false,deviceRenewed=false;
        for(int attempt=0;attempt<3;attempt++) {
            JSONObject response;
            try{response=new JSONObject(Http.postJson(host()+route(path),body==null?"{}":body.toString(),auth()));}
            catch(org.json.JSONException ex){throw new Exception("云盘响应不是有效 JSON，请检查网络或稍后重试");}
            String code=response.optString("code","");
            if(code.isEmpty()||"200".equals(code)||"0".equals(code))return response;
            if((code.equals("AccessTokenInvalid")||code.equals("AccessTokenExpired")||code.equals("I400JD"))&&!refreshed){refreshed=true;refresh();continue;}
            if(Config.MODE_WEB.equals(Config.aliMode)&&code.equals("DeviceSessionSignatureInvalid")&&!deviceRenewed){deviceRenewed=true;createDeviceSession();continue;}
            if(code.contains("TooMany")||code.contains("RateLimit"))throw new Exception("云盘请求过于频繁，请稍后重试（"+code+"）");
            if(code.contains("Forbidden")||code.contains("Denied"))throw new Exception("账号授权或文件权限不足（"+code+"）");
            throw new Exception("云盘请求失败（"+code+"），请在设置中重新验证登录");
        }
        throw new Exception("云盘凭据重试后仍无效，请重新扫码");
    }
    public static void verifyLogin()throws Exception {
        final long epoch=Config.authVersion();
        JSONObject user=api("/adrive/v1.0/user/getDriveInfo",new JSONObject());
        String id=user.optString("resource_drive_id","");if(id.isEmpty())id=user.optString("default_drive_id","");if(id.isEmpty())id=user.optString("backup_drive_id","");
        if(id.isEmpty())throw new Exception("账号确认成功，但未返回可访问的云盘空间");
        if(epoch!=Config.authVersion())throw new Exception("登录操作已取消");
        Config.aliDriveId=id;Config.aliUserId=user.optString("user_id",Config.aliUserId);
        String nickname=user.optString("nick_name",user.optString("user_name",user.optString("name","")));if(!nickname.isEmpty())Config.aliDriveName=nickname;
        Config.aliAvatar=user.optString("avatar",user.optString("avatar_url",Config.aliAvatar));
        Config.persist();
    }

    public static String driveId() throws Exception {
        if(Config.MODE_WEB.equals(Config.aliMode)&&Config.aliUserId.isEmpty())verifyLogin();
        if (Config.aliDriveId != null && Config.aliDriveId.length() > 0) return Config.aliDriveId;
        verifyLogin();return Config.aliDriveId;
    }

    public static List<Entry> list(String parentId) throws Exception {
        List<Entry> out = new ArrayList<>();
        String drive = driveId();
        String marker = "";
        for (int guard = 0; guard < 20; guard++) {
            JSONObject body = new JSONObject();
            body.put("drive_id", drive);
            body.put("parent_file_id", parentId == null || parentId.length() == 0 ? "root" : parentId);
            body.put("limit", 100);
            body.put("marker", marker);
            body.put("order_by", "name");
            body.put("order_direction", "ASC");
            body.put("fields", "*");
            JSONObject o = api("/adrive/v1.0/openFile/list", body);
            JSONArray items = o.optJSONArray("items");
            if (items == null) break;
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.getJSONObject(i);
                boolean dir = "folder".equals(it.optString("type"));
                Entry e = new Entry(it.optString("name"), dir, Entry.SRC_ALI);
                e.id = it.optString("file_id");
                e.path = it.optString("parent_file_id");
                e.size = it.optLong("size", 0L);
                out.add(e);
            }
            marker = o.optString("next_marker", "");
            if (marker.length() == 0) break;
        }
        return out;
    }

    public static String downloadUrl(String fileId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("drive_id", driveId());
        body.put("file_id", fileId);
        body.put("expire_sec", 14400);
        JSONObject o = api("/adrive/v1.0/openFile/getDownloadUrl", body);
        String url = o.optString("url", "");
        if (url.length() == 0) throw new Exception("获取播放地址失败，文件可能不允许下载");
        return url;
    }

    public static Map<String, String> playHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Referer", "https://www.aliyundrive.com/");
        return h;
    }
}
