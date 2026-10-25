#!/usr/bin/env python3
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
OUT=Path.cwd()/'screenshots'
adb('shell','am','force-stop','com.yunying.music')
adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5)
enter_player();tap(desc='更多');find('更多操作');find('重新匹配歌词和封面')
lyrics=next(n.get('text') for n in nodes() if n.get('text','').startswith('自动搜索歌词：'))
tap(lyrics);changed='自动搜索歌词：'+('关闭' if lyrics.endswith('开启') else '开启');find(changed)
tap(changed);find(lyrics)
offset=next(n.get('text') for n in nodes() if n.get('text','').startswith('歌词时间偏移：'))
tap(offset);tap('提前 0.5 秒');find('延后 0.5 秒');tap('延后 0.5 秒');find(offset);back()
tap(desc='更多');snap('v1.5-player-more.png');tap('重新匹配歌词和封面');time.sleep(.5)
find(desc='更多');back();menu('设置');find('音乐目录');find('群晖 NAS');find('阿里云盘')
assert not any(n.get('text') in ('播放','联网自动搜索歌词','联网自动搜索封面') for n in nodes())
focus('群晖 NAS');snap('v1.5-glass-settings.png');time.sleep(.35);snap('v1.5-glass-settings-motion.png')
from PIL import Image,ImageChops
x=Image.open(OUT/'v1.5-glass-settings.png').convert('RGB');y=Image.open(OUT/'v1.5-glass-settings-motion.png').convert('RGB')
assert ImageChops.difference(x,y).getbbox(), '玻璃边框没有持续旋转'
tap('群晖 NAS');find('使用 HTTPS');back();back()
print('PASS: 播放更多设置迁移、开关往返、歌词偏移、重新匹配入口、设置移除播放分类、全局边框动画')
