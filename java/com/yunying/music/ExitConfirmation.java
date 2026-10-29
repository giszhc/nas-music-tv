package com.yunying.music;

import android.app.Activity;
import android.content.Intent;

/** 退出属于显式操作，返回默认停留在应用。 */
final class ExitConfirmation {
    static void show(Activity activity) {
        new TvChoiceDialog(activity,"退出聆境音乐？",new String[]{"继续使用","退出并停止播放"},-1,index->{
            if(index==1){activity.stopService(new Intent(activity,PlayerService.class));activity.finishAffinity();}
        }).show();
    }
}
