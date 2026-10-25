#!/usr/bin/env python3
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
OUT=Path(__file__).resolve().parents[1]/'screenshots'
def settext(value):tree(adb('shell','am','instrument','-w','-e','setText',"''",'-e','value',shlex.quote(value),'com.yunying.music.test/.Probe'))
def manage():
    menu('设置');tap('音乐目录');tap('自定义首页页签')
def close_settings():back();back();time.sleep(.3)
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5)
find('我的');assert not any(n.get('text')=='家庭乐房' for n in nodes())
manage();tap('创建首页页签');settext('我的');tap('创建页签');find('“我的”为固定页签，请使用其他名称');settext('页签回归');tap('创建页签');close_settings()
focus('页签回归');snap('v1.5-custom-tabs.png');tap('页签回归');find('页签回归');back()
manage();tap('页签回归');tap('重命名');settext('新页签回归');tap('保存名称');close_settings();find('新页签回归')
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);find('新页签回归')
manage();tap('新页签回归');tap('删除页签');tap('删除页签（保留源文件）');close_settings();find('我的');assert not any(n.get('text')=='新页签回归' for n in nodes())
print('PASS: 音乐目录创建页签、固定我的保护、首页打开独立列表、重命名、重启持久化、删除同步')
