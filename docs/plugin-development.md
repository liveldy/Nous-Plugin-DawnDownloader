# Nous Plugin Interface 1.1

插件拥有独立仓库、代码、版本、页面和 Release。Nous 只实现通用安装、导航、权限和运行模块适配器，四个核心模块不导入插件业务代码。全能下载器是一个插件；其模块随插件自动校验和加载，不是额外安装的应用。

## 工程与发布

```text
manifest.json
ui/index.html
ui/style.css
ui/app.js
ui/nous-plugin.js
sdk/protocol.mjs
sdk/pack.mjs
sdk/dev.mjs
engine/                 # 可选：插件自己的原生模块源码
vendor/                 # 实际复用的上游源码及许可证
LICENSE
README.md
```

SDK 使用 MIT 许可。插件根据实际依赖的许可证独立发布；全能下载器使用 AGPL-3.0，完整修改及对应上游源代码在其独立仓库和 Release 提供。不得将私有签名密钥、账号、令牌或宿主源码打进插件。

```sh
node sdk/pack.mjs .
node sdk/dev.mjs .
```

打包器生成 `dist/<id>-<version>.nous-plugin.json`。开发预览是明确标记的模拟宿主，不执行原生模块；模块必须在客户端进行集成测试。需要执行本地业务时，另行提供清单中声明的 `.module.zip`，与页面包一起上传到插件自己的 GitHub Release。它是插件资源，不能启动系统安装器、创建桌面快捷方式或要求用户额外安装一个程序。

发布流程：构建模块 → 计算模块 SHA256 与字节数 → 填入 manifest → 打包页面 → 计算页面包 SHA256 → 上传独立 Release → 更新宿主签名插件目录。模块地址必须指向 manifest.repository 对应的 GitHub Release。应用服务器不保存插件或客户端二进制。

## 清单

```json
{
  "id": "org.example.files",
  "name": "文件工具",
  "version": "1.1.0",
  "api": "1.1",
  "requiresHost": "1.0.1",
  "description": "在当前设备处理文件。",
  "repository": "example/files-plugin",
  "license": "MIT",
  "platforms": ["windows-x64", "android-arm64"],
  "permissions": ["storage", "native.runtime"],
  "entry": "ui/index.html",
  "navigation": [
    {"id":"files", "label":"文件", "icon":"home"},
    {"id":"settings", "label":"设置", "icon":"settings"}
  ],
  "defaultView": "files",
  "resources": ["ui/index.html","ui/style.css","ui/app.js","ui/nous-plugin.js"],
  "runtime": {
    "windows-x64": {
      "type":"jvm", "entry":"org.example.files.PluginMain",
      "asset":{"url":"https://github.com/example/files-plugin/releases/download/v1.1.0/windows.module.zip","sha256":"实际的64位小写十六进制摘要","bytes":1234}
    },
    "android-arm64": {
      "type":"android-dex", "entry":"org.example.files.AndroidPlugin", "minSdk":30,
      "asset":{"url":"https://github.com/example/files-plugin/releases/download/v1.1.0/android.module.zip","sha256":"实际的64位小写十六进制摘要","bytes":1234}
    }
  }
}
```

不需要原生执行能力的插件省略 runtime 与 native.runtime 权限，可支持 web。平台声明不代表支持所有系统版本；Android Dex 模块目前需要 Android 11 / API 30。网页不加载任何原生模块。

id 带命名空间，仅小写字母、数字、点与连字符，最长100字符；名称最长60字符；version 使用语义版本。1.1 宿主兼容原有1.0页面插件，新模块插件声明1.1，旧宿主会明确拒绝。页面资源最多4MB、256文件，禁止绝对路径、反斜线、越界和符号链接。图标是小于64KB的 base64 PNG/WebP/SVG。

navigation 是1至12个二级菜单，不重复最左侧核心模块栏。每项 id 唯一，使用小写字母数字与连字符；label 最长24字符。defaultView 必须存在。宿主负责链接、选中状态和移动端菜单折叠；插件通过 context.view 和 navigation.changed 切换业务视图。插件管理的二级菜单只显示插件管理。

## 页面握手与消息

页面使用 `sandbox="allow-scripts allow-forms"` 的不透明来源 iframe。宿主的通用装载页完成 `frame.ready → render → rendered → init`，每次装载生成新 nonce。校验 event.source、协议、nonce、插件启用状态和当前账号，不接受其他窗口的请求。

```js
const context = await NousPlugin.ready;
// pluginId, platform, hostVersion, api, theme, locale, capabilities, view, navigation
NousPlugin.on('navigation.changed', view => render(view));
NousPlugin.on('theme.changed', theme => applyTheme(theme));
await NousPlugin.request('navigation.go', {view:'settings'});
```

请求为 `{protocol:'nous.plugin/1',kind:'request',nonce,id,method,params}`；结果为同一协议、nonce、id、`kind:'result'`，以及 value 或 error:{code,message}。SDK 管理请求编号、超时和 Promise。事件使用 kind:'event'、event、value。

allow-forms 只让插件表单触发本地验证和 submit 事件；CSP 的 form-action none 仍禁止网络表单提交，也不授予同源权限。页面禁止直接网络、动态脚本、CDN、远程图片及嵌套框架。脚本、样式、字体和图像需随页面包提供。宿主不将模型密钥、会话或内部数据库传进页面。

## 权限与方法

| 权限 | 方法 | 参数与结果 |
| --- | --- | --- |
| 无 | host.info | 当前平台、宿主版本、协议和主题 |
| 无 | navigation.go | {view}，仅允许已声明菜单 |
| storage | storage.get/set/remove | {key,value?}，当前账号和插件私有；64KB上限 |
| clipboard.write | clipboard.write | {text}，最多30000字符 |
| external.open | external.open | {url}，仅 HTTPS |
| native.runtime | runtime.status | {installed,ready,running} |
| native.runtime | runtime.start | 校验并启动当前平台模块 |
| native.runtime | runtime.invoke | {method,params}，调用插件自己的业务方法 |
| native.runtime | runtime.stop | 停止当前账号的当前插件任务 |

**native.runtime 是可信本地代码权限**，拥有与 Nous 相同的设备权限，可访问文件与网络。iframe 隔离不构成本地代码的安全沙箱。安装窗口必须明确说明这一点并让用户选择授权；只安装信任来源的插件。原有 native.application/native.bridge 仅为1.0兼容保留，全能下载器1.1不申请也不调用它们。

安装时校验签名目录、页面摘要、平台、权限，以及模块的摘要和大小。模块只在本机插件目录解压，拒绝越界条目和符号链接。先完成模块校验，再保存安装记录。更新保留账号私有数据；新增权限重新授权。停用和卸载先停止模块，后续调用立即拒绝。卸载移除入口和页面存储，保留本机业务数据与下载文件，供重新安装恢复。

## Windows JVM 模块

```text
app/Nous-Engine.jar
app/*.jar
runtime/bin/java.exe
runtime/...             # 模块自己的完整 JVM
```

宿主在插件目录启动无窗口子进程。命令由适配器固定，不接受插件页面提供的命令或任意可执行路径。入口来自已校验 manifest；插件主 JAR 位于类路径首位。系统不安装 JVM，不注册独立程序，不创建快捷方式。

宿主设置 `nous.plugin.root` 为当前账号的插件数据目录，`nous.engine.home` 为已校验模块目录。入口 main 从 UTF-8 stdin 每行读取 `{method,params}`，stdout 每行返回 `{value}` 或 `{error:"错误信息"}`。所有日志写 stderr，禁止在 stdout 输出诊断文字。一个进程对应一个账号和插件，串行处理请求；停用、升级或退出 Nous 时结束进程。插件需持久化可恢复任务，并在重新启动时恢复为可安全继续的状态。

## Android Dex 模块

外层 `.module.zip` 含 `module.apk`（仅作为 Dex/资源容器，不经过 PackageManager 安装）和可选 `lib/arm64-v8a/*.so`。清单无 Activity、Service 或独立 Application 入口。使用非宿主资源包 ID `0x80` 编译资源，避免与宿主 `0x7f` 冲突。

宿主以 DexClassLoader 加载模块，以 API30 ResourcesLoader 加载其资源，解压代码设为只读。入口为公开的无参类：

```java
public void initialize(android.content.Context context,
  java.util.function.BiFunction<String, org.json.JSONObject, Object> host);
public Object execute(String method, org.json.JSONObject params) throws Exception;
public void close() throws Exception;
```

context 提供模块资源、类加载器和当前账号的私有目录/偏好；不能假设宿主内部组件、包名或路由。execute 在工作线程运行，返回 JSON 可序列化的类型。插件自己保存文件、凭证和业务状态；close 必须暂停任务、关闭数据库并取消协程。Android 已加载类的释放由系统管理，停用保证停止业务执行，不承诺立即从进程内存卸载 Dex。

通用 host 回调包括 activity.current（用于原生模块的 Activity 生命周期接入）、directory.choose（系统目录选择器，返回持久授权的 URI 或 null）、file.open（打开插件文件/已授权 content URI）和 foreground.update（{text}，更新 Nous 的通用插件前台通知）。宿主只注册通用文件提供器和插件执行服务，业务服务不进入宿主源码。不要启动上游 Application 或把上游程序当成另一款应用安装。

## 验证与兼容

测试平台与权限拒绝、资源越界、原生模块校验、不同账号的数据隔离、导航回退、升级保留数据、停用后拒绝调用、卸载、断网/超时/下载失败、实际任务的暂停与恢复。真实界面检查1440、768、390宽度和深色主题。开发预览不能替代原生设备验证；发布说明需区分已构建/已测试/未实机验证的内容。

后续扩展增加通用 capability 与适配器，不把某个插件的 URL、模型、网盘名称或业务方法写入 Nous。插件业务协议在插件仓库维护，宿主只转发 opaque method/params。
