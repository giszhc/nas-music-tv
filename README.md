# 云影音乐 · Android 电视播放器 1.9

1.9：云盘音频在 Android 6.0+ 使用原生 MediaDataSource 和应用 HTTP 读取层，处理重定向、Range 分段及约 2MB 内存缓存，连续读取复用连接；仍由系统解码，不引入额外解码库。地址获取、预读、解析和缓冲分别显示阶段及超时，网络或直链失败最多重新获取地址一次。播放错误独立显示，避免被歌词覆盖。Android 5.x 保留系统直连，并增加阶段超时和错误提示。

验证通过 47 项隔离测试：重定向、Range/忽略 Range、缓存、未知长度、403/429、错误页面、截断、超时、取消，以及真实后台服务 MP3 时长/暂停/定位/切歌与错误提示可见性。真实电视与阿里云账号的端到端效果仍需实测。

云盘链路复测：先执行 `YY_BUILD_DIR=/tmp/yy-cloud-playback-build bash build.sh`，启动 `python3 test/cloud-playback/server.py`（需要 ffmpeg 生成测试 MP3），执行 `bash test/cloud-playback/build.sh`，再执行 `adb shell am instrument -w com.yunying.music.cloudtest/com.yunying.music.CloudPlaybackProbe`。探针使用隔离应用和本机 HTTP 夹具，不使用真实阿里云账号。日志标签 `YYPlayback` 只记录阶段、耗时和字节数，不记录令牌或签名直链。

1.8：移除 LibVLC 和 WMA 解码，恢复系统播放器与原有音效，安装包恢复为轻量体积。WMA 不再列入可导入格式；已导入的 WMA 记录在读取时跳过，云盘源文件不会删除。1.7 的专用网络缓存和重连随内核一起移除，云盘 MP3 卡顿仍需后续单独排查。

当前播放的曲目在列表右侧显示音柱，播放时跳动、暂停时静止；选择焦点与当前播放标记独立。验证脚本：`test/playing_marker_regression.py`。

1.6：列表选中项放大约 4.5%，增加 190ms 缩放与卡面过渡，焦点光圈加粗并预留裁剪边距。滚动容器不参与焦点缩放，首页冷启动不再重复构建。音乐资料库卡片、顶部入口直接进入播放列表。专项验证：`python3 test/focus_presentation_regression.py`。

1.5 更新：歌曲、队列、目录、搜索结果和分类网格改为原生可回收虚拟列表。设置 → 音乐目录 → 自定义首页页签，可创建相声、听书等页签，独立导入 NAS/阿里云盘目录，支持重命名、排序和删除。首页菜单也保留管理快捷入口。删除页签保留服务器源文件。

电视统一使用遥控器返回键：设置详情返回分类，分类返回首页；目录返回上级，到根目录后退出。设置、目录与新建页签不显示关闭、返回、上级或取消按钮；保存和导入等业务操作保留。

长列表回归：先启动 `YY_VIRTUAL_TRACKS=2000 python3 test/fake_dsm.py 18081`，再运行 `test/virtual_list_regression.py`、`test/home_modules_regression.py` 和 `test/layout_clipping_regression.py`。覆盖 2000+ 条数据、遥控器连续选择、返回恢复位置、模块生命周期和短屏按钮裁剪。

原生 Android 应用，默认进入音乐首页。按照最新电视截图改为深灰背景、灰色卡片、白色遥控焦点；所有音乐源和配置集中在设置。

## 安装与使用

安装 `YunYingMusic-1.9.apk`，支持同签名覆盖升级。

```bash
adb connect <电视IP>:5555
adb install -r YunYingMusic-1.9.apk
```

1. 启动进入音乐首页；按遥控器菜单键选择「设置」或「播放列表」。
2. 配置群晖 NAS 并测试，或扫码登录阿里云盘。
3. 目录列表按右键可直达底部导入按钮，末项按下进入确认；确认按上回到列表。在「音乐目录」打开「添加音乐目录」，进入文件夹，选择「导入音乐」或「导入听书」，添加单个文件或当前目录。重复内容跳过，不递归子目录。
4. 关闭设置，播放列表选曲。播放页可收藏、切换模式、上一首/下一首、定位进度、打开队列与音效。
5. 播放工具栏的「音效」直接切换五种模式；「更多」中调整播放偏好。首页菜单中的「我的」提供收藏、历史、歌单、听书和服务设置。「搜索」提供五列遥控键盘、首字母/全拼检索、建议和双列结果。

## 已实现

- DSM FileStation、阿里云盘网页扫码及开放平台两套凭据分别处理。
- 后台播放、前台通知、MediaSession 媒体键；列表/单曲循环、随机播放。
- 收藏、最近播放、听书历史、播放列表、队列和进度保存本机。恢复上次队列时保持暂停。
- 在线歌词/封面/歌手头像及缓存。推荐、数量与资料库使用真实状态；没有虚构会员和已购资源。
- 原声、3D丽音、超重低音、HiFi现场、3D环绕。使用 Equalizer/BassBoost/Virtualizer/PresetReverb 处理解码后的音频；设备不支持的处理器自动降级。
- 播放页音效按钮直接打开五种模式单选，立即生效并记忆选择；设置页已移除重复音效分类。
- 各页面标题与滚动内容隔离，白色聚焦、底部渐隐、页面/抽屉过渡，尊重系统关闭动画。

## 阿里云盘扫码修复

网页扫码凭据与开放平台凭据使用不同 API；修复刷新接口、设备签名/会话、扫码确认后的凭据解析与保存、关闭设置覆盖凭据、过期请求覆盖新登录等问题。二维码超时或服务初始化失败会显示明确阶段，保留已取得凭据供验证重试。

协议夹具验证通过，尚未使用真实账号完成扫码确认后的端到端测试。接口仍可能受服务端调整影响。

## 构建与验证

需要 JDK、`~/android-sdk/platforms/android-34` 和 build-tools34.0.0。

```bash
bash build.sh
```

继续使用 `keystore/yunying.jks`，生成1.9版本 APK（minSdk21，targetSdk34）。不要替换签名文件。

测试探针是独立 APK，不进入正式包。模拟器 NAS 指向 `10.0.2.2:18080`，测试账号任意。

```bash
YY_SCROLL_TRACKS=20 python3 test/fake_dsm.py 18080
bash test/ui-probe/build.sh
adb install -r YunYingMusic-1.9.apk
python3 test/tv_smoke.py
python3 test/scroll_regression.py
python3 test/motion_regression.py
python3 test/playback_regression.py
python3 test/effects_dialog_regression.py
adb shell am instrument -w com.yunying.music.test/.AuthProbe
adb shell am instrument -w com.yunying.music.test/.SoundProbe
```

音效探针的7种格式夹具需要预先位于 `/tmp/yy-effects-formats/tone.*`（MP3、FLAC、AAC、M4A、WAV、OGG、OPUS）。测试源额外提供20条可播放的音频用于长列表滚动验证，正式 APK 不附带这些测试数据。

当前验证：27项登录协议测试、7种实际解码格式×5种模式，28次非原声处理器应用有效。

新版直接截图：`screenshots/reference-*.png`。布局依据及内容约定见 DESIGN.md、PRODUCT.md，拼音字典许可见 THIRD_PARTY_NOTICES.md。

## 限制

解码和音效最终取决于电视设备；未新增 APE/DSD 解码器。在线元数据可能匹配同名版本。没有商业平台购买、会员、付费听书或在线推荐接口；已购区域为真实空状态。真实电视遥控器、硬件帧率和真实阿里云盘扫码仍需实机验证。

## 1.5 更新

- 音乐首页采用补充参考图排版；顶部第三个按钮进入当前播放，播放音柱随播放/暂停状态变化。
- 设置全屏布局、表单间距及白色焦点统一到首页风格。
- 导入目录固定确认区，明确连接遥控器右键、上下键的焦点路径。无音频目录显示说明。
- 首页返回或菜单退出均先确认，默认「继续使用」，确认退出会停止播放。
- 本次回归：`python3 test/settings_home_regression.py`（先启动 fake_dsm）。

## Git 仓库与签名

仓库仅跟踪源码、资源、脚本、测试和文档；构建输出、截图、备份、APK 与 `keystore/` 均留在本地。要生成可覆盖现有安装的 APK，须自行安全备份并恢复原 `keystore/yunying.jks`。缺少密钥时构建脚本会生成新的开发签名，该签名不能覆盖原签名安装。请勿将私钥或真实账号凭据提交到仓库。
