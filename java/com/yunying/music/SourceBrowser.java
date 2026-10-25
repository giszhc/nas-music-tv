package com.yunying.music;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import android.os.Handler;
import java.util.*;
import java.util.concurrent.*;

/** 仅从设置进入的音乐目录管理器，浏览结果不会占用首页。 */
final class SourceBrowser {
    private final Activity activity;
    private final Dialog dialog;
    private final Handler ui=new Handler();
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final List<Entry> entries=new ArrayList<>();
    private final List<Entry> stack=new ArrayList<>();
    private final Runnable onChanged;
    private FrameLayout rows;private TvVirtualList directoryList;private final String moduleId;
    private TextView path,status,add;
    private TextView sourceNas,sourceAli,musicMode,bookMode;
    private String source=Entry.SRC_NAS;
    private int request;
    private boolean closed,loading,books,importing;
    static void show(Activity activity,Runnable changed){new SourceBrowser(activity,null,changed).show();}
    static void show(Activity activity,String moduleId,Runnable changed){new SourceBrowser(activity,moduleId,changed).show();}
    private SourceBrowser(Activity activity,String moduleId,Runnable changed){this.moduleId=moduleId;HomeModules.Module module=moduleId==null?null:HomeModules.get(activity,moduleId);books=module!=null&&module.spoken;this.activity=activity;this.onChanged=changed;this.dialog=new Dialog(activity);}
    private TextView button(String text,View.OnClickListener click) {
        TextView view=Ui.text(activity,text,14,Ui.TEXT,false);view.setPadding(Ui.dp(activity,16),Ui.dp(activity,10),Ui.dp(activity,16),Ui.dp(activity,10));view.setId(View.generateViewId());view.setFocusable(true);view.setClickable(true);view.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));view.setBackground(Ui.cardFocusable(activity,12));view.setOnClickListener(click);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=Ui.dp(activity,8);view.setLayoutParams(lp);return view;
    }
    private void show() {
        dialog.requestWindowFeature(1);
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(1);panel.setPadding(Ui.dp(activity,24),Ui.dp(activity,20),Ui.dp(activity,24),Ui.dp(activity,20));panel.setBackground(Ui.round(Ui.BG_TOP,activity,16));panel.setClipChildren(false);panel.setClipToPadding(false);
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=Ui.text(activity,"添加音乐目录",23,Ui.TEXT,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));panel.addView(header);
        LinearLayout sources=new LinearLayout(activity);sources.setPadding(0,Ui.dp(activity,14),0,Ui.dp(activity,10));sourceNas=button("群晖 NAS",v->{if(importing)return;source=Entry.SRC_NAS;stack.clear();load();});sourceAli=button("阿里云盘",v->{if(importing)return;source=Entry.SRC_ALI;stack.clear();load();});sources.addView(sourceNas);sources.addView(sourceAli);musicMode=button("导入音乐",v->{if(importing)return;books=false;add.setText("导入当前目录及子目录");status.setText("当前导入到播放列表");updateTabs();});bookMode=button("导入听书",v->{if(importing)return;books=true;add.setText("导入当前目录及子目录");status.setText("当前导入到听书馆");updateTabs();});sources.addView(musicMode);sources.addView(bookMode);panel.addView(sources);
        path=Ui.text(activity,"",13,Ui.TEXT2,false);path.setSingleLine(true);path.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);panel.addView(path);
        rows=new FrameLayout(activity);panel.addView(rows,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout footer=new LinearLayout(activity);footer.setGravity(Gravity.CENTER_VERTICAL);footer.setClipChildren(false);footer.setClipToPadding(false);footer.setPadding(0,Ui.dp(activity,12),0,0);status=Ui.text(activity,"",12,Ui.TEXT2,false);status.setMaxLines(2);footer.addView(status,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));panel.addView(footer);
        add=button("导入当前目录及子目录",v->importDirectory());add.setSingleLine(true);add.setEllipsize(android.text.TextUtils.TruncateAt.END);footer.addView(add,new LinearLayout.LayoutParams(Ui.dp(activity,240),Ui.dp(activity,48)));
        add.setOnKeyListener((v,key,event)->{if((key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_LEFT)&&directoryList!=null&&!entries.isEmpty()){if(event.getAction()==KeyEvent.ACTION_DOWN)directoryList.focusPosition(directoryList.position());return true;}return false;});
        if(moduleId!=null){title.setText("导入内容 · "+HomeModules.name(activity,moduleId));title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);musicMode.setVisibility(View.GONE);bookMode.setVisibility(View.GONE);add.setText("导入到“"+HomeModules.name(activity,moduleId)+"”");}
        dialog.setContentView(panel);
        dialog.setOnDismissListener(d->{closed=true;request++;ui.removeCallbacksAndMessages(null);io.shutdownNow();});
        dialog.setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP&&!stack.isEmpty()){back();return true;}return false;});
        dialog.show();if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setDimAmount(.15f);dialog.getWindow().setLayout((int)(activity.getResources().getDisplayMetrics().widthPixels*.82),(int)(activity.getResources().getDisplayMetrics().heightPixels*.88));}
        if(!Config.nasReady()&&Config.aliReady())source=Entry.SRC_ALI;
        Ui.installFocusMotion(panel);Ui.reveal(panel);load();
    }
    private void updateTabs(){TextView[] tabs={sourceNas,sourceAli,musicMode,bookMode};boolean[] selected={Entry.SRC_NAS.equals(source),Entry.SRC_ALI.equals(source),!books,books};for(int i=0;i<tabs.length;i++){tabs[i].setSelected(selected[i]);tabs[i].setBackground(Ui.tabBackground(activity));}}
    private int importEntries(List<Entry> items){return moduleId!=null?HomeModules.add(activity,moduleId,items):books?MusicLibrary.addToBooks(activity,items):MusicLibrary.addToPlaylist(activity,items);}
    private void importDirectory(){
        if(loading||importing||closed)return;
        final List<Entry> initial=new ArrayList<>(entries);
        final int ticket=++request;importing=true;
        add.setEnabled(false);add.setAlpha(.45f);status.setText("正在扫描当前目录及所有子目录…");
        io.execute(()->{
            try {
                DirectoryImport.Result result=DirectoryImport.scan(initial,
                    directory->Entry.SRC_NAS.equals(directory.source)?Synology.list(directory.path):Aliyun.list(directory.id),
                    (directories,songs)->ui.post(()->{if(!closed&&ticket==request)status.setText("已扫描 "+directories+" 个子目录，找到 "+songs+" 首音频…");}));
                ui.post(()->{
                    if(closed||ticket!=request)return;
                    int count=importEntries(result.songs);importing=false;add.setEnabled(true);add.setAlpha(1f);
                    status.setText("已添加 "+count+" 首，跳过重复 "+(result.songs.size()-count)+" 首"+
                        (result.failedDirectories>0?"；"+result.failedDirectories+" 个子目录读取失败，可重试":"（包含子目录）"));
                    if(onChanged!=null)onChanged.run();
                });
            }catch(InterruptedException ignored){Thread.currentThread().interrupt();}
        });
    }
    private void back(){if(importing){dialog.dismiss();return;}if(stack.isEmpty()){dialog.dismiss();return;}stack.remove(stack.size()-1);load();}
    private void load() {
        if(importing)return;
        final int ticket=++request;final String selected=source;
        final String location=stack.isEmpty()?(Entry.SRC_NAS.equals(source)?null:"root"):(Entry.SRC_NAS.equals(source)?stack.get(stack.size()-1).path:stack.get(stack.size()-1).id);
        boolean restoreRows=rows.hasFocus();directoryList=null;loading=true;entries.clear();rows.removeAllViews();add.setEnabled(false);add.setAlpha(.45f);
        updateTabs();status.setText("正在读取目录…");
        StringBuilder crumb=new StringBuilder(Entry.SRC_NAS.equals(source)?"群晖 NAS":"阿里云盘");for(Entry e:stack)crumb.append("  /  ").append(e.name);path.setText(crumb);
        if(Entry.SRC_NAS.equals(source)?!Config.nasReady():!Config.aliReady()){loading=false;status.setText("请先在设置中完成连接或登录");return;}
        io.execute(()->{
            List<Entry> result=null;String error=null;
            try {if(Entry.SRC_NAS.equals(selected)){if(Config.nasSid.isEmpty())Synology.login();result=location==null?Synology.listShares():Synology.list(location);}else{Aliyun.ensureToken();result=Aliyun.list(location);}}
            catch(Exception ex){error=ex.getMessage()==null?"读取失败":ex.getMessage();}
            final List<Entry> data=result;final String message=error;
            ui.post(()->{if(closed||ticket!=request)return;loading=false;if(message!=null){status.setText("读取失败："+message+"，可重新选择来源重试");return;}
                for(Entry e:data)if(e.dir||e.isAudio())entries.add(e);
                Collections.sort(entries,(a,b)->a.dir!=b.dir?(a.dir?-1:1):a.name.compareToIgnoreCase(b.name));
                int songs=0;
                for(Entry e:entries)if(e.isAudio())songs++;
                directoryList=new TvVirtualList(activity,entries.size(),1,44,8,(i,recycled)->{
                    Entry e=entries.get(i);TextView row=recycled instanceof TextView?(TextView)recycled:Ui.text(activity,"",14,Ui.TEXT,false);row.setText((e.dir?"打开目录  ":"音频  ")+e.title());row.setSingleLine(true);row.setEllipsize(android.text.TextUtils.TruncateAt.END);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(activity,16),0,Ui.dp(activity,16),0);return row;
                });
                rows.addView(directoryList,new FrameLayout.LayoutParams(-1,-1));
                directoryList.up(()->sourceNas.requestFocus()).right(()->{if(add.isEnabled())add.requestFocus();}).clicked(i->{if(importing)return;Entry e=entries.get(i);if(e.dir){stack.add(e);load();}else{int count=importEntries(Collections.singletonList(e));status.setText(count==0?"内容已存在":"已添加到"+(moduleId!=null?HomeModules.name(activity,moduleId):books?"听书馆":"播放列表"));if(onChanged!=null)onChanged.run();}});
                boolean canImport=!entries.isEmpty();status.setText(canImport?songs+" 首音频 · 导入将包含所有子目录":"当前目录为空");add.setEnabled(canImport);add.setAlpha(canImport?1f:.45f);
                // 末行按下进入固定操作栏，其他行始终沿数据位置继续向下。
                directoryList.bottom(()->{if(add.isEnabled())add.requestFocus();});
                if(restoreRows&&!entries.isEmpty())directoryList.focusPosition(0);

            });
        });
    }
}
