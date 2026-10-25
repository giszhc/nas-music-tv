package com.yunying.music;

import java.util.*;

/** 迭代扫描子目录，按来源和资源标识去重，避免重复目录或循环链接。 */
final class DirectoryImport {
    interface Listing { List<Entry> children(Entry directory) throws Exception; }
    interface Progress { void update(int directories,int songs); }
    static final class Result {
        final List<Entry> songs=new ArrayList<>();
        int failedDirectories;
    }
    static Result scan(List<Entry> initial,Listing listing,Progress progress) throws InterruptedException {
        Result result=new Result();
        Deque<Entry> pending=new ArrayDeque<>(initial);
        Set<String> visited=new HashSet<>();int directories=0;
        while(!pending.isEmpty()) {
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            Entry entry=pending.removeFirst();
            String identity=Entry.SRC_ALI.equals(entry.source)?entry.id:entry.path;
            if(identity.isEmpty()) {if(entry.dir)result.failedDirectories++;continue;}
            if(!visited.add(entry.source+":"+identity))continue;
            if(entry.dir) {
                try {pending.addAll(listing.children(entry));}
                catch(InterruptedException ex){throw ex;}
                catch(Exception ex){result.failedDirectories++;}
                directories++;
                progress.update(directories,result.songs.size());
            }else if(entry.isAudio())result.songs.add(entry);
        }
        progress.update(directories,result.songs.size());
        return result;
    }
}
