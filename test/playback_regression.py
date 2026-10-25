from pathlib import Path
from PIL import Image
script=Path('test/tv_smoke.py').resolve();exec(script.read_text().split("adb('shell','am','force-stop'")[0],globals());OUT=Path.cwd()/'screenshots'
adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.5);find('单曲 (23)');no_sources()
adb('shell','input','keyevent','82');tap('我的');focus('订阅');tap('订阅');find('听书');focus('Ocean Drive');tap('Ocean Drive');time.sleep(.7);find('听书');tap(desc='暂停');snap('reference-audiobook.png');back();back();back()
focus('夜曲');tap('夜曲');time.sleep(.8);tap(desc='暂停');focus('播放');snap('reference-player.png');assert len(set(Image.open(OUT/'reference-player.png').convert('RGB').crop((800,0,1100,180)).getdata()))>20;tap(desc='音效 · ',prefix=True);find('选择音效');focus('原声');snap('reference-effects.png');tap('原声');find(desc='播放')
# 进度条遥控左右键应直接定位五秒，暂停状态不能被改变。
focus('播放进度，左右键快进快退');before=next(n.get('text') for n in nodes() if re.fullmatch(r'\d+:\d+ / \d+:\d+',n.get('text','')))
adb('shell','input','keyevent','22');time.sleep(.5);after=next(n.get('text') for n in nodes() if re.fullmatch(r'\d+:\d+ / \d+:\d+',n.get('text','')))
def seconds(value):m,s=map(int,value.split(' / ')[0].split(':'));return m*60+s
assert seconds(after)-seconds(before)==5,(before,after)
find(desc='播放');back();focus('夜曲');snap('reference-list.png')
print('PASS: 最终安装包23条曲库、听书独立播放、音乐封面/歌词恢复、音效弹窗、遥控五秒定位且保持暂停')
