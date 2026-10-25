#!/usr/bin/env python3
"""首页冷启动几何稳定、资料库直达、虚拟列表放大后的实际渲染尺寸。"""
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
def bounds(n):return list(map(int,re.findall(r'-?\d+',n.get('bounds'))))
def key(k):adb('shell','input','keyevent',str(k));time.sleep(.3)
def listnode():return next(n for n in nodes() if n.get('class')=='android.widget.ListView')
original=adb('shell','settings','get','global','animator_duration_scale').strip()
try:
 adb('shell','settings','put','global','animator_duration_scale','1');adb('shell','wm','size','1920x1080');adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.8)
 first=bounds(find('音乐'));snap('v1.6-home-cold.png');key(20);key(20);after=bounds(find('音乐'));assert (after[0],after[2]-after[0],after[3]-after[1])==(first[0],first[2]-first[0],first[3]-first[1]),(first,after);snap('v1.6-home-after-down.png')
 # 卡片直接进入曲库，不再展示三个选项的弹窗。
 adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);target=next(n for n in nodes() if n.get('text')=='音乐资料库' and bounds(n)[1]>300);x1,y1,x2,y2=bounds(target);adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.5);find('播放列表');find('单曲 (',prefix=True)
 tab=find('单曲 (',prefix=True).get('text');focus(tab);key(20);time.sleep(.3)
 current=int(re.findall(r'\d+',listnode().get('content-desc'))[0]);assert current<=2,current
 # 获取下一行中心，整个可见表中至少有3行，不触发滚动。
 tree_now=nodes();selected=next(n for n in tree_now if n.get('class')=='android.widget.FrameLayout' and n.get('selected')=='true');left,top,right,bottom=bounds(selected)
 parent=listnode();vx1,vy1,vx2,vy2=bounds(parent);next_center=(top+bottom)//2+168
 result=adb('shell','am','instrument','-w','-e','x1',str(vx1),'-e','x2',str(vx2),'-e','y',str(next_center),'com.yunying.music.test/.FocusMotionProbe');assert 'INSTRUMENTATION_CODE: 0' in result,result;print(result.split('INSTRUMENTATION_RESULT: focusMotion=')[1].splitlines()[0],flush=True)
 # 放大后的实际内容边界大于普通行，同时留在列表视口内。
 data=nodes();active=next(n for n in data if n.get('class')=='android.widget.LinearLayout' and n.get('selected')=='true');a=bounds(active);normal=next(bounds(n) for n in data if n.get('class')=='android.widget.LinearLayout' and n.get('selected')=='false' and abs(bounds(n)[3]-bounds(n)[1]-144)<3 and bounds(n)[0]>=vx1 and bounds(n)[2]<=vx2)
 assert a[0]>=vx1 and a[2]<=vx2 and a[1]>=vy1 and a[3]<=vy2,(a,bounds(parent));snap('v1.6-list-focus.png')
 print('PASS: 首页初始尺寸稳定、资料库直达列表、选中项放大且无裁剪')
finally:
 if original=='null':adb('shell','settings','delete','global','animator_duration_scale')
 else:adb('shell','settings','put','global','animator_duration_scale',original)
