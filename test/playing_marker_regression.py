#!/usr/bin/env python3
"""当前音频在列表尾部显示标记，播放/暂停/切歌时同步，不随遥控选择移动。"""
from pathlib import Path
from PIL import Image,ImageChops
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
def box(n):return tuple(map(int,re.findall(r'\d+',n.get('bounds'))))
def image_pair(prefix):
 snap(prefix+'-a.png');time.sleep(.2);snap(prefix+'-b.png');return Image.open(OUT/(prefix+'-a.png')).convert('RGB'),Image.open(OUT/(prefix+'-b.png')).convert('RGB')
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.6);menu('播放列表');time.sleep(.4);tap('夜曲');time.sleep(1);find(desc='暂停');back();time.sleep(.5)
mark=find('当前播放');original=box(mark);assert original[0]>700,original
# 选择相邻项，当前播放标记仍留在原曲目行。
adb('shell','input','keyevent','20');time.sleep(.3);assert abs(box(find('当前播放'))[1]-original[1])<=5;assert len([n for n in nodes() if n.get('text')=='当前播放'])==1
area=box(find('当前播放'));a,b=image_pair('v1.6-marker-playing');assert ImageChops.difference(a.crop(area),b.crop(area)).getbbox(),'playing bars static'
adb('shell','input','keyevent','127');time.sleep(.6);area=box(find('当前播放'));a,b=image_pair('v1.6-marker-paused');assert ImageChops.difference(a.crop(area),b.crop(area)).getbbox() is None,'paused bars moving'
adb('shell','input','keyevent','87');time.sleep(.8);new=box(find('当前播放'));assert abs(new[1]-original[1])>80;assert len([n for n in nodes() if n.get('text')=='当前播放'])==1;snap('v1.6-marker-next.png')
print('PASS: 列表尾部当前播放音柱、播放动态/暂停静止、焦点分离与切歌跟随')
