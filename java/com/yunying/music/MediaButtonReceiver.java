package com.yunying.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

/** 转发耳机/遥控器媒体键到播放服务。 */
public class MediaButtonReceiver extends BroadcastReceiver {

    @Override public void onReceive(Context c, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) return;
        KeyEvent e = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (e == null || e.getAction() != KeyEvent.ACTION_DOWN) return;
        String action = PlayerService.ACTION_TOGGLE;
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                action = PlayerService.ACTION_NEXT;
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                action = PlayerService.ACTION_PREV;
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                action = PlayerService.ACTION_TOGGLE;
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                action = PlayerService.ACTION_TOGGLE;
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                action = PlayerService.ACTION_TOGGLE;
                break;
            default:
                return;
        }
        PlayerService.command(c, action, -1);
    }
}
