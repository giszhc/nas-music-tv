package com.yunying.music;

import android.annotation.TargetApi;
import android.media.MediaDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/** 云盘 HTTP 读取层：系统仍负责解码，连接/重定向/分段缓存由应用控制。 */
@TargetApi(23)
final class CloudAudioSource extends MediaDataSource {
    static final int BLOCK=64*1024;
    private final String url;
    private final Map<String,String> headers;
    private final LinkedHashMap<Long,byte[]> cache=new LinkedHashMap<Long,byte[]>(32,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,byte[]> e){return size()>32;}
    };
    private volatile HttpURLConnection connection;
    private volatile InputStream input;
    private volatile boolean closed;
    private long size=-1,networkPosition;
    private volatile String failure="";
    private volatile long bytesRead;
    private final long started=System.nanoTime();
    private volatile long firstByteMs=-1;

    CloudAudioSource(String url,Map<String,String> headers){this.url=url;this.headers=new java.util.HashMap<>(headers);}
    void prepare()throws IOException{readAt(0,new byte[1],0,1);}
    String failure(){return failure;}
    long bytesRead(){return bytesRead;}
    long firstByteMs(){return firstByteMs;}
    @Override public synchronized long getSize(){return size;}

    @Override public synchronized int readAt(long position,byte[] buffer,int offset,int count)throws IOException {
        if(position<0 || offset<0 || count<0 || offset>buffer.length-count)throw new IllegalArgumentException();
        if(count==0)return 0;
        ensureOpen();
        if(size>=0 && position>=size)return -1;
        long blockStart=position/BLOCK*BLOCK;
        try {
            byte[] block=cache.get(blockStart);
            if(block==null){
                if(input==null || networkPosition!=blockStart)connect(blockStart);
                int length=size>=0?(int)Math.min(BLOCK,size-blockStart):BLOCK;
                byte[] data=new byte[length];int read=0;
                long deadline=System.nanoTime()+15000000000L;
                InputStream stream=input;
                while(read<length){
                    ensureOpen();
                    if(System.nanoTime()>deadline)throw new java.net.SocketTimeoutException();
                    int got=stream.read(data,read,length-read);
                    if(got<0){if(size>=0 && blockStart+read<size)throw new IOException("音频传输中断，请重试");size=blockStart+read;break;}
                    if(got==0)continue;
                    if(firstByteMs<0)firstByteMs=(System.nanoTime()-started)/1000000;
                    read+=got;bytesRead+=got;networkPosition+=got;
                }
                if(read==0)return -1;
                block=read==data.length?data:java.util.Arrays.copyOf(data,read);
                cache.put(blockStart,block);
            }
            int inBlock=(int)(position-blockStart);
            if(inBlock>=block.length)return -1;
            int copied=Math.min(count,block.length-inBlock);
            System.arraycopy(block,inBlock,buffer,offset,copied);
            return copied;
        }catch(IOException ex){
            failure=describe(ex);disconnect();throw new IOException(failure);
        }
    }

    private void connect(long start)throws IOException {
        disconnect();
        URL target=new URL(url);
        for(int redirect=0;redirect<6;redirect++){
            ensureOpen();
            if(!"https".equals(target.getProtocol()) && !"http".equals(target.getProtocol()))throw new IOException("播放地址协议无效");
            HttpURLConnection c=(HttpURLConnection)target.openConnection();
            connection=c;
            c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent",Http.UA);
            c.setRequestProperty("Accept-Encoding","identity");
            for(Map.Entry<String,String> h:headers.entrySet())c.setRequestProperty(h.getKey(),h.getValue());
            c.setRequestProperty("Range","bytes="+start+"-");
            int code=c.getResponseCode();
            if(code==301 || code==302 || code==303 || code==307 || code==308){
                String location=c.getHeaderField("Location");
                disconnect();
                if(location==null || location.isEmpty())throw new IOException("云盘跳转未返回地址");
                target=new URL(target,location);continue;
            }
            if(code==401 || code==403)throw new IOException("云盘直链不可访问（HTTP "+code+"）");
            if(code==429)throw new IOException("云盘请求过于频繁（HTTP 429）");
            if(code!=200 && code!=206)throw new IOException("音频下载失败（HTTP "+code+"）");
            String type=c.getContentType();
            if(type!=null && (type.toLowerCase(java.util.Locale.ROOT).contains("text/") || type.contains("json") || type.contains("xml")))
                throw new IOException("播放地址返回了错误页面，没有返回音频");
            long total=-1;
            if(code==206){
                String range=c.getHeaderField("Content-Range");
                try{
                    java.util.regex.Matcher m=java.util.regex.Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+|\\*)").matcher(range==null?"":range);
                    if(!m.matches() || Long.parseLong(m.group(1))!=start)throw new IllegalArgumentException();
                    if(!"*".equals(m.group(3)))total=Long.parseLong(m.group(3));
                }catch(RuntimeException ex){throw new IOException("云盘分段响应不正确");}
            }else try{total=Long.parseLong(c.getHeaderField("Content-Length"));}catch(RuntimeException ignored){}
            if(total>=0){if(size>=0 && size!=total)throw new IOException("音频文件大小发生变化，请重新播放");size=total;}
            input=c.getInputStream();networkPosition=start;
            // 少数端点忽略 Range；限时跳过前缀，仍能读取小型音频，不能静默把文件开头当成尾部。
            if(code==200 && start>0){
                long remaining=start,deadline=System.nanoTime()+15000000000L;byte[] skip=new byte[16384];
                while(remaining>0){
                    ensureOpen();if(System.nanoTime()>deadline)throw new java.net.SocketTimeoutException();
                    int got=input.read(skip,0,(int)Math.min(skip.length,remaining));
                    if(got<0)throw new IOException("音频分段读取提前结束");remaining-=got;bytesRead+=got;
                }
            }
            return;
        }
        throw new IOException("云盘播放地址跳转过多");
    }
    private void ensureOpen()throws IOException{if(closed || Thread.currentThread().isInterrupted())throw new InterruptedIOException("播放已取消");}
    private static String describe(IOException ex){
        if(ex instanceof java.net.SocketTimeoutException)return "云盘音频连接或读取超时，请检查电视网络";
        if(ex instanceof java.io.EOFException || ex instanceof java.net.ProtocolException)return "音频传输中断，请重试";
        if(ex instanceof java.net.UnknownHostException)return "无法解析云盘下载域名，请检查电视 DNS";
        if(ex instanceof javax.net.ssl.SSLException)return "云盘音频 HTTPS 连接失败";
        String message=ex.getMessage();
        // 系统异常可能携带签名 URL，只有本类生成的中文消息可以显示/记录。
        return message!=null && (message.startsWith("云盘") || message.startsWith("音频") || message.startsWith("播放"))?message:"云盘音频网络读取失败";
    }
    private void disconnect(){
        HttpURLConnection c=connection;connection=null;
        input=null;
        // disconnect already owns response-body cleanup. Closing the same stream
        // concurrently with readAt can corrupt Android Okio timeout state.
        if(c!=null)try{c.disconnect();}catch(RuntimeException ignored){}
    }
    @Override public void close(){closed=true;disconnect();}
}
