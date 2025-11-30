# CatuPlayer - 音乐播放器

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2021+-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](https://opensource.org/licenses/Apache-2.0)

</div>

## 🎵 项目简介

CatuPlayer 是一款现代化的 Android 音乐播放器应用，采用 Jetpack Compose 构建，提供流畅的用户体验和丰富的音乐播放功能。该应用支持多种音频格式，具备播放列表管理、歌词显示、专辑封面识别等特性。

## ✨ 主要功能

- 🎵 **音乐播放控制** - 播放、暂停、上一首、下一首
- 📁 **播放列表管理** - 添加、删除、管理本地音乐文件
- 📖 **歌词显示** - 实时同步歌词显示与滚动
- 🖼️ **专辑封面** - 自动识别和显示专辑封面
- 🔍 **搜索功能** - 搜索歌曲和艺术家（开发中）
- 👤 **个人中心** - 用户信息管理（开发中）
- 🧩 **插件系统** - 支持扩展功能的插件架构
- 📱 **现代UI** - 基于 Material Design 3 的美观界面
- 🎨 **主题定制** - 支持深色/浅色主题切换

## 🛠️ 技术栈

- **编程语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构模式**: MVVM (Model-View-ViewModel)
- **状态管理**: Kotlin Flow / State
- **音频处理**: Android MediaPlayer
- **图片加载**: Coil
- **插件系统**: GraalVM Polyglot
- **依赖注入**: 未使用（可扩展）
- **构建工具**: Gradle
- **网络请求**: OkHttp (插件系统中)

## 📦 安装与使用

### 系统要求
- Android 5.0 (API level 21) 或更高版本
- 至少 2GB RAM
- 存储权限用于访问本地音乐文件

### 安装步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/bil-fis/CatuPlayer.git
   cd CatuPlayer
   ```

2. **使用 Android Studio 打开项目**
   - 打开 Android Studio
   - 选择 "Open an existing project"
   - 选择项目文件夹

3. **同步 Gradle 项目**
   - 等待 Gradle 同步完成
   - 检查是否有依赖错误

4. **构建并运行**
   - 点击 "Run" 按钮或使用快捷键
   - 选择目标设备或模拟器
   - 等待应用安装并启动

### 使用指南

1. **添加音乐**
   - 打开播放列表页面
   - 点击右下角的 "+" 按钮
   - 选择要添加的音乐文件

2. **播放音乐**
   - 在播放列表中点击任意歌曲开始播放
   - 使用底部导航栏切换到 "现在播放" 页面
   - 控制播放、暂停、上一首、下一首

3. **查看歌词**
   - 在 "现在播放" 页面中会显示同步歌词
   - 支持 LRC 格式歌词文件

4. **专辑封面**
   - 自动从音乐文件中提取专辑封面
   - 显示在播放页面

## 🚀 特色功能

### 插件系统
CatuPlayer 集成了基于 GraalVM 的插件系统，允许开发者扩展应用功能：

- **安全沙箱**: 插件运行在隔离环境中
- **权限控制**: 严格控制插件可访问的 API
- **动态加载**: 支持运行时加载和卸载插件

### 智能歌词同步
- **LRC 解析**: 支持标准 LRC 格式歌词文件
- **实时同步**: 根据播放进度自动滚动歌词
- **多行显示**: 支持显示多行歌词

### 高效音频处理
- **低延迟**: 优化音频播放延迟
- **内存管理**: 高效的内存使用和资源管理
- **格式支持**: 支持主流音频格式

## 🤝 贡献

我们欢迎各种形式的贡献！请查看以下方式参与项目开发：

1. Fork 项目
2. 创建功能分支
3. 提交你的修改
4. 发起 Pull Request

### 开发指南

- 代码风格遵循 Kotlin 编码规范
- 提交前请运行所有测试
- 为新功能添加相应的测试用例
- 确保代码注释清晰

## 📄 许可证

本项目采用 GPL 3.0 许可证。详细信息请查看 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- 感谢所有为项目做出贡献的开发者
- 感谢 Android 开源项目
- 感谢 Jetpack Compose 团队
- 感谢所有使用和反馈的用户

## 📞 联系方式

如果你有任何问题或建议，请随时联系：

- 通过 GitHub Issues 提交问题

---

<div align="center">

**Made with ❤️ by 林晚晚ss.**

[回到顶部](#catuplayer---音乐播放器)

</div>