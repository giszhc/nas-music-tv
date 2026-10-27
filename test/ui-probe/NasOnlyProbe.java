package com.yunying.music.test;
import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import com.yunying.music.*;
import java.util.*;

/** 在隔离探针应用中验证覆盖升级，不触碰正式应用数据。 */
public class NasOnlyProbe extends Instrumentation {
    private int checks;
    private void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    public void onCreate(Bundle args){super.onCreate(args);start();}
    public void onStart(){Bundle result=new Bundle();try{
        Context c=getTargetContext();
        SharedPreferences config=c.getSharedPreferences("yy_music",0);
        config.edit().clear().putString("nasHost","fixture-nas").putString("nasUser","fixture-user")
            .putString("nasPass","fixture-pass").putString("aliRefresh","legacy-token").putString("aliPrivateKey","legacy-key").commit();
        Config.load(c);Config.save(c);
        check(Config.nasReady()&&Config.nasHost.equals("fixture-nas"),"NAS config retained");
        check(config.getString("nasPass","").equals("fixture-pass"),"NAS password retained");
        check(!config.contains("aliRefresh")&&!config.contains("aliPrivateKey"),"legacy credentials removed");
        String mixed="[{\"name\":\"cloud.mp3\",\"source\":\"ali\",\"id\":\"cloud-id\"},{\"name\":\"nas.mp3\",\"source\":\"nas\",\"path\":\"/music/nas.mp3\"}]";
        SharedPreferences library=c.getSharedPreferences("music_library",0);library.edit().clear().commit();
        Entry nas=new Entry("nas.mp3",false,Entry.SRC_NAS);nas.path="/music/nas.mp3";
        MusicLibrary.correctMetadata(c,nas,"保留的歌名","保留的歌手");
        for(String name:new String[]{"playlist","queue","favorites","recent","books","bookRecent","catalog","module_custom"}){
            library.edit().putString(name,mixed).commit();
            List<Entry> entries=MusicLibrary.read(c,name);
            check(entries.size()==1&&entries.get(0).path.equals(nas.path),name+" filters removed sources");
        }
        check(MusicLibrary.favorite(c,nas),"NAS favorite retained");
        check(MusicLibrary.songTitle(c,nas).equals("保留的歌名"),"NAS metadata identity retained");
        library.edit().putString("queue","[{\"name\":\"cloud.mp3\",\"source\":\"ali\"}]").commit();
        check(MusicLibrary.queue(c).isEmpty(),"cloud-only queue empty");
        library.edit().putString("catalog","[{\"name\":\"old.mp3\",\"path\":\"/music/old.mp3\"}]").commit();
        check(MusicLibrary.read(c,"catalog").size()==1,"legacy NAS defaults retained");
        result.putString("summary","PASS: "+checks+" NAS-only upgrade checks");finish(0,result);
    }catch(Throwable ex){result.putString("error",ex.toString());finish(1,result);}}
}
