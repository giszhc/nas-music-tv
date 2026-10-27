#!/usr/bin/env python3
"""短屏设置表单与服务式分类布局回归。"""
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
OUT=Path(__file__).resolve().parents[1]/'screenshots'
try:
 adb('shell','wm','size','1920x720');adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.8)
 menu('设置');snap('nas-settings-hub.png');tap('群晖 NAS');focus('保存');snap('nas-settings-short.png')
 n=find('保存');assert int(re.findall(r'\d+',n.get('bounds'))[3])<=720
 back();back()
 print('PASS: 1920×720 群晖设置保存按钮可达')
finally:adb('shell','wm','size','reset')
