# MusicPlayer

一个纯本地、纯离线的 Android 音乐播放器项目，目标是做成：

- 偏 `Winamp` 气质的紧凑播放界面
- 偏 `foobar2000` 风格的本地曲库浏览
- 基于 `SAF + Room + Media3` 的可维护架构

## 当前能力

- 选择本地音乐目录并持久化授权
- 递归扫描音频文件并建立本地索引
- 曲目、艺术家、专辑、文件夹、收藏、最近播放视图
- 本地歌单创建、重命名、删除、加歌、移除
- `Media3` 播放队列与后台播放服务
- 简单均衡器预设
- 内嵌封面读取
- 同名 `.lrc` 歌词预览与完整歌词面板
- 睡眠定时

## 技术栈

- `Kotlin`
- `Jetpack Compose`
- `Room`
- `Media3 / ExoPlayer`
- `SAF`

## 目录结构

```text
app/src/main/java/com/example/musicplayer
├─ data
│  ├─ db           # Room 实体与 DAO
│  ├─ repository   # 曲库数据协调
│  └─ scanner      # SAF 扫描与元数据提取
├─ playback        # 播放控制、MediaSession、均衡器
├─ ui              # Compose 界面与 ViewModel
├─ util            # 封面、歌词、时间格式化等工具
└─ widget          # 主屏播放小组件
```

## 本机构建

项目根目录：

`D:\Python\musicplayer`

当前机器已经带有本地 Android 相关目录，但这些内容不会进入 Git：

- `android-sdk/`
- `.gradle/`
- `.android-home/`
- `.android-user-home/`
- `.keystore/`
- `local.properties`
- `keystore.properties`

常用构建命令：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

## 当前重点

近期已经完成或正在推进：

- 项目独立 Git 化
- 作者信息统一
- README 乱码清理
- 扫描进度提示增强

## 下一步建议

1. 做更细的扫描进度与增量扫描策略。
2. 持久化播放队列与上次播放位置。
3. 完善专辑、艺术家详情视图。
4. 增强歌词体验，比如时间轴滚动。
5. 增加正式发布说明和签名流程文档。
