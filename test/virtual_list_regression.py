#!/usr/bin/env python3
"""2000条真实目录数据：复用视图、连续方向键、滚动位置与搜索。先启动 YY_VIRTUAL_TRACKS=2000 fake_dsm.py 18081。"""
from pathlib import Path
import os
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
def key(code):adb('shell','input','keyevent',str(code));time.sleep(.12)
def virtual():
 n=next(n for n in nodes() if n.get('class') in ('android.widget.ListView','android.widget.GridView'))
 selected,total=map(int,re.findall(r'\d+',n.get('content-desc')));return selected,total

def advance(count):
 before,total=virtual();previous=before
 for step in range(count):
  key(20)
  if step%8==7:
   current,total=virtual();assert current>previous,(previous,current);previous=current
 current,total=virtual();assert current>=before+count-1,(before,current)
 assert len(nodes())<100,'视图数量随数据量增长，未虚拟化'
 return current,total

adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5)
menu('设置');tap('群晖 NAS');fields=[n for n in nodes() if n.get('class')=='android.widget.EditText'];port=fields[1].get('text');tree(adb('shell','am','instrument','-w','-e','setText',shlex.quote(port),'-e','value','18081','com.yunying.music.test/.Probe'))
tap('保存并测试 NAS');time.sleep(.6);find('NAS 连接成功',prefix=True);back();tap('音乐目录');tap('添加音乐目录');time.sleep(.4);tap('打开目录  music');time.sleep(.6)
_,total=virtual();assert total>=2000;assert len(nodes())<100;snap('v1.5-virtual-source.png');tap('添加当前目录歌曲');back();back();back();back()
menu('播放列表');time.sleep(.4);tab=find('单曲 (',prefix=True).get('text');focus(tab);key(20);key(20);current,total=advance(40);assert total>=2000;snap('v1.5-virtual-library.png')
key(23);time.sleep(.6);tap('播放队列',prefix=True);time.sleep(.4);key(20);start,total=virtual();current,total=advance(50);assert current>start;snap('v1.5-virtual-queue.png')
key(123);time.sleep(.4);last,total=virtual();assert last==total,(last,total);key(20);assert virtual()[0]==total;key(19);assert virtual()[0]==total-1
# 关闭再打开队列，初始选中当前播放项；库页面返回恢复此前选择，而非第一行。
key(22);back();time.sleep(.4);current,total=virtual();assert current>1
back();adb('shell','input','keyevent','84');time.sleep(.5)
# 搜索键从首页进入。
try:find('输入 首字母/全拼 搜索')
except AssertionError:tap(desc='搜索')
adb('shell','input','text','CHANGLIEBIAO');time.sleep(.8)
focus('“CHANGLIEBIAO”的全部结果…');tap('“CHANGLIEBIAO”的全部结果…');time.sleep(.4);key(20)
_,total=virtual();assert total>=2000;advance(20);snap('v1.5-virtual-search.png')
print('PASS: 2000+条目录/曲库/队列/搜索仅保留可见视图，连续下选不回顶部，返回恢复位置')
