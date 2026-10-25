package com.yunying.music;

import android.content.Context;
import org.json.*;
import java.util.*;

public final class ProtocolChecks {
    private static int count;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);count++;}
    private static String qr(Object biz)throws Exception{return new JSONObject().put("content",new JSONObject().put("data",new JSONObject().put("qrCodeStatus","CONFIRMED").put("bizExt",biz))).toString();}
    public static String run(Context context)throws Exception {
        Config.load(context);count=0;
        Config.aliMode=Config.MODE_NONE;Config.aliManualRefresh="fixture_manual";
        JSONObject tokens=new JSONObject().put("refresh_token","fixture_refresh");
        String encoded=android.util.Base64.encodeToString(new JSONObject().put("pds_login_result",tokens.toString()).toString().getBytes("UTF-8"),android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP);
        AliyunLogin.PollResult confirmed=AliyunLogin.parsePoll(qr(encoded));
        check(confirmed.loggedIn&&confirmed.terminal,"refresh-only QR supported");check(Config.MODE_NONE.equals(Config.aliMode),"poll must not commit credentials");
        int[] refreshes={0},sessions={0},lists={0};
        Http.testTransport=(url,method,headers,body)->{
            try {
                if(url.equals("https://auth.alipan.com/v2/account/token")){refreshes[0]++;return "{\"access_token\":\"fixture_access\",\"refresh_token\":\"fixture_rotated\",\"expires_in\":7200}";}
                if(url.endsWith("/v2/user/get")){check(url.startsWith("https://api.alipan.com/"),"web user route");return "{\"resource_drive_id\":\"\",\"default_drive_id\":\"drive_fixture\",\"user_id\":\"user_fixture\",\"nick_name\":\"fixture\"}";}
                if(url.endsWith("/users/v1/users/device/create_session")){sessions[0]++;JSONObject json=new JSONObject(body);check(json.getString("pubKey").length()==130,"device public key");return "{\"success\":true}";}
                if(url.endsWith("/v2/file/list")){
                    check(url.startsWith("https://api.alipan.com/"),"web list route");check(headers.get("X-Signature").length()==130,"signed device headers");
                    if(lists[0]++==0)return "{\"code\":\"DeviceSessionSignatureInvalid\"}";
                    if(new JSONObject(body).optString("marker").isEmpty())return "{\"items\":[{\"file_id\":\"song1\",\"name\":\"fixture.mp3\",\"type\":\"file\"}],\"next_marker\":\"page2\"}";
                    return "{\"items\":[{\"file_id\":\"song2\",\"name\":\"fixture.flac\",\"type\":\"file\"}]}";
                }
                if(url.endsWith("/v2/file/get_download_url"))return "{\"url\":\"https://example.invalid/fixture.flac\"}";
                throw new AssertionError("unexpected route: "+url);
            }catch(JSONException ex){throw new java.io.IOException("fixture JSON invalid");}
        };
        try {
            AliyunLogin.accept(confirmed);check(Config.MODE_WEB.equals(Config.aliMode),"web mode retained");check(refreshes[0]==1,"refresh-only token exchange");check(Config.aliManualRefresh.equals("fixture_manual"),"manual token is separate");
            Aliyun.verifyLogin();check(Config.aliDriveId.equals("drive_fixture"),"empty resource drive falls back");check(Aliyun.list("root").size()==2,"pagination");check(sessions[0]==1,"device session retry bounded");check(Aliyun.downloadUrl("song2").endsWith(".flac"),"web download route");
            check(AliyunLogin.parsePoll(qr(new JSONObject().put("accessToken","fixture_access"))).loggedIn,"object credentials");
            check(AliyunLogin.parsePoll(qr("{\"pds_login_result\":{\"access_token\":\"fixture_access\"}}")).loggedIn,"raw JSON credentials");
            AliyunLogin.PollResult missing=AliyunLogin.parsePoll(qr(""));check(missing.terminal&&!missing.loggedIn,"missing credentials terminate polling");
            AliyunLogin.PollResult expired=AliyunLogin.parsePoll("{\"content\":{\"data\":{\"qrCodeStatus\":\"EXPIRED\"}}}");check(expired.terminal,"expiry terminal");
            int before=refreshes[0];Http.testTransport=(url,method,headers,body)->"{\"code\":\"InvalidParameter\"}";
            boolean failed=false;try{Aliyun.list("root");}catch(Exception ex){failed=true;}check(failed&&refreshes[0]==before,"parameter errors never rotate credentials");
            Config.aliMode=Config.MODE_OPEN;Config.aliAccess="fixture_open";Config.aliExpire=System.currentTimeMillis()+3600000;Config.aliDriveId="fixture_open_drive";
            Http.testTransport=(url,method,headers,body)->{check(url.equals("https://openapi.alipan.com/adrive/v1.0/openFile/list"),"open API kept separate");return "{\"items\":[]}";};check(Aliyun.list("root").isEmpty(),"open API response");
            String one="0000000000000000000000000000000000000000000000000000000000000001";
            check(AliyunDevice.publicKey(one).startsWith("0479be667ef9dcbbac"),"known secp256k1 generator");String signature=AliyunDevice.signature(one,"fixture-device","fixture-user");check(AliyunDevice.verify(one,"fixture-device","fixture-user",signature),"signature verifies");check(!AliyunDevice.verify(one,"fixture-device","wrong-user",signature),"signature rejects changed identity");
        } finally {Http.testTransport=null;context.getSharedPreferences("yy_music",0).edit().clear().commit();}
        return "PASS: "+count+" protocol checks; no real account or tokens used";
    }
}
