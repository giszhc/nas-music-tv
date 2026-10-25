#!/usr/bin/env python3
"""缩短模拟器视口制造真实滚动，逐像素确认标题区不被滚动内容覆盖。"""
from pathlib import Path
import time,re
from PIL import Image,ImageChops
script=Path(__file__).with_name('tv_smoke.py').resolve()
exec(script.read_text().split("adb('shell','am','force-stop'")[0],globals())
original=adb('shell','wm','size')
def rectangle(node):return list(map(int,re.findall(r'\d+',node.get('bounds'))))
def viewport(right=False):
    choices=[rectangle(n) for n in nodes() if n.get('class','')=='android.widget.ScrollView']
    if right:choices=[b for b in choices if b[0]>400]
    return max(choices,key=lambda b:(b[2]-b[0])*(b[3]-b[1]))
def capture(name):
    snap(name);return Image.open(OUT/name).convert('RGB')
def check_scroll(label,right=False):
    bounds=viewport(right);x1,y1,x2,y2=bounds;adb('shell','input','tap','1900','180');time.sleep(.5)
    before=capture('scroll-'+label+'-before.png')
    adb('shell','input','swipe',str((x1+x2)//2),str(y2-80),str((x1+x2)//2),str(y1+60),'450');time.sleep(.8)
    after=capture('scroll-'+label+'-after.png')
    if label=='queue' and ImageChops.difference(before.crop(tuple(bounds)),after.crop(tuple(bounds))).getbbox() is None:
        focus('月光');time.sleep(.7);after=capture('scroll-'+label+'-after.png')
    header=(0,0,before.width,max(1,y1-4))
    assert ImageChops.difference(before.crop(header),after.crop(header)).getbbox() is None,label+' header changed while scrolling'
    assert ImageChops.difference(before.crop(tuple(bounds)),after.crop(tuple(bounds))).getbbox() is not None,label+' did not actually scroll'
    print('PASS:',label,'viewport scrolled; title pixels unchanged')
try:
    adb('shell','wm','size','1920x1080');adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.6)
    find('单曲 (23)');check_scroll('playlist-fullhd');focus('滚动测试20');snap('reference-long-list.png')
    tap('歌手 (',prefix=True);check_scroll('artists-fullhd');tap('单曲 (',prefix=True)
    adb('shell','input','keyevent','84');time.sleep(.4);adb('shell','input','text','GUNDONG');time.sleep(.6);tap('“GUNDONG”的全部结果…');check_scroll('search-fullhd');snap('reference-long-search.png');back();back();back()
    adb('shell','input','keyevent','82');tap('正在播放');time.sleep(1);tap('播放队列',prefix=True);check_scroll('queue-fullhd');snap('reference-long-queue.png');back();back()
    adb('shell','wm','size','1920x720');adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.7)
    find('播放列表');check_scroll('playlist')
    adb('shell','input','keyevent','82');tap('设置');tap('群晖 NAS');check_scroll('settings-nas',True);find('保存并测试 NAS')
    focus('阿里云盘');tap('阿里云盘');focus('高级：开放平台凭据');tap('高级：开放平台凭据');check_scroll('settings-alipan',True)
    tap('关闭');adb('shell','input','keyevent','82');tap('正在播放');time.sleep(.6);tap('播放队列',prefix=True)
    # 队列在更短窗口中也必须有独立滚动边界。
    adb('shell','wm','size','1920x600');back();back();adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);adb('shell','input','keyevent','82');tap('正在播放');tap('播放队列',prefix=True);check_scroll('queue')
finally:
    match=re.search(r'Override size: (\d+x\d+)',original)
    if match:adb('shell','wm','size',match.group(1))
    else:adb('shell','wm','size','reset')
    adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity')
