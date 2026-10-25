package com.yunying.music;

import android.app.Instrumentation;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import java.io.File;
public final class SoundChecks {
    private static final class VolumePlayer extends MediaPlayer {
        float left = 1f, right = 1f;
        @Override public void setVolume(float left, float right) {
            this.left = left; this.right = right;
            super.setVolume(left, right);
        }
    }
    public static String run(Instrumentation test)throws Exception {
        File folder=new File(test.getTargetContext().getCacheDir(),"formats");folder.mkdirs();
        String[] formats={"wav","mp3","flac","m4a","aac","ogg","opus"};int applied=0;
        for(String format:formats) {
            VolumePlayer player=new VolumePlayer();SoundEffects effects=new SoundEffects();
            try {
                player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                File file=new File(folder,"tone."+format);
                try(java.io.InputStream input=test.getTargetContext().getAssets().open("formats/tone."+format);java.io.FileOutputStream output=new java.io.FileOutputStream(file)){byte[] buffer=new byte[16384];int read;while((read=input.read(buffer))!=-1)output.write(buffer,0,read);}
                try(java.io.FileInputStream input=new java.io.FileInputStream(file)){player.setDataSource(input.getFD());player.prepare();}player.start();
                for(String mode:new String[]{"off","clear","bass","live","surround","bass","clear","off"}) {
                    String[] state={null};test.runOnMainSync(()->state[0]=effects.apply(player,mode));
                    java.lang.reflect.Field loudnessField=SoundEffects.class.getDeclaredField("loudness");loudnessField.setAccessible(true);
                    android.media.audiofx.LoudnessEnhancer compensation=(android.media.audiofx.LoudnessEnhancer)loudnessField.get(effects);
                    if("off".equals(mode)){
                        for(String field:new String[]{"equalizer","bass","virtualizer","reverb","loudness"}){
                            java.lang.reflect.Field effect=SoundEffects.class.getDeclaredField(field);effect.setAccessible(true);
                            if(effect.get(effects)!=null)throw new AssertionError("Original mode retained "+field);
                        }
                    }else if(!state[0].contains("响度补偿")&&!state[0].startsWith("设备未提供")){
                        if(compensation==null||!compensation.getEnabled()||compensation.getTargetGain()!=400)throw new AssertionError("Loudness compensation inactive: "+format+"/"+mode);
                    }
                    if(state[0]==null)throw new AssertionError("No effect state for "+format);
                    if(state[0].startsWith("音效已生效")||state[0].startsWith("已生效"))applied++;
                    android.os.SystemClock.sleep(60);
                    if(player.left!=1f||player.right!=1f)throw new AssertionError("Volume attenuated: "+format+"/"+mode+"="+player.left+","+player.right);
                    if(!player.isPlaying())throw new AssertionError("Playback stopped: "+format+"/"+mode);
                }
                if(player.getCurrentPosition()<=0)throw new AssertionError("Playback clock not advancing: "+format);
            } finally {test.runOnMainSync(()->effects.release());player.release();}
        }
        return "PASS: 7 decoded formats x 8 effect transitions, unity playback volume; "+applied+" non-bypass effect applications active";
    }
}
