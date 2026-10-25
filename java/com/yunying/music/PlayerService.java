package com.yunying.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 后台播放服务：MediaPlayer + 前台通知 + MediaSession（TV 遥控器媒体键可用）。
 */
public class PlayerService extends Service {

    public static final String ACTION_PLAY_PENDING = "com.yunying.music.PLAY";
    public static final String ACTION_TOGGLE = "com.yunying.music.TOGGLE";
    public static final String ACTION_NEXT = "com.yunying.music.NEXT";
    public static final String ACTION_PREV = "com.yunying.music.PREV";
    public static final String ACTION_SEEK = "com.yunying.music.SEEK";
    public static final String ACTION_STOP = "com.yunying.music.STOP";

    private static final String CHANNEL = "yy_play";
    private static final int NOTIF_ID = 1201;

    public static PlayerService instance;

    public interface Listener { void onPlayerState(); }

    /** 待播放队列（同进程传递，避免 Intent 序列化上限） */
    private static List<Entry> pendingQueue;
    private static int pendingIndex;
    private static int pendingPosition;
    private static boolean pendingPaused;

    public static boolean hasPending() { return pendingQueue != null && !pendingQueue.isEmpty(); }

    public static void playList(Context c, List<Entry> queue, int index) {
        pendingPosition = 0; pendingPaused = false;
        pendingQueue = new ArrayList<>(queue);
        pendingIndex = index;
        Intent i = new Intent(c, PlayerService.class).setAction(ACTION_PLAY_PENDING);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void command(Context c, String action, int arg) {
        Intent i = new Intent(c, PlayerService.class).setAction(action);
        if (arg >= 0) i.putExtra("arg", arg);
        try {
            c.startService(i);
        } catch (Throwable ignored) {
            // 后台限制下可能不允许启动；忽略即可，避免崩溃
        }
    }

    private final List<Entry> queue = new ArrayList<>();
    private int index = -1;
    private MediaPlayer mp;
    private final SoundEffects effects=new SoundEffects();
    public String applyAudioEffects(){return effects.apply(preparing?null:mp,Config.audioEffect);}
    public String audioEffectStatus(){return effects.status();}
    private boolean preparing, playing;
    private String error = "";
    private int durationMs, positionMs;
    private Listener listener;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private MediaSession session;
    private AudioManager am;
    private AudioManager.OnAudioFocusChangeListener focusListener;
    private long lastNotify = 0L;
    private volatile int generation;
    private int mode;
    private int restorePosition;
    private boolean restorePaused;
    private final java.util.Random random = new java.util.Random();
    public String modeLabel() { return new String[]{"列表循环", "单曲循环", "随机播放"}[mode]; }
    public void cycleMode() { mode = (mode + 1) % 3; saveQueue(); notifyState(); }
    private void saveQueue() {
        MusicLibrary.saveQueue(this, queue, index, mode, getPosition());
    }
    private final Runnable checkpoint = new Runnable() {
        public void run() { if (!queue.isEmpty()) saveQueue(); ui.postDelayed(this, 5000); }
    };

    /* ------------------------------ 生命周期 ------------------------------ */

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        mode = getSharedPreferences("music_library", 0).getInt("mode", 0);
        ui.postDelayed(checkpoint, 5000);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createChannel();
        initSession();
        try {
            startForeground(NOTIF_ID, buildNotification("云影音乐", "准备就绪", false));
        } catch (Throwable ignored) { }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String a = intent == null ? null : intent.getAction();
        if (ACTION_PLAY_PENDING.equals(a) && pendingQueue != null) {
            queue.clear();
            queue.addAll(pendingQueue);
            pendingQueue = null;
            restorePosition = pendingPosition; restorePaused = pendingPaused;
            pendingPosition = 0; pendingPaused = false;
            playAt(pendingIndex);
        } else if (ACTION_TOGGLE.equals(a)) {
            toggle();
        } else if (ACTION_NEXT.equals(a)) {
            next();
        } else if (ACTION_PREV.equals(a)) {
            prev();
        } else if (ACTION_SEEK.equals(a)) {
            seekTo(intent.getIntExtra("arg", 0));
        } else if (ACTION_STOP.equals(a)) {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        saveQueue();
        generation++;
        ui.removeCallbacksAndMessages(null);
        instance = null;
        releasePlayer();
        if (session != null) {
            session.setActive(false);
            session.release();
            session = null;
        }
        abandonFocus();
        io.shutdownNow();
        try { stopForeground(true); } catch (Throwable ignored) { }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    /* ------------------------------- 播放控制 ------------------------------- */

    public void playAt(int i) {
        if (queue.isEmpty()) return;
        index = ((i % queue.size()) + queue.size()) % queue.size();
        final Entry e = queue.get(index);
        final int request = ++generation;
        MusicLibrary.remember(this, e);
        releasePlayer();
        preparing = true;
        playing = false;
        error = "";
        durationMs = 0;
        positionMs = 0;
        saveQueue();
        notifyState();
        updateNotification();
        io.execute(new Runnable() {
            @Override public void run() {
                try {
                    if (request != generation) return;
                    final String url;
                    final Map<String, String> headers;
                    if (Entry.SRC_ALI.equals(e.source)) {
                        url = Aliyun.downloadUrl(e.id);
                        headers = Aliyun.playHeaders();
                    } else {
                        url = Synology.downloadUrl(e.path);
                        headers = Synology.playHeaders();
                    }
                    ui.post(new Runnable() {
                        @Override public void run() { if (request == generation) startPlayer(url, headers); }
                    });
                } catch (final Exception ex) {
                    ui.post(new Runnable() {
                        @Override public void run() {
                            if (request != generation) return;
                            preparing = false;
                            playing = false;
                            error = ex.getMessage() == null ? "打开失败" : ex.getMessage();
                            notifyState();
                            updateNotification();
                        }
                    });
                }
            }
        });
    }

    private void startPlayer(String url, Map<String, String> headers) {
        if (!requestFocus()) {
            preparing = false; error = "未获得音频焦点，请重试"; notifyState(); return;
        }
        releasePlayer();
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer p) {
                    preparing = false;
                    durationMs = Math.max(0, p.getDuration());
                    effects.apply(p,Config.audioEffect);
                    if (restorePosition > 0) { p.seekTo(restorePosition); restorePosition = 0; }
                    if (!restorePaused) p.start();
                    playing = !restorePaused;
                    saveQueue();
                    restorePaused = false;
                    notifyState();
                    updateNotification();
                }
            });
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer p) { if (mode == 1) playAt(index); else next(); }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer p, int what, int extra) {
                    preparing = false;
                    playing = false;
                    error = "播放失败（" + what + "/" + extra + "），格式可能不受支持";
                    notifyState();
                    updateNotification();
                    return true;
                }
            });
            mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override public void onBufferingUpdate(MediaPlayer p, int percent) { }
            });
            mp.setDataSource(this, android.net.Uri.parse(url), headers);
            mp.prepareAsync();
        } catch (Throwable t) {
            preparing = false;
            playing = false;
            error = "无法播放：" + t.getMessage();
            notifyState();
        }
    }

    public void toggle() {
        if (preparing) return;
        if (mp == null) {
            if (!queue.isEmpty()) playAt(index < 0 ? 0 : index);
            return;
        }
        try {
            if (mp.isPlaying()) {
                mp.pause();
                playing = false;
            } else {
                mp.start();
                playing = true;
            }
            saveQueue();
            notifyState();
            updateNotification();
        } catch (Throwable ignored) { }
    }

    public void switchVersion(Entry version){if(index<0||index>=queue.size())return;restorePosition=getPosition();restorePaused=!playing;queue.set(index,version);playAt(index);}

    public void next() {
        if (queue.isEmpty()) return;
        int next = index + 1;
        if (mode == 2 && queue.size() > 1) next = (index + 1 + random.nextInt(queue.size() - 1)) % queue.size();
        playAt(next);
    }
    public static void restore(Context c) {
        List<Entry> saved = MusicLibrary.queue(c);
        if (saved.isEmpty()) return;
        int position = c.getSharedPreferences("music_library", 0).getInt("position", 0);
        playList(c, saved, Math.max(0, Math.min(saved.size() - 1, c.getSharedPreferences("music_library", 0).getInt("index", 0))));
        pendingPosition = position; pendingPaused = true;
    }

    public void prev() {
        if (getPosition() > 4000 && mp != null) {
            seekTo(0);
            return;
        }
        if (!queue.isEmpty()) playAt(index - 1);
    }

    public void seekTo(int ms) {
        if (mp != null && durationMs > 0) {
            int p = Math.max(0, Math.min(durationMs, ms));
            try { mp.seekTo(p); positionMs = p; saveQueue(); notifyState(); } catch (Throwable ignored) { }
        }
    }

    private void releasePlayer() {
        effects.release();
        if (mp != null) {
            try { mp.reset(); } catch (Throwable ignored) { }
            try { mp.release(); } catch (Throwable ignored) { }
            mp = null;
        }
    }

    /* ------------------------------- 状态读取 ------------------------------- */

    public boolean isPlaying() { return playing; }
    public boolean isPreparing() { return preparing; }
    public String getError() { return error; }
    public int getDuration() { return durationMs; }
    public List<Entry> getQueue() { return new ArrayList<>(queue); }
    public int getIndex() { return index; }

    public int getPosition() {
        if (mp != null && durationMs > 0) {
            try { positionMs = mp.getCurrentPosition(); } catch (Throwable ignored) { }
        }
        return positionMs;
    }

    public Entry current() {
        if (index < 0 || index >= queue.size()) return null;
        return queue.get(index);
    }

    public String currentTitle() {
        Entry e = current();
        return e == null ? "未在播放" : e.title();
    }

    public void setListener(Listener l) { listener = l; }

    private void notifyState() {
        if (listener != null) {
            ui.post(new Runnable() {
                @Override public void run() {
                    if (listener != null) listener.onPlayerState();
                }
            });
        }
        updateSessionState();
    }

    /* ------------------------------ 通知 / 会话 ------------------------------ */

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = new NotificationChannel(CHANNEL, "音乐播放",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
    }

    private void initSession() {
        try {
            session = new MediaSession(this, "YunYingMusic");
            session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                    | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            session.setCallback(new MediaSession.Callback() {
                @Override public void onPlay() { if (!playing) toggle(); }
                @Override public void onPause() { if (playing) toggle(); }
                @Override public void onSkipToNext() { next(); }
                @Override public void onSkipToPrevious() { prev(); }
                @Override public void onSeekTo(long pos) { seekTo((int) pos); }
                @Override public void onStop() { stopSelf(); }
            });
            session.setActive(true);
            updateSessionState();
        } catch (Throwable ignored) { }
    }

    private void updateSessionState() {
        if (session == null) return;
        try {
            int state = playing ? PlaybackState.STATE_PLAYING
                    : (preparing ? PlaybackState.STATE_BUFFERING : PlaybackState.STATE_PAUSED);
            session.setPlaybackState(new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                            | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SEEK_TO
                            | PlaybackState.ACTION_SKIP_TO_NEXT
                            | PlaybackState.ACTION_SKIP_TO_PREVIOUS)
                    .setState(state, getPosition(), playing ? 1f : 0f)
                    .build());
            Entry e = current();
            if (e != null) {
                MediaMetadata.Builder mb = new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, e.title())
                        .putString(MediaMetadata.METADATA_KEY_ARTIST,
                                Entry.SRC_ALI.equals(e.source) ? "阿里云盘" : "NAS 音乐")
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs);
                session.setMetadata(mb.build());
            }
        } catch (Throwable ignored) { }
    }

    private PendingIntent pi(String action) {
        Intent i = new Intent(this, PlayerService.class).setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getService(this, action.hashCode(), i, flags);
    }

    private Notification buildNotification(String title, String sub, boolean isPlaying) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        int icon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        Intent open = new Intent(this, PlayerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int pf = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) pf |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent content = PendingIntent.getActivity(this, 9, open, pf);

        b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(sub)
                .setContentIntent(content)
                .setOngoing(isPlaying)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(android.R.drawable.ic_media_previous, "上一首", pi(ACTION_PREV))
                .addAction(icon, isPlaying ? "暂停" : "播放", pi(ACTION_TOGGLE))
                .addAction(android.R.drawable.ic_media_next, "下一首", pi(ACTION_NEXT));
        if (Build.VERSION.SDK_INT >= 21 && session != null) {
            Notification.MediaStyle st = new Notification.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(session.getSessionToken());
            b.setStyle(st);
        }
        return b.build();
    }

    private void updateNotification() {
        long now = System.currentTimeMillis();
        if (now - lastNotify < 400 && !preparing) return;
        lastNotify = now;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            String sub = error.length() > 0 ? error
                    : (preparing ? "正在缓冲…"
                    : (Entry.SRC_ALI.equals(current() == null ? "" : current().source)
                        ? "阿里云盘" : "NAS 音乐"));
            nm.notify(NOTIF_ID, buildNotification(currentTitle(), sub, playing));
        } catch (Throwable ignored) { }
    }

    /* -------------------------------- 音频焦点 -------------------------------- */

    @SuppressWarnings("deprecation")
    private boolean requestFocus() {
        if (am == null) return true;
        if (focusListener == null) {
            focusListener = new AudioManager.OnAudioFocusChangeListener() {
                @Override public void onAudioFocusChange(int change) {
                    if (change == AudioManager.AUDIOFOCUS_LOSS) {
                        if (playing) toggle();
                    } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        if (playing) toggle();
                    }
                }
            };
        }
        int r;
        if (Build.VERSION.SDK_INT >= 26) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            android.media.AudioFocusRequest req = new android.media.AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(focusListener).build();
            r = am.requestAudioFocus(req);
        } else {
            r = am.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    @SuppressWarnings("deprecation")
    private void abandonFocus() {
        if (am == null || focusListener == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                am.abandonAudioFocusRequest(new android.media.AudioFocusRequest.Builder(
                        AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(focusListener).build());
            } else {
                am.abandonAudioFocus(focusListener);
            }
        } catch (Throwable ignored) { }
    }
}
