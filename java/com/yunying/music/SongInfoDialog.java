package com.yunying.music;

import android.app.*;
import android.view.*;
import android.widget.*;

/** 更正显示及匹配信息，不修改远端文件名或路径。 */
final class SongInfoDialog {
    static void show(Activity activity,Entry entry,Runnable saved){
        Dialog dialog=new Dialog(activity);dialog.requestWindowFeature(1);
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(1);panel.setPadding(Ui.dp(activity,28),Ui.dp(activity,24),Ui.dp(activity,28),Ui.dp(activity,24));Ui.dialogGlass(activity,panel,16);
        panel.addView(Ui.text(activity,"修改歌曲信息",24,Ui.TEXT,true));
        EditText title=field(activity,panel,"歌曲名",MusicLibrary.songTitle(activity,entry));
        String singer=MusicLibrary.artist(activity,entry);EditText artist=field(activity,panel,"歌手名称","未知歌手".equals(singer)?"":singer);
        panel.addView(Ui.text(activity,"保存后使用更正的信息重新匹配歌词与封面",13,Ui.TEXT2,false));
        TextView error=Ui.text(activity,"",13,Ui.TEXT2,false);panel.addView(error);
        LinearLayout actions=new LinearLayout(activity);actions.setGravity(Gravity.CENTER_VERTICAL);actions.setClipChildren(false);actions.setClipToPadding(false);actions.setPadding(Ui.dp(activity,8),Ui.dp(activity,8),Ui.dp(activity,8),Ui.dp(activity,8));LinearLayout.LayoutParams actionsLp=new LinearLayout.LayoutParams(-1,-2);actionsLp.topMargin=Ui.dp(activity,16);panel.addView(actions,actionsLp);
        TextView cancel=Ui.action(activity,"取消",18,v->dialog.dismiss());
        TextView save=Ui.action(activity,"保存并重新匹配",18,v->{
            String name=title.getText().toString().trim();if(name.isEmpty()){error.setText("请输入歌曲名");title.requestFocus();return;}
            MusicLibrary.correctMetadata(activity,entry,name,artist.getText().toString().trim());dialog.dismiss();saved.run();
        });
        LinearLayout.LayoutParams cancelLp=new LinearLayout.LayoutParams(0,Ui.dp(activity,52),1);cancelLp.rightMargin=Ui.dp(activity,16);actions.addView(cancel,cancelLp);actions.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(activity,52),1));
        View[] fields={title,artist,save};for(int i=0;i<2;i++){final int at=i;fields[i].setOnKeyListener((v,key,event)->{if(key!=KeyEvent.KEYCODE_DPAD_DOWN&&key!=KeyEvent.KEYCODE_DPAD_UP)return false;if(event.getAction()==KeyEvent.ACTION_DOWN)fields[Math.max(0,Math.min(2,at+(key==KeyEvent.KEYCODE_DPAD_DOWN?1:-1)))].requestFocus();return true;});}
        dialog.setContentView(panel);dialog.show();if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(Ui.dp(activity,580),-2);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);}
        Ui.installFocusMotion(panel);title.requestFocus();
    }
    private static EditText field(Activity activity,LinearLayout panel,String label,String value){
        TextView caption=Ui.text(activity,label,15,Ui.TEXT2,false);caption.setPadding(0,Ui.dp(activity,16),0,Ui.dp(activity,8));panel.addView(caption);
        EditText field=new EditText(activity);field.setSingleLine(true);field.setContentDescription(label);field.setText(value);field.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,Ui.dp(activity,18));field.setPadding(Ui.dp(activity,16),0,Ui.dp(activity,16),0);field.setBackground(Ui.cardFocusable(activity,10));field.setTextColor(Ui.textStates(Ui.TEXT_ON_LIGHT,Ui.TEXT));panel.addView(field,new LinearLayout.LayoutParams(-1,Ui.dp(activity,52)));return field;
    }
}
