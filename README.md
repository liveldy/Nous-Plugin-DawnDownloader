# 全能下载器 · Nous 插件

独立的 Nous Plugin Interface 1.0 插件。Windows 直接使用 [YunX-Desktop](https://github.com/tidain/YunX-Desktop)，Android 直接使用 [YunX](https://github.com/CYQawa/YunX)。支持平台为 Windows x64 与 Android ARM64，网页端不支持安装。

在 Nous 插件管理中安装插件并确认权限，打开插件页面，按提示安装对应平台的原版云析。粘贴网盘、视频链接或带提取码的分享内容后选择“交给 YunX”。Android 使用系统分享；Windows 复制内容并打开云析，文件直链可通过原版 loopback 桥提交。解析、下载、暂停、重试、保存目录在原版 YunX 中继续；插件不伪造这些进度。

插件 UI、业务适配及 manifest 全部位于本仓库。Nous 本体不导入本仓库代码，也不包含 YunX 专用接口。插件通过通用受限 SDK 调用宿主的安装、启动、分享和本机桥能力。

## 构建与检查

Node.js 22+，无第三方 npm 依赖：

```sh
npm test
npm run build
```

输出 `dist/app.nous.universal-downloader-1.0.0.nous-plugin.json`。`manifest.json` 与 `upstream.lock.json` 固定上游版本、二进制 SHA-256、大小和源码归档 SHA-256。安装资源均来自本仓库 Releases，未改动上游安装程序。仅提供 Windows 安装版。

## 许可与对应源码

本插件 AGPL-3.0-only，见 LICENSE。SDK 文件另以 MIT 许可提供，见 sdk/LICENSE。上游两个项目使用 AGPL-3.0，版权归各自作者。本仓库 v1.0.0 Release 同时提供原版 APK、Windows 安装器及两份**对应版本的完整上游源码归档**，不只提供链接。适配器自身完整源代码在本仓库及 Release 的 source 归档中。

上游构建方式保留在对应源码归档的 README 和 Gradle 工程内；版本分别为 Android v1.3.1 与 Windows v1.3.3。没有对上游程序做二进制修改。开发文档见 docs/plugin-development.md。下载内容的使用和支持范围以 YunX 自身功能为准。
