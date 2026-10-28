package com.yunying.music;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.util.*;
import java.util.concurrent.*;

/** 参考图五列遥控器键盘、搜索建议、双列结果与分区退回交互。 */
public final class SearchActivity extends Activity {
    private FrameLayout root,keyboard,suggestions,results;
    private TextView input,lastKey;
    private final List<Entry> catalog=new ArrayList<>(),matches=new ArrayList<>();
    private final Handler ui=new Handler();private final ExecutorService io=Executors.newSingleThreadExecutor();
    private volatile int generation;private PinyinIndex index;
    private String query="";private int mode,tab,lastRenderedMode=-1;
    @Override public void onCreate(Bundle state){super.onCreate(state);Ui.immersive(this);catalog.addAll(MusicLibrary.read(this,"catalog"));if(catalog.isEmpty())catalog.addAll(MusicLibrary.playlist(this));root=new FrameLayout(this);root.setClipChildren(false);root.setBackground(new ImmersiveBackground());setContentView(root);buildKeyboard();Ui.installFocusMotion(keyboard);render(false);search();lastKey.requestFocus();}
    private TextView key(String text,String kind,Runnable action){
        TextView button=kind==null?Ui.text(this,text,19,Ui.TEXT,false):new IconTextView(this,kind);button.setText(text);button.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));button.setGravity(Gravity.CENTER);button.setFocusable(true);button.setClickable(true);button.setBackground(Ui.cardFocusable(this,4));if(android.os.Build.VERSION.SDK_INT>=26)button.setDefaultFocusHighlightEnabled(false);button.setOnClickListener(v->{lastKey=button;action.run();});return button;
    }
    private void buildKeyboard(){
        keyboard=new FrameLayout(this);
        // 输入框与键盘区域等宽，允许聚焦放大后的边缘绘制到区域外。
        keyboard.setClipChildren(false);keyboard.setClipToPadding(false);
        Ui.place(root,keyboard,48,28,272,470);
        input=new SearchInputView(this);input.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(this,18));input.setTextColor(Ui.TEXT);input.setHint("输入 首字母/全拼 搜索");input.setHintTextColor(Ui.TEXT2);input.setSingleLine(true);input.setFocusable(true);input.setClickable(true);input.setOnClickListener(v->lastKey.requestFocus());input.setGravity(Gravity.CENTER_VERTICAL);input.setPadding(Ui.dp(this,24),0,Ui.dp(this,18),0);input.setBackground(Ui.round(Ui.CARD,this,26));Ui.place(keyboard,input,0,0,272,48);
        String chars="ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
        for(int i=0;i<chars.length();i++){String value=String.valueOf(chars.charAt(i));TextView button=key(value,null,()->append(value));Ui.place(keyboard,button,(i%5)*56,72+(i/5)*44,48,36);if(i==12)lastKey=button;}
        Ui.place(keyboard,key("0",null,()->append("0")),0,380,48,36);Ui.place(keyboard,key("空格","space",()->append(" ")),56,380,104,36);Ui.place(keyboard,key("清空","trash",()->setQuery("")),168,380,48,36);Ui.place(keyboard,key("退格","backspace",this::delete),224,380,48,36);
    }
    private void append(String text){setQuery(query+text);}
    private void delete(){if(!query.isEmpty())setQuery(query.substring(0,query.length()-1));}
    private void setQuery(String value){query=value;input.setText(query);input.setTextColor(query.isEmpty()?Ui.TEXT2:Ui.TEXT);search();}
    private void search(){final int ticket=++generation;final String text=query;io.execute(()->{if(index==null)index=new PinyinIndex(this);List<Entry> found=new ArrayList<>();for(Entry e:catalog){if(ticket!=generation)return;String doc=MusicLibrary.songTitle(this,e)+" "+MusicLibrary.artist(this,e)+" "+MusicLibrary.album(this,e);if(index.matches(doc,text))found.add(e);}ui.post(()->{if(ticket!=generation||isFinishing())return;matches.clear();matches.addAll(found);render(mode==2);});});}
    private void render(boolean focusResult){
        final boolean switching=lastRenderedMode>=0&&lastRenderedMode!=mode;final float previousX=results==null?0:results.getLeft();final int previousMode=lastRenderedMode;lastRenderedMode=mode;
        keyboard.animate().cancel();if(mode==0){keyboard.setVisibility(View.VISIBLE);if(switching&&Ui.motionEnabled(this)){keyboard.setAlpha(0);keyboard.setTranslationX(-Ui.dp(this,24));keyboard.animate().alpha(1).translationX(0).setDuration(260).setInterpolator(Ui.EASE).start();}else{keyboard.setAlpha(1);keyboard.setTranslationX(0);}}else if(keyboard.getVisibility()==View.VISIBLE&&Ui.motionEnabled(this)){keyboard.animate().alpha(0).translationX(-Ui.dp(this,24)).setDuration(180).withEndAction(()->{if(mode!=0)keyboard.setVisibility(View.GONE);}).start();}else keyboard.setVisibility(View.GONE);if(suggestions!=null)root.removeView(suggestions);if(results!=null)root.removeView(results);
        if(mode<2){suggestions=new FrameLayout(this);Ui.place(root,suggestions,mode==0?368:48,44,198,Math.max(80,Ui.height(this)-44));renderSuggestions();Ui.installFocusMotion(suggestions);}
        results=new FrameLayout(this);float x=mode==0?614:mode==1?306:48;Ui.place(root,results,x,40,960-x,Math.max(80,Ui.height(this)-40));
        if(query.isEmpty()&&mode<2&&Config.recommendations)recommendations();else renderResults(focusResult);
        if(switching&&Ui.motionEnabled(this)){results.setTranslationX(previousX-Ui.dp(this,x));results.setAlpha(.5f);results.animate().translationX(0).alpha(1).setDuration(280).setInterpolator(Ui.EASE).start();if(suggestions!=null){suggestions.setAlpha(.4f);suggestions.setTranslationX(Ui.dp(this,previousMode==0?320:previousMode==1?-320:16));suggestions.animate().translationX(0).alpha(1).setDuration(260).setInterpolator(Ui.EASE).start();}}
        else Ui.reveal(results);
    }
    private List<String> history(){List<String> list=new ArrayList<>();try{JSONArray values=new JSONArray(getPreferences(0).getString("history","[]"));for(int i=0;i<values.length();i++)list.add(values.getString(i));}catch(Exception ignored){}return list;}
    private void remember(String value){if(value.trim().isEmpty())return;List<String> list=history();list.remove(value);list.add(0,value);if(list.size()>15)list=new ArrayList<>(list.subList(0,15));getPreferences(0).edit().putString("history",new JSONArray(list).toString()).apply();}
    private TextView suggestion(String value,Runnable click){TextView button=key(value,null,click);button.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(this,16));button.setGravity(Gravity.CENTER_VERTICAL);button.setPadding(Ui.dp(this,12),0,Ui.dp(this,10),0);button.setSingleLine(true);button.setEllipsize(android.text.TextUtils.TruncateAt.END);button.setOnFocusChangeListener((view,focused)->{if(focused&&mode==0){ui.post(()->{if(mode==0){mode=1;render(false);Ui.focusText(suggestions,value);}});}});return button;}
    private void renderSuggestions(){
        boolean empty=query.isEmpty();TextView title=Ui.text(this,empty?"搜索历史":"猜你想搜",20,Ui.TEXT,false);title.setGravity(Gravity.CENTER_VERTICAL);Ui.place(suggestions,title,0,0,160,40);
        if(empty){TextView clear=key("清空搜索历史","trash",()->{getPreferences(0).edit().remove("history").apply();render(false);});clear.setBackground(Ui.states(Ui.round(0x18202020,this,30),Ui.focusRing(this,30),null));Ui.place(suggestions,clear,164,3,34,34);}
        ViewportScrollView scroll=new ViewportScrollView(this);scroll.setPadding(Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,12));Ui.place(suggestions,scroll,0,56,198,Math.max(50,Ui.height(this)-100));LinearLayout list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);
        List<String> values=new ArrayList<>();
        if(empty)values.addAll(history());else {String all="“"+query+"”的全部结果…";values.add(all);LinkedHashSet<String> names=new LinkedHashSet<>();for(Entry e:matches){names.add(MusicLibrary.songTitle(this,e));if(!"未知歌手".equals(MusicLibrary.artist(this,e)))names.add(MusicLibrary.artist(this,e));if(names.size()>=12)break;}values.addAll(names);}
        if(values.isEmpty()){TextView none=Ui.text(this,"暂无搜索记录",16,Ui.TEXT2,false);list.addView(none,new LinearLayout.LayoutParams(-1,Ui.dp(this,44)));}
        for(int i=0;i<values.size();i++){String value=values.get(i);final boolean all=!empty&&i==0;TextView button=suggestion(value,()->{if(!all){query=value;input.setText(value);remember(value);search();}else remember(query);mode=2;render(true);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(this,36));lp.bottomMargin=Ui.dp(this,8);list.addView(button,lp);}
        if(empty){TextView popular=Ui.text(this,"热门搜索",20,Ui.TEXT,false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(this,48));lp.topMargin=Ui.dp(this,16);list.addView(popular,lp);LinkedHashSet<String> names=new LinkedHashSet<>();for(Entry e:MusicLibrary.read(this,"recent"))names.add(MusicLibrary.artist(this,e));for(Entry e:catalog)names.add(MusicLibrary.songTitle(this,e));int count=0;for(String name:names){if(++count>8)break;TextView button=suggestion(name,()->{remember(name);query=name;input.setText(name);mode=2;search();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,Ui.dp(this,36));p.bottomMargin=Ui.dp(this,8);list.addView(button,p);}}
    }
    private void selectSearchTab(int selected){
        if(tab==selected)return;
        tab=selected;render(false);String[] names={"单曲","艺人","专辑","歌单","听书"};Ui.focusText(results,names[selected]);
    }
    private void renderResults(boolean focusFirst){
        int width=mode==0?298:mode==1?606:864;
        LinearLayout tabs=new LinearLayout(this);tabs.setClipChildren(false);tabs.setClipToPadding(false);Ui.place(results,tabs,12,6,width-24,44);String[] names={"单曲","艺人","专辑","歌单","听书"};List<TextView> buttons=new ArrayList<>();
        for(int i=0;i<names.length;i++){final int selected=i;TextView button=Ui.text(this,names[i],mode==0?17:20,Ui.TEXT,false);button.setGravity(Gravity.CENTER);button.setFocusable(true);button.setClickable(true);button.setSelected(i==tab);button.setBackground(Ui.tabBackground(this));button.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,i==tab?Ui.TEXT:Ui.TEXT2));button.setOnClickListener(v->selectSearchTab(selected));button.setOnFocusChangeListener((v,focused)->{if(focused&&tab!=selected)ui.post(()->{if(v.hasFocus())selectSearchTab(selected);});});tabs.addView(button,new LinearLayout.LayoutParams(0,-1,1));buttons.add(button);}
        List<Entry> source=tab==4?MusicLibrary.read(this,"books"):new ArrayList<>(matches);
        TvVirtualList list;
        if(tab==0||tab==4){
            List<Entry> songs=new ArrayList<>();for(Entry e:source)if(tab==0||query.isEmpty()||e.name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))songs.add(e);
            list=new TvVirtualList(this,songs.size(),mode==2?2:1,72,12,(i,recycled)->LibraryActivity.songCard(this,songs.get(i),false,recycled));
            list.clicked(i->{remember(query);PlayerService.selectTrack(this,songs,i);Ui.open(this,new Intent(this,PlayerActivity.class));});
            if(songs.isEmpty())Ui.place(results,Ui.text(this,"没有匹配的内容",17,Ui.TEXT2,false),12,80,width-24,48);
        }else if(tab==3){
            list=new TvVirtualList(this,1,1,72,12,(i,recycled)->{
                TextView text=recycled instanceof TextView?(TextView)recycled:Ui.text(this,"",19,Ui.TEXT,false);
                text.setText("播放列表 · "+MusicLibrary.playlist(this).size()+" 首");text.setGravity(Gravity.CENTER_VERTICAL);
                text.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);text.setSingleLine(true);text.setEllipsize(android.text.TextUtils.TruncateAt.END);return text;
            });list.clicked(i->Ui.open(this,new Intent(this,LibraryActivity.class).putExtra("collection",0)));
        }else{
            LinkedHashSet<String> unique=new LinkedHashSet<>();for(Entry e:source)unique.add(tab==1?MusicLibrary.artist(this,e):MusicLibrary.album(this,e));List<String> groups=new ArrayList<>(unique);
            list=new TvVirtualList(this,groups.size(),mode==2?2:1,72,12,(i,recycled)->{TextView text=recycled instanceof TextView?(TextView)recycled:Ui.text(this,"",20,Ui.TEXT,false);text.setText(groups.get(i));text.setGravity(Gravity.CENTER_VERTICAL);text.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);text.setSingleLine(true);text.setEllipsize(android.text.TextUtils.TruncateAt.END);return text;});
            list.clicked(i->Ui.open(this,new Intent(this,LibraryActivity.class).putExtra("collection",5).putExtra("category",tab==1?1:2).putExtra("group",groups.get(i))));
        }
        Ui.place(results,list,0,66,width,Math.max(50,Ui.height(this)-120));list.up(()->buttons.get(tab).requestFocus()).left(()->{if(mode<2&&suggestions!=null){ArrayList<View> targets=new ArrayList<>();suggestions.addFocusables(targets,View.FOCUS_FORWARD);if(!targets.isEmpty())targets.get(0).requestFocus();}});if(focusFirst)list.focusPosition(0);Ui.installFocusMotion(results);
    }
    private void recommendations(){
        Ui.place(results,Ui.text(this,"曲库推荐",20,Ui.TEXT,false),0,4,300,34);HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setHorizontalScrollBarEnabled(false);Ui.place(results,horizontal,0,54,mode==0?346:654,Math.max(100,Ui.height(this)-94));FrameLayout cards=new FrameLayout(this);horizontal.addView(cards,new HorizontalScrollView.LayoutParams(Ui.dp(this,452),Ui.dp(this,448)));
        LinkedHashMap<String,List<Entry>> albums=new LinkedHashMap<>();for(Entry e:catalog){String name=MusicLibrary.album(this,e);if(!albums.containsKey(name))albums.put(name,new ArrayList<>());albums.get(name).add(e);}int at=0;
        for(String name:albums.keySet()){if(at>=6)break;List<Entry> tracks=albums.get(name);FrameLayout card=new FrameLayout(this);card.setFocusable(true);card.setClickable(true);card.setBackgroundColor(android.graphics.Color.TRANSPARENT);float x=8+(at%3)*148,y=8+(at/3)*218;Ui.place(cards,card,x,y,140,196);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(Ui.round(Ui.CARD,this,12));image.setClipToOutline(true);Bitmap art=MusicLibrary.artwork(this,tracks.get(0));image.setImageBitmap(art);FrameLayout coverFocus=new FrameLayout(this);coverFocus.setForeground(new GlassDrawable(this,12,false).outlineOnly());coverFocus.setDuplicateParentStateEnabled(true);Ui.place(card,coverFocus,4,4,132,132);coverFocus.addView(image,new FrameLayout.LayoutParams(-1,-1));coverFocus.setPadding(Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,4),Ui.dp(this,4));TextView label=Ui.text(this,name,17,Ui.TEXT,false);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);Ui.place(card,label,8,140,124,48);card.setOnClickListener(v->Ui.open(this,new Intent(this,LibraryActivity.class).putExtra("collection",5).putExtra("category",2).putExtra("group",name)));at++;}
        if(albums.isEmpty())Ui.place(cards,Ui.text(this,"添加音乐后显示推荐",17,Ui.TEXT2,false),0,0,300,48);Ui.installFocusMotion(results);
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_DEL){delete();return true;}int unicode=event.getUnicodeChar();if(unicode>=32&&unicode<127&&key!=KeyEvent.KEYCODE_DPAD_CENTER){if(mode!=0){mode=0;render(false);}append(String.valueOf((char)unicode).toUpperCase(Locale.ROOT));return true;}return super.onKeyDown(key,event);}
    @Override public void onBackPressed(){if(mode==2){mode=1;render(false);return;}if(mode==1){mode=0;render(false);lastKey.requestFocus();return;}super.onBackPressed();}
    @Override public void finish(){super.finish();Ui.exit(this);}
    @Override public void onDestroy(){generation++;ui.removeCallbacksAndMessages(null);io.shutdownNow();super.onDestroy();}
}
