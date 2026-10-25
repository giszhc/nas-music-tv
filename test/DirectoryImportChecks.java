package com.yunying.music;
import java.util.*;
public class DirectoryImportChecks {
    static Entry item(String source,String id,boolean dir){Entry e=new Entry(dir?id:id+".mp3",dir,source);e.path=id;e.id=id;return e;}
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args)throws Exception {
        for(String source:new String[]{Entry.SRC_NAS,Entry.SRC_ALI}) {
            Entry root=item(source,"root",true),nested=item(source,"nested",true),deep=item(source,"deep",true),bad=item(source,"bad",true);
            Entry song=item(source,"song",false),leaf=item(source,"leaf",false),text=new Entry("note.txt",false,source);text.id="text";text.path="text";
            DirectoryImport.Result result=DirectoryImport.scan(Arrays.asList(root),e->{
                if(e==root)return Arrays.asList(nested,bad);
                if(e==nested)return Arrays.asList(deep,song,root);
                if(e==deep)return Arrays.asList(leaf,song,text);
                throw new Exception("offline");
            },(dirs,songs)->{});
            check(result.songs.size()==2&&result.failedDirectories==1);
            check(DirectoryImport.scan(Collections.emptyList(),e->Collections.emptyList(),(d,n)->{}).songs.isEmpty());
        }
        Thread.currentThread().interrupt();
        try {DirectoryImport.scan(Arrays.asList(item("nas","cancel",true)),e->Collections.emptyList(),(d,n)->{});throw new AssertionError();}
        catch(InterruptedException expected){Thread.interrupted();}
        System.out.println("PASS: NAS/云盘多层级、仅含目录、重复/循环、非音频、部分失败、空目录和取消");
    }
}
