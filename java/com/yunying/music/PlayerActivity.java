package com.yunying.music;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 播放页：模糊封面做背景，左侧大封面，右侧标题/歌手/歌词，底部进度与圆形主控键。
 */
public class PlayerActivity extends Activity implements PlayerService.Listener {

    private ImageView backdrop, coverImg;
    private TextView title, artistLine, coverText, timeText, btnPlay, lyricTip;
    private SeekBar seek;
    private TextView modeButton, favoriteButton, queueButton, effectButton;
    private int metadataRequest;
    private boolean rematching;
    private String shownTrack = "";
    private LinearLayout lyricsBox;
    private final List<TextView> lyricLines = new ArrayList<>();

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private boolean dragging, bound;

    private Lrc lrc = new Lrc();
    private int shownLyricIndex=Integer.MIN_VALUE;
    private Bitmap cover;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            sync();
            ui.postDelayed(this, 400);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.immersive(this);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
        if (PlayerService.instance == null) return;
        PlayerService.instance.setListener(this);
        bound = true;
        sync();
    }

    @Override protected void onResume() {
        super.onResume();
        if (PlayerService.instance != null) {
            PlayerService.instance.setListener(this);
            bound = true;
        }
        ui.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        ui.removeCallbacks(tick);
        if (bound && PlayerService.instance != null) PlayerService.instance.setListener(null);
        bound = false;
    }

    @Override protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        io.shutdownNow();
        super.onDestroy();
    }

    /* -------------------------------- 界面 -------------------------------- */

    private FrameLayout root, coverWrap, queuePanel, closingQueue;
    private boolean wide;

    private void buildUi() {
        wide = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || Ui.isTv(this);
        root = new FrameLayout(this);
        root.setBackground(Ui.gradient(Ui.BG_TOP, Ui.BG_BOT));
        backdrop = new ImageView(this); backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);backdrop.setAlpha(1f);
        root.addView(backdrop, new FrameLayout.LayoutParams(-1,-1));
        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0x77000000,0x66000000,0x66000000}));
        root.addView(scrim,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout page = new LinearLayout(this); page.setOrientation(1);
        page.setPadding(Ui.dp(this,48),Ui.dp(this,32),Ui.dp(this,48),Ui.dp(this,36));
        root.addView(page,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top = new LinearLayout(this); top.setOrientation(wide?0:1); top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top,new LinearLayout.LayoutParams(-1,0,1));
        int heightDp=(int)Ui.height(this);boolean compact=heightDp<440;
        int size=Ui.dp(this,wide?Math.max(160,Math.min(272,heightDp-220)):170);
        coverWrap=new FrameLayout(this); coverWrap.setBackground(Ui.round(Ui.CARD,this,12)); coverWrap.setClipToOutline(true);
        LinearLayout.LayoutParams coverLp=new LinearLayout.LayoutParams(size,size); coverLp.rightMargin=wide?Ui.dp(this,70):0; top.addView(coverWrap,coverLp);
        coverImg=new ImageView(this); coverImg.setScaleType(ImageView.ScaleType.CENTER_CROP); coverWrap.addView(coverImg,new FrameLayout.LayoutParams(-1,-1));
        coverText=Ui.text(this,"音乐",48,Ui.TEXT2,false); coverText.setGravity(Gravity.CENTER); coverWrap.addView(coverText,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout info=new LinearLayout(this); info.setOrientation(1); info.setGravity(Gravity.CENTER_HORIZONTAL);
        info.setPadding(0,Ui.dp(this,compact?12:59),0,0);info.setTranslationX(-Ui.dp(this,24));top.addView(info,wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,-2));
        title=Ui.text(this,"未在播放",wide&&!compact?29:23,Ui.TEXT,false); title.setSingleLine(true); title.setGravity(Gravity.CENTER); title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        info.addView(title,new LinearLayout.LayoutParams(-1,-2));
        artistLine=Ui.text(this,"",wide?17:12,Ui.TEXT2,false); artistLine.setSingleLine(true); artistLine.setGravity(Gravity.CENTER); artistLine.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams artistLp=new LinearLayout.LayoutParams(-1,-2); artistLp.topMargin=Ui.dp(this,6); info.addView(artistLine,artistLp);
        lyricsBox=new LinearLayout(this); lyricsBox.setOrientation(1); lyricsBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lyricsLp=new LinearLayout.LayoutParams(-1,-2); lyricsLp.topMargin=Ui.dp(this,wide&&!compact?40:12); info.addView(lyricsBox,lyricsLp);
        for(int i=0;i<(wide&&!compact?5:3);i++) {
            TextView line=Ui.text(this,"",16,Ui.TEXT2,false); line.setSingleLine(true); line.setEllipsize(android.text.TextUtils.TruncateAt.END); line.setGravity(Gravity.CENTER);
            lyricsBox.addView(line,new LinearLayout.LayoutParams(-1,Ui.dp(this,compact?22:29))); lyricLines.add(line);
        }
        lyricTip=Ui.text(this,"",11,Ui.TEXT2,false); lyricTip.setGravity(Gravity.CENTER); info.addView(lyricTip,new LinearLayout.LayoutParams(-1,-2));
        timeText=Ui.text(this,"",14,Ui.TEXT2,false);updateTimes(0,0); timeText.setGravity(Gravity.RIGHT);timeText.setTranslationY(-Ui.dp(this,10)); page.addView(timeText,new LinearLayout.LayoutParams(-1,-2));
        seek=new SeekBar(this); seek.setMax(1000); seek.setContentDescription("播放进度，左右键快进快退");
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.TEXT)); seek.setThumbTintList(android.content.res.ColorStateList.valueOf(Ui.TEXT)); seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0x44FFFFFF));
        page.addView(seek,new LinearLayout.LayoutParams(-1,Ui.dp(this,18)));
        seek.setPadding(0,0,0,0);seek.setThumb(Ui.states(Ui.round(0,this,4),Ui.round(Ui.TEXT,this,4),null));
        android.graphics.drawable.GradientDrawable track=Ui.round(0x44FFFFFF,this,2),fill=Ui.round(Ui.TEXT,this,2);track.setSize(1,Ui.dp(this,4));fill.setSize(1,Ui.dp(this,4));android.graphics.drawable.LayerDrawable progress=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{track,new android.graphics.drawable.ScaleDrawable(fill,Gravity.LEFT,1f,0f)});progress.setId(0,android.R.id.background);progress.setId(1,android.R.id.progress);seek.setProgressDrawable(progress);seek.setMinHeight(Ui.dp(this,4));seek.setMaxHeight(Ui.dp(this,4));
        seek.setOnKeyListener((view,key,event)->{if((key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)&&event.getAction()==KeyEvent.ACTION_DOWN){if(PlayerService.instance!=null){PlayerService.instance.seekTo(PlayerService.instance.getPosition()+(key==KeyEvent.KEYCODE_DPAD_RIGHT?5000:-5000));sync();}return true;}return false;});
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int progress,boolean user){ if(user&&PlayerService.instance!=null) updateTimes(PlayerService.instance.getDuration()*progress/1000,PlayerService.instance.getDuration()); }
            public void onStartTrackingTouch(SeekBar bar){dragging=true;}
            public void onStopTrackingTouch(SeekBar bar){dragging=false;if(PlayerService.instance!=null)PlayerService.instance.seekTo((int)(PlayerService.instance.getDuration()*(long)bar.getProgress()/1000));}
        });
        LinearLayout controls=new LinearLayout(this); controls.setGravity(Gravity.CENTER);controls.setTranslationX(Ui.dp(this,44));
        LinearLayout.LayoutParams controlLp=new LinearLayout.LayoutParams(-1,Ui.dp(this,64)); controlLp.topMargin=Ui.dp(this,8); page.addView(controls,controlLp);
        favoriteButton=iconButton("heart","收藏",v->{PlayerService service=PlayerService.instance;if(service!=null)MusicLibrary.toggleFavorite(this,service.current());sync();});
        modeButton=iconButton("mode","列表循环",v->{if(PlayerService.instance!=null)PlayerService.instance.cycleMode();sync();});
        TextView prev=iconButton("transport","⏮",v->PlayerService.command(this,PlayerService.ACTION_PREV,-1));
        btnPlay=iconButton("transport","▶",v->PlayerService.command(this,PlayerService.ACTION_TOGGLE,-1));
        btnPlay.setBackground(new PlayerFocusDrawable(this)); btnPlay.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));
        TextView next=iconButton("transport","⏭",v->PlayerService.command(this,PlayerService.ACTION_NEXT,-1));
        queueButton=iconButton("queue","播放队列",v->showQueue());
        effectButton=iconButton("soundSliders","音效 · "+SoundEffects.name(Config.audioEffect),v->SoundEffectsDialog.show(this,()->sync()));
        TextView more=iconButton("more","更多",v->showMore());
        TextView[] items={favoriteButton,modeButton,prev,btnPlay,next,queueButton,effectButton,more};
        for(TextView item:items) {int d=item==btnPlay?(wide?48:48):(wide?44:34);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(Ui.dp(this,d),Ui.dp(this,d));lp.leftMargin=Ui.dp(this,wide?20:2);lp.rightMargin=Ui.dp(this,wide?20:2);controls.addView(item,lp);}
        setContentView(root); Ui.installFocusMotion(root);Ui.reveal(page);btnPlay.requestFocus();
    }
    private TextView iconButton(String kind,String label,View.OnClickListener click) {
        TextView t=new IconTextView(this,kind);Ui.setText(t,label);t.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));t.setFocusable(true);t.setClickable(true);
        t.setBackground(new PlayerFocusDrawable(this));t.setOnClickListener(click);return t;
    }
    private TextView actionButton(String label,View.OnClickListener click) {
        TextView t=Ui.text(this,label,11,Ui.TEXT,false);t.setGravity(Gravity.CENTER);t.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));t.setFocusable(true);t.setClickable(true);t.setBackground(Ui.states(Ui.round(0,this,8),Ui.focusRing(this,8),Ui.focusRing(this,8)));t.setOnClickListener(click);return t;
    }
    private void showMore() {
        new TvChoiceDialog(this,"更多操作",new String[]{
                "自动搜索歌词："+(Config.autoLyrics?"开启":"关闭"),
                "自动搜索封面："+(Config.autoCover?"开启":"关闭"),
                "歌词时间偏移："+Config.lyricOffset+" ms",
                rematching?"正在重新匹配歌词和封面…":"重新匹配歌词和封面",
                "修改歌曲名和歌手","返回播放列表","停止播放"},-1,index->{
            if(index==0||index==1){
                if(index==0)Config.autoLyrics=!Config.autoLyrics;else Config.autoCover=!Config.autoCover;
                Config.save(this);shownTrack="";sync();showMore();
            } else if(index==2)showLyricOffset();
            else if(index==3)rematchMetadata();
            else if(index==4)editSongInfo();
            else if(index==5){Ui.open(this,new android.content.Intent(this,MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}
            else{PlayerService.command(this,PlayerService.ACTION_STOP,-1);finish();}
        }).show();
    }
    private void editSongInfo(){
        PlayerService service=PlayerService.instance;Entry entry=service==null?null:service.current();
        if(entry==null){android.widget.Toast.makeText(this,"请先播放一首歌曲",0).show();return;}
        SongInfoDialog.show(this,entry,()->{
            metadataRequest++;rematching=false;sync();
            if(PlayerService.instance!=null&&PlayerService.instance.current()==entry)rematchMetadata();
        });
    }
    private void showLyricOffset() {
        new TvChoiceDialog(this,"歌词时间偏移："+Config.lyricOffset+" ms",new String[]{"提前 0.5 秒","延后 0.5 秒","重置"},-1,index->{
            Config.lyricOffset=index==2?0:Math.max(-10000,Math.min(10000,Config.lyricOffset+(index==0?500:-500)));
            Config.save(this);shownLyricIndex=Integer.MIN_VALUE;sync();showLyricOffset();
        }).show();
    }
    private void rematchMetadata() {
        PlayerService service=PlayerService.instance;
        Entry entry=service==null?null:service.current();
        if(entry==null){android.widget.Toast.makeText(this,"请先播放一首音乐",0).show();return;}
        if(Config.basicService||MusicLibrary.isBook(this,entry)){
            android.widget.Toast.makeText(this,Config.basicService?"基础服务模式下无法联网匹配":"听书不匹配音乐歌词与封面",0).show();return;
        }
        if(rematching)return;
        onTrackChanged(entry,service.getDuration(),true);
    }
    private View queueList;
    private void closeQueue(){
        if(queuePanel==null)return;final FrameLayout closing=queuePanel;queuePanel=null;closingQueue=closing;queueButton.requestFocus();
        if(Ui.motionEnabled(this)){if(queueList!=null)queueList.animate().translationX(-Ui.dp(this,24)).setDuration(180).setInterpolator(Ui.EASE).start();closing.animate().alpha(0).setDuration(180).withEndAction(()->{root.removeView(closing);if(closingQueue==closing)closingQueue=null;}).start();}else root.removeView(closing);
    }
    private void showQueue(){
        if(closingQueue!=null){closingQueue.animate().cancel();root.removeView(closingQueue);closingQueue=null;}
        if(queuePanel!=null){closeQueue();return;}PlayerService service=PlayerService.instance;if(service==null)return;
        queuePanel=new FrameLayout(this);queuePanel.setBackgroundColor(0x77000000);queuePanel.setContentDescription("播放队列 · "+service.getQueue().size()+" 首");root.addView(queuePanel,new FrameLayout.LayoutParams(-1,-1));
        TvFadeFrame viewport=new TvFadeFrame(this);queueList=viewport;Ui.place(queuePanel,viewport,20,30,440,Math.max(60,Ui.height(this)-50));
        List<Entry> queue=new java.util.ArrayList<>(service.getQueue());
        TvVirtualList list=new TvVirtualList(this,queue.size(),1,76,12,(i,recycled)->LibraryActivity.songCard(this,queue.get(i),i==service.getIndex(),recycled));
        viewport.addView(list,new FrameLayout.LayoutParams(-1,-1));
        list.right(()->closeQueue()).clicked(i->{service.playAt(i);sync();closeQueue();});
        list.focusPosition(Math.max(0,service.getIndex()));
        if(Ui.motionEnabled(this)){viewport.setTranslationX(-Ui.dp(this,24));viewport.animate().translationX(0).setDuration(220).setInterpolator(Ui.EASE).start();queuePanel.setAlpha(0);queuePanel.animate().alpha(1).setDuration(180).start();}
    }
    @Override public void finish(){super.finish();Ui.exit(this);}
    @Override public void onBackPressed(){if(queuePanel!=null){closeQueue();return;}super.onBackPressed();}

    /* ------------------------------ 状态同步 ------------------------------ */

    @Override public void onPlayerState() { sync(); }

    private void sync() {
        PlayerService s = PlayerService.instance;
        if (s == null) {
            Ui.setText(title,"未在播放");
            Ui.setText(artistLine,"");
            Ui.setText(btnPlay,"▶");
            return;
        }
        if(!bound){s.setListener(this);bound=true;}
        Entry e = s.current();
        Ui.setText(modeButton,s.modeLabel());
        Ui.setText(favoriteButton,MusicLibrary.favorite(this, e) ? "已收藏" : "收藏");
        favoriteButton.setSelected(MusicLibrary.favorite(this, e));
        Ui.setText(queueButton,"播放队列 · " + s.getQueue().size());
        if (e != null && !(MusicLibrary.key(e)+(e.audiobook?":book":":music")).equals(shownTrack)) {
            shownTrack = MusicLibrary.key(e)+(e.audiobook?":book":":music");
            onTrackChanged(e, s.getDuration());
        }
        Ui.setText(effectButton,"音效 · "+SoundEffects.name(Config.audioEffect));
        Ui.setText(title,e == null ? "未在播放" : MusicLibrary.songTitle(this,e));
        String from = e == null ? "" : (Entry.SRC_ALI.equals(e.source) ? "阿里云盘" : "NAS 音乐");
        String extra = s.getError() != null && s.getError().length() > 0 ? s.getError()
                : (s.isPreparing() ? "缓冲中" : (s.getIndex() + 1) + " / "
                        + Math.max(1, s.getQueue().size()));
        Ui.setText(artistLine,e==null?"":MusicLibrary.artist(this,e)+" - "+MusicLibrary.album(this,e));
        if(s.getError().length()>0)Ui.setText(lyricTip,s.getError());
        else if(s.isPreparing())Ui.setText(lyricTip,"正在缓冲…");
        else if(e!=null&&e.audiobook){lyricTip.setVisibility(View.VISIBLE);Ui.setText(lyricTip,"听书");}
        else if(!Config.autoLyrics||Config.basicService)Ui.setText(lyricTip,"");
        Ui.setText(btnPlay,s.isPlaying() ? "❚❚" : "▶");

        int d = s.getDuration();
        int p = s.getPosition();
        updateTimes(p, d);
        if (!dragging && d > 0) seek.setProgress((int) (1000L * p / d));
        updateLyrics(p+Config.lyricOffset);
    }

    private void updateTimes(int pos, int dur) {
        String current=fmt(pos);
        android.text.SpannableString value=new android.text.SpannableString(current+" / "+fmt(dur));
        value.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE),0,current.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Ui.setText(timeText,value);
    }

    private String fmt(int ms) {
        if (ms <= 0) return "00:00";
        int total = ms / 1000;
        int m = total / 60, sec = total % 60;
        return (m < 10 ? "0" + m : String.valueOf(m)) + ":" + (sec < 10 ? "0" + sec : String.valueOf(sec));
    }

    /* --------------------------- 在线封面 / 歌词 --------------------------- */

    private void onTrackChanged(final Entry e, final int durationMs) {
        onTrackChanged(e,durationMs,false);
    }
    private void onTrackChanged(final Entry e, final int durationMs, final boolean refresh) {
        final int request=++metadataRequest;
        final String matchName=MusicLibrary.matchName(this,e);
        rematching=refresh;
        if(!refresh){
        coverImg.setImageBitmap(null);
        cover = null;
        coverText.setVisibility(View.VISIBLE);
        Ui.setText(coverText,firstChar(e.title()));
        backdrop.setImageBitmap(null);
        lrc = new Lrc();shownLyricIndex=Integer.MIN_VALUE;
        for (TextView t : lyricLines) Ui.setText(t,"");
        lyricTip.setVisibility(View.VISIBLE);
        Ui.setText(lyricTip,MusicLibrary.isBook(this,e)?"听书":Config.autoLyrics&&!Config.basicService?"正在搜索歌词…":"");

        } else android.widget.Toast.makeText(this,"正在重新匹配歌词和封面…",0).show();

        final boolean wantLrc = (refresh||Config.autoLyrics)&&!Config.basicService&&!MusicLibrary.isBook(this,e);
        final boolean wantCover = (refresh||Config.autoCover)&&!Config.basicService&&!MusicLibrary.isBook(this,e);
        if (!wantLrc && !wantCover) return;

        io.execute(new Runnable() {
            @Override public void run() {
                if(wantCover&&!refresh){final Bitmap cached=MusicLibrary.artwork(PlayerActivity.this,e);if(cached!=null){final Bitmap background=Ui.blur(cached);ui.post(()->{if(request!=metadataRequest||isFinishing()||isDestroyed()||PlayerService.instance==null||PlayerService.instance.current()!=e)return;cover=cached;coverImg.setImageBitmap(roundBitmap(cached,Ui.dp(PlayerActivity.this,10)));coverText.setVisibility(View.GONE);backdrop.setImageBitmap(background);backdrop.setAlpha(1f);});}}
                OnlineMeta.Meta m = OnlineMeta.fetch(metaDir(), matchName, durationMs, refresh);

                final Bitmap bmp = (wantCover && m.cover != null)
                        ? BitmapFactory.decodeByteArray(m.cover, 0, m.cover.length) : null;

                final Bitmap frosted=bmp==null?null:Ui.blur(bmp);
                final Lrc parsed = wantLrc ? Lrc.parse(m.lrc) : new Lrc();
                final String src = m.lyricSource;
                final String err = m.error;
                ui.post(new Runnable() {
                    @Override public void run() {
                        if (isFinishing()) return;
                        Entry cur = PlayerService.instance == null ? null
                                : PlayerService.instance.current();
                        if (cur != e || request!=metadataRequest || isFinishing() || isDestroyed()) return;
                        MusicLibrary.metadata(PlayerActivity.this,e,m);
                        rematching=false;
                        if(refresh)android.widget.Toast.makeText(PlayerActivity.this,
                            (m.hasLrc()?"歌词已更新":"未找到新歌词，保留原歌词")+"；"+(bmp!=null?"封面已更新":"未找到新封面，保留原封面"),1).show();
                        if (bmp != null) {
                            cover = bmp;
                            coverImg.setImageBitmap(roundBitmap(bmp, Ui.dp(PlayerActivity.this, 10)));
                            coverText.setVisibility(View.GONE);
                            backdrop.setImageBitmap(frosted);backdrop.setAlpha(1f);

                        }
                        if(!refresh||parsed.size()>0){lrc = parsed;shownLyricIndex=Integer.MIN_VALUE;}
                        if (wantLrc && (!refresh||parsed.size()>0)) {
                            lyricTip.setVisibility(lrc.size()>0?View.INVISIBLE:View.VISIBLE);
                            Ui.setText(lyricTip,lrc.size() > 0
                                    ? (("kugou".equals(src) || "netease".equals(src))
                                        ? "歌词 · 在线匹配" : "歌词 · 本地缓存")
                                    : ("暂无歌词" + (err.length() > 0 ? "（" + err + "）" : "")));
                        }
                    }
                });
            }
        });
    }

    private Bitmap roundBitmap(Bitmap src, int radius) {
        try {
            Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                    Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(out);
            android.graphics.Paint p = new android.graphics.Paint();
            p.setAntiAlias(true);
            p.setShader(new android.graphics.BitmapShader(src,
                    android.graphics.Shader.TileMode.CLAMP,
                    android.graphics.Shader.TileMode.CLAMP));
            c.drawRoundRect(new android.graphics.RectF(0, 0, src.getWidth(), src.getHeight()),
                    radius, radius, p);
            return out;
        } catch (Throwable t) {
            return src;
        }
    }

    private String metaDir() {
        java.io.File d = new java.io.File(getCacheDir(), "meta");
        if (!d.exists()) d.mkdirs();
        return d.getAbsolutePath();
    }

    private String firstChar(String s) {
        if (s == null || s.length() == 0) return "♫";
        return s.substring(0, 1).toUpperCase();
    }

    private void updateLyrics(int positionMs) {
        if (lrc.size() == 0) return;
        int idx = lrc.indexAt(positionMs);
        int n = lyricLines.size();
        int start = idx<0?0:idx-n/2;
        if(idx!=shownLyricIndex){shownLyricIndex=idx;lyricsBox.animate().cancel();if(Ui.motionEnabled(this)){lyricsBox.setTranslationY(Ui.dp(this,10));lyricsBox.setAlpha(.72f);lyricsBox.animate().translationY(0).alpha(1).setDuration(260).setInterpolator(Ui.EASE).start();}else{lyricsBox.setTranslationY(0);lyricsBox.setAlpha(1);}}
        for (int i = 0; i < n; i++) {
            int li = start + i;
            TextView t = lyricLines.get(i);
            if (li < 0 || li >= lrc.size()) {
                Ui.setText(t,"");
                continue;
            }
            Ui.setText(t,lrc.texts[li]);
            if (li == idx) {
                t.setTextColor(Ui.TEXT);
                t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(this,18));
                t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            } else {
                boolean near = Math.abs(li - idx) == 1;
                t.setTextColor(near ? 0xFFB8B4B0 : 0xFF8F8B86);
                t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(this,16));
                t.setTypeface(android.graphics.Typeface.DEFAULT);
            }
        }
    }

    /* ------------------------------- LRC 解析 ------------------------------- */

    private static class Lrc {
        long[] times = new long[0];
        String[] texts = new String[0];

        int size() { return texts.length; }

        int indexAt(long ms) {
            if (times.length == 0 || ms < times[0]) return -1;
            int lo = 0, hi = times.length - 1, ans = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (times[mid] <= ms) { ans = mid; lo = mid + 1; }
                else hi = mid - 1;
            }
            return ans;
        }

        static Lrc parse(String text) {
            Lrc r = new Lrc();
            if (text == null || text.trim().length() == 0) return r;
            List<Long> ts = new ArrayList<>();
            List<String> tx = new ArrayList<>();
            for (String raw : text.split("\n")) {
                String line = raw.trim();
                if (line.length() == 0) continue;
                List<Long> lineTimes = new ArrayList<>();
                int i = 0;
                while (i < line.length() && line.charAt(i) == '[') {
                    int close = line.indexOf(']', i);
                    if (close < 0) break;
                    long t = parseTime(line.substring(i + 1, close));
                    if (t >= 0) lineTimes.add(t);
                    i = close + 1;
                }
                if (lineTimes.isEmpty()) continue;
                String content = line.substring(i).trim();
                if (content.length() == 0) continue;
                for (long t : lineTimes) {
                    ts.add(t);
                    tx.add(content);
                }
            }
            Integer[] order = new Integer[ts.size()];
            for (int i = 0; i < order.length; i++) order[i] = i;
            final List<Long> fts = ts;
            java.util.Arrays.sort(order, new java.util.Comparator<Integer>() {
                @Override public int compare(Integer a, Integer b) {
                    return Long.compare(fts.get(a), fts.get(b));
                }
            });
            r.times = new long[ts.size()];
            r.texts = new String[ts.size()];
            for (int i = 0; i < order.length; i++) {
                r.times[i] = ts.get(order[i]);
                r.texts[i] = tx.get(order[i]);
            }
            return r;
        }

        private static long parseTime(String tag) {
            if (tag.length() < 4) return -1;
            char c0 = tag.charAt(0);
            if (c0 < '0' || c0 > '9') return -1;
            int colon = tag.indexOf(':');
            if (colon <= 0) return -1;
            try {
                int min = Integer.parseInt(tag.substring(0, colon).trim());
                double sec = Double.parseDouble(tag.substring(colon + 1).replace(':', '.'));
                return (long) (min * 60000 + sec * 1000);
            } catch (Exception e) {
                return -1;
            }
        }
    }

    /* ------------------------------- 按键处理 ------------------------------- */

    @Override public boolean onKeyDown(int code, KeyEvent e) {
        PlayerService s = PlayerService.instance;
        if (s != null) {
            if (code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == KeyEvent.KEYCODE_HEADSETHOOK) {
                PlayerService.command(this, PlayerService.ACTION_TOGGLE, -1);
                return true;
            }
            if (code == KeyEvent.KEYCODE_MEDIA_NEXT) {
                PlayerService.command(this, PlayerService.ACTION_NEXT, -1);
                return true;
            }
            if (code == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                PlayerService.command(this, PlayerService.ACTION_PREV, -1);
                return true;
            }
            View f = getCurrentFocus();
            boolean onSeek = f == seek || f == null;
            if (onSeek && code == KeyEvent.KEYCODE_DPAD_LEFT) {
                s.seekTo(s.getPosition() - 5000);
                sync();
                return true;
            }
            if (onSeek && code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                s.seekTo(s.getPosition() + 5000);
                sync();
                return true;
            }
        }
        return super.onKeyDown(code, e);
    }
}
