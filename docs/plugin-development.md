# Nous Plugin Interface 1.0

插件是一份独立发布、独立版本、声明平台和权限的页面包。Nous 只承担安装、隔离、能力调用与导航，不导入插件的业务代码。插件不能依赖 React 内部组件、应用数据库、后端路由或模型密钥。

## 工程与发布

每个插件使用自己的 GitHub 仓库及 Releases。完整示例与可复用 SDK：[Nous-Plugin-YunX](https://github.com/liveldy/Nous-Plugin-YunX)。SDK 的 `protocol.mjs`、`pack.mjs`、`nous-plugin.js` 使用 MIT 许可；示例插件及其上游使用 AGPL-3.0。开发其他插件时按其依赖的实际许可证发布。

```text
manifest.json
ui/index.html
ui/app.js
ui/style.css
ui/nous-plugin.js
sdk/pack.mjs
sdk/protocol.mjs
LICENSE
README.md
```

Node.js 22+，无需安装 Nous，也无需导入宿主源码：

```sh
node sdk/pack.mjs .
```

独立预览（本机 Node HTTP 服务，默认端口 8088，可用 PORT 环境变量修改）：

```sh
node sdk/dev.mjs .
```

打开 http://127.0.0.1:8088，可切换平台与主题，修改资源后刷新。开发宿主明确标注模拟状态，仅模拟内存存储及“尚未安装”，不会执行原生安装、启动、剪贴板或外部网络操作；这些能力在真实客户端中完成集成验证。预览工具与宿主应用代码没有依赖。

产物为 `dist/<id>-<version>.nous-plugin.json`，包含 UTF-8 文件映射和清单，不包含原生安装程序。将该文件上传到插件自己的 GitHub Release。原生依赖的安装包及其对应源码同样放在该插件 Release，不放在 Nous 服务器。SDK 打包器拒绝越界路径和符号链接。

## 清单

```json
{
  "id": "org.example.notes",
  "name": "便签",
  "version": "1.0.0",
  "api": "1.0",
  "description": "当前设备的独立便签。",
  "platforms": ["web", "windows-x64", "android-arm64"],
  "permissions": ["storage"],
  "entry": "ui/index.html",
  "repository": "example/notes",
  "license": "MIT",
  "resources": ["ui/index.html", "ui/style.css", "ui/app.js", "ui/nous-plugin.js"]
}
```

`resources` 是打包输入，产物中替换为 `files`。`id` 必须带命名空间，仅小写字母、数字、点与连字符，最长 100 字符。`name` 最长 60 字符。`version` 使用语义版本；正式目录使用 `x.y.z`。`api` 当前必须为 `1.0`。平台必须来自 `web`、`windows-x64`、`android-arm64`；不支持的平台不能安装。图标可选，使用小于 64KB 的 base64 PNG、WebP 或 SVG data URL。页面总资源最大 4MB、256 个文件，路径相对包根目录，不允许 `..`、绝对路径或反斜线。

页面以 HTML/CSS/普通 JS 开发。所有脚本和样式随包提供；入口的相对 `<script src>` 与 `<link href>` 由宿主内联，禁止 CDN、动态 import、远程图片和直接 fetch。将 SDK 作为第一个脚本引入。复杂框架可先构建为普通脚本，再列入 `resources`。

## 生命周期与协议

安装时校验签名目录给出的 SHA-256、平台与协议，展示全部权限并要求用户确认。安装记录和私有存储按当前账户保存在当前设备。升级保留私有存储；新增权限重新确认。停用立即拒绝后续能力调用并移除导航入口；卸载删除页面及私有配置。独立原生程序及其下载文件由原程序管理，不自动删除。

宿主使用 `sandbox="allow-scripts"` 的不透明来源 iframe。页面不能访问父页面 DOM、Cookie、LocalStorage、宿主 JS 或其他插件。初始化通过 `postMessage` 传递上下文：

正式宿主的固定 HTTP 装载页先完成 `frame.ready → render → rendered → init` 握手，再初始化 SDK。文档内容仍来自当前设备，服务器只返回通用装载器。HTTP 装载页及包内文档分别限制 CSP，避免 srcdoc 继承宿主严格脚本策略导致插件不执行；图片和字体只允许 data URL，网络与嵌套框架被禁止。每次装载都使用新 nonce。

```js
const context = await NousPlugin.ready;
// {pluginId, platform, hostVersion, api, locale, theme, capabilities}
const saved = await NousPlugin.request('storage.get', {key: 'draft'});
await NousPlugin.request('storage.set', {key: 'draft', value: '你好'});
const unsubscribe = NousPlugin.on('theme.changed', theme => {
  // SDK 同步 document.documentElement.dataset.theme
});
```

底层消息均带 `protocol: "nous.plugin/1"` 与每个页面实例的随机 `nonce`。宿主只接受当前 iframe 的消息且检查实例、启用状态与权限。请求 `{kind:"request", id, method, params}`；响应 `{kind:"result",id,value}` 或 `{kind:"result",id,error:{code,message}}`。初始化为 `kind:"init"`，事件为 `kind:"event",event,value`。普通请求 150 秒超时，安装 15 分钟超时。卸载、账户切换、路由离开销毁页面，插件应自行清理定时器和草稿。重要草稿在编辑后及时保存。

| 方法 | 权限 | 输入及结果 |
| --- | --- | --- |
| `host.info` | 无 | 当前平台、宿主版本、API、语言、主题 |
| `storage.get` | `storage` | `{key}` → JSON 值或 null |
| `storage.set` | `storage` | `{key,value}` → `{ok:true}` |
| `storage.remove` | `storage` | `{key}` → `{ok:true}` |
| `clipboard.write` | `clipboard.write` | `{text}`，最多 30000 字符 |
| `external.open` | `external.open` | `{url}`，仅 HTTPS，由宿主外部浏览器打开 |
| `native.status` | `native.application` | 安装状态、声明版本、可用连接和当前下载字节；Android 不查询其他应用的进程 |
| `native.install` | `native.application` | 无输入；下载声明的安装包，SHA-256 校验后打开系统安装界面 |
| `native.open` | `native.application` | 打开声明的独立程序 |
| `native.share` | `native.application` | `{text}`，Android ACTION_SEND；Windows 还需 `clipboard.write`，复制后打开程序 |
| `bridge.request` | `native.application` + `native.bridge` | Windows `{route,body}`，仅调用清单允许的本机接口 |

存储键为 1–80 个字母、数字、点、下划线或连字符，保留原型键被拒绝；每个插件每账户最多 64KB。插件本身不提供任意文件读取、任意进程执行、任意网络访问能力。下载程序在用户确认安装后作为独立原生应用运行，拥有其自身操作系统权限，这与 iframe 的限制是不同的边界，安装权限说明必须真实。

## 原生程序声明

```json
{
 "native": {
  "windows-x64": {
   "directory": "ExampleApp", "executable": "ExampleApp.exe", "version": "1.2.0",
   "asset": {"url":"https://github.com/example/notes/releases/download/v1.0.0/Example-Setup.exe","sha256":"64位小写十六进制值","bytes":123456},
   "bridge": {"ports":[38100],"app":"example","tokenHeader":"X-Example-Token","routes":[{"name":"submit","method":"POST","path":"/submit"}]}
  },
  "android-arm64": {
   "packageName":"org.example.app", "version":"1.2.0",
   "asset":{"url":"https://github.com/example/notes/releases/download/v1.0.0/Example.apk","sha256":"64位小写十六进制值","bytes":123456}
  }
 }
}
```

所有原生资源必须来自 `repository` 声明仓库的 GitHub Releases，最大 1GB，大小与哈希必须准确。Windows 只定位 `%LOCALAPPDATA%/Programs` 和 Program Files 下声明的单层目录及 exe；不接受任意路径、参数或命令。Android 校验 APK 的包名，系统继续校验签名和安装授权。

本机桥只允许 127.0.0.1、1024–65535 范围内最多 16 个声明端口。`GET /ping` 返回 `{app,token}`，`app` 必须匹配声明。宿主保管 token，仅将允许的 POST 路由请求结果返给插件；不将 token、任意 HTTP 接口或文件系统交给 iframe。请求最大 100KB、响应最大 1MB。原生应用应自行限制本机接口来源和请求内容。不同平台可使用不同原生程序；业务适配逻辑全部留在插件中。

## 调试、检查与提交目录

先用浏览器本地页面检查独立 UI，再用 SDK 的 `pluginDocument` 加载到同样的隔离 iframe；本地开发可在测试宿主显式模拟能力，不能把模拟下载进度带入正式页面。正式安装仅接受签名目录中的包，不提供跳过签名的生产入口。全能下载器仓库包含独立打包、协议和沙箱测试，可直接运行 `npm test`、`npm run build`。

提交插件时提供：独立仓库、许可、版本、支持平台、权限用途、包 URL/SHA-256/字节数、原生资源来源与哈希、完整对应源码。维护者审核后将元数据加入 `plugins.json`，用发布私钥签名并更新公共目录；客户端内置公钥验证签名。安装包哈希位于签名目录，原生资源哈希位于已验证插件包。私钥、GitHub token、宿主应用源码不进入插件仓库。

兼容策略：API 1.x 的新增方法必须可检测、可拒绝，已有参数与含义不破坏；不兼容升级提高 API 主版本，旧宿主拒绝安装。插件版本独立于 Nous 版本。主题通过 `data-theme=light|dark` 实现，移动端触摸目标至少 44px，错误显示在对应操作旁，禁止伪造下载成功或运行状态。
