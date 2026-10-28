package com.yunying.music;

import android.app.Application;

import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** 全局初始化：放行群晖自签名证书。 */
public class App extends Application {

    @Override public void onCreate() {
        super.onCreate();
        Config.load(this);
        Ui.applyTheme(Config.lightTheme);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
            final java.util.WeakHashMap<android.app.Activity,Integer> versions=new java.util.WeakHashMap<>();
            public void onActivityCreated(android.app.Activity a,android.os.Bundle b){versions.put(a,Ui.themeVersion);}
            public void onActivityResumed(android.app.Activity a){Integer v=versions.get(a);if(v!=null&&v!=Ui.themeVersion){versions.put(a,Ui.themeVersion);a.recreate();}}
            public void onActivityStarted(android.app.Activity a){}public void onActivityPaused(android.app.Activity a){}public void onActivityStopped(android.app.Activity a){}public void onActivitySaveInstanceState(android.app.Activity a,android.os.Bundle b){}public void onActivityDestroyed(android.app.Activity a){versions.remove(a);}
        });
        trustAll();
    }

    public static void trustAll() {
        try {
            TrustManager[] tm = new TrustManager[]{new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String auth) { }
                @Override public void checkServerTrusted(X509Certificate[] chain, String auth) { }
                @Override public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tm, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override public boolean verify(String h, SSLSession s) { return true; }
            });
        } catch (Throwable ignored) { }
    }
}
