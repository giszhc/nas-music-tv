package com.yunying.music;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设置面板：全屏浮层，一个面板装下所有设置（NAS / 阿里云盘扫码 / 通用）。
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
    private volatile int qrGeneration;

    private EditText nasHost, nasPort, nasUser, nasPass;
    private CheckBox nasHttps;
    private EditText aliId, aliSecret, aliRefresh;
    private TextView status, qrHint, aliStatus;
    private ImageView qrImage,aliAvatar;
    private TextView aliAccountName;
    private LinearLayout moduleRows;
    private String shownAvatar="";private boolean profileRequested;
    private LinearLayout qrBox;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean polling;
    private AliyunLogin.QrSession qrSession;
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
        overlay.setBackgroundColor(Ui.BG_TOP);
        overlay.setClickable(true);
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { }
        });


        LinearLayout panel = new LinearLayout(act);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(Ui.BG_TOP);
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
        TextView guide=Ui.text(act,"群晖 NAS：填写服务器和账号，保存连接配置。\n阿里云盘：用手机扫码登录后，返回这里添加目录。\n音乐与听书分别保存，已有内容会自动跳过。",14,Ui.TEXT2,false);guide.setLineSpacing(pad(10),1);guide.setPadding(0,pad(14),0,0);form.addView(guide);

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


        /* ---- 阿里云盘 ---- */
        form=newPage();
        form.addView(section("阿里云盘"));
        LinearLayout accountCard=new LinearLayout(act);accountCard.setGravity(Gravity.CENTER_VERTICAL);accountCard.setPadding(pad(20),pad(20),pad(20),pad(20));accountCard.setBackground(Ui.round(Ui.PANEL,act,16));
        aliAvatar=new ImageView(act);aliAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);aliAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);aliAvatar.setBackground(Ui.round(Ui.CARD,act,36));aliAvatar.setClipToOutline(true);accountCard.addView(aliAvatar,new LinearLayout.LayoutParams(pad(64),pad(64)));
        LinearLayout accountCardText=new LinearLayout(act);accountCardText.setOrientation(1);accountCardText.setPadding(pad(20),0,0,0);accountCard.addView(accountCardText,new LinearLayout.LayoutParams(0,-2,1));
        aliAccountName=Ui.text(act,"",22,Ui.TEXT,true);aliAccountName.setGravity(Gravity.LEFT);aliAccountName.setTextDirection(View.TEXT_DIRECTION_LTR);aliAccountName.setSingleLine(true);aliAccountName.setEllipsize(android.text.TextUtils.TruncateAt.END);accountCardText.addView(aliAccountName);accountCardText.addView(Ui.text(act,"连接私人曲库，导入后即可在电视播放",13,Ui.TEXT2,false));
        form.addView(accountCard,new LinearLayout.LayoutParams(-1,pad(112)));
        aliStatus = Ui.text(act, aliStatusText(), 12,
                Config.aliReady() ? Ui.BRAND : Ui.TEXT3, false);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.bottomMargin = pad(8);
        form.addView(aliStatus, alp);

        LinearLayout qrRow = new LinearLayout(act);
        qrRow.setOrientation(LinearLayout.HORIZONTAL);
        form.addView(qrRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        qrRow.addView(action("扫码登录阿里云盘", true, new View.OnClickListener() {
            @Override public void onClick(View v) { startQr(); }
        }));
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(pad(10), 1);
        qrRow.addView(new View(act), gap);
        qrRow.addView(action("验证当前登录",false,v->verifyAli()));
        qrRow.addView(action("退出登录", false, new View.OnClickListener() {
            @Override public void onClick(View v) { logoutAli(); }
        }));

        for(int i=0;i<qrRow.getChildCount();i++){View child=qrRow.getChildAt(i);if(child instanceof TextView){LinearLayout.LayoutParams buttonLp=new LinearLayout.LayoutParams(0,pad(52),1);buttonLp.setMargins(0,pad(12),pad(8),pad(8));child.setLayoutParams(buttonLp);}}
        refreshAliAccount();
        qrBox = new LinearLayout(act);
        qrBox.setOrientation(LinearLayout.VERTICAL);
        qrBox.setGravity(Gravity.CENTER_HORIZONTAL);
        qrBox.setVisibility(View.GONE);
        qrBox.setPadding(0, pad(14), 0, pad(6));
        form.addView(qrBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        qrImage = new ImageView(act);
        int qs = Ui.dp(act, 160);
        qrBox.addView(qrImage, new LinearLayout.LayoutParams(qs, qs));
        qrHint = Ui.text(act, "", 12, Ui.TEXT2, false);
        qrHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams qhp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qhp.topMargin = pad(8);
        qrBox.addView(qrHint, qhp);

        LinearLayout advancedRow=new LinearLayout(act);
        form.addView(advancedRow);
        LinearLayout manual = new LinearLayout(act);
        manual.setOrientation(LinearLayout.VERTICAL);manual.setVisibility(View.GONE);
        advancedRow.addView(action("高级：开放平台凭据",false,v->{android.transition.TransitionManager.beginDelayedTransition(scroller,new android.transition.AutoTransition().setDuration(Ui.motionEnabled(act)?200:0));manual.setVisibility(manual.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);}));
        form.addView(manual, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView manualTip = Ui.text(act,
                "或使用开放平台凭据（client_id + refresh_token）：", 11, Ui.TEXT3, false);
        LinearLayout.LayoutParams mtp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mtp.topMargin = pad(16);
        manual.addView(manualTip, mtp);
        aliId = field(manual, "client_id", Config.aliClientId, false, false);
        aliSecret = field(manual, "client_secret（可空）", Config.aliSecret, true, false);
        aliRefresh = field(manual, "refresh_token", Config.aliManualRefresh, true, false);
        manual.addView(action("保存并测试云盘凭据", false, new View.OnClickListener() {
            @Override public void onClick(View v) { testAliOpen(); }
        }));

        String[] sections={"音乐目录","群晖 NAS","阿里云盘"};
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
    public void back(){if(detail)showHub();else close();}
    private void hubDescription(int index){String[] descriptions={"添加音乐或听书目录，管理自己的音乐资料库","连接群晖 NAS，读取服务器中的音乐目录","扫码登录阿里云盘，或配置开放平台凭据"};hubCaption.setText(descriptions[index]);}
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
    private void refreshAliAccount(){
        if(aliAccountName==null)return;
        String nickname=Config.aliDriveName;
        if(nickname.isEmpty()||nickname.indexOf('�')>=0)nickname="阿里云盘用户";
        aliAccountName.setText(Config.aliReady()?nickname:"连接阿里云盘");
        if(Config.aliReady()&&!profileRequested){profileRequested=true;io.execute(()->{try{Aliyun.verifyLogin();ui.post(()->{if(!closed){aliStatus.setText(aliStatusText());refreshAliAccount();}});}catch(Exception ignored){}});}
        String url=Config.aliReady()?Config.aliAvatar:"";
        if(url.equals(shownAvatar))return;shownAvatar=url;aliAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        if(url.isEmpty())return;
        io.execute(()->{try{byte[] bytes=Http.bytes(url,new java.util.HashMap<>());Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length);ui.post(()->{if(!closed&&url.equals(shownAvatar))aliAvatar.setImageBitmap(bitmap);});}catch(Exception ignored){}});
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

    private String aliStatusText() {
        if (Config.aliReady()) {
            String who = Config.aliDriveName == null || Config.aliDriveName.length() == 0 || Config.aliDriveName.indexOf('�')>=0
                    ? "已登录" : Config.aliDriveName;
            return "当前状态：" + who + " · " + Config.aliModeText();
        }
        return "当前状态：未登录";
    }

    /* -------------------------------- 行为 -------------------------------- */

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
        Config.aliClientId = aliId.getText().toString().trim();
        Config.aliSecret = aliSecret.getText().toString().trim();
        Config.aliManualRefresh=aliRefresh.getText().toString().trim();
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

    private void testAliOpen() {
        collect();
        status.setText("正在连接阿里云盘…");
        io.execute(new Runnable() {
            @Override public void run() {
                String msg;
                try {
                    Config.invalidateAuth();Config.aliMode = Config.MODE_OPEN;Config.aliRefresh=Config.aliManualRefresh;
                    Config.aliAccess = "";
                    Config.aliDriveId = "";
                    Aliyun.refresh();
                    String drive = Aliyun.driveId();
                    Config.save(act);
                    List<Entry> root = Aliyun.list("root");
                    msg = "云盘连接成功：drive " + (drive.length() > 8 ? drive.substring(0, 8) : drive)
                            + "… 根目录 " + root.size() + " 项";
                } catch (Exception e) {
                    msg = "云盘连接失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage());
                }
                final String m = msg;
                ui.post(new Runnable() {
                    @Override public void run() {
                        status.setText(m);
                        aliStatus.setText(aliStatusText());refreshAliAccount();
                    }
                });
            }
        });
    }

    private void logoutAli() {
        Config.invalidateAuth();
        Config.aliMode = Config.MODE_NONE;
        Config.aliAccess = "";
        Config.aliRefresh = "";
        Config.aliDriveId = "";
        Config.aliDriveName = "";Config.aliAvatar="";Config.aliUserId="";Config.aliDeviceId="";Config.aliPrivateKey="";qrGeneration++;
        aliRefresh.setText("");
        Config.save(act);
        aliStatus.setText(aliStatusText());refreshAliAccount();
        status.setText("已退出阿里云盘登录");
        qrBox.setVisibility(View.GONE);
        polling = false;
    }

    /* ------------------------------- 扫码登录 ------------------------------- */

    private void startQr() {
        collect();final int ticket=++qrGeneration;polling=true;qrSession=null;
        qrBox.setVisibility(View.VISIBLE);Ui.reveal(qrBox);qrHint.setText("正在申请二维码…");qrImage.setImageDrawable(null);status.setText("");
        io.execute(()->{
            try {
                final AliyunLogin.QrSession session=AliyunLogin.start();final Bitmap image=qrBitmap(session.codeContent);
                ui.post(()->{if(closed||ticket!=qrGeneration)return;qrSession=session;qrImage.setImageBitmap(image);qrHint.setText("用阿里云盘 App 扫码并在手机确认，二维码约 2 分钟有效");pollLoop(ticket,session);});
            }catch(Exception ex){ui.post(()->{if(closed||ticket!=qrGeneration)return;polling=false;qrHint.setText("二维码生成失败："+friendly(ex));});}
        });
    }
    private String friendly(Exception ex){
        if(ex instanceof org.json.JSONException)return "服务响应无法解析，请稍后重试";
        if(ex instanceof java.io.IOException)return "网络连接失败，请检查网络并重试";
        String message=ex.getMessage();return message==null?"服务暂不可用，请重试":message;
    }
    private void verifyAli() {
        status.setText("正在验证当前登录…");
        io.execute(()->{String message;try{Aliyun.verifyLogin();Config.save(act);message="登录有效，可以添加云盘音乐目录";}catch(Exception ex){message="验证失败："+friendly(ex);}final String text=message;ui.post(()->{if(closed)return;status.setText(text);aliStatus.setText(aliStatusText());refreshAliAccount();});});
    }
    private void pollLoop(final int ticket,final AliyunLogin.QrSession session) {
        if(closed||!polling||ticket!=qrGeneration)return;
        if(System.currentTimeMillis()-session.at>130000){polling=false;qrHint.setText("二维码已过期，请重新生成");return;}
        io.execute(()->{
            String message;boolean done=false,ok=false;
            try {
                AliyunLogin.PollResult result=AliyunLogin.poll(session);message=result.message;done=result.terminal;
                if(result.loggedIn) {
                    if(closed||ticket!=qrGeneration)return;
                    ui.post(()->{if(!closed&&ticket==qrGeneration)qrHint.setText("手机已确认，正在验证云盘连接…");});
                    AliyunLogin.accept(result);Config.save(act);
                    try{Aliyun.verifyLogin();Config.save(act);ok=true;message="阿里云盘登录成功，可添加音乐目录";}
                    catch(Exception ex){message="手机已确认，凭据已保存；云盘初始化失败："+friendly(ex)+"。可点「验证当前登录」重试";}
                }
            }catch(Exception ex){message="扫码状态查询失败："+friendly(ex);}
            final String text=message;final boolean success=ok,terminal=done;
            ui.post(()->{
                if(closed||ticket!=qrGeneration)return;qrHint.setText(text);
                if(terminal){polling=false;status.setText(text);aliStatus.setText(aliStatusText());refreshAliAccount();if(success)qrBox.setVisibility(View.GONE);}
                else ui.postDelayed(()->pollLoop(ticket,session),2200);
            });
        });
    }

    /** 用内置编码器画二维码位图 */
    private Bitmap qrBitmap(String content) throws Exception {
        boolean[][] m = QrCode.encode(content);
        int quiet = 3;
        int scale = Math.max(3, 400 / (m.length + quiet * 2));
        int dim = (m.length + quiet * 2) * scale;
        Bitmap bmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.WHITE);
        for (int y = 0; y < m.length; y++) {
            for (int x = 0; x < m.length; x++) {
                if (!m[y][x]) continue;
                int px = (x + quiet) * scale;
                int py = (y + quiet) * scale;
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        bmp.setPixel(px + dx, py + dy, Color.BLACK);
                    }
                }
            }
        }
        return bmp;
    }

    /* --------------------------------- 关闭 --------------------------------- */

    public void close() {
        if (closed) return;
        closed = true;
        polling = false;qrGeneration++;ui.removeCallbacksAndMessages(null);
        collect();
        try { io.shutdownNow(); } catch (Throwable ignored) { }
        final View closing=root;root=null;
        Runnable finish=()->{if(closing!=null&&closing.getParent()instanceof ViewGroup)((ViewGroup)closing.getParent()).removeView(closing);if(onClose!=null)onClose.run();};
        if(closing!=null&&Ui.motionEnabled(act))closing.animate().alpha(0).setDuration(160).withEndAction(finish).start();else finish.run();
    }
}
