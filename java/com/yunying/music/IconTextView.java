package com.yunying.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.TextView;

/** 固定笔画的原生矢量控件；语义标签在状态变化时更新，避免重复无障碍事件。 */
public final class IconTextView extends TextView {
    private final Paint paint = new Paint(3);
    private final String icon;
    private float iconFraction=.48f;private String trackKey;
    public void bindTrack(Entry entry){trackKey=MusicLibrary.key(entry);updateTrack();}
    private void updateTrack(){if(trackKey==null)return;Entry current=PlayerService.instance==null?null:PlayerService.instance.current();int visible=current!=null&&trackKey.equals(MusicLibrary.key(current))?VISIBLE:INVISIBLE;if(getVisibility()!=visible)setVisibility(visible);}
    public void setIconFraction(float fraction){iconFraction=fraction;invalidate();}
    public IconTextView(Context c, String icon) { super(c); this.icon = icon; }
    private final Runnable pulse=new Runnable(){public void run(){updateTrack();invalidate();postDelayed(this,getVisibility()==VISIBLE&&PlayerService.instance!=null&&PlayerService.instance.isPlaying()?70:400);}};
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if("nowPlaying".equals(icon))post(pulse);}
    @Override protected void onDetachedFromWindow(){removeCallbacks(pulse);super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas canvas) {
        String label = getText().toString();
        boolean transport = "transport".equals(icon);
        String kind = transport ? (label.contains("❚") ? "pause" : label.contains("⏮") ? "previous" : label.contains("⏭") ? "next" : "play") : icon;
        String description = "pause".equals(kind) ? "暂停" : "previous".equals(kind) ? "上一首" : "next".equals(kind) ? "下一首" : "folder".equals(kind) ? "文件夹" : "music".equals(kind) ? "音乐" : "play".equals(kind) ? "播放" : label;
        if (!description.contentEquals(getContentDescription() == null ? "" : getContentDescription())) setContentDescription(description);
        float size = Math.min(getWidth(), getHeight()) * iconFraction;
        canvas.save(); canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2); canvas.scale(size / 24, size / 24);
        paint.setColor(getCurrentTextColor()); paint.setStyle(Paint.Style.FILL); paint.setStrokeWidth(1.6f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        if ("pause".equals(kind)) { canvas.drawRect(5, 3, 9, 21, paint); canvas.drawRect(15, 3, 19, 21, paint); }
        else if ("folder".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);
            Path p = new Path(); p.moveTo(2, 6); p.lineTo(10, 6); p.lineTo(12, 9); p.lineTo(22, 9); p.lineTo(22, 20); p.lineTo(2, 20); p.close(); canvas.drawPath(p, paint);
        } else if ("music".equals(kind) || "queue".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);canvas.drawLine(8,17,8,4,paint);canvas.drawLine(8,4,15,3,paint);
            canvas.drawOval(2,15,8,21,paint);
            if("music".equals(kind)){canvas.drawLine(19,3,19,15,paint);canvas.drawLine(8,5,19,3,paint);canvas.drawOval(13,13,19,19,paint);}
            else {canvas.drawLine(13,8,23,8,paint);canvas.drawLine(13,12,23,12,paint);canvas.drawLine(13,16,20,16,paint);}
        } else if("search".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.4f);canvas.drawCircle(10,10,7,paint);canvas.drawLine(15.5f,15.5f,22,22,paint);
        } else if("clock".equals(kind)||"update".equals(kind)||"info".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(12,12,10,paint);
            if("clock".equals(kind)){canvas.drawLine(12,5,12,12,paint);canvas.drawLine(12,12,17,16,paint);}
            else if("update".equals(kind)){canvas.drawLine(12,19,12,5,paint);canvas.drawLine(6,11,12,5,paint);canvas.drawLine(12,5,18,11,paint);}
            else{canvas.drawLine(12,10,12,18,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(12,6,1.3f,paint);}
        } else if("library".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);for(int y=0;y<2;y++)for(int x=0;x<2;x++)canvas.drawRoundRect(2+x*12,2+y*12,10+x*12,10+y*12,2,2,paint);
        } else if("nowPlaying".equals(kind)) {
            boolean playing=PlayerService.instance!=null&&PlayerService.instance.isPlaying();double time=android.os.SystemClock.uptimeMillis()/210.0;
            for(int i=0;i<4;i++){float h=playing&&Ui.motionEnabled(getContext())?(float)(5+14*(.5+.5*Math.sin(time+i*1.7))):new int[]{9,17,12,6}[i];canvas.drawRoundRect(3+i*5,21-h,5+i*5,21,1,1,paint);}
        } else if("soundSliders".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);
            float[] positions={8,16,10};
            for(int i=0;i<3;i++) {
                float x=5+i*7, y=positions[i];
                canvas.drawLine(x,3,x,y-2.5f,paint);
                canvas.drawLine(x,y+2.5f,x,21,paint);
                canvas.drawCircle(x,y,2.5f,paint);
            }
        } else if("equalizer".equals(kind)) {
            int[] heights={9,17,12,6};for(int i=0;i<4;i++)canvas.drawRoundRect(3+i*5,21-heights[i],5+i*5,21,1,1,paint);
        } else if("settings".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(12,12,6,paint);canvas.drawCircle(12,12,2,paint);for(int i=0;i<8;i++){canvas.save();canvas.rotate(i*45,12,12);canvas.drawLine(12,2,12,5,paint);canvas.restore();}
        } else if("document".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(4,2,20,22,2,2,paint);canvas.drawLine(8,8,16,8,paint);canvas.drawLine(8,12,16,12,paint);canvas.drawLine(8,16,13,16,paint);
        } else if("trash".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(6,6,18,22,1,1,paint);canvas.drawLine(4,5,20,5,paint);canvas.drawLine(9,2,15,2,paint);canvas.drawLine(10,9,10,18,paint);canvas.drawLine(14,9,14,18,paint);
        } else if("backspace".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);Path p=new Path();p.moveTo(2,12);p.lineTo(8,5);p.lineTo(22,5);p.lineTo(22,19);p.lineTo(8,19);p.close();canvas.drawPath(p,paint);canvas.drawLine(12,9,18,15,paint);canvas.drawLine(18,9,12,15,paint);
        } else if("space".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);canvas.drawLine(5,13,5,17,paint);canvas.drawLine(5,17,19,17,paint);canvas.drawLine(19,17,19,13,paint);
        } else if("quality".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(1,5,23,19,2,2,paint);paint.setStyle(Paint.Style.FILL);paint.setTextSize(10);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText(label,12,16,paint);
        } else if("bookmark".equals(kind)||"broadcast".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);
            if("bookmark".equals(kind)){Path p=new Path();p.moveTo(5,2);p.lineTo(19,2);p.lineTo(19,22);p.lineTo(12,17);p.lineTo(5,22);p.close();canvas.drawPath(p,paint);}
            else {canvas.drawCircle(12,10,8,paint);canvas.drawCircle(12,10,4,paint);canvas.drawLine(12,11,8,23,paint);canvas.drawLine(12,11,16,23,paint);canvas.drawLine(8,23,16,23,paint);}
        } else if("heart".equals(kind)) {
            if(!isSelected())paint.setStyle(Paint.Style.STROKE);else paint.setColor(0xFFFF0056);
            Path p=new Path();p.moveTo(12,21);p.cubicTo(9,18,1,13,2,7);p.cubicTo(3,1,9,1,12,6);p.cubicTo(15,1,21,1,22,7);p.cubicTo(23,13,15,19,12,21);canvas.drawPath(p,paint);
        } else if("mode".equals(kind)) {
            paint.setStyle(Paint.Style.STROKE);
            if(label.contains("随机")) {
                Path p=new Path();p.moveTo(2,5);p.lineTo(6,5);p.cubicTo(12,5,12,19,18,19);p.lineTo(22,19);p.moveTo(2,19);p.lineTo(6,19);p.cubicTo(12,19,12,5,18,5);p.lineTo(22,5);p.moveTo(19,2);p.lineTo(22,5);p.lineTo(19,8);p.moveTo(19,16);p.lineTo(22,19);p.lineTo(19,22);canvas.drawPath(p,paint);
            } else {
                Path p=new Path();p.moveTo(3,8);p.lineTo(3,5);p.lineTo(21,5);p.lineTo(18,2);p.moveTo(21,5);p.lineTo(18,8);p.moveTo(21,16);p.lineTo(21,19);p.lineTo(3,19);p.lineTo(6,22);p.moveTo(3,19);p.lineTo(6,16);canvas.drawPath(p,paint);
                if(label.contains("单曲")){paint.setStyle(Paint.Style.FILL);paint.setTextSize(10);paint.setTextAlign(Paint.Align.CENTER);canvas.drawText("1",12,15,paint);}
            }
        } else if("more".equals(kind)) {canvas.drawCircle(12,4,1.5f,paint);canvas.drawCircle(12,12,1.5f,paint);canvas.drawCircle(12,20,1.5f,paint);}
        else {
            boolean prev = "previous".equals(kind), next = "next".equals(kind);
            if (prev) { canvas.translate(24, 0); canvas.scale(-1, 1); }
            Path p = new Path(); p.moveTo(6, 3); p.lineTo(20, 12); p.lineTo(6, 21); p.close(); canvas.drawPath(p, paint);
            if (prev || next) canvas.drawRect(21, 3, 23, 21, paint);
        }
        canvas.restore();
    }
}
