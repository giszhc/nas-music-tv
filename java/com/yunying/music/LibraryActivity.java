package com.yunying.music;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** 与电视参考图一致的列表/封面、页签、焦点和底部渐隐。 */
public class LibraryActivity extends Activity {
    private final List<Entry> tracks=new ArrayList<>();
    private final List<TextView> tabs=new ArrayList<>();
    private final Handler ui=new Handler();
    private final ExecutorService io=Executors.newFixedThreadPool(2);
    private FrameLayout root,content;
    private TextView heading,hint,placeholder;
    private ImageView cover;
    private SettingsPanel settings;
    private int collection,tab,request;
    private boolean editing,loaded;
    private String group="",selectedKey="";
    private int category;private String moduleId;private TvVirtualList songList;private android.os.Parcelable listState;
    protected int defaultCollection(){return 3;}
    @Override public void onCreate(Bundle state){super.onCreate(state);Ui.immersive(this);Config.load(this);moduleId=getIntent().getStringExtra("moduleId");collection=getIntent().getIntExtra("collection",defaultCollection());category=getIntent().getIntExtra("category",0);tab=Math.max(0,Math.min(2,category));group=getIntent().getStringExtra("group");if(group==null)group="";reload();build();render(false);loaded=true;tabs.get(tab).requestFocus();}
    @Override public void onResume(){super.onResume();if(loaded){boolean focused=content!=null&&content.findFocus()!=null;reload();render(focused);}}
    @Override public void onPause(){if(songList!=null)listState=songList.snapshot();super.onPause();}
    private void reload(){tracks.clear();tracks.addAll(moduleId!=null?HomeModules.entries(this,moduleId):(collection==0||collection==6)?MusicLibrary.playlist(this):MusicLibrary.read(this,collection==3?"favorites":collection==4?"recent":collection==8?"books":collection==12?"bookRecent":collection==9?"purchased":collection==10?"purchasedBooks":"catalog"));}
    private String title(){if(moduleId!=null)return HomeModules.name(this,moduleId);if(!group.isEmpty())return group;return collection==0?"播放列表":collection==3?"我喜欢":collection==4?"最近播放":collection==8?"听书":collection==12?"收听历史":collection==9?"已购歌曲":collection==10?"已购听书":collection==6?"我的歌单 · 播放列表":collection==11?"儿童专区":tab==1?"歌手":tab==2?"专辑":"音乐";}
    private void build(){
        if(collection==11){java.util.Iterator<Entry> iterator=tracks.iterator();while(iterator.hasNext()){Entry e=iterator.next();if(!(e.path+e.name).matches(".*(儿童|儿歌|宝宝|童谣|kids).*"))iterator.remove();}}
        root=new FrameLayout(this);root.setBackgroundColor(Ui.BG_TOP);setContentView(root);
        heading=Ui.text(this,title(),29,Ui.TEXT,false);heading.setSingleLine(true);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.place(root,heading,48,40,520,44);
        hint=Ui.text(this,collection==3?"按菜单键编辑":"更多",16,Ui.TEXT2,false);hint.setGravity(Gravity.CENTER);hint.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);hint.setFocusable(true);hint.setClickable(true);hint.setBackground(Ui.tabBackground(this));hint.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT2));hint.setOnClickListener(v->menu());Ui.place(root,hint,740,43,172,40);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);Ui.place(root,bar,46,98,560,44);
        for(int i=0;i<3;i++){final int index=i;TextView t=Ui.text(this,"",20,Ui.TEXT,false);t.setGravity(Gravity.CENTER);t.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);t.setFocusable(true);t.setClickable(true);t.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT2));t.setBackground(Ui.tabBackground(this));t.setOnClickListener(v->{tab=index;group="";editing=false;listState=null;songList=null;render(false);t.requestFocus();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-1);lp.rightMargin=Ui.dp(this,14);bar.addView(t,lp);tabs.add(t);}
        content=new FrameLayout(this);content.setClipChildren(true);Ui.place(root,content,0,156,960,Math.max(30,Ui.height(this)-156));Ui.installFocusMotion(root);
    }
    private LinkedHashMap<String,List<Entry>> groups(int kind){LinkedHashMap<String,List<Entry>> map=new LinkedHashMap<>();for(Entry e:tracks){String name=kind==1?MusicLibrary.artist(this,e):MusicLibrary.album(this,e);if(kind==1&&"未知歌手".equals(name))continue;if(!map.containsKey(name))map.put(name,new ArrayList<>());map.get(name).add(e);}return map;}
    private void render(boolean focusSong){
        request++;content.removeAllViews();heading.setText(editing?"我喜欢 · 编辑":title());
        String[] labels={"单曲 ("+tracks.size()+")","歌手 ("+groups(1).size()+")","专辑 ("+groups(2).size()+")"};
        for(int i=0;i<3;i++){tabs.get(i).setText(labels[i]);tabs.get(i).setSelected(i==tab);tabs.get(i).setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,i==tab?Ui.TEXT:Ui.TEXT2));}
        if(tab==0||!group.isEmpty())songs(group.isEmpty()?tracks:groups(tab).get(group),focusSong);else grid();
        Ui.installFocusMotion(content);Ui.reveal(content);
    }
    private void songs(List<Entry> data,boolean focusSong){
        final List<Entry> queue=data==null?new ArrayList<>():new ArrayList<>(data);
        TvFadeFrame viewport=new TvFadeFrame(this);Ui.place(content,viewport,36,0,438,Math.max(30,Ui.height(this)-156));
        FrameLayout art=new FrameLayout(this);art.setBackground(Ui.round(Ui.PANEL,this,10));art.setClipToOutline(true);Ui.place(content,art,565,4,272,272);
        cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);art.addView(cover,new FrameLayout.LayoutParams(-1,-1));
        placeholder=Ui.text(this,"选择歌曲",22,Ui.TEXT2,false);placeholder.setGravity(Gravity.CENTER);art.addView(placeholder,new FrameLayout.LayoutParams(-1,-1));
        if(queue.isEmpty()){TextView empty=Ui.text(this,collection==3?"还没有喜欢的歌曲":collection==9||collection==10?"暂无已购内容":collection==8||collection==12?"暂无听书内容\n在设置的音乐目录中添加听书":"还没有歌曲\n按菜单键进入设置添加音乐",17,Ui.TEXT2,false);empty.setPadding(0,Ui.dp(this,35),0,0);viewport.addView(empty);return;}
        Entry initial=queue.get(0);for(Entry e:queue)if(MusicLibrary.key(e).equals(selectedKey))initial=e;preview(initial);
        int initialIndex=queue.indexOf(initial);
        songList=new TvVirtualList(this,queue.size(),1,72,12,(i,recycled)->songCard(this,queue.get(i),false,recycled));
        viewport.addView(songList,new FrameLayout.LayoutParams(-1,-1));
        songList.selected(i->{selectedKey=MusicLibrary.key(queue.get(i));preview(queue.get(i));}).up(()->tabs.get(tab).requestFocus()).clicked(i->{
            if(editing&&collection==3){listState=null;MusicLibrary.toggleFavorite(this,queue.get(i));reload();render(true);}else play(queue,i);
        });
        if(focusSong){if(listState!=null)songList.restore(listState);else songList.focusPosition(initialIndex);}
    }

    public static LinearLayout songCard(android.content.Context context,Entry e,boolean current,View recycled){
        if(!(recycled instanceof LinearLayout)||recycled.findViewWithTag("song_title")==null)return songCard(context,e,current);
        LinearLayout row=(LinearLayout)recycled;bindSong(row,context,e);return row;
    }
    public static LinearLayout songCard(android.content.Context context,Entry e,boolean current){
        LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setFocusable(true);card.setClickable(true);card.setBackground(Ui.cardFocusable(context,12));card.setPadding(Ui.dp(context,20),Ui.dp(context,10),Ui.dp(context,20),Ui.dp(context,10));
        LinearLayout text=new LinearLayout(context);text.setOrientation(LinearLayout.VERTICAL);card.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        TextView name=Ui.text(context,"",17,Ui.TEXT,false);name.setTag("song_title");name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);text.addView(name,new LinearLayout.LayoutParams(-1,-2));
        TextView meta=Ui.text(context,"",14,Ui.TEXT2,false);meta.setTag("song_meta");meta.setSingleLine(true);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=Ui.dp(context,5);text.addView(meta,lp);
        IconTextView mark=new IconTextView(context,"nowPlaying");mark.setTag("song_playing");mark.setText("当前播放");mark.setTextColor(Ui.TEXT);mark.setIconFraction(.85f);LinearLayout.LayoutParams markLp=new LinearLayout.LayoutParams(Ui.dp(context,28),Ui.dp(context,28));markLp.leftMargin=Ui.dp(context,12);card.addView(mark,markLp);
        bindSong(card,context,e);return card;
    }
    private static void bindSong(LinearLayout card,android.content.Context context,Entry e){
        ((TextView)card.findViewWithTag("song_title")).setText(MusicLibrary.songTitle(context,e));String ext=quality(e);
        android.text.SpannableString formatted=new android.text.SpannableString(ext+"  "+MusicLibrary.artist(context,e)+" - "+MusicLibrary.album(context,e));formatted.setSpan(new QualitySpan("SQ".equals(ext)),0,ext.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);((TextView)card.findViewWithTag("song_meta")).setText(formatted);
        ((IconTextView)card.findViewWithTag("song_playing")).bindTrack(e);
    }
    private static final class QualitySpan extends android.text.style.ReplacementSpan {
        private final boolean lossless;QualitySpan(boolean lossless){this.lossless=lossless;}
        @Override public int getSize(android.graphics.Paint p,CharSequence text,int start,int end,android.graphics.Paint.FontMetricsInt fm){android.graphics.Paint small=new android.graphics.Paint(p);small.setTextSize(p.getTextSize()*.64f);return (int)(small.measureText(text,start,end)+p.getTextSize()*.3f);}
        @Override public void draw(android.graphics.Canvas canvas,CharSequence text,int start,int end,float x,int top,int y,int bottom,android.graphics.Paint original){android.graphics.Paint p=new android.graphics.Paint(original);float size=p.getTextSize();p.setTextSize(size*.64f);float width=getSize(original,text,start,end,null),height=size*.72f,baseline=y-size*.09f;p.setColor(lossless?0xFFE6CF92:0xFFBFC0C1);canvas.drawRoundRect(x,baseline-height,x+width,baseline+size*.06f,size*.04f,size*.04f,p);p.setColor(0xFF494741);canvas.drawText(text,start,end,x+size*.15f,baseline-size*.04f,p);}
    }
    public static String quality(Entry e){String n=e.name.toLowerCase(java.util.Locale.ROOT);return n.endsWith(".flac")||n.endsWith(".wav")||n.endsWith(".aiff")?"SQ":n.endsWith(".mp3")?"MP3":n.endsWith(".m4a")||n.endsWith(".aac")?"AAC":"AUDIO";}
    public static void recolor(LinearLayout card,boolean focus){((TextView)card.findViewWithTag("song_title")).setTextColor(focus?Ui.TEXT_ON_LIGHT:Ui.TEXT);((TextView)card.findViewWithTag("song_meta")).setTextColor(focus?Ui.TEXT2_ON_LIGHT:Ui.TEXT2);((TextView)card.findViewWithTag("song_playing")).setTextColor(focus?Ui.TEXT_ON_LIGHT:Ui.TEXT);}

    private void preview(Entry e){
        final int ticket=++request;Bitmap image=MusicLibrary.artwork(this,e);cover.setImageBitmap(image);if(image!=null)Ui.reveal(cover);placeholder.setText(MusicLibrary.songTitle(this,e));placeholder.setVisibility(image==null?View.VISIBLE:View.GONE);
        if(image!=null||!Config.autoCover||Config.basicService)return;
        ui.postDelayed(()->{if(ticket!=request||isFinishing())return;io.execute(()->{java.io.File folder=new java.io.File(getCacheDir(),"meta");folder.mkdirs();OnlineMeta.Meta meta=OnlineMeta.fetch(folder.getAbsolutePath(),e.name,0);MusicLibrary.metadata(this,e,meta);Bitmap result=MusicLibrary.artwork(this,e);ui.post(()->{if(ticket!=request||isFinishing())return;cover.setImageBitmap(result);if(result!=null)Ui.reveal(cover);placeholder.setVisibility(result==null?View.VISIBLE:View.GONE);});});},250);
    }
    private void grid(){
        LinkedHashMap<String,List<Entry>> grouped=groups(tab);List<String> names=new ArrayList<>(grouped.keySet());
        if(names.isEmpty()){Ui.place(content,Ui.text(this,"播放音乐后会自动归类",17,Ui.TEXT2,false),48,20,500,44);return;}
        TvVirtualList grid=new TvVirtualList(this,names.size(),6,164,16,(position,recycled)->{
            String name=names.get(position);Entry sample=grouped.get(name).get(0);LinearLayout item=new LinearLayout(this);item.setOrientation(1);item.setGravity(Gravity.CENTER);item.setPadding(Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,8));
            FrameLayout art=new FrameLayout(this);art.setBackground(Ui.round(Ui.PANEL,this,tab==1?100:10));art.setClipToOutline(true);item.addView(art,new LinearLayout.LayoutParams(Ui.dp(this,104),Ui.dp(this,104)));
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);art.addView(image,new FrameLayout.LayoutParams(-1,-1));Bitmap bitmap=MusicLibrary.artwork(this,sample);image.setImageBitmap(bitmap);
            if(bitmap==null){TextView letter=Ui.text(this,name.isEmpty()?"?":name.substring(0,1),30,Ui.TEXT,false);letter.setGravity(Gravity.CENTER);art.addView(letter,new FrameLayout.LayoutParams(-1,-1));}
            if(tab==1&&Config.autoCover&&!Config.basicService){int ticket=request;image.postDelayed(()->{if(ticket!=request||!image.isAttachedToWindow()||isFinishing())return;io.execute(()->{if(ticket!=request||!image.isAttachedToWindow())return;byte[] bytes=OnlineMeta.artistPicture(new java.io.File(getCacheDir(),"artists").getAbsolutePath(),name);Bitmap portrait=bytes==null?null:android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length);ui.post(()->{if(ticket==request&&image.isAttachedToWindow()&&portrait!=null){image.setImageBitmap(portrait);if(art.getChildCount()>1)art.getChildAt(1).setVisibility(View.GONE);}});});},250);}
            TextView label=Ui.text(this,name,16,Ui.TEXT,false);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);label.setGravity(Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=Ui.dp(this,10);item.addView(label,lp);return item;
        });
        grid.up(()->tabs.get(tab).requestFocus()).clicked(i->{listState=null;songList=null;group=names.get(i);render(true);});Ui.place(content,grid,36,0,888,Math.max(30,Ui.height(this)-156));
    }
    private void play(List<Entry> queue,int index){if(queue.isEmpty())return;PlayerService.playList(this,queue,index);Ui.open(this,new Intent(this,PlayerActivity.class));}
    private void openPlayer(){if(PlayerService.instance==null&&!PlayerService.hasPending()){if(MusicLibrary.queue(this).isEmpty()){play(tracks,0);return;}PlayerService.restore(this);}Ui.open(this,new Intent(this,PlayerActivity.class));}
    private void menu(){
        if(moduleId!=null){HomeModules.manage(this,moduleId,()->{if(HomeModules.get(this,moduleId)==null){finish();return;}reload();render(false);});return;}
        if(collection==3){editing=!editing;hint.setText(editing?"按返回键完成":"按菜单键编辑");render(true);return;}
        new TvChoiceDialog(this,"更多操作",new String[]{"播放全部","正在播放","搜索","我的","设置","清空导入内容"},-1,index->{if(index==0)play(tracks,0);else if(index==1)openPlayer();else if(index==2)Ui.open(this,new Intent(this,SearchActivity.class));else if(index==3)Ui.open(this,new Intent(this,MusicHomeActivity.class));else if(index==4)openSettings();else confirmClearImportedAudio();}).show();
    }
    private void confirmClearImportedAudio(){
        new TvChoiceDialog(this,"清空所有导入内容？",new String[]{"取消","确认清空（保留原始文件）"},-1,index->{
            if(index!=1)return;
            PlayerService.clearImportedAudio(this);
            group="";selectedKey="";listState=null;songList=null;
            reload();render(false);hint.requestFocus();
            Toast.makeText(this,"已清空导入内容、收藏和播放记录，原始文件保留",Toast.LENGTH_LONG).show();
        }).show();
    }
    private void openSettings(){if(settings!=null&&settings.isOpen())return;settings=SettingsPanel.show(this,root,()->{if(!isFinishing()){reload();render(false);}});}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_MENU){menu();return true;}if(key==KeyEvent.KEYCODE_SEARCH){Ui.open(this,new Intent(this,SearchActivity.class));return true;}return super.onKeyDown(key,event);}
    @Override public void onBackPressed(){if(settings!=null&&settings.isOpen()){settings.back();return;}if(editing){editing=false;hint.setText("按菜单键编辑");render(true);return;}if(!group.isEmpty()){listState=null;songList=null;group="";render(false);tabs.get(tab).requestFocus();return;}if(isTaskRoot())ExitConfirmation.show(this);else super.onBackPressed();}
    @Override public void finish(){super.finish();Ui.exit(this);}
    @Override public void onDestroy(){request++;ui.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
