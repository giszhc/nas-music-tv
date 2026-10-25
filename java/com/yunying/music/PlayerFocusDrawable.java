package com.yunying.music;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

/** 播放工具栏专用：白色圆形底与旋转的半透明外圈。 */
final class PlayerFocusDrawable extends Drawable {
    private final Context context;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable frame = () -> invalidateSelf();
    private boolean active;
    private int alpha = 255;
    PlayerFocusDrawable(Context context) { this.context = context; }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] states) {
        boolean next = false;
        for (int state : states) if (state == android.R.attr.state_focused || state == android.R.attr.state_pressed) next = true;
        if (next == active) return false;
        active = next;
        if (!active) unscheduleSelf(frame);
        invalidateSelf();
        return true;
    }
    @Override public void draw(Canvas canvas) {
        if (!active) return;
        Rect b = getBounds();
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float width = Ui.dp(context, 2), radius = Math.min(b.width(), b.height()) / 2f - width / 2f;
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF6F6F6);
        paint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, Math.max(0, radius - Ui.dp(context, 4)), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setShader(new SweepGradient(cx, cy,
                new int[]{0x10FFFFFF, 0x22FFFFFF, 0x99FFFFFF, 0x10FFFFFF},
                new float[]{0, .45f, .82f, 1}));
        canvas.save();
        boolean motion = Ui.motionEnabled(context);
        canvas.rotate(motion ? (SystemClock.uptimeMillis() % 2400) * 360f / 2400 : 0, cx, cy);
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.restore();
        paint.setShader(null);
        if (motion && isVisible()) scheduleSelf(frame, SystemClock.uptimeMillis() + 16);
    }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
