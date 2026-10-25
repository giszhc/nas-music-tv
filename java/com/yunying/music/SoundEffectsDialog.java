package com.yunying.music;

import android.app.Activity;
import android.widget.Toast;

/** 播放工具栏的音效单选弹层，切换立即应用并保存。 */
final class SoundEffectsDialog {
    static void show(Activity activity,Runnable changed){
        new TvChoiceDialog(activity,"选择音效",SoundEffects.NAMES,SoundEffects.index(Config.audioEffect),index->{
            Config.audioEffect=SoundEffects.IDS[index];Config.save(activity);
            String state=PlayerService.instance==null?"将在下一次播放时应用":PlayerService.instance.applyAudioEffects();
            if(changed!=null)changed.run();
            Toast.makeText(activity,SoundEffects.NAMES[index]+"："+state,Toast.LENGTH_SHORT).show();
        }).show();
    }
}
