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
        form.addView(section("音乐目录"));
        TextView manage = action("添加音乐目录", true, v -> {collect();SourceBrowser.show(act, () -> status.setText("播放列表已更新，按返回键回到首页即可播放"));});
        form.addView(manage);
        form.addView(action("自定义首页页签",false,v->{collect();HomeModules.show(act,()->{refreshModules();status.setText("首页页签已更新，按返回键回到首页即可查看");});}));
        moduleRows=new LinearLayout(act);moduleRows.setOrientation(1);form.addView(moduleRows);refreshModules();
        TextView tabTip=Ui.text(act,"“我的”为固定页签，其余页签可创建、重命名、排序，并导入各自的音乐目录。",12,Ui.TEXT2,false);
        tabTip.setPadding(0,pad(12),0,pad(12));form.addView(tabTip);
        TextView libraryTip = Ui.text(act,"配置连接后，在目录中选择歌曲添加到首页播放列表。",12,Ui.TEXT2,false);
        libraryTip.setPadding(0,pad(18),0,pad(24));form.addView(libraryTip);
        form.addView(Ui.text(act,"先连接音乐源，再选择目录",18,Ui.TEXT,true));
        TextView guide=Ui.text(act,"群晖 NAS：填写服务器和账号，保存连接配置。\n音乐与听书分别保存，已有内容会自动跳过。",14,Ui.TEXT2,false);guide.setLineSpacing(pad(10),1);guide.setPadding(0,pad(14),0,0);form.addView(guide);

        /* ---- 群晖 NAS ---- */
        form=newPage();
        form.addView(section("群晖 NAS"));
        EditText[] server=pair(form,"地址 / IP",Config.nasHost,false,"端口",String.valueOf(Config.nasPort),true);
        nasHost=server[0];nasPort=server[1];
        nasHttps=check(form,"使用 HTTPS",Config.nasHttps);
        EditText[] account=pair(form,"账号",Config.nasUser,false,"密码",Config.nasPass,false);
        nasUser=account[0];nasPass=account[1];nasPass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);nasPass.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        TextView saveNas=action("保存",true,v->{collect();Config.nasSid="";Config.nasToken="";Config.save(act);status.setText("NAS 配置已保存");});form.addView(saveNas);
        View[] nasFields={nasHost,nasPort,nasHttps,nasUser,nasPass,saveNas};
        for(int at=0;at<nasFields.length;at++){final int position=at;View field=nasFields[at];field.setId(View.generateViewId());field.setOnKeyListener((v,key,event)->{
            if(key!=android.view.KeyEvent.KEYCODE_DPAD_UP&&key!=android.view.KeyEvent.KEYCODE_DPAD_DOWN)return false;
            if(event.getAction()==android.view.KeyEvent.ACTION_DOWN){int next=Math.max(0,Math.min(nasFields.length-1,position+(key==android.view.KeyEvent.KEYCODE_DPAD_DOWN?1:-1)));nasFields[next].requestFocus();}
            return true;
        });}


        form=newPage();form.addView(section("主题外观"));
        TextView themeTip=Ui.text(act,"选择明暗外观，立即应用并自动记住选择。",14,Ui.TEXT2,false);form.addView(themeTip);
        form.addView(action((Ui.light?"":"当前 · ")+"深色光感玻璃",false,v->changeTheme(false)));
        form.addView(action((Ui.light?"当前 · ":"")+"亮色光感玻璃",false,v->changeTheme(true)));
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
        close();act.recreate();
    }
    public void back(){if(detail)showHub();else close();}
    private void hubDescription(int index){String[] descriptions={"添加音乐或听书目录，管理自己的音乐资料库","连接群晖 NAS，读取服务器中的音乐目录","切换深色或亮色光感玻璃外观"};hubCaption.setText(descriptions[index]);}
    private void showHub(){detail=false;heading.setText("设置");navPane.setVisibility(View.VISIBLE);hubInfo.setVisibility(View.VISIBLE);scroller.setVisibility(View.GONE);hubDescription(currentPage);if(!navigation.isEmpty())navigation.get(currentPage).requestFocus();}
    private void selectPage(int index) {
        currentPage=index;detail=true;heading.setText(navigation.get(index).getText());navPane.setVisibility(View.GONE);hubInfo.setVisibility(View.GONE);scroller.setVisibility(View.VISIBLE);
        for(int i=0;i<pages.size();i++)pages.get(i).setVisibility(i==index?View.VISIBLE:View.GONE);
        for(int i=0;i<navigation.size();i++){TextView item=navigation.get(i);item.setSelected(i==index);item.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,i==index?Ui.TEXT:Ui.TEXT2));}
        scroller.scrollTo(0,0);Ui.reveal(pages.get(index));pages.get(index).post(()->{java.util.ArrayList<View> focusables=new java.util.ArrayList<>();pages.get(index).addFocusables(focusables,View.FOCUS_FORWARD);if(!focusables.isEmpty())focusables.get(0).requestFocus();});
    }
    private EditText[] pair(LinearLayout parent,String left,String leftValue,boolean leftNumber,String right,String rightValue,boolean rightNumber) {
        LinearLayout row=new LinearLayout(act);parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout a=new LinearLayout(act),b=new LinearLayout(act);a.setOrientation(LinearLayout.VERTICAL);b.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.rightMargin=pad(14);row.addView(a,lp);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        return new EditText[]{field(a,left,leftValue,false,leftNumber),field(b,right,rightValue,false,rightNumber)};
    }

    private void refreshModules(){
        if(moduleRows==null)return;moduleRows.removeAllViews();
        for(HomeModules.Module module:HomeModules.all(act))moduleRows.addView(action(module.name+" · "+HomeModules.entries(act,module.id).size()+" 首",false,v->HomeModules.manage(act,module.id,()->{refreshModules();Ui.installFocusMotion(moduleRows);} )));
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

    private void testNas() {
        collect();
        status.setText("正在连接 NAS…");
        io.execute(new Runnable() {
            @Override public void run() {
                String msg;
                try {
                    Synology.login();
                    Config.save(act);
                    List<Entry> shares = Synology.listShares();
                    msg = "NAS 连接成功：" + (Config.nasName.length() > 0 ? Config.nasName + " · " : "")
                            + shares.size() + " 个共享文件夹";
                } catch (Exception e) {
                    msg = "NAS 连接失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage());
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override public void run() { status.setText(m); }
                });
            }
        });
    }

    /* --------------------------------- 关闭 --------------------------------- */

    public void close() {
        if (closed) return;
        closed = true;
        ui.removeCallbacksAndMessages(null);
        collect();
        try { io.shutdownNow(); } catch (Throwable ignored) { }
        final View closing=root;root=null;
        Runnable finish=()->{if(closing!=null&&closing.getParent()instanceof ViewGroup)((ViewGroup)closing.getParent()).removeView(closing);if(onClose!=null)onClose.run();};
        if(closing!=null&&Ui.motionEnabled(act))closing.animate().alpha(0).setDuration(160).withEndAction(finish).start();else finish.run();
    }
}
