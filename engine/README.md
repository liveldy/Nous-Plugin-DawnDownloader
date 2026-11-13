# Downloader business protocol

The host routes opaque runtime.invoke {method,params} calls; it contains no downloader-specific routing or code. EngineCore exposes bootstrap, settings.get/set/directory, vm.snapshot/call, accounts.list/action, downloads.list/add/action/open, cloud.share, video.download/accounts/account.action, history.list and cache.info.

vm.call uses a declared action allowlist for the upstream resolve, video, cloud:<provider> and account:<provider> state machines. State snapshots expose meaningful fields and opaque file references, never serialized credential strings. File references expire with the plugin process. Download rows are read from the upstream task database and statistics flow. Pause/resume/removal call the actual upstream DownloadManager.

Windows PluginMain is only a host-managed UTF-8 stdio worker. AndroidPlugin is only a Dex entry with initialize/execute/close. Both use the same EngineCore adapter, platform data paths and existing upstream repositories. No original application entry or interface runs. The adapter's desktop AppContext replacement prevents accessing the user's original YunX configuration. Android scopes Room instances by database path and preferences by host account.

The Android DownloadService facade forwards notification text to the generic host service. HeadlessWebView receives the current host activity for existing JS challenge parsing; this remains plugin-owned code. HLS, DASH/motion composition and file transfer algorithms remain in the upstream modules. Upstream UI sources remain as corresponding source and compile-time dependencies, but their activities and applications are not registered in the plugin resource container.


1.1.3 uses the upstream ShareFile schema isdir/fsize directly. Quark sharing on Android and cloud listing stop on empty/repeated pages instead of assuming a requested page size. The desktop adapter compiles the documented BaiduApi Kotlin source patch before original dependencies, including per-cookie token cache and complete directory paging. API public signatures remain compatible.

Before resolving a Douyin URL the desktop platform lazily initializes the bundled upstream JCEF and uses its original HeadlessWebView extraction logic. Other video parser implementations are retained. JCEF resources unpack within the plugin profile and do not launch an independent application. Login readiness is a generic host capability: the plugin supplies platform URL/schema/UA, revalidates before save and only declares completion after actual persisted account change.
