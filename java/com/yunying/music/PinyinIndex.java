package com.yunying.music;

import android.content.Context;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** 离线首字母/全拼搜索，支持多音字，兼容 Android 5。 */
final class PinyinIndex {
    private final String[][] readings=new String[65536][];
    private final LinkedHashMap<String,String> cache=new LinkedHashMap<String,String>(128,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,String> entry){return size()>512;}};
    PinyinIndex(Context context){try(BufferedReader input=new BufferedReader(new InputStreamReader(new GZIPInputStream(context.getResources().openRawResource(R.raw.pinyin_table)),"UTF-8"))){String line;while((line=input.readLine())!=null){String[] parts=line.split("\t");if(parts.length==2)readings[Integer.parseInt(parts[0],16)]=parts[1].split(",");}}catch(Exception ignored){}}
    boolean matches(String text,String query){
        String normalized=query.toLowerCase(Locale.ROOT).replace(" ","");if(normalized.isEmpty()||text.toLowerCase(Locale.ROOT).contains(normalized))return true;
        String index=cache.get(text);if(index==null){index=build(text);cache.put(text,index);}return index.contains(normalized);
    }
    private String build(String text){
        LinkedHashSet<String> full=new LinkedHashSet<>(),initials=new LinkedHashSet<>();full.add("");initials.add("");
        for(char raw:text.toCharArray()){
            if(Character.isWhitespace(raw)||"-·_()（）".indexOf(raw)>=0)continue;
            String[] options=readings[raw];if(options==null)options=new String[]{String.valueOf(Character.toLowerCase(raw))};
            full=append(full,options,false);initials=append(initials,options,true);
        }
        StringBuilder out=new StringBuilder();for(String value:full)out.append(value).append('|');for(String value:initials)out.append(value).append('|');return out.toString();
    }
    private LinkedHashSet<String> append(Set<String> previous,String[] options,boolean initials){LinkedHashSet<String> result=new LinkedHashSet<>();for(String before:previous)for(String option:options){if(option.isEmpty())continue;result.add(before+(initials?option.substring(0,1):option));if(result.size()>=32)return result;}return result;}
}
