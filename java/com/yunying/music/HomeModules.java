package com.yunying.music;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** 首页用户分区；每个分区保存独立媒体列表，删除分区不删除源文件。 */
final class HomeModules {
    static final class Module {final String id,name;final boolean spoken;Module(String id,String name,boolean spoken){this.id=id;this.name=name;this.spoken=spoken;}}
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("home_modules",0);}
    static List<Module> all(Context c){List<Module> result=new ArrayList<>();try{JSONArray a=new JSONArray(prefs(c).getString("modules","[]"));for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);result.add(new Module(j.getString("id"),j.getString("name"),j.optBoolean("spoken")));}}catch(Exception ignored){}return result;}
    private static void save(Context c,List<Module> modules){JSONArray a=new JSONArray();for(Module m:modules)try{JSONObject j=new JSONObject();j.put("id",m.id);j.put("name",m.name);j.put("spoken",m.spoken);a.put(j);}catch(Exception ignored){}prefs(c).edit().putString("modules",a.toString()).apply();}
    static Module get(Context c,String id){for(Module m:all(c))if(m.id.equals(id))return m;return null;}
    static String name(Context c,String id){Module m=get(c,id);return m==null?"自定义内容":m.name;}
    static List<Entry> entries(Context c,String id){return MusicLibrary.read(c,"module_"+id);}
    static int add(Context c,String id,List<Entry> source){Module m=get(c,id);if(m==null)return 0;List<Entry> list=entries(c,id);Set<String> keys=new HashSet<>();for(Entry e:list)keys.add(MusicLibrary.key(e));int n=0;for(Entry e:source)if(e.isAudio()&&keys.add(MusicLibrary.key(e))){Entry copy=new Entry(e.name,false,e.source);copy.path=e.path;copy.id=e.id;copy.size=e.size;copy.audiobook=m.spoken;list.add(copy);n++;}MusicLibrary.write(c,"module_"+id,list);return n;}
    static void show(Activity a,Runnable changed){List<Module> modules=all(a);String[] choices=new String[modules.size()+1];choices[0]="创建首页页签";for(int i=0;i<modules.size();i++)choices[i+1]=modules.get(i).name;showList(a,"自定义首页页签",choices,i->{if(i==0)edit(a,null,changed);else manage(a,modules.get(i-1).id,changed);});}
    static void manage(Activity a,String id,Runnable changed){Module m=get(a,id);if(m==null){changed.run();return;}new TvChoiceDialog(a,m.name,new String[]{"导入专属目录","重命名","上移","下移","清空导入内容","删除页签"},-1,i->{
        if(i==0)SourceBrowser.show(a,id,changed);
        else if(i==1)edit(a,m,changed);
        else if(i==2||i==3){List<Module> modules=all(a);for(int at=0;at<modules.size();at++)if(modules.get(at).id.equals(id)){int next=at+(i==2?-1:1);if(next>=0&&next<modules.size())Collections.swap(modules,at,next);break;}save(a,modules);changed.run();}
        else if(i==4)new TvChoiceDialog(a,"清空“"+m.name+"”的导入内容？",new String[]{"保留内容","清空内容（保留源文件）"},-1,choice->{
            if(choice==1){MusicLibrary.write(a,"module_"+id,Collections.emptyList());changed.run();android.widget.Toast.makeText(a,"已清空页签内容，源文件保留",0).show();}
        }).show();
        else new TvChoiceDialog(a,"删除“"+m.name+"”？",new String[]{"保留模块","删除页签（保留源文件）"},-1,choice->{if(choice==1){List<Module> modules=all(a);for(int at=modules.size()-1;at>=0;at--)if(modules.get(at).id.equals(id))modules.remove(at);save(a,modules);MusicLibrary.write(a,"module_"+id,Collections.emptyList());changed.run();}}).show();
    }).show();}
    private static void showList(Activity a,String title,String[] choices,TvChoiceDialog.Choice action){
        Dialog d=new Dialog(a);d.requestWindowFeature(1);LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);panel.setPadding(Ui.dp(a,24),Ui.dp(a,24),Ui.dp(a,24),Ui.dp(a,20));Ui.dialogGlass(a,panel,16);panel.addView(Ui.text(a,title,24,Ui.TEXT,false),new LinearLayout.LayoutParams(-1,Ui.dp(a,48)));
        TvVirtualList list=new TvVirtualList(a,choices.length,1,52,10,(p,old)->{TextView text=old instanceof TextView?(TextView)old:Ui.text(a,"",18,Ui.TEXT,false);text.setText(choices[p]);text.setGravity(Gravity.CENTER_VERTICAL);text.setPadding(Ui.dp(a,18),0,Ui.dp(a,18),0);return text;});list.clicked(i->{d.dismiss();action.selected(i);});panel.addView(list,new LinearLayout.LayoutParams(-1,0,1));d.setContentView(panel);d.show();if(d.getWindow()!=null){d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.getWindow().setLayout(Ui.dp(a,500),(int)(a.getResources().getDisplayMetrics().heightPixels*.8));}list.focusPosition(0);
    }
    private static void edit(Activity a,Module original,Runnable changed){
        Dialog d=new Dialog(a);d.requestWindowFeature(1);LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);panel.setPadding(Ui.dp(a,28),Ui.dp(a,24),Ui.dp(a,28),Ui.dp(a,24));Ui.dialogGlass(a,panel,16);
        panel.addView(Ui.text(a,original==null?"创建首页页签":"重命名页签",24,Ui.TEXT,false));TextView hint=Ui.text(a,"页签名称，例如：音乐汇、相声、听书",14,Ui.TEXT2,false);hint.setPadding(0,Ui.dp(a,18),0,Ui.dp(a,10));panel.addView(hint);
        EditText name=new EditText(a);name.setSingleLine(true);name.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(a,18));name.setTextColor(Ui.TEXT);name.setBackground(Ui.outline(a,10));name.setPadding(Ui.dp(a,14),Ui.dp(a,12),Ui.dp(a,14),Ui.dp(a,12));name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});name.setHint("输入页签名称");name.setHintTextColor(Ui.TEXT3);if(original!=null)name.setText(original.name);panel.addView(name,new LinearLayout.LayoutParams(-1,Ui.dp(a,52)));
        final boolean[] spoken={original==null||original.spoken};TextView type=Ui.action(a,spoken[0]?"内容类型：有声内容（相声 / 听书）":"内容类型：音乐",16,v->{});type.setOnClickListener(v->new TvChoiceDialog(a,"内容类型",new String[]{"有声内容（相声 / 听书，不匹配歌词）","音乐（使用歌词与封面）"},spoken[0]?0:1,i->{spoken[0]=i==0;type.setText(spoken[0]?"内容类型：有声内容（相声 / 听书）":"内容类型：音乐");}).show());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(a,48));lp.topMargin=Ui.dp(a,16);panel.addView(type,lp);if(original!=null)type.setVisibility(View.GONE);
        TextView error=Ui.text(a,"",13,Ui.TEXT2,false);error.setMinHeight(Ui.dp(a,24));panel.addView(error);LinearLayout actions=new LinearLayout(a);actions.setClipChildren(false);panel.addView(actions);
        TextView save=Ui.action(a,original==null?"创建页签":"保存名称",18,v->{String text=name.getText().toString().trim();if(text.isEmpty()){error.setText("请填写页签名称");name.requestFocus();return;}if("我的".equals(text)){error.setText("“我的”为固定页签，请使用其他名称");return;}List<Module> modules=all(a);for(Module m:modules)if(m.name.equals(text)&&(original==null||!m.id.equals(original.id))){error.setText("已有同名页签，请换一个名称");return;}Module updated=new Module(original==null?UUID.randomUUID().toString():original.id,text,spoken[0]);if(original==null)modules.add(updated);else for(int i=0;i<modules.size();i++)if(modules.get(i).id.equals(original.id))modules.set(i,updated);save(a,modules);d.dismiss();changed.run();});
        actions.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(a,48),1));d.setContentView(panel);d.show();if(d.getWindow()!=null){d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.getWindow().setLayout(Ui.dp(a,580),-2);d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}name.requestFocus();
    }
}
