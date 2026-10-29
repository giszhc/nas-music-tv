package com.yunying.music;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设置面板：全屏浮层，一个面板装下所有设置（群晖 NAS / 音乐目录）。
 * 首页不再放「设置」入口，只保留右上角齿轮。
 */
public class SettingsPanel {

    private final Activity act;
    private final ViewGroup host;
    private final Runnable onClose;
    private View root;
    private ScrollView scroller;
    private FrameLayout pageHost;
    private View navPane;private LinearLayout hubInfo;private TextView heading,hubCaption;private View hubIcon;private boolean detail;private int currentPage;
    private final java.util.List<LinearLayout> pages=new java.util.ArrayList<>();
    private final java.util.List<TextView> navigation=new java.util.ArrayList<>();

    private EditText nasHost, nasPort, nasUser, nasPass;
    private CheckBox nasHttps;
    private LinearLayout nasLoginForm,nasAccount;
    private TextView nasNickname,nasInitial,loginNas;
    private boolean nasBusy,themeChanged,scanRunning;
    private int scanGeneration;private java.util.concurrent.Future<?> scanTask;
    private LinearLayout discoveredNas;private TextView scanStatus;
    private TextView status;
    private LinearLayout moduleRows;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean closed;

    public static SettingsPanel show(Activity a, ViewGroup host, Runnable onClose) {
        SettingsPanel p = new SettingsPanel(a, host, onClose);
        p.build();
        return p;
    }

    private SettingsPanel(Activity a, ViewGroup host, Runnable onClose) {
        this.act = a;
        this.host = host;
        this.onClose = onClose;
    }

    public void showSection(int index){if(index>0)selectPage(Math.min(pages.size()-1,index));else showHub();}

    public boolean isOpen() { return root != null && !closed; }

    /* -------------------------------- 构建 -------------------------------- */

    private void build() {
        Config.load(act);
        FrameLayout overlay = new FrameLayout(act);
        overlay.setBackground(new ImmersiveBackground());
        overlay.setClickable(true);
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { }
        });


        LinearLayout panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        panel.setPadding(pad(48), pad(34), pad(48), pad(20));
        panel.setClickable(true);

        int screenH=act.getResources().getDisplayMetrics().heightPixels;
        int screenW=act.getResources().getDisplayMetrics().widthPixels;
        FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(screenW,screenH,Gravity.CENTER);
        panel.setClipChildren(true);panel.setClipToPadding(true);
        overlay.addView(panel, plp);

        /* 标题栏 */
        LinearLayout head = new LinearLayout(act);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = Ui.text(act, "设置",29,Ui.TEXT,false);heading=title;
        head.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));


        TextView sub = Ui.text(act, "音乐目录、连接、登录与播放偏好统一在这里管理",
                13, Ui.TEXT2, false);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = pad(6);
        slp.bottomMargin = pad(28);
        panel.addView(sub, slp);sub.setVisibility(View.GONE);
        View headerGap=new View(act);panel.addView(headerGap,new LinearLayout.LayoutParams(1,pad(18)));

        LinearLayout body=new LinearLayout(act);body.setOrientation(LinearLayout.HORIZONTAL);
        body.setClipChildren(true);body.setClipToPadding(true);
        panel.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(act);nav.setOrientation(LinearLayout.VERTICAL);nav.setClipChildren(false);nav.setClipToPadding(false);
        LinearLayout.LayoutParams navLp=new LinearLayout.LayoutParams(pad(468),-1);navLp.leftMargin=0;navLp.rightMargin=pad(36);ViewportScrollView navScroll=new ViewportScrollView(act);navScroll.setPadding(pad(12),pad(8),pad(12),pad(8));navScroll.addView(nav,new ScrollView.LayoutParams(-1,-2));body.addView(navScroll,navLp);navPane=navScroll;
        scroller=new ViewportScrollView(act);scroller.setVerticalScrollBarEnabled(false);scroller.setFocusable(false);
        body.addView(scroller,new LinearLayout.LayoutParams(0,-1,1));
        pageHost=new FrameLayout(act);pageHost.setClipChildren(false);scroller.addView(pageHost,new ScrollView.LayoutParams(-1,-2));
        hubInfo=new LinearLayout(act);hubInfo.setOrientation(1);hubInfo.setGravity(Gravity.CENTER);body.addView(hubInfo,new LinearLayout.LayoutParams(0,-1,1));
        hubIcon=new ServiceSettingsActivity.ServiceMark(act);hubInfo.addView(hubIcon,new LinearLayout.LayoutParams(pad(Ui.height(act)<440?100:160),pad(Ui.height(act)<440?100:160)));
        hubCaption=Ui.text(act,"",14,Ui.TEXT2,false);hubCaption.setGravity(Gravity.CENTER);hubCaption.setPadding(0,pad(28),0,0);hubInfo.addView(hubCaption,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout form=newPage();
        TextView directoryIntro=Ui.text(act,"从群晖导入音乐，或为首页页签管理独立内容",14,Ui.TEXT2,false);directoryIntro.setPadding(0,0,0,pad(20));form.addView(directoryIntro);
        form.addView(directoryAction("添加音乐目录","浏览群晖文件夹 · 支持导入音乐与听书",v->{collect();SourceBrowser.show(act,()->status.setText("播放列表已更新"));}));
        form.addView(directoryAction("导入本地目录","选择设备或外接存储中的文件夹，导入到播放列表",v->{collect();act.startActivity(new android.content.Intent(act,LocalImportActivity.class));}));
        TextView tabsHeading=Ui.text(act,"首页页签",20,Ui.TEXT,true);tabsHeading.setPadding(0,pad(24),0,pad(12));form.addView(tabsHeading);
        form.addView(directoryAction("自定义首页页签","创建、重命名与排序；“我的”为固定页签",v->{collect();HomeModules.show(act,()->{refreshModules();status.setText("首页页签已更新");});}));
        moduleRows=new LinearLayout(act);moduleRows.setOrientation(1);form.addView(moduleRows);refreshModules();
        TextView importTip=Ui.text(act,"导入包含所有子目录，已有内容自动跳过。移除页签或清空内容不会删除群晖源文件。",13,Ui.TEXT2,false);importTip.setPadding(0,pad(16),0,0);importTip.setLineSpacing(pad(4),1);form.addView(importTip);

        /* ---- 群晖 NAS ---- */
        form=newPage();
        LinearLayout nasPage=form;
        nasAccount=new LinearLayout(act);nasAccount.setOrientation(1);nasAccount.setGravity(Gravity.CENTER_HORIZONTAL);nasAccount.setPadding(pad(28),pad(32),pad(28),pad(32));nasAccount.setBackground(Ui.glass(act,16));nasPage.addView(nasAccount);
        LinearLayout identity=new LinearLayout(act);identity.setGravity(Gravity.CENTER_VERTICAL);nasAccount.addView(identity,new LinearLayout.LayoutParams(-2,-2));
        nasInitial=Ui.text(act,"",28,Ui.TEXT,true);nasInitial.setGravity(Gravity.CENTER);nasInitial.setBackground(Ui.glass(act,32));identity.addView(nasInitial,new LinearLayout.LayoutParams(pad(64),pad(64)));
        nasNickname=Ui.text(act,"",24,Ui.TEXT,true);nasNickname.setSingleLine(true);nasNickname.setEllipsize(android.text.TextUtils.TruncateAt.END);nasNickname.setMaxWidth(pad(360));LinearLayout.LayoutParams nameLp=new LinearLayout.LayoutParams(-2,-2);nameLp.leftMargin=pad(18);identity.addView(nasNickname,nameLp);
        TextView logout=action("退出登录",false,v->logoutNas());logout.setGravity(Gravity.CENTER);LinearLayout.LayoutParams logoutLp=new LinearLayout.LayoutParams(-2,pad(48));logoutLp.topMargin=pad(28);logoutLp.gravity=Gravity.CENTER_HORIZONTAL;nasAccount.addView(logout,logoutLp);
        nasLoginForm=new LinearLayout(act);nasLoginForm.setOrientation(1);nasPage.addView(nasLoginForm);form=nasLoginForm;
        scanStatus=Ui.text(act,"进入此页自动发现局域网群晖",14,Ui.TEXT2,false);scanStatus.setPadding(0,0,0,pad(10));form.addView(scanStatus);
        form.addView(action("重新扫描",false,v->scanNas()));
        discoveredNas=new LinearLayout(act);discoveredNas.setOrientation(1);form.addView(discoveredNas);
        EditText[] server=pair(form,"地址 / IP",Config.nasHost,false,"端口",String.valueOf(Config.nasPort),true);
        nasHost=server[0];nasPort=server[1];
        nasHttps=check(form,"使用 HTTPS",Config.nasHttps);
        EditText[] account=pair(form,"账号",Config.nasUser,false,"密码",Config.nasPass,false);
        nasUser=account[0];nasPass=account[1];nasPass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);nasPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        TextView saveNas=action("登录",true,v->testNas());loginNas=saveNas;form.addView(saveNas);
        View[] nasFields={nasHost,nasPort,nasHttps,nasUser,nasPass,saveNas};
        for(int at=0;at<nasFields.length;at++){final int position=at;View field=nasFields[at];field.setId(View.generateViewId());field.setOnKeyListener((v,key,event)->{
            if(key!=android.view.KeyEvent.KEYCODE_DPAD_UP&&key!=android.view.KeyEvent.KEYCODE_DPAD_DOWN)return false;
            if(event.getAction()==android.view.KeyEvent.ACTION_DOWN){int next=Math.max(0,Math.min(nasFields.length-1,position+(key==android.view.KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));nasFields[next].requestFocus();}
            return true;
        });}


        refreshNasAccount();
        form=newPage();
        TextView themeTip=Ui.text(act,"选择喜欢的外观，立即应用并自动保存",14,Ui.TEXT2,false);themeTip.setPadding(0,0,0,pad(20));form.addView(themeTip);
        LinearLayout themes=new LinearLayout(act);themes.setClipChildren(false);themes.setClipToPadding(false);form.addView(themes,new LinearLayout.LayoutParams(-1,-2));
        for(int index=0;index<2;index++){
            final boolean light=index==1;final boolean enabled=Ui.light==light;
            LinearLayout card=new LinearLayout(act);card.setOrientation(1);card.setPadding(pad(12),pad(12),pad(12),pad(16));card.setBackground(Ui.cardFocusable(act,16));card.setFocusable(true);card.setClickable(true);card.setOnClickListener(v->changeTheme(light));card.setContentDescription((light?"亮色光感玻璃":"深色光感玻璃")+(enabled?"，已启用":""));
            LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(0,-2,1);if(index==0)cardLp.rightMargin=pad(18);themes.addView(card,cardLp);
            card.addView(new ThemePreviewView(act,light),new LinearLayout.LayoutParams(-1,pad(188)));
            TextView name=Ui.text(act,light?"亮色光感玻璃":"深色光感玻璃",18,Ui.TEXT,true);name.setPadding(pad(4),pad(16),0,pad(6));card.addView(name);
            LinearLayout stateRow=new LinearLayout(act);stateRow.setGravity(Gravity.CENTER_VERTICAL);stateRow.setPadding(pad(4),0,0,0);card.addView(stateRow,new LinearLayout.LayoutParams(-1,pad(28)));
            View check=new View(act){final android.graphics.Paint paint=new android.graphics.Paint(3);protected void onDraw(android.graphics.Canvas canvas){if(!enabled)return;paint.setColor(Ui.BRAND);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(pad(2));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);android.graphics.Path path=new android.graphics.Path();path.moveTo(getWidth()*.15f,getHeight()*.5f);path.lineTo(getWidth()*.4f,getHeight()*.72f);path.lineTo(getWidth()*.85f,getHeight()*.25f);canvas.drawPath(path,paint);}};
            if(enabled){LinearLayout.LayoutParams checkLp=new LinearLayout.LayoutParams(pad(18),pad(18));checkLp.rightMargin=pad(8);stateRow.addView(check,checkLp);}
            TextView state=Ui.text(act,enabled?"当前使用":"选择此主题",14,enabled?Ui.BRAND:Ui.TEXT2,enabled);stateRow.addView(state);
            card.setOnFocusChangeListener((v,focused)->{name.setTextColor(focused?Ui.TEXT_ON_LIGHT:Ui.TEXT);state.setTextColor(focused?Ui.TEXT2_ON_LIGHT:enabled?Ui.BRAND:Ui.TEXT2);});

        }
        String[] sections={"音乐目录","群晖 NAS","主题外观"};
        for(int i=0;i<sections.length;i++) {final int index=i;TextView item=button(sections[i],Ui.PANEL,Ui.LINE,Ui.TEXT,v->selectPage(index));item.setTypeface(android.graphics.Typeface.DEFAULT);item.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,pad(20));item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(pad(16),0,pad(16),0);item.setOnFocusChangeListener((v,focused)->{if(focused)hubDescription(index);});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,pad(48));lp.bottomMargin=pad(8);nav.addView(item,lp);navigation.add(item);}

        /* ---- 状态 ---- */
        status = Ui.text(act, "", 12, Ui.BRAND, false);status.setMaxLines(2);status.setMinHeight(pad(30));
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stp.topMargin = pad(14);
        panel.addView(status, stp);

        host.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root = overlay;
        showHub();Ui.installFocusMotion(overlay);Ui.reveal(panel);navigation.get(0).requestFocus();
    }

    /* -------------------------------- 组件 -------------------------------- */

    private LinearLayout newPage() {
        LinearLayout page=new LinearLayout(act);page.setOrientation(LinearLayout.VERTICAL);page.setClipChildren(false);page.setClipToPadding(false);page.setPadding(pad(28),pad(12),pad(28),pad(40));
        pages.add(page);pageHost.addView(page,new FrameLayout.LayoutParams(-1,-2));return page;
    }
    private void changeTheme(boolean light){
        if(Ui.light==light)return;
        collect();Config.lightTheme=light;Config.save(act);Ui.applyTheme(light);
        themeChanged=true;cancelScan();ui.removeCallbacksAndMessages(null);host.removeView(root);pages.clear();navigation.clear();build();selectPage(2);
    }
    public void back(){if(detail)showHub();else close();}
    private void hubDescription(int index){String[] descriptions={"添加音乐或听书目录，管理自己的音乐资料库","连接群晖 NAS，读取服务器中的音乐目录","切换深色或亮色光感玻璃外观"};hubCaption.setText(descriptions[index]);}
    private void showHub(){cancelScan();detail=false;heading.setText("设置");navPane.setVisibility(View.VISIBLE);hubInfo.setVisibility(View.VISIBLE);scroller.setVisibility(View.GONE);hubDescription(currentPage);if(!navigation.isEmpty())navigation.get(currentPage).requestFocus();}
    private void selectPage(int index) {
        if(index!=1)cancelScan();
        currentPage=index;detail=true;heading.setText(navigation.get(index).getText());navPane.setVisibility(View.GONE);hubInfo.setVisibility(View.GONE);scroller.setVisibility(View.VISIBLE);
        for(int i=0;i<pages.size();i++)pages.get(i).setVisibility(i==index?View.VISIBLE:View.GONE);
        for(int i=0;i<navigation.size();i++){TextView item=navigation.get(i);item.setSelected(i==index);item.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,i==index?Ui.TEXT:Ui.TEXT2));}
        if(index==1&&Config.nasSid.isEmpty())scanNas();
        scroller.scrollTo(0,0);Ui.reveal(pages.get(index));pages.get(index).post(()->{java.util.ArrayList<View> focusables=new java.util.ArrayList<>();pages.get(index).addFocusables(focusables,View.FOCUS_FORWARD);if(!focusables.isEmpty())focusables.get(0).requestFocus();});
    }
    private EditText[] pair(LinearLayout parent,String left,String leftValue,boolean leftNumber,String right,String rightValue,boolean rightNumber) {
        LinearLayout row=new LinearLayout(act);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout a=new LinearLayout(act),b=new LinearLayout(act);a.setOrientation(LinearLayout.VERTICAL);b.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.rightMargin=pad(14);row.addView(a,lp);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        return new EditText[]{field(a,left,leftValue,false,leftNumber),field(b,right,rightValue,false,rightNumber)};
    }

    private View directoryAction(String title,String description,View.OnClickListener click){
        LinearLayout row=new LinearLayout(act);row.setOrientation(1);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(pad(20),pad(14),pad(20),pad(14));row.setFocusable(true);row.setClickable(true);row.setBackground(Ui.cardFocusable(act,14));row.setOnClickListener(click);
        TextView name=Ui.text(act,title,18,Ui.TEXT,true);row.addView(name);
        TextView caption=Ui.text(act,description,13,Ui.TEXT2,false);caption.setPadding(0,pad(6),0,0);caption.setMaxLines(2);row.addView(caption);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=pad(10);row.setLayoutParams(lp);
        row.setOnFocusChangeListener((v,focused)->{name.setTextColor(focused?Ui.TEXT_ON_LIGHT:Ui.TEXT);caption.setTextColor(focused?Ui.TEXT2_ON_LIGHT:Ui.TEXT2);});return row;
    }
    private void refreshModules(){
        if(moduleRows==null)return;moduleRows.removeAllViews();
        for(HomeModules.Module module:HomeModules.all(act))moduleRows.addView(directoryAction(module.name+" · "+HomeModules.entries(act,module.id).size()+" 首","导入专属目录、排序或管理内容",v->HomeModules.manage(act,module.id,()->{refreshModules();Ui.installFocusMotion(moduleRows);} )));
        Ui.installFocusMotion(moduleRows);
    }
    private int pad(int dp) { return Ui.dp(act, dp); }

    private TextView section(String t) {
        TextView tv = Ui.text(act, t, Ui.isTv(act) ? 17 : 15, Ui.TEXT, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad(16);
        lp.bottomMargin = pad(8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText field(LinearLayout parent, String label, String value,
                           boolean password, boolean number) {
        TextView l = Ui.text(act, label, 11, Ui.TEXT3, false);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = pad(8);
        parent.addView(l, llp);

        EditText e = new EditText(act);
        e.setText(value == null ? "" : value);
        int type = number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT;
        if (password) type |= InputType.TYPE_TEXT_VARIATION_PASSWORD;
        e.setInputType(type);if(password)e.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        e.setSingleLine(true);
        e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(act,Ui.isTv(act)?15:14));
        e.setTextColor(Ui.TEXT);
        e.setHintTextColor(Ui.TEXT3);
        e.setPadding(pad(12), pad(10), pad(12), pad(10));
        e.setBackground(Ui.cardFocusable(act, 10));e.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));e.setMinHeight(pad(48));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = pad(2);
        parent.addView(e, lp);
        return e;
    }

    private CheckBox check(LinearLayout parent, String label, boolean value) {
        CheckBox c = new CheckBox(act);
        c.setText(label);
        c.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));c.setBackground(Ui.cardFocusable(act,10));
        c.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(act,13));
        c.setChecked(value);
        c.setPadding(pad(14), pad(12), pad(14), pad(12));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,pad(50));clp.bottomMargin=pad(12);c.setLayoutParams(clp);
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.BRAND));
        parent.addView(c);
        return c;
    }

    private TextView action(String label, boolean primary, View.OnClickListener l) {
        return button(label, primary ? Ui.BRAND : Ui.PANEL,
                primary ? Ui.BRAND : Ui.LINE, primary ? Ui.ON_BRAND : Ui.TEXT2, l);
    }

    private TextView button(String label, int bg, int stroke, int fg, View.OnClickListener l) {
        TextView t = Ui.text(act, label, Ui.isTv(act) ? 15 : 13, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(pad(18), pad(11), pad(18), pad(11));
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(Ui.cardFocusable(act, 10));
        t.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));
        t.setMinHeight(pad(48));LinearLayout.LayoutParams buttonLp=new LinearLayout.LayoutParams(-2,-2);buttonLp.rightMargin=pad(12);buttonLp.bottomMargin=pad(14);t.setLayoutParams(buttonLp);
        t.setOnClickListener(l);
        return t;
    }

    private void collect() {
        Config.nasHost = nasHost.getText().toString().trim();
        try {
            Config.nasPort = Integer.parseInt(nasPort.getText().toString().trim());
        } catch (Exception e) {
            Config.nasPort = 5000;
        }
        Config.nasHttps = nasHttps.isChecked();
        Config.nasUser = nasUser.getText().toString().trim();
        Config.nasPass = nasPass.getText().toString();
        Config.save(act);
    }

    private void cancelScan(){scanGeneration++;if(scanTask!=null)scanTask.cancel(true);scanTask=null;scanRunning=false;}
    private void scanNas(){
        if(scanRunning||closed||!Config.nasSid.isEmpty())return;
        final int ticket=++scanGeneration;scanRunning=true;discoveredNas.removeAllViews();scanStatus.setText("正在扫描当前局域网（DSM 5000 / 5001）…");
        scanTask=io.submit(()->{String error=null;try{NasDiscovery.scan(act,(host,port,https)->ui.post(()->{
            if(closed||ticket!=scanGeneration)return;
            discoveredNas.addView(action(host+":"+port,false,v->{nasHost.setText(host);nasPort.setText(String.valueOf(port));nasHttps.setChecked(https);scanStatus.setText("已选择设备，请填写账号密码登录");nasUser.requestFocus();}));
        }));}catch(InterruptedException ex){Thread.currentThread().interrupt();return;}catch(Exception ex){error=ex.getMessage();}
        final String message=error;ui.post(()->{if(closed||ticket!=scanGeneration)return;scanRunning=false;scanStatus.setText(message!=null?message+"，可手动填写地址":discoveredNas.getChildCount()==0?"未发现群晖，可重新扫描或手动填写地址":"发现 "+discoveredNas.getChildCount()+" 台设备，选择后登录");});});
    }
    private void refreshNasAccount(){
        boolean signedIn=!Config.nasSid.isEmpty()&&!Config.nasLoggedOut;
        nasAccount.setVisibility(signedIn?View.VISIBLE:View.GONE);nasLoginForm.setVisibility(signedIn?View.GONE:View.VISIBLE);
        String name=Config.nasUser;nasNickname.setText(name);nasInitial.setText(name.isEmpty()?"NAS":name.substring(0,name.offsetByCodePoints(0,1)).toUpperCase(java.util.Locale.ROOT));
    }
    private void logoutNas(){
        if(nasBusy)return;
        Config.nasLoggedOut=true;Config.nasSid="";Config.nasToken="";Config.nasPass="";nasPass.setText("");Config.save(act);
        if(PlayerService.instance!=null)PlayerService.command(act,PlayerService.ACTION_STOP,-1);
        refreshNasAccount();status.setText("已退出登录");nasHost.requestFocus();scanNas();
    }
    private void testNas(){
        if(nasBusy)return;cancelScan();collect();nasBusy=true;loginNas.setEnabled(false);status.setText("正在登录群晖 NAS…");
        Config.nasLoggedOut=false;Config.nasSid="";Config.nasToken="";
        io.execute(()->{
            String message;
            try{Synology.login();Config.save(act);message="";}
            catch(Exception ex){Config.nasSid="";Config.nasToken="";Config.nasLoggedOut=true;Config.save(act);message="登录失败："+ex.getMessage();}
            final String text=message;
            ui.post(()->{nasBusy=false;if(closed)return;loginNas.setEnabled(true);refreshNasAccount();status.setText(text);if(!Config.nasSid.isEmpty())Ui.focusText(nasAccount,"退出登录");});
        });
    }

    /* --------------------------------- 关闭 --------------------------------- */

    public void close() {
        if (closed) return;
        closed = true;cancelScan();
        ui.removeCallbacksAndMessages(null);
        collect();
        try { io.shutdownNow(); } catch (Throwable ignored) { }
        final View closing=root;root=null;
        Runnable finish=()->{if(closing!=null&&closing.getParent()instanceof ViewGroup)((ViewGroup)closing.getParent()).removeView(closing);if(themeChanged&&!act.isFinishing())act.recreate();else if(onClose!=null)onClose.run();};
        if(closing!=null&&Ui.motionEnabled(act))closing.animate().alpha(0).setDuration(160).withEndAction(finish).start();else finish.run();
    }
}
