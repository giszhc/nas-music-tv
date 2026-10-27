#!/usr/bin/env python3
"""1920x720 下长菜单、表单、目录确认和播放器操作保持完整可见。"""
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
def key(k):adb('shell','input','keyevent',str(k));time.sleep(.3)
def move_to(target):
 for step in range(12):
  lists=[n for n in nodes() if n.get('class')=='android.widget.ListView']
  if not lists:time.sleep(.3);continue
  at=int(re.findall(r'\d+',lists[0].get('content-desc'))[0])
  if at==target:return
  key(20 if at<target else 19)
 raise AssertionError('menu selection did not advance')
def full(n,minheight=0):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));assert 0<=x1<x2<=1920 and 0<=y1<y2<=720,n.get('bounds');assert y2-y1>=minheight,n.get('bounds')
original=adb('shell','wm','size')
try:
 adb('shell','wm','size','1920x720');adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(1.2)
 adb('shell','input','keyevent','82');time.sleep(.6);key(19);move_to(5);key(20);selected=[n for n in nodes() if n.get('class')=='android.widget.FrameLayout' and n.get('selected')=='true'];assert selected;full(selected[0],96);find('退出应用');snap('v1.5-short-menu.png');back()
 adb('shell','input','keyevent','82');time.sleep(.5);key(19);move_to(4);key(23);tap('创建首页页签');full(find('创建页签'),96);snap('v1.5-short-module-editor.png');back()
 menu('设置');tap('群晖 NAS');focus('保存');full(find('保存'),96);snap('nas-short-save.png');back();back()
 menu('设置');tap('音乐目录');tap('添加音乐目录');time.sleep(.4);tap('打开目录  music');time.sleep(.5);full(find('导入当前目录及子目录'),96);snap('v1.5-short-import.png');back();back();back();back()
 menu('正在播放');time.sleep(1)
 for desc in ['播放队列','音效 ·','更多']:full(find(desc=desc,prefix=True),88)
 snap('v1.5-short-player.png')
 print('PASS: 短屏菜单末项、新建页签、NAS保存、目录确认与播放工具栏完整可见')
finally:
 match=re.search(r'Override size: (\d+x\d+)',original)
 if match:adb('shell','wm','size',match.group(1))
 else:adb('shell','wm','size','reset')
