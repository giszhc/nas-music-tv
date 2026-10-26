#!/usr/bin/env python3
from pathlib import Path
exec(Path(__file__).with_name('tv_smoke.py').read_text().split("adb('shell','am','force-stop'")[0])
OUT=Path(__file__).resolve().parents[1]/'screenshots'
def restart():
 adb('shell','am','force-stop','com.yunying.music');adb('shell','am','start','-n','com.yunying.music/.MainActivity');time.sleep(.6)
def key(k):adb('shell','input','keyevent',str(k));time.sleep(.4)
restart();assert not any(n.get('content-desc')=='已购歌曲' for n in nodes())
focus('其他设置');snap('refine-home-bottom.png');back();assert find('我的').get('focused')=='true';snap('refine-home-back-top.png')
focus('其他设置');tap('其他设置');focus('音乐目录');snap('refine-settings-focus.png');tap('音乐目录');find('自定义首页页签');find('相声 · ',prefix=True);snap('refine-directory-tabs.png');back();tap('群晖 NAS');
fields=[n for n in nodes() if n.get('class')=='android.widget.EditText'];assert len(fields)==4
for i in range(5):key(20)
assert find('保存').get('focused')=='true';snap('refine-nas-save.png');tap('保存');find('NAS 配置已保存');back();tap('阿里云盘');find('连接私人曲库，导入后即可在电视播放');snap('refine-aliyun-account.png');back();back()
restart();tap(desc='搜索');focus('M');snap('refine-search-keyboard.png');tap('Y');time.sleep(.5)
focus('艺人');assert find('艺人').get('selected')=='true';snap('refine-search-tab-artist.png');focus('歌单');assert find('歌单').get('selected')=='true';find('播放列表 · ',prefix=True)
restart();tap(desc='搜索');focus('M');tap('清空');time.sleep(.4);focus('未知歌手');snap('refine-search-history.png');key(22);snap('refine-cover-focus.png');back();back();restart();menu('正在播放');tap(desc='更多');tap('修改歌曲名和歌手');find('修改歌曲信息');snap('refine-song-info.png');tap('取消')
print('PASS: 首页回顶/移除模块、设置焦点、标签直达维护、NAS上下导航和保存、云盘账号布局、搜索聚焦切换/历史/封面、歌曲编辑入口')
