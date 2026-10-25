package com.yunying.music;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

/** 按参考图的「我的」卡片、分区和设置网格呈现真实曲库状态。 */
public class MusicHomeActivity extends Activity {
    private static final int MODULE_ICON_SIZE=40;
    private static final float MODULE_ICON_FRACTION=.9f;
    private FrameLayout root,canvas;private ViewportScrollView viewport;private SettingsPanel settings;private boolean resumed;
    @Override public void onCreate(Bundle state){super.onCreate(state);Ui.immersive(this);build();}
    @Override public void onResume(){super.onResume();if(!resumed){resumed=true;return;}if(root!=null&&(settings==null||!settings.isOpen())){int y=viewport.getScrollY();View focused=root.findFocus();String label=focused instanceof TextView?((TextView)focused).getText().toString():focused!=null&&focused.getContentDescription()!=null?focused.getContentDescription().toString():"";build();viewport.post(()->{if(!label.isEmpty())Ui.focusText(root,label);viewport.scrollTo(0,y);});}}
    private void openLibrary(int collection,int category){Ui.open(this,new Intent(this,LibraryActivity.class).putExtra("collection",collection).putExtra("category",category));}
    private TextView label(String text,float size,int color){return Ui.text(this,text,size,color,false);}
    private void build(){
        root=new FrameLayout(this);root.setBackgroundColor(Ui.BG_TOP);ViewportScrollView scroll=new ViewportScrollView(this);viewport=scroll;scroll.setFocusable(false);root.setFocusable(false);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        canvas=new FrameLayout(this);canvas.setFocusable(false);scroll.addView(canvas,new ScrollView.LayoutParams(-1,Ui.dp(this,996)));setContentView(root);
        Ui.place(canvas,label("音乐",29,Ui.TEXT),48,34,300,44);
        String[] kinds={"clock","library","equalizer","search"};String[] descriptions={"最近播放","音乐资料库","正在播放","搜索"};
        for(int i=0;i<4;i++){final int index=i;TextView icon=new IconTextView(this,i==2?"nowPlaying":kinds[i]);icon.setText(descriptions[i]);icon.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));icon.setGravity(Gravity.CENTER);icon.setFocusable(true);icon.setClickable(true);icon.setBackground(Ui.states(Ui.round(0x18202020,this,30),Ui.focusRing(this,30),null));icon.setOnClickListener(v->{if(index==0)openLibrary(4,0);else if(index==1)overview();else if(index==2)openPlayer();else Ui.open(this,new Intent(this,SearchActivity.class));});Ui.place(canvas,icon,586+i*56,37,34,34);}
        TextView avatar=Ui.action(this,"云",19,v->overview());avatar.setBackground(Ui.focusRing(this,30));avatar.setTextColor(Ui.TEXT_ON_LIGHT);Ui.place(canvas,avatar,810,37,34,34);Ui.place(canvas,label("云影音乐",17,Ui.TEXT2),864,44,90,26);
        HorizontalScrollView tabScroll=new HorizontalScrollView(this);tabScroll.setHorizontalScrollBarEnabled(false);tabScroll.setClipToPadding(false);
        LinearLayout tabRow=new LinearLayout(this);tabRow.setGravity(Gravity.CENTER_VERTICAL);tabRow.setPadding(Ui.dp(this,6),Ui.dp(this,4),Ui.dp(this,6),Ui.dp(this,4));tabScroll.addView(tabRow,new HorizontalScrollView.LayoutParams(-2,-1));
        List<HomeModules.Module> homeTabs=HomeModules.all(this);
        for(int i=0;i<=homeTabs.size();i++){
            final HomeModules.Module module=i==0?null:homeTabs.get(i-1);
            TextView tab=Ui.text(this,module==null?"我的":module.name,20,Ui.TEXT,false);tab.setSingleLine(true);tab.setGravity(Gravity.CENTER);tab.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);tab.setFocusable(true);tab.setClickable(true);tab.setSelected(module==null);
            tab.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));
            tab.setBackground(Ui.tabBackground(this));
            tab.setOnClickListener(v->{if(module==null)viewport.smoothScrollTo(0,0);else Ui.open(this,new Intent(this,LibraryActivity.class).putExtra("collection",13).putExtra("moduleId",module.id));});
            LinearLayout.LayoutParams tabLp=new LinearLayout.LayoutParams(-2,Ui.dp(this,38));tabLp.rightMargin=Ui.dp(this,24);tabRow.addView(tab,tabLp);if(module==null)tab.requestFocus();
        }
        Ui.place(canvas,tabScroll,132,92,780,46);
        FrameLayout library=new FrameLayout(this);library.setBackground(Ui.states(Ui.round(Ui.PANEL,this,10),Ui.focusRing(this,10),null));library.setFocusable(true);library.setClickable(true);library.setOnClickListener(v->overview());Ui.place(canvas,library,48,160,428,140);
        TextView libraryTitle=label("音乐资料库",23,0xFFE4CE95),libraryCount=label("已添加 "+MusicLibrary.playlist(this).size()+" 首音乐",17,0xFFCDBD91);
        Ui.place(library,libraryTitle,32,45,340,36);Ui.place(library,libraryCount,32,78,340,28);
        library.setOnFocusChangeListener((v,focus)->{libraryTitle.setTextColor(focus?Ui.TEXT_ON_LIGHT:0xFFE4CE95);libraryCount.setTextColor(focus?Ui.TEXT2_ON_LIGHT:0xFFCDBD91);});
        String[] featureNames={"自有曲库","无损格式","遥控操作","后台播放"},notes={"已添加的音乐目录","FLAC / WAV 等文件","方向键与 OK 确认","返回后继续收听"};
        Ui.place(canvas,label("音乐库功能",16,0xFFE4CE95),656,178,160,28);
        View leftRule=new View(this),rightRule=new View(this);leftRule.setBackgroundColor(0xFFB4A67F);rightRule.setBackgroundColor(0xFFB4A67F);Ui.place(canvas,leftRule,532,185,100,1);Ui.place(canvas,rightRule,792,185,100,1);
        for(int i=0;i<4;i++){float fx=532+(i%2)*182,fy=214+(i/2)*52;TextView featureIcon=new IconTextView(this,new String[]{"library","quality","settings","broadcast"}[i]);featureIcon.setText(i==1?"SQ":featureNames[i]);featureIcon.setTextColor(0xFFE4CE95);Ui.place(canvas,featureIcon,fx,fy,32,32);Ui.place(canvas,label(featureNames[i],17,0xFFE4CE95),fx+44,fy-6,150,28);Ui.place(canvas,label(notes[i],12,0xFFCDBD91),fx+44,fy+18,150,22);}
        Ui.place(canvas,label("音乐",20,Ui.TEXT),48,342,200,30);
        List<Entry> recent=MusicLibrary.read(this,"recent"),favorites=MusicLibrary.read(this,"favorites"),playlist=MusicLibrary.playlist(this);
        tile("最近播放",recent.size(),recent,48,390,"clock",()->openLibrary(4,0));tile("我喜欢",favorites.size(),favorites,270,390,"heart",()->openLibrary(3,0));tile("我的歌单",playlist.isEmpty()?0:1,playlist,492,390,"library",()->openLibrary(6,0));tile("已购歌曲",0,Collections.emptyList(),714,390,"music",()->openLibrary(9,0));
        Ui.place(canvas,label("设置",20,Ui.TEXT),48,586,200,30);
        String[] names={"检查更新","关于","其他设置"};String[] icons={"update","info","settings"};
        for(int i=0;i<names.length;i++){final int index=i;LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER);card.setFocusable(true);card.setClickable(true);card.setBackground(Ui.states(Ui.round(Ui.PANEL,this,12),Ui.focusRing(this,12),null));IconTextView icon=new IconTextView(this,icons[i]);icon.setIconFraction(MODULE_ICON_FRACTION);icon.setText(names[i]);icon.setTextColor(Ui.TEXT);card.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,MODULE_ICON_SIZE),Ui.dp(this,MODULE_ICON_SIZE)));TextView text=label(names[i],18,Ui.TEXT);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=Ui.dp(this,10);card.addView(text,lp);card.setOnFocusChangeListener((v,focused)->{text.setTextColor(focused?Ui.TEXT_ON_LIGHT:Ui.TEXT);icon.setTextColor(focused?Ui.TEXT_ON_LIGHT:Ui.TEXT);});card.setOnClickListener(v->{if(index==0)message("检查更新","当前版本 1.6，通过新的安装包升级");else if(index==1)message("关于","云影音乐 1.6 · Android 电视音乐播放器");else openSettings(0);});Ui.place(canvas,card,48+(i%4)*222,628+(i/4)*140,198,112);}
        Ui.installFocusMotion(root);
    }
    private void tile(String title,int count,List<Entry> data,float x,float y,String kind,Runnable click){
        FrameLayout card=new FrameLayout(this);card.setFocusable(true);card.setClickable(true);card.setBackground(Ui.states(Ui.round(Ui.PANEL,this,12),Ui.focusRing(this,12),null));card.setContentDescription(title);card.setOnClickListener(v->click.run());Ui.place(canvas,card,x,y,198,156);
        Bitmap art=data.isEmpty()?null:MusicLibrary.artwork(this,data.get(0));
        final IconTextView icon;
        if(art!=null){
            icon=null;ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImageBitmap(art);image.setBackground(Ui.round(Ui.CARD,this,4));image.setClipToOutline(true);Ui.place(card,image,24,16,64,64);
        }else{
            icon=new IconTextView(this,kind);icon.setText(title);icon.setTextColor(Ui.TEXT);icon.setIconFraction(MODULE_ICON_FRACTION);Ui.place(card,icon,36,28,MODULE_ICON_SIZE,MODULE_ICON_SIZE);
        }
        TextView name=Ui.text(this,title,20,Ui.TEXT,false),number=Ui.text(this,String.valueOf(count),17,Ui.TEXT2,false);name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.place(card,name,24,84,166,30);Ui.place(card,number,24,116,166,28);
        card.setOnFocusChangeListener((v,focus)->{
            name.setTextColor(focus?Ui.TEXT_ON_LIGHT:Ui.TEXT);
            number.setTextColor(focus?Ui.TEXT2_ON_LIGHT:Ui.TEXT2);
            if(icon!=null)icon.setTextColor(focus?Ui.TEXT_ON_LIGHT:Ui.TEXT);
        });
    }
    private void refreshHome(){if(isFinishing())return;int y=viewport.getScrollY();build();viewport.post(()->viewport.scrollTo(0,y));}
    private void overview(){openLibrary(0,0);}
    private void message(String title,String text){new TvChoiceDialog(this,title,new String[]{text},-1,index->{}).show();}
    private void openSettings(int section){if(settings!=null&&settings.isOpen())return;settings=SettingsPanel.show(this,root,()->{if(!isFinishing()){int y=viewport.getScrollY();build();viewport.post(()->viewport.scrollTo(0,y));}});settings.showSection(section);}
    private void openPlayer(){if(PlayerService.instance==null&&!PlayerService.hasPending()){List<Entry> saved=MusicLibrary.queue(this);if(saved.isEmpty()){saved=MusicLibrary.playlist(this);if(saved.isEmpty()){openSettings(0);return;}PlayerService.playList(this,saved,0);}else PlayerService.restore(this);}Ui.open(this,new Intent(this,PlayerActivity.class));}
    @Override public void onBackPressed(){if(settings!=null&&settings.isOpen()){settings.back();return;}if(isTaskRoot())ExitConfirmation.show(this);else super.onBackPressed();}
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_MENU){new TvChoiceDialog(this,"音乐",new String[]{"播放列表","正在播放","设置","自定义首页","退出应用"},-1,index->{if(index==0)openLibrary(0,0);else if(index==1)openPlayer();else if(index==2)openSettings(0);else if(index==3)HomeModules.show(this,()->refreshHome());else ExitConfirmation.show(this);}).show();return true;}return super.onKeyDown(key,event);}
    @Override public void finish(){super.finish();Ui.exit(this);}
}
