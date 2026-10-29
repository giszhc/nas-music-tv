package com.yunying.music;
import android.content.Context;
import android.net.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
/** 只在当前连接的 IPv4 本地网段探测 DSM API，不提交账号密码。 */
final class NasDiscovery {
 interface Found{void accept(String host,int port,boolean https);}
 static void scan(Context context,Found found)throws Exception{
  ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);LinkProperties properties=null;
  if(android.os.Build.VERSION.SDK_INT>=23)properties=manager.getLinkProperties(manager.getActiveNetwork());
  else for(Network network:manager.getAllNetworks()){NetworkInfo info=manager.getNetworkInfo(network);if(info!=null&&info.isConnected()){properties=manager.getLinkProperties(network);break;}}
  if(properties==null)throw new Exception("未连接局域网");
  LinkAddress local=null;for(LinkAddress address:properties.getLinkAddresses())if(address.getAddress() instanceof Inet4Address&&!address.getAddress().isLoopbackAddress()){local=address;break;}
  if(local==null)throw new Exception("当前网络没有可扫描的 IPv4 地址");
  byte[] bytes=local.getAddress().getAddress();long ip=0;for(byte b:bytes)ip=(ip<<8)|(b&255);int prefix=Math.max(24,local.getPrefixLength());long mask=(0xffffffffL<<(32-prefix))&0xffffffffL,start=ip&mask,end=start+(1L<<(32-prefix));
  ExecutorService pool=Executors.newFixedThreadPool(20);List<Future<?>> tasks=new ArrayList<>();
  try{for(long candidate=start+1;candidate<end-1;candidate++){final String host=((candidate>>24)&255)+"."+((candidate>>16)&255)+"."+((candidate>>8)&255)+"."+(candidate&255);tasks.add(pool.submit(()->{
   for(int port:new int[]{5000,5001}){if(Thread.currentThread().isInterrupted())return;HttpURLConnection connection=null;try{
    boolean secure=port==5001;URL url=new URL((secure?"https":"http")+"://"+host+":"+port+"/webapi/query.cgi?api=SYNO.API.Info&version=1&method=query&query=SYNO.API.Auth");connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(650);connection.setReadTimeout(650);connection.setInstanceFollowRedirects(false);
    try(java.io.InputStream input=connection.getInputStream()){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;while(out.size()<32768&&(n=input.read(buffer))!=-1)out.write(buffer,0,n);org.json.JSONObject result=new org.json.JSONObject(out.toString("UTF-8"));org.json.JSONObject data=result.optJSONObject("data");if(result.optBoolean("success")&&data!=null&&data.has("SYNO.API.Auth")){found.accept(host,port,secure);return;}}
   }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}}
  }));}for(Future<?> task:tasks)task.get();}finally{pool.shutdownNow();}
 }
}
