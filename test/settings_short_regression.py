#!/usr/bin/env python3
"""短屏设置表单与服务式分类布局回归。"""
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
OUT=Path(__file__).resolve().parents[1]/'screenshots'
try:
 adb('shell','wm','size','1920x720');adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.8)
 menu('设置');snap('v1.4-settings-hub.png');tap('阿里云盘');focus('高级：开放平台凭据');tap('高级：开放平台凭据');focus('保存并测试云盘凭据');snap('v1.4-settings-advanced-short.png')
 n=find('保存并测试云盘凭据');assert int(re.findall(r'\d+',n.get('bounds'))[3])<=720
 tap('返回');tap('播放');focus('重置');snap('v1.4-settings-playback-short.png');tap('返回');tap('关闭')
 print('PASS: 1920×720高级凭据和播放设置滚动可达')
finally:adb('shell','wm','size','reset')
