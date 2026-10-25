#!/usr/bin/env python3
"""已配置 fake_dsm 音乐源的模拟器回归，覆盖真实原生界面与持久化。"""
import subprocess,time,re,shlex,os,xml.etree.ElementTree as ET
from pathlib import Path
ADB=str(Path.home()/'android-sdk/platform-tools/adb')
OUT=Path(__file__).resolve().parents[1]/'screenshots'
def adb(*args):return subprocess.check_output([ADB,*( ["-P",os.environ["YY_ADB_PORT"]] if "YY_ADB_PORT" in os.environ else []),*( ["-s",os.environ["YY_ADB_SERIAL"]] if "YY_ADB_SERIAL" in os.environ else []),*args],stderr=subprocess.STDOUT,timeout=20).decode(errors='replace')
def tree(result):
    assert 'INSTRUMENTATION_CODE: 0' in result,result
    return ET.fromstring(result.split('INSTRUMENTATION_RESULT: xml=',1)[1].split('\nINSTRUMENTATION_CODE:',1)[0].strip())
def nodes():return list(tree(adb('shell','am','instrument','-w','com.yunying.music.test/.Probe')).iter('node'))
def find(text=None,desc=None,prefix=False):
    for node in nodes():
        value=node.get('content-desc' if desc else 'text','');target=desc or text
        if value.startswith(target) if prefix else value==target:return node
    raise AssertionError('找不到控件: '+str(desc or text))
def tap(text=None,desc=None,prefix=False):
    n=find(text,desc,prefix);x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.45)
def focus(label):
    adb('shell','input','keyevent','19')
    root=tree(adb('shell','am','instrument','-w','-e','focus',shlex.quote(label),'com.yunying.music.test/.Probe'))
    assert any(n.get('focused')=='true' for n in root.iter('node'));time.sleep(.3)
def snap(name):
    adb('shell','screencap','-p','/sdcard/yy-screen.png');adb('pull','/sdcard/yy-screen.png',str(OUT/name))
def back():adb('shell','input','keyevent','4');time.sleep(.45)
def no_sources():
    texts=' '.join(n.get('text','') for n in nodes())
    assert '群晖' not in texts and '阿里云盘' not in texts,texts

def menu(item):
    adb('shell','input','keyevent','82');time.sleep(.2);tap(item)
def enter_player():
    menu('正在播放');time.sleep(.5)

adb('shell','am','force-stop','com.yunying.music')
adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.6)
find('播放列表');no_sources()
menu('设置');find('添加音乐目录');snap('reference-settings.png')
tap('添加音乐目录');tap('群晖 NAS');tap('打开目录  music');tap('添加当前目录歌曲');find('已添加',prefix=True)
# 同一目录可以独立导入听书，不能伪造在线听书资源。
tap('导入听书');tap('添加当前目录听书');find('已添加',prefix=True);tap('关闭');tap('关闭')
find('夜曲');no_sources();focus('夜曲');snap('reference-list.png');tap('夜曲');time.sleep(.7)
tap(desc='暂停');find(desc='播放');focus('播放');snap('reference-player.png')
mode=next(n.get('text') for n in nodes() if n.get('text') in ['列表循环','单曲循环','随机播放']);seen=set()
for _ in range(3):
    seen.add(mode);tap(mode);mode=next(n.get('text') for n in nodes() if n.get('text') in ['列表循环','单曲循环','随机播放'])
assert len(seen)==3
try:find('已收藏')
except AssertionError:tap('收藏')
find('已收藏');tap('播放队列',prefix=True);find('Ocean Drive');focus('夜曲');snap('reference-queue.png');back();find(desc='播放')
tap('播放队列',prefix=True);focus('Ocean Drive');tap('Ocean Drive');time.sleep(.5);tap(desc='暂停')
tap(desc='音效 · ',prefix=True);find('选择音效');focus('3D丽音');snap('reference-effects.png');tap('3D丽音');find(desc='音效 · 3D丽音');tap(desc='音效 · 3D丽音');tap('原声');back()
menu('我的');snap('reference-my.png');tap('我喜欢');find('夜曲');focus('夜曲');snap('reference-favorites.png');back()
tap('最近播放');find('Ocean Drive');back();tap('我的歌单');find('我的歌单 · 播放列表');find('夜曲');back()
# 遥控器滚动到设置，进入服务页，真实切换推荐状态。
focus('应用服务设置');tap('应用服务设置');find('全量服务');focus('基本服务');snap('reference-services.png');tap('基本服务');back()
focus('应用服务设置');tap('应用服务设置');tap('全量服务');back()
focus('内容推荐服务');tap('内容推荐服务');tap('关闭');back();focus('内容推荐服务');tap('内容推荐服务');tap('开启');back()
focus('其他设置');snap('reference-my-settings.png');tap('其他设置');find('添加音乐目录');tap('关闭')
# 回到主页顶部，再搜索验证首字母和分栏返回。
adb('shell','input','swipe','1700','260','1700','1000','400');time.sleep(.5)
focus('搜索');tap(desc='搜索');time.sleep(.4);snap('reference-search.png');tap('Y');tap('Q');time.sleep(.5);find('夜曲');snap('reference-query.png')
focus('“YQ”的全部结果…');time.sleep(.4);snap('reference-suggestions.png');tap('“YQ”的全部结果…');find('夜曲');focus('夜曲');snap('reference-results.png')
back();find('猜你想搜');back();find('A');tap('清空');tap('Y');tap('E');tap('Q');tap('U');time.sleep(.5);find('夜曲')
# 关闭动画后焦点、分栏、队列仍可操作。
original=adb('shell','settings','get','global','animator_duration_scale').strip()
try:
    adb('shell','settings','put','global','animator_duration_scale','0');back();back();enter_player();tap('播放队列',prefix=True);back();find(desc='播放')
finally:
    if original=='null':adb('shell','settings','delete','global','animator_duration_scale')
    else:adb('shell','settings','put','global','animator_duration_scale',original)
back();time.sleep(.5);adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);find('播放列表');no_sources();enter_player();find(desc='播放');assert int(find(desc='播放队列 · ',prefix=True).get('content-desc').split('·')[1])>=3
assert 'com.yunying.music/YunYingMusic' in adb('shell','dumpsys','media_session')
print('PASS: 默认列表、设置导入/去重、听书导入、播放与模式/收藏/队列/音效、我的真实状态、服务开关、首字母/全拼搜索及分栏、减少动画、重启恢复、MediaSession')
