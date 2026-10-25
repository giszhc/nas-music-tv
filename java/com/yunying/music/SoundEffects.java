package com.yunying.music;

import android.media.MediaPlayer;
import android.media.audiofx.*;
import java.util.ArrayList;
import java.util.List;

/** 原生会话音效，作用于解码后的音频，格式切换时重新绑定，原声完全旁路。 */
final class SoundEffects {
    static final String[] IDS={"off","clear","bass","live","surround"};
    static final String[] NAMES={"原声","3D丽音","超重低音","HiFi现场","3D环绕"};
    static final String[] DESCRIPTIONS={"保留原始声音，不启用处理","提升高频清晰度，加入轻微空间感","增强低频力度，保留播放音量","均衡提升与音乐厅混响","立体声空间扩展与轻度混响"};
    private Equalizer equalizer;
    private BassBoost bass;
    private Virtualizer virtualizer;
    private PresetReverb reverb;
    private LoudnessEnhancer loudness;
    private String status="原声直通";
    private int boundSession=-1;
    static int index(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return i;return 0;}
    static String name(String id){return NAMES[index(id)];}
    String status(){return status;}
    String apply(MediaPlayer player,String id) {
        int preset=index(id);
        if(player==null){status="将在下一次播放时应用";return status;}
        try{player.attachAuxEffect(0);player.setAuxEffectSendLevel(0);}catch(RuntimeException ignored){}
        int session=player.getAudioSessionId();
        // 每次切换重建处理链，原声模式彻底释放处理器，避免设备保留旧预设增益。
        release();boundSession=session;
        // 音效仅调整音色，不按预设衰减整首音频的播放音量。
        player.setVolume(1f,1f);
        if(preset==0){disable(equalizer);disable(bass);disable(virtualizer);disable(reverb);status="原声直通";return status;}
        if(preset!=2)disable(bass);
        if(preset==2){disable(virtualizer);disable(reverb);}
        List<String> missing=new ArrayList<>();
        float[][] gains={{0,0,0},{2,1.5f,4},{9,-1,1},{2,2,2},{2,0,2}};
        try {
            if(equalizer==null)equalizer=new Equalizer(0,session);short[] range=equalizer.getBandLevelRange();
            for(short band=0;band<equalizer.getNumberOfBands();band++){
                int hz=equalizer.getCenterFreq(band)/1000;float gain=gains[preset][hz<=250?0:hz<4000?1:2];
                equalizer.setBandLevel(band,(short)Math.max(range[0],Math.min(range[1],Math.round(gain*100))));
            }
            if(equalizer.setEnabled(true)!=AudioEffect.SUCCESS)throw new IllegalStateException();
        }catch(RuntimeException ex){if(equalizer!=null)equalizer.release();equalizer=null;missing.add("均衡器");}
        if(preset==2 && equalizer==null)try{if(bass==null)bass=new BassBoost(0,session);bass.setStrength((short)700);if(bass.setEnabled(true)!=AudioEffect.SUCCESS)throw new IllegalStateException();}
        catch(RuntimeException ex){if(bass!=null)bass.release();bass=null;missing.add("低音增强");}
        if(preset==1||preset==3||preset==4)try{
            if(virtualizer==null)virtualizer=new Virtualizer(0,session);virtualizer.setStrength((short)(preset==4?1000:preset==3?650:400));
            if(virtualizer.setEnabled(true)!=AudioEffect.SUCCESS)throw new IllegalStateException();
        }catch(RuntimeException ex){if(virtualizer!=null)virtualizer.release();virtualizer=null;missing.add("空间增强");}
        if(preset==1||preset==3||preset==4)try{
            if(reverb==null)reverb=new PresetReverb(0,0);reverb.setPreset(preset==3?PresetReverb.PRESET_MEDIUMHALL:PresetReverb.PRESET_SMALLROOM);
            if(reverb.setEnabled(true)!=AudioEffect.SUCCESS)throw new IllegalStateException();player.attachAuxEffect(reverb.getId());player.setAuxEffectSendLevel(preset==3?.24f:preset==4?.3f:.13f);
        }catch(RuntimeException ex){if(reverb!=null)reverb.release();reverb=null;missing.add("混响");}
        boolean any=equalizer!=null||bass!=null||virtualizer!=null||reverb!=null;
        if(any)try {
            loudness=new LoudnessEnhancer(session);
            // 会话内补偿处理链的响度损失，超出输出范围由系统压缩器处理。
            loudness.setTargetGain(400);
            if(loudness.setEnabled(true)!=AudioEffect.SUCCESS)throw new IllegalStateException();
        }catch(RuntimeException ex){if(loudness!=null)loudness.release();loudness=null;missing.add("响度补偿");}
        status=!any?"设备未提供可用音效，已保留原声":missing.isEmpty()?"音效已生效":"已生效；设备不支持："+android.text.TextUtils.join("、",missing);
        return status;
    }
    private void disable(AudioEffect effect){if(effect!=null)try{effect.setEnabled(false);}catch(RuntimeException ignored){}}
    void release(){
        boundSession=-1;
        AudioEffect[] effects={loudness,equalizer,bass,virtualizer,reverb};
        for(AudioEffect effect:effects)if(effect!=null)try{effect.release();}catch(RuntimeException ignored){}
        equalizer=null;bass=null;virtualizer=null;reverb=null;loudness=null;
    }
}
