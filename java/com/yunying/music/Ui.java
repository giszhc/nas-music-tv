package com.yunying.music;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.TextView;

/** 视觉规范：按 1920×1080 电视参考图缩放的深灰界面与白色焦点态。 */
public class Ui {

    public static int BG_TOP   = 0xFF141C29;
    public static int BG_BOT   = 0xFF141C29;
    public static int PANEL    = 0xE6283344;
    public static int CARD     = 0xDC354153;
    public static int CARD_HI  = 0xFFF6F6F6;   // 焦点卡片：接近白
    public static int LINE     = 0x607E94AF;
    public static int TEXT     = 0xFFF4F4F4;
    public static int TEXT2    = 0xFFD4DEEC;
    public static int TEXT3    = 0xFFB7C5D8;
    public static int TEXT_ON_LIGHT = 0xFF101112;
    public static int TEXT2_ON_LIGHT = 0xFF56575A;
    public static int BRAND    = 0xFF82BBFF;
    public static int BRAND_D  = 0xFF2F66C8;
    public static int BRAND_LT = 0xFF41434A;
    public static int ORANGE   = 0xFFFF9F45;
    public static int ON_BRAND = 0xFFFFFFFF;
    public static int SQ       = 0xFFE6CF92;
    public static int SQ_BG    = 0xFF6C624A;

    public static boolean light;
    public static int themeVersion;
    public static void applyTheme(boolean value){
        light=value;themeVersion++;
        BG_TOP=value?0xFFE8EEF5:0xFF141C29;BG_BOT=BG_TOP;
        PANEL=value?0xE6F5F8FC:0xE6283344;CARD=value?0xDCF8FBFF:0xDC354153;
        LINE=value?0x608093AC:0x607E94AF;
        TEXT=value?0xFF152338:0xFFF4F4F4;TEXT2=value?0xFF3D5068:0xFFD4DEEC;TEXT3=value?0xFF52647A:0xFFB7C5D8;
        BRAND=value?0xFF235EAD:0xFF82BBFF;SQ=value?0xFF715217:0xFFE6CF92;
        SQ_BG=value?0xFFE9DFC6:0xFF6C624A;
    }

    public static boolean isTv(Context c) {
        try {
            android.app.UiModeManager m = (android.app.UiModeManager)
                    c.getSystemService(Context.UI_MODE_SERVICE);
            return m != null && m.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
        } catch (Throwable t) {
            return false;
        }
    }


    public static void immersive(android.app.Activity activity) {
        android.view.Window window=activity.getWindow();
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        if(android.os.Build.VERSION.SDK_INT>=28){
            android.view.WindowManager.LayoutParams attributes=window.getAttributes();
            attributes.layoutInDisplayCutoutMode=android.os.Build.VERSION.SDK_INT>=30
                ? android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                : android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attributes);
        }
        if(android.os.Build.VERSION.SDK_INT>=29){window.setStatusBarContrastEnforced(false);window.setNavigationBarContrastEnforced(false);}

        activity.getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().widthPixels / 960f);
    }

    public static float ts(Context c) {
        return 1f;
    }

    public static float height(Context context){return context.getResources().getDisplayMetrics().heightPixels*960f/context.getResources().getDisplayMetrics().widthPixels;}
    public static void place(android.widget.FrameLayout parent,View child,float x,float y,float width,float height){
        android.widget.FrameLayout.LayoutParams lp=new android.widget.FrameLayout.LayoutParams(width<0?(int)width:dp(parent.getContext(),width),height<0?(int)height:dp(parent.getContext(),height));
        lp.leftMargin=dp(parent.getContext(),x);lp.topMargin=dp(parent.getContext(),y);parent.addView(child,lp);
    }
    public static TextView action(Context context,String label,float size,View.OnClickListener click){
        TextView view=text(context,label,size,TEXT,false);view.setGravity(android.view.Gravity.CENTER);view.setFocusable(true);view.setClickable(true);
        view.setTextColor(textStates(TEXT_ON_LIGHT,TEXT));view.setBackground(cardFocusable(context,10));view.setOnClickListener(click);return view;
    }

    public static GradientDrawable round(int color, Context c, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        if(color==PANEL||color==CARD||color==BG_TOP){d.setOrientation(GradientDrawable.Orientation.TL_BR);d.setColors(new int[]{color, light?0xDDE5EDF8:(color==BG_TOP?0xF21A2332:0xED202C3E)});d.setStroke(Math.max(1,dp(c,.5f)),0x427E94AF);}else d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundStroke(int color, int stroke, Context c, float radiusDp) {
        GradientDrawable d = round(color, c, radiusDp);
        d.setStroke(Math.max(1, dp(c, 1f)), stroke);
        return d;
    }

    public static GradientDrawable gradient(int from, int to) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{from, to});
    }

    public static StateListDrawable states(Drawable normal, Drawable focused, Drawable pressed) {
        StateListDrawable s = new StateListDrawable();
        if (pressed != null) s.addState(new int[]{android.R.attr.state_pressed}, pressed);
        if (focused != null) s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{}, normal);
        return s;
    }

    /** 深色卡片：聚焦时变成近白色卡（对齐参考图的选中态） */
    public static Drawable cardFocusable(Context c, float r) {
        return new GlassDrawable(c,r,true);
    }

    public static android.graphics.drawable.Drawable tabBackground(final Context context) {
        return new android.graphics.drawable.Drawable() {
            final android.graphics.Paint paint = new android.graphics.Paint(3);
            final Drawable focusSurface=focusRing(context,22);
            final Runnable frame=()->invalidateSelf();
            protected boolean onStateChange(int[] state) { invalidateSelf(); return true; }
            public boolean isStateful() { return true; }
            public void draw(android.graphics.Canvas canvas) {
                boolean focus=false, selected=false;
                for(int state:getState()) {if(state==android.R.attr.state_focused)focus=true;if(state==android.R.attr.state_selected)selected=true;}
                android.graphics.Rect b=getBounds();
                if(focus) {focusSurface.setBounds(b);focusSurface.draw(canvas);if(motionEnabled(context))scheduleSelf(frame,android.os.SystemClock.uptimeMillis()+16);}
                else {unscheduleSelf(frame);}
                if(!focus && selected) {paint.setColor(TEXT);canvas.drawRect(b.left+dp(context,12),b.bottom-dp(context,2),b.right-dp(context,12),b.bottom,paint);}
            }
            public void setAlpha(int alpha) {paint.setAlpha(alpha);}
            public void setColorFilter(android.graphics.ColorFilter filter) {paint.setColorFilter(filter);}
            public int getOpacity() {return android.graphics.PixelFormat.TRANSLUCENT;}
        };
    }

    public static Drawable focusRing(Context c, float radius) { return new GlassDrawable(c,radius,true).focused(); }
    public static Drawable glass(Context c,float radius) {return new GlassDrawable(c,radius,false);}
    public static Drawable outline(Context c,float radius) {return new GlassDrawable(c,radius,false);}

    public static ColorStateList textStates(int focused, int normal) {
        return new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}},
                new int[]{focused, normal});
    }

    public static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,dp(c,sp)*c.getResources().getConfiguration().fontScale);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static void setText(TextView view, CharSequence value) {
        if (!view.getText().toString().contentEquals(value)) view.setText(value);
    }

    public static int alpha(int color, float a) {
        return Color.argb((int) (255 * a), Color.red(color), Color.green(color), Color.blue(color));
    }

    /** 「SQ / FLAC / MP3」小角标 */
    public static TextView badge(Context c, String label) {
        TextView t = text(c, label, 9, SQ, true);
        t.setGravity(android.view.Gravity.CENTER);
        t.setPadding(dp(c, 5), dp(c, 1), dp(c, 5), dp(c, 1));
        t.setBackground(round(SQ_BG, c, 3));
        return t;
    }

    public static android.graphics.Bitmap blur(android.graphics.Bitmap src) {
        try {
            int width=80,height=80;
            android.graphics.Bitmap small=android.graphics.Bitmap.createScaledBitmap(src,width,height,true);
            int[] pixels=new int[width*height],temp=new int[pixels.length],out=new int[pixels.length];small.getPixels(pixels,0,width,0,0,width,height);
            for(int pass=0;pass<3;pass++) {
                for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                    int r=0,g=0,b=0,n=0;for(int k=-7;k<=7;k++){int color=pixels[y*width+Math.max(0,Math.min(width-1,x+k))];r+=(color>>16)&255;g+=(color>>8)&255;b+=color&255;n++;}temp[y*width+x]=0xFF000000|(r/n<<16)|(g/n<<8)|b/n;
                }
                for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
                    int r=0,g=0,b=0,n=0;for(int k=-7;k<=7;k++){int color=temp[Math.max(0,Math.min(height-1,y+k))*width+x];r+=(color>>16)&255;g+=(color>>8)&255;b+=color&255;n++;}out[y*width+x]=0xFF000000|(r/n<<16)|(g/n<<8)|b/n;
                }
                int[] swap=pixels;pixels=out;out=swap;
            }
            return android.graphics.Bitmap.createBitmap(pixels,width,height,android.graphics.Bitmap.Config.ARGB_8888);
        } catch(Throwable ignored) {return null;}
    }

    private static final java.util.concurrent.ExecutorService BLUR_WORKER=java.util.concurrent.Executors.newFixedThreadPool(2);
    public static void frostedBackdrop(View source,android.widget.FrameLayout host,int cropWidth,float opacity) {
        if(source.getWidth()==0||source.getHeight()==0)return;
        final android.graphics.Bitmap snapshot=android.graphics.Bitmap.createBitmap(Math.max(1,cropWidth/6),Math.max(1,source.getHeight()/6),android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(snapshot);canvas.scale(1f/6,1f/6);source.draw(canvas);
        final android.widget.ImageView frost=new android.widget.ImageView(source.getContext());frost.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);frost.setAlpha(0f);host.addView(frost,new android.widget.FrameLayout.LayoutParams(-1,-1));
        BLUR_WORKER.execute(()->{final android.graphics.Bitmap blurred=blur(snapshot);snapshot.recycle();frost.post(()->{if(host.getParent()==null)return;frost.setImageBitmap(blurred);if(motionEnabled(source.getContext()))frost.animate().alpha(opacity).setDuration(180).setInterpolator(EASE).start();else frost.setAlpha(opacity);});});
    }
    /** 捕获宿主页面的对应区域，在后台模糊，叠加可读的玻璃表面。 */
    public static void dialogGlass(android.app.Activity activity,View panel,float radius){
        panel.setBackground(new GlassDrawable(activity,radius,false).panelSurface());
        panel.addOnLayoutChangeListener(new View.OnLayoutChangeListener(){
            public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){
                if(v.getWidth()==0||v.getHeight()==0)return;v.removeOnLayoutChangeListener(this);
                View source=activity.getWindow().getDecorView();
                int[] origin=new int[2],location=new int[2];source.getLocationOnScreen(origin);v.getLocationOnScreen(location);
                final android.graphics.Bitmap capture=android.graphics.Bitmap.createBitmap(Math.max(1,v.getWidth()/8),Math.max(1,v.getHeight()/8),android.graphics.Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas=new android.graphics.Canvas(capture);canvas.scale(1f/8,1f/8);canvas.translate(origin[0]-location[0],origin[1]-location[1]);float originalAlpha=v.getAlpha();v.setAlpha(0);try{if(!(activity instanceof PlayerActivity)||!((PlayerActivity)activity).drawGlassBackdrop(canvas))source.draw(canvas);}finally{v.setAlpha(originalAlpha);}
                BLUR_WORKER.execute(()->{android.graphics.Bitmap blurred=blur(capture);capture.recycle();v.post(()->{
                    if(!v.isAttachedToWindow()||blurred==null)return;
                    android.graphics.drawable.BitmapDrawable backdrop=new android.graphics.drawable.BitmapDrawable(activity.getResources(),blurred);backdrop.setGravity(android.view.Gravity.FILL);backdrop.setAlpha(255);
                    GlassDrawable surface=new GlassDrawable(activity,radius,false).panelSurface();surface.setAlpha(235);
                    v.setBackground(new android.graphics.drawable.LayerDrawable(new Drawable[]{backdrop,surface}));
                    v.setClipToOutline(true);v.setOutlineProvider(new android.view.ViewOutlineProvider(){public void getOutline(View view,android.graphics.Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(activity,radius));}});
                });});
            }
        });
    }

    public static boolean focusText(View root,String label){
        if(root instanceof TextView&&label.contentEquals(((TextView)root).getText())){View candidate=root;while(!candidate.isFocusable()&&candidate.getParent()instanceof View)candidate=(View)candidate.getParent();return candidate.requestFocus();}
        if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++)if(focusText(group.getChildAt(i),label))return true;}return false;
    }

    public static final android.view.animation.Interpolator EASE = new android.view.animation.PathInterpolator(.16f,1f,.3f,1f);
    private static final java.util.WeakHashMap<View,Boolean> motionViews = new java.util.WeakHashMap<>();
    public static boolean motionEnabled(Context c) {
        return android.provider.Settings.Global.getFloat(c.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1)>0;
    }
    public static void open(android.app.Activity activity,android.content.Intent intent) {
        activity.startActivity(intent);
        if(motionEnabled(activity))activity.overridePendingTransition(R.anim.page_enter,R.anim.page_exit);
        else activity.overridePendingTransition(0,0);
    }
    public static void exit(android.app.Activity activity) {
        if(motionEnabled(activity))activity.overridePendingTransition(R.anim.page_return,R.anim.page_leave);
        else activity.overridePendingTransition(0,0);
    }
    public static void reveal(View view) {
        view.animate().cancel();
        if(!motionEnabled(view.getContext())){view.setAlpha(1);view.setTranslationY(0);return;}
        view.setAlpha(.65f);view.setTranslationY(dp(view.getContext(),12));
        view.animate().alpha(1).translationY(0).setDuration(300).setInterpolator(EASE).start();
    }
    public static void installFocusMotion(View root) {
        if(root instanceof TvVirtualList)return;
        if(android.os.Build.VERSION.SDK_INT>=26&&root.isFocusable())root.setDefaultFocusHighlightEnabled(false);
        if(root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)root;
            // 滚动边界由各容器自己管理，焦点动画不得改变祖先裁剪规则。
            for(int i=0;i<group.getChildCount();i++)installFocusMotion(group.getChildAt(i));
        }
        if(root instanceof android.widget.ScrollView||root instanceof android.widget.HorizontalScrollView||root instanceof android.widget.AbsListView){root.setScaleX(1);root.setScaleY(1);return;}
        if(!root.isFocusable()||root instanceof android.widget.EditText||root instanceof android.widget.SeekBar||motionViews.containsKey(root))return;
        motionViews.put(root,true);
        if(root.getBackground()instanceof GlassDrawable)root.setElevation(dp(root.getContext(),1));
        final View.OnFocusChangeListener original=root.getOnFocusChangeListener();
        root.setOnFocusChangeListener((view,focus)->{
            if(original!=null)original.onFocusChange(view,focus);
            view.animate().cancel();float scale=focus?1.05f:1f;
            if(motionEnabled(view.getContext()))view.animate().scaleX(scale).scaleY(scale).translationZ(focus?dp(view.getContext(),4):0).setDuration(180).setInterpolator(EASE).start();
            else {view.setScaleX(1);view.setScaleY(1);view.setTranslationZ(0);}
        });
        if(root.hasFocus())root.getOnFocusChangeListener().onFocusChange(root,true);
    }

    public static void fadeIn(View v, long delay) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setStartDelay(delay).setDuration(260).start();
    }
}
