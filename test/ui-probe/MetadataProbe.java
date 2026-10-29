package com.yunying.music.test;
import android.app.Instrumentation;
import android.os.Bundle;
import android.content.Context;
import com.yunying.music.*;
public class MetadataProbe extends Instrumentation {
 public void onCreate(Bundle args){super.onCreate(args);start();}
 public void onStart(){Bundle result=new Bundle();Context context=getTargetContext();android.content.SharedPreferences prefs=context.getSharedPreferences("music_library",0);java.util.Set<String> before=new java.util.HashSet<>(prefs.getAll().keySet());
  try{
   Entry entry=new Entry("wrong artist - wrong song.flac",false,Entry.SRC_NAS);entry.path="/fixtures/correction-only.flac";
   MusicLibrary.correctMetadata(context,entry,"晴天.现场","周杰伦");
   OnlineMeta.Meta auto=new OnlineMeta.Meta();auto.title="错误匹配";auto.artist="错误歌手";MusicLibrary.metadata(context,entry,auto);
   if(!MusicLibrary.songTitle(context,entry).equals("晴天.现场")||!MusicLibrary.artist(context,entry).equals("周杰伦"))throw new AssertionError("Online result overwrote correction");
   String[] match=OnlineMeta.splitName(MusicLibrary.matchName(context,entry));if(!match[0].equals("晴天.现场")||!match[1].equals("周杰伦"))throw new AssertionError("Corrected query not used");
   Entry restored=new Entry(entry.name,false,entry.source);restored.path=entry.path;
   if(!MusicLibrary.songTitle(context,restored).equals("晴天.现场"))throw new AssertionError("Correction not persistent");
   if(!entry.path.equals("/fixtures/correction-only.flac")||!entry.name.startsWith("wrong"))throw new AssertionError("Source identity modified");
   Entry untouched=new Entry("原歌手 - 原歌曲.mp3",false,Entry.SRC_NAS);untouched.path="/fixtures/cover-only.mp3";
   MusicLibrary.metadata(context,untouched,auto,false);
   if(!MusicLibrary.songTitle(context,untouched).equals("原歌曲")||!MusicLibrary.artist(context,untouched).equals("原歌手"))throw new AssertionError("Cover rematch changed names");
   result.putString("summary","PASS: 更正歌曲/歌手持久化、在线匹配覆盖保护、更正查询和源文件标识保持");finish(0,result);
  }catch(Throwable ex){result.putString("error",ex.toString());finish(1,result);}
  finally{android.content.SharedPreferences.Editor editor=prefs.edit();for(String key:prefs.getAll().keySet())if(!before.contains(key))editor.remove(key);editor.commit();}
 }
}
