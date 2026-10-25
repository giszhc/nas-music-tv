#!/usr/bin/env python3
"""1.4：首页、遥控目录确认、退出保护与播放状态动效。需 fake_dsm。"""
from pathlib import Path
import time
from PIL import Image,ImageChops
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
OUT=Path(__file__).resolve().parents[1]/'screenshots'
def focused(text):return find(text).get('focused')=='true'
def key(code):adb('shell','input','keyevent',str(code));time.sleep(.8)
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.8)
find('音乐');find('我的');find(desc='正在播放');snap('v1.4-home.png')
back();find('退出云影音乐？');key(19);key(23);find('音乐')
back();snap('v1.4-exit-confirmation.png');back();find('音乐')
menu('设置');tap('群晖 NAS');tap('保存并测试 NAS');time.sleep(.7);find('NAS 连接成功',prefix=True);snap('v1.4-settings-nas.png')
tap('返回');tap('音乐目录');tap('添加音乐目录');time.sleep(.5);tap('打开目录  music');time.sleep(.6)
# 不借助触摸确认：目录列表首项右键直接到固定底栏，向上恢复到末项。
focus('打开目录  专辑');key(22);assert focused('添加当前目录歌曲');snap('v1.4-import-focused.png');key(19)
assert any(n.get('focused')=='true' and n.get('text','').startswith('歌曲  ') for n in nodes())
key(20);assert focused('添加当前目录歌曲');key(23);find('已添加',prefix=True)
tap('关闭');tap('返回');tap('关闭');tap(desc='正在播放');time.sleep(1);
try:find(desc='暂停')
except AssertionError:tap(desc='播放')
find(desc='暂停');back();time.sleep(.6)
# 右上角第三个正在播放入口，实际播放时音柱变化。
snap('v1.4-playing-a.png');time.sleep(.25);snap('v1.4-playing-b.png')
a=Image.open(OUT/'v1.4-playing-a.png').convert('RGB');b=Image.open(OUT/'v1.4-playing-b.png').convert('RGB');crop=(1396,85,1454,140)
assert ImageChops.difference(a.crop(crop),b.crop(crop)).getbbox(),'播放音柱没有变化'
tap(desc='正在播放');find(desc='暂停');tap(desc='暂停');back();time.sleep(.7)
snap('v1.4-paused-a.png');time.sleep(.25);snap('v1.4-paused-b.png')
a=Image.open(OUT/'v1.4-paused-a.png').convert('RGB');b=Image.open(OUT/'v1.4-paused-b.png').convert('RGB')
assert ImageChops.difference(a.crop(crop),b.crop(crop)).getbbox() is None,'暂停音柱仍在运动'
back();find('退出云影音乐？');key(20);key(23);time.sleep(.5)
assert 'ACTIVITY com.yunying.music/' not in adb('shell','dumpsys','activity','top')
assert 'ServiceRecord{' not in adb('shell','dumpsys','activity','services','com.yunying.music')
print('PASS: 首页、退出取消/确认、NAS、长目录右键/上下键确认导入、播放跳转、播放动效/暂停静止')
