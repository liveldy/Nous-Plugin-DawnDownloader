# 全能下载器 · Nous 插件 1.1.1

在 Nous 内完成网盘分享解析、网盘文件管理、视频/图文解析和下载任务管理。插件直接复用 [YunX](https://github.com/CYQawa/YunX) 与 [YunX-Desktop](https://github.com/tidain/YunX-Desktop) 的业务层，并提供适配 Nous 的界面。

安装和更新仅通过 Nous 插件管理。运行模块由宿主自动校验、加载；没有额外应用、系统安装器、快捷方式或原版 YunX 窗口。插件版本独立维护，上游版本只记录在来源与许可证文件中。

| 页面 | 操作 |
| --- | --- |
| 解析 | 网盘/GitHub 分享、提取码、目录浏览、转存、选择下载、视频清晰度与图文批量下载 |
| 网盘 | 夸克、UC、迅雷、百度、移动云盘、123、115、光鸭、蓝奏优享、蓝奏；账户切换和备注、文件夹、重命名、移动、分享、删除 |
| 下载 | 真实进度和速度、暂停、恢复、重试、打开文件、删除记录，可选删除本地文件 |
| 设置 | 保存目录、线程数、并行任务、重试、限速、免转存、多账户与多源分片、HTTP/2、视频平台凭证 |

功能取决于平台接口、登录状态和上游支持范围。账户支持导入凭证；相应平台另支持账号密码登录。此版本没有照搬原程序的桌面壳、更新器或外观设置。

## 平台与权限

需要 Nous 1.0.1 / Plugin Interface 1.1。支持 Windows x64、Android ARM64（Android 11+）；网页不运行原生插件。Windows 模块包含自己的 JVM，Android 模块是宿主加载的 Dex/资源，不需要系统安装应用。

UI 在隔离 iframe 中，通过标准 SDK 请求插件自己的运行模块。本地模块属于可信代码，拥有 Nous 的设备权限；安装时需要显式授权。业务数据按 Nous 账户分开保存，网络跟随全局代理。退出 Nous 时桌面插件进程结束；正在下载的记录可在下次启动继续。卸载移除入口，保留本机下载文件和业务数据。

## 独立构建

Node.js 22+、Python 3.11+、JDK17+。Windows 构建输入是来源锁中精确校验的上游发行 JAR/JVM；完整对应源码包含在源代码发行包；Git 仓库通过 prepare-vendor.py 恢复 vendor，再应用可审核的 vendor-overrides。原版便携包只是可复现的构建输入，不作为本插件发行物。

```powershell
python prepare-vendor.py
python prepare-vendor.py
python fetch-desktop-input.py
$env:YUNX_BUILD_INPUT = (Resolve-Path build-input).Path
$env:JAVA_HOME = "你的 JDK 目录"
python build-desktop-module.py
$env:ANDROID_TOOLCHAIN = "含 gradle-8.13 与 sdk 子目录的工具链目录"
python build-android-module.py
python update-manifest.py
node sdk/pack.mjs .
node --test test/*.test.mjs
```

Android 工具链使用 SDK36/BuildTools36，Gradle8.13，依赖和插件版本保留在 vendor/android 工程中。构建产生的 APK 仅是中间 Dex/资源容器，最终发行的是 .module.zip。不得把它当独立程序安装。

发行物仅为页面插件包、两个平台的模块 ZIP、完整对应源码和校验清单。每次改模块后先重新构建并更新摘要，再打包页面。没有任何构建步骤需要 Nous 私有源码。插件自己的业务协议见 engine/README.md；通用协议见 docs/plugin-development.md。

## 来源、许可与验证

插件和业务移植采用 AGPL-3.0，保留原作者版权与 LICENSE；SDK 为 MIT；IBM Plex Sans 为 OFL。完整修改源码、实际使用的上游源码和来源锁在本仓库及每次 Release 提供，第三方二进制的原许可证保留在 JAR/META-INF 和 runtime/legal 中。

已验证 Windows 真正运行模块、十种网盘模块初始化、实际文件下载/暂停/重启恢复/SHA256/删除、不同账号隔离、设置保存、浏览器交互和三个屏幕宽度。Android 模块与宿主适配器通过构建和 lint；Android 实机行为及需要真实网盘账号的端到端登录/文件操作仍需在相应设备和账号验证。

1.1.1 修复设置页的旧响应覆盖显示，以及目录选择与其他设置采用同一次保存。运行模块复用已校验的 1.1.0 组件。
