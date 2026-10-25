#!/usr/bin/env python3
"""模块过渡必须有实际中间渲染帧；不是仅检查源码或动画配置。"""
from pathlib import Path
import re,time
script=Path(__file__).with_name('tv_smoke.py').resolve()
exec(script.read_text().split("adb('shell','am','force-stop'")[0],globals())
def motion(label,desc=False):
    n=find(desc=label) if desc else find(label)
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    result=adb('shell','am','instrument','-w','-e','x',str((x1+x2)//2),'-e','y',str((y1+y2)//2),'com.yunying.music.test/.MotionProbe')
    assert 'INSTRUMENTATION_CODE: 0' in result,result
    print(label,result.split('INSTRUMENTATION_RESULT: motion=')[1].splitlines()[0],flush=True)
original=adb('shell','settings','get','global','animator_duration_scale').strip()
try:
    adb('shell','settings','put','global','animator_duration_scale','1')
    adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5)
    motion('歌手 (23)');tap('单曲 (',prefix=True)
    adb('shell','input','keyevent','82');motion('我的');motion('搜索',True)
    tap('Y');tap('Q');time.sleep(.4);motion('“YQ”的全部结果…');back();back();back()
    focus('应用服务设置');motion('应用服务设置');back();focus('其他设置');motion('其他设置');motion('群晖 NAS');tap('关闭');back()
    adb('shell','input','keyevent','82');tap('正在播放');time.sleep(.6);motion('播放队列 · 23',True);back();motion('音效 · 原声',True);back()
finally:
    if original=='null':adb('shell','settings','delete','global','animator_duration_scale')
    else:adb('shell','settings','put','global','animator_duration_scale',original)
