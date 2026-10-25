#!/usr/bin/env python3
"""电视设置/目录/编辑页仅用遥控器返回，不提供屏幕关闭或返回按钮。"""
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
def no_close():assert not any(n.get('text') in ('关闭','返回','上级','取消') for n in nodes())
def settext(match,value):tree(adb('shell','am','instrument','-w','-e','setText',shlex.quote(match),'-e','value',shlex.quote(value),'com.yunying.music.test/.Probe'))
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(1)
menu('设置');no_close();tap('群晖 NAS');no_close();fields=[n.get('text') for n in nodes() if n.get('class')=='android.widget.EditText']
for old,value in zip(fields,['10.0.2.2','18081','home','test']):settext(old,value)
tap('保存并测试 NAS');time.sleep(.5);back();find('设置');tap('音乐目录');tap('添加音乐目录');time.sleep(.5);no_close();tap('打开目录  music');time.sleep(.5);find('群晖 NAS  /  music');no_close();snap('v1.5-tv-directory.png')
back();find('打开目录  music');back();find('添加音乐目录');back();find('设置');no_close();snap('v1.5-tv-settings.png');back();find('音乐')
menu('自定义首页');tap('创建首页页签');no_close();find('创建页签');snap('v1.5-tv-module-editor.png');back();find('音乐')
print('PASS: 设置详情→分类→首页，目录→上级→设置，创建页签返回取消；无屏幕关闭/返回按钮')
