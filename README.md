# MusicPlayer

一个纯本地、纯离线的 Android 音乐播放器工程，目标是做出：

- `Winamp` 风格的紧凑播放界面
- `foobar2000` 风格的目录化、本地化曲库逻辑
- 基于 `SAF + Room + Media3` 的可维护架构

## 当前已完成

- Android 工程骨架
- `SAF` 目录选择入口
- 持久化目录授权后的递归扫描
- 本地数据库索引：歌曲、艺术家、专辑、文件夹、收藏、最近播放
- `Media3` 播放队列
- 后台 `MediaSessionService`
- 简单均衡器预设：`关闭 / 流行 / 古典`
- 内嵌封面读取
- 同名 `.lrc` 歌词预览
- 第一版 Winamp 气质界面

## 技术栈

- `Kotlin`
- `Jetpack Compose`
- `Room`
- `Media3 / ExoPlayer`
- `SAF (ACTION_OPEN_DOCUMENT_TREE)`

## 目录结构

```text
app/src/main/java/com/example/musicplayer
├─ data
│  ├─ db            # Room 实体与 DAO
│  ├─ repository    # 曲库数据协调
│  └─ scanner       # SAF 递归扫描与元数据提取
├─ playback         # Media3 播放与均衡器
├─ ui
│  ├─ model         # UI 状态模型
│  └─ theme         # 主题与配色
└─ util             # 时间格式、歌词读取
```

## 运行前提

本机当前目录里已经保留了完整源码，但这台环境里还没有可用的：

- `Java 17`
- `Android SDK`
- `Gradle Wrapper`

因此我这次先把源码和结构搭好了，还没在这里实际编译 APK。

你后续在 Android Studio 中打开 `D:\Python\musicplayer` 后，建议按下面顺序补齐环境：

1. 安装 `Android Studio`
2. 安装 `JDK 17`
3. 安装 `Android SDK 34`
4. 在 Android Studio 中同步 Gradle
5. 生成或补齐 `Gradle Wrapper`
6. 连接手机调试并安装

## 下一步建议

优先把下面这些补完，项目就会更接近可长期使用的版本：

1. 扫描进度与增量扫描
2. 专辑详情页、艺术家详情页的更细浏览
3. 播放队列持久化
4. 自定义歌单
5. 完整歌词页
6. 更贴近经典 Winamp 的视觉皮肤系统
