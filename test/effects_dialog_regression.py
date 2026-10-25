#!/usr/bin/env python3
from pathlib import Path
script=Path(__file__).with_name('tv_smoke.py').resolve();exec(script.read_text().split("adb('shell','am','force-stop'")[0],globals())
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);tap('夜曲');time.sleep(.7);tap(desc='暂停')
for name in ['3D丽音','超重低音','HiFi现场','3D环绕','原声']:
    tap(desc='音效 · ',prefix=True);find('选择音效');tap(name);find(desc='音效 · '+name);find(desc='播放')
tap(desc='音效 · 原声');focus('3D环绕');snap('reference-effects.png');tap('3D环绕');back()
adb('shell','input','keyevent','82');tap('设置');assert not any(n.get('text')=='音效' for n in nodes());tap('关闭')
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);adb('shell','input','keyevent','82');tap('正在播放');time.sleep(.5);find(desc='音效 · 3D环绕');tap(desc='音效 · 3D环绕');find('选择音效');tap('原声')
print('PASS: 播放页五种音效即时切换、保持暂停、重启保存、设置页移除重复配置')
