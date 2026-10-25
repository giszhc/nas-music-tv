#!/usr/bin/env python3
"""自定义首页模块的完整生命周期；使用模拟 NAS 目录。"""
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
OUT=Path(__file__).resolve().parents[1]/'screenshots'
def key(k):adb('shell','input','keyevent',str(k));time.sleep(.35)
def settext(value):tree(adb('shell','am','instrument','-w','-e','setText',"''",'-e','value',shlex.quote(value),'com.yunying.music.test/.Probe'))
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.6)
menu('自定义首页');time.sleep(.4)
try:find('相声');back()
except AssertionError:
 tap('创建首页页签');time.sleep(.4);find('创建首页页签');settext('相声');tap('创建页签');time.sleep(.5)
menu('自定义首页');tap('相声');tap('导入专属目录');time.sleep(.4)
# 遥控进入 music，使用底部确认导入模块。
focus('群晖 NAS');key(20);key(23);time.sleep(.6);n=next(n for n in nodes() if n.get('text')=='导入到“相声”' and n.get('clickable')=='true');x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.5);find('已添加',prefix=True);back();back();time.sleep(.5)
focus('相声');snap('v1.5-custom-home.png');tap('相声');time.sleep(.5);find('相声');snap('v1.5-custom-list.png')
tap('Ocean Drive');time.sleep(.8);find('听书');snap('v1.5-custom-player.png')
print('PASS: 创建相声模块、专属导入、首页卡片、独立列表与听书播放')

back();back();menu('自定义首页');tap('创建首页页签');settext('模块回归');tap('创建页签');time.sleep(.4)
menu('自定义首页');tap('模块回归');tap('重命名');settext('模块重命名');tap('保存名称');time.sleep(.4)
menu('自定义首页');tap('模块重命名');tap('上移');time.sleep(.4)
menu('自定义首页');names=[n.get('text') for n in nodes()];assert names.index('模块重命名')<names.index('相声');tap('模块重命名');tap('删除页签');tap('保留模块');menu('自定义首页');find('模块重命名');tap('模块重命名');tap('删除页签');tap('删除页签（保留源文件）');time.sleep(.4)
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);menu('自定义首页');find('相声');assert not any(n.get('text')=='模块重命名' for n in nodes());back()
print('PASS: 模块重命名、排序、删除取消/确认与重启持久化')
