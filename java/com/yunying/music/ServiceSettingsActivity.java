package com.yunying.music;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** 参考图中左侧两项单选、右侧图标说明的设置页。 */
public final class ServiceSettingsActivity extends Activity {
    private boolean recommendation;private FrameLayout root;private TextView description;
    @Override public void onCreate(Bundle state){super.onCreate(state);Ui.immersive(this);recommendation=getIntent().getBooleanExtra("recommendation",false);build();}
    private void build(){
        root=new FrameLayout(this);root.setBackground(new ImmersiveBackground());setContentView(root);TextView title=Ui.text(this,recommendation?"内容推荐服务":"应用服务设置",29,Ui.TEXT,false);Ui.place(root,title,48,36,600,48);
        Ui.place(root,new ServiceMark(this),634,180,160,160);description=Ui.text(this,"",14,Ui.TEXT2,false);description.setGravity(Gravity.CENTER);Ui.place(root,description,560,380,340,48);
        String[] labels=recommendation?new String[]{"开启","关闭"}:new String[]{"全量服务","基本服务"};int selected=recommendation?(Config.recommendations?0:1):(Config.basicService?1:0);
        for(int i=0;i<2;i++){final int index=i;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(this,12),0,Ui.dp(this,12),0);row.setFocusable(true);row.setClickable(true);row.setBackground(Ui.states(Ui.round(Ui.PANEL,this,12),Ui.focusRing(this,12),null));TextView text=Ui.text(this,labels[i],20,Ui.TEXT,false);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));row.addView(new TvChoiceDialog.RadioMark(this,i==selected),new LinearLayout.LayoutParams(Ui.dp(this,24),Ui.dp(this,24)));row.setOnFocusChangeListener((v,focus)->{text.setTextColor(focus?Ui.TEXT_ON_LIGHT:Ui.TEXT);if(focus)caption(index);});row.setOnClickListener(v->{if(recommendation)Config.recommendations=index==0;else Config.basicService=index==1;Config.save(this);build();});Ui.place(root,row,48,100+i*56,444,48);if(i==selected)row.post(()->row.requestFocus());}
        caption(selected);Ui.installFocusMotion(root);Ui.reveal(root);
    }
    private void caption(int index){description.setText(recommendation?(index==0?"展示本机曲库中的推荐内容":"关闭曲库推荐，保留搜索和播放"):(index==0?"可使用已添加的曲库、在线封面与歌词":"仅使用已添加的音乐目录播放"));}
    @Override public void finish(){super.finish();Ui.exit(this);}
    static final class ServiceMark extends View{
        private final Paint paint=new Paint(3);ServiceMark(Activity c){super(c);}
        @Override protected void onDraw(Canvas canvas){paint.setColor(0xFF4D4E50);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),12));for(int y=0;y<2;y++)for(int x=0;x<2;x++){float left=Ui.dp(getContext(),8+x*84),top=Ui.dp(getContext(),8+y*84);canvas.drawRoundRect(left,top,left+Ui.dp(getContext(),60),top+Ui.dp(getContext(),60),Ui.dp(getContext(),17),Ui.dp(getContext(),17),paint);}}
    }
}
