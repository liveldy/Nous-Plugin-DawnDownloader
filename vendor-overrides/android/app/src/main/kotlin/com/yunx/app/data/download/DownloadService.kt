// Nous plugin adaptation. Original YunX download algorithms remain unchanged. AGPL-3.0-or-later.
package com.yunx.app.data.download
import android.content.Context
import org.nous.downloader.AndroidPlugin
class DownloadService { companion object {
 const val MERGE_TEXT = "合并中"
 const val AUDIO_TEXT = "提取中"
 fun notifyResult(context: Context, taskId: Long, fileName: String, success: Boolean, error: String = "", promote: Boolean = true) { AndroidPlugin.foreground(if(success) "下载完成：$fileName" else "下载失败：$fileName") }
 fun start(context: Context, title: String, progress: Int = 0) { AndroidPlugin.foreground(title) }
 fun update(context: Context, title: String, progress: Int, speed: String, showSpeed: Boolean) { AndroidPlugin.foreground(if(showSpeed) "$title · $speed" else title) }
 fun stop(context: Context) { AndroidPlugin.foreground("插件已就绪") }
 fun acquire(context: Context, title: String) { AndroidPlugin.foreground(title) }
 fun release(context: Context) { AndroidPlugin.foreground("插件已就绪") }
} }
