package com.yunying.music;
import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Intent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class CloudPlaybackProbe extends Instrumentation {
    static final String BASE="http://10.0.2.2:18083";
    int checks;
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private void main(Runnable task){
        final Throwable[] error={null};runOnMainSync(()->{try{task.run();}catch(Throwable ex){error[0]=ex;}});
        if(error[0]!=null)throw new AssertionError(error[0]);
    }
    private void await(java.util.concurrent.Callable<Boolean> condition,String message)throws Exception{
        long until=System.currentTimeMillis()+25000;final boolean[] ok={false};
        do{Thread.sleep(100);main(()->{try{ok[0]=condition.call();}catch(Exception ex){throw new RuntimeException(ex);}});}
        while(!ok[0] && System.currentTimeMillis()<until);check(ok[0],message);
    }
    private CloudAudioSource source(String path){return new CloudAudioSource(BASE+path,Aliyun.playHeaders());}
    private void failSource(String path,String message)throws Exception{
        try(CloudAudioSource source=source(path)){
            boolean failed=false;try{source.prepare();}catch(java.io.IOException ex){failed=ex.getMessage().contains(message);}
            check(failed,path+" should fail with "+message+" actual "+source.failure());
        }
    }
    private void sourceChecks()throws Exception{
        for(String path:new String[]{"/data","/redirect-data","/no-range","/unknown"}){
            try(CloudAudioSource source=source(path)){
                source.prepare();byte[] buffer=new byte[500];
                check(source.readAt(123,buffer,0,500)==500,path+" prefix read");
                for(int i=0;i<500;i++)if(buffer[i]!=(byte)((123+i)%251))throw new AssertionError("prefix mismatch");
                long before=source.bytesRead();source.readAt(123,buffer,0,500);check(before==source.bytesRead(),"cache miss");
                check(source.readAt(600123,buffer,0,500)==500,path+" tail read");
                for(int i=0;i<500;i++)if(buffer[i]!=(byte)((600123+i)%251))throw new AssertionError("tail mismatch");
                source.readAt(699999,buffer,0,1);
                check(source.getSize()==700000,path+" size");check(source.readAt(700000,buffer,0,1)==-1,"EOF");
            }
        }
        failSource("/denied","403");failSource("/rate","429");failSource("/html","错误页面");
        failSource("/bad-range","分段响应");failSource("/truncated","传输中断");
        long start=System.currentTimeMillis();failSource("/slow-header","超时");check(System.currentTimeMillis()-start<11000,"HTTP timeout not bounded");
        CloudAudioSource stalled=source("/stall");
        Thread thread=new Thread(()->{try{stalled.prepare();}catch(Exception ignored){}});thread.start();Thread.sleep(300);
        start=System.currentTimeMillis();stalled.close();thread.join(2000);
        check(!thread.isAlive() && System.currentTimeMillis()-start<2500,"cancel blocked");
    }
    private void serviceChecks()throws Exception{
        Config.aliMode=Config.MODE_OPEN;Config.aliDriveId="fixture";Config.aliAccess="fixture";Config.aliExpire=System.currentTimeMillis()+3600000;
        AtomicInteger requests=new AtomicInteger();
        Http.testTransport=(url,method,headers,body)->"{\"url\":\""+BASE+(requests.incrementAndGet()==1?"/denied":"/redirect")+"\"}";
        Entry song=new Entry("tone.mp3",false,Entry.SRC_ALI);song.id="fixture";
        main(()->PlayerService.playList(getTargetContext(),Arrays.asList(song),0));
        await(()->PlayerService.instance!=null && PlayerService.instance.getPosition()>500,"service did not play");
        main(()->{
            PlayerService s=PlayerService.instance;check(requests.get()==2,"expired URL retry count");
            check(s.getDuration()>30000,"MP3 duration missing");check(s.getError().isEmpty(),"unexpected failure");
            s.toggle();check(!s.isPlaying(),"pause failed");s.seekTo(15000);
        });
        Thread.sleep(600);
        main(()->{PlayerService s=PlayerService.instance;check(!s.isPlaying(),"seek unpaused");check(s.getPosition()>14000,"seek failed");s.toggle();});
        await(()->PlayerService.instance.getPosition()>16000,"resume failed");
        // 永久拒绝最多刷新一次，错误必须明确，不能一直 loading。
        requests.set(0);Http.testTransport=(url,method,headers,body)->{requests.incrementAndGet();return "{\"url\":\""+BASE+"/denied\"}";};
        main(()->PlayerService.instance.playAt(0));
        await(()->!PlayerService.instance.getError().isEmpty(),"403 left loading forever");
        main(()->{check(requests.get()==2,"unbounded retry");check(!PlayerService.instance.isPreparing(),"error still preparing");check(PlayerService.instance.getError().contains("403"),"HTTP error lost");});
        Config.autoLyrics=false;Config.autoCover=false;
        android.app.Activity activity=startActivitySync(new Intent(getTargetContext(),PlayerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        main(()->{
            try{
                java.lang.reflect.Field lyricField=PlayerActivity.class.getDeclaredField("lyricTip");lyricField.setAccessible(true);
                android.widget.TextView lyric=(android.widget.TextView)lyricField.get(activity);
                lyric.setText("fixture lyric");lyric.setVisibility(android.view.View.INVISIBLE);
                ((PlayerActivity)activity).onPlayerState();
                java.lang.reflect.Field statusField=PlayerActivity.class.getDeclaredField("playbackStatus");statusField.setAccessible(true);
                android.widget.TextView status=(android.widget.TextView)statusField.get(activity);
                check(status.getVisibility()==android.view.View.VISIBLE,"lyrics hid HTTP failure");
                android.graphics.Rect visible=new android.graphics.Rect();
                check(status.getGlobalVisibleRect(visible) && visible.height()>=status.getLineHeight(),"HTTP failure clipped offscreen");
                check(status.getText().toString().contains("403"),"lyrics replaced HTTP failure");
                activity.finish();
            }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
        });
        // 失败后重试；快速切歌取消旧网络连接。
        Http.testTransport=(url,method,headers,body)->"{\"url\":\""+BASE+(body.contains("stall")?"/stall":"/jitter")+"\"}";
        main(()->PlayerService.instance.toggle());
        await(()->PlayerService.instance.getPosition()>500,"retry failed");
        Entry slow=new Entry("stall.mp3",false,Entry.SRC_ALI);slow.id="stall";
        main(()->PlayerService.playList(getTargetContext(),Arrays.asList(slow,song),0));Thread.sleep(500);
        long start=System.currentTimeMillis();main(()->PlayerService.instance.playAt(1));
        await(()->PlayerService.instance.getIndex()==1 && PlayerService.instance.getPosition()>500,"stale load blocked next song");
        check(System.currentTimeMillis()-start<8000,"switch waited for stale network");
        main(()->getTargetContext().stopService(new Intent(getTargetContext(),PlayerService.class)));
        await(()->PlayerService.instance==null,"service not released");
    }
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){Bundle result=new Bundle();try{
        sourceChecks();serviceChecks();result.putString("summary","PASS: "+checks+" cloud HTTP/cache/service checks; no real account");finish(0,result);
    }catch(Throwable ex){main(()->getTargetContext().stopService(new Intent(getTargetContext(),PlayerService.class)));result.putString("error",android.util.Log.getStackTraceString(ex));finish(1,result);}
    finally{Http.testTransport=null;}}
}
