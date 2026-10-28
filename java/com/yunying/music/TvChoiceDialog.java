package com.yunying.music;

import android.app.*;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** 1920×1080 参考图中的居中品质/单选弹层。 */
final class TvChoiceDialog {
    interface Choice{void selected(int index);}
    private final Activity activity;private final String title;private final String[] choices;private final int selected;private final Choice callback;private boolean[] available;
    TvChoiceDialog(Activity activity,String title,String[] choices,int selected,Choice callback){this.activity=activity;this.title=title;this.choices=choices;this.selected=selected;this.callback=callback;}
    TvChoiceDialog availability(boolean[] values){available=values;return this;}
    void show(){
        Dialog dialog=new Dialog(activity);dialog.requestWindowFeature(1);
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(1);panel.setPadding(Ui.dp(activity,20),Ui.dp(activity,20),Ui.dp(activity,20),Ui.dp(activity,20));Ui.dialogGlass(activity,panel,20);
        TextView heading=Ui.text(activity,title,22,Ui.TEXT,false);heading.setGravity(Gravity.CENTER);heading.setMaxLines(2);heading.setEllipsize(android.text.TextUtils.TruncateAt.END);panel.addView(heading,new LinearLayout.LayoutParams(-1,Ui.dp(activity,54)));
        TvVirtualList list=new TvVirtualList(activity,choices.length,1,48,12,(i,recycled)->{
            LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Ui.dp(activity,12),0,Ui.dp(activity,14),0);
            TextView label=Ui.text(activity,choices[i],18,Ui.TEXT,false);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            if(selected>=0){RadioMark radio=new RadioMark(activity,i==selected);row.addView(radio,new LinearLayout.LayoutParams(Ui.dp(activity,24),Ui.dp(activity,24)));}
            if(available!=null&&!available[i])row.setAlpha(.5f);return row;
        });
        list.clicked(i->{if(available!=null&&!available[i])android.widget.Toast.makeText(activity,"曲库中没有该品质版本，请先添加对应文件",0).show();else dismiss(dialog,panel,()->callback.selected(i));});
        panel.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        dialog.setContentView(panel);dialog.setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)dismiss(dialog,panel,()->{});return true;}return false;});dialog.show();
        int height=Ui.dp(activity,Math.min(Ui.height(activity)*.86f,54+40+52+choices.length*60));
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setDimAmount(.56f);dialog.getWindow().setLayout(Ui.dp(activity,420),height);}
        list.focusPosition(selected<0?0:selected);Ui.reveal(panel);
    }
    private boolean closing;
    private void dismiss(Dialog dialog,View panel,Runnable next){if(closing)return;closing=true;Runnable done=()->{dialog.dismiss();next.run();};if(Ui.motionEnabled(activity))panel.animate().alpha(0).translationY(Ui.dp(activity,6)).setDuration(160).setInterpolator(Ui.EASE).withEndAction(done).start();else done.run();}
    static final class RadioMark extends View {
        private final Paint paint=new Paint(3);private final boolean chosen;
        RadioMark(Context c,boolean chosen){super(c);this.chosen=chosen;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){float x=getWidth()/2f,y=getHeight()/2f,r=getWidth()*.37f;paint.setColor(chosen?Ui.BRAND:0xFFBEBFC0);paint.setStyle(chosen?Paint.Style.FILL:Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),1));canvas.drawCircle(x,y,r,paint);if(chosen){paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(x,y,r*.45f,paint);}}
    }
}
