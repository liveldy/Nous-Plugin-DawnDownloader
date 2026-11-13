package org.nous.downloader

import android.content.Context
import androidx.compose.runtime.MutableState
import com.yunx.app.data.network.video.*
import com.yunx.app.ui.viewmodel.ResolveUiState
import com.yunx.app.util.AppStrings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import android.os.Looper
import org.robolectric.Shadows

/** Executes the real Android adapter and Android view-models, not desktop stand-ins. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],manifest=Config.NONE)
class AndroidEngineContractTest {
 @Test fun allPagesVideoStateAndDownloadContract() {
  val context=ProfileContext(RuntimeEnvironment.getApplication(),"adapter-contract")
  AppStrings.install(context)
  com.yunx.app.data.network.XunleiDeviceFingerprint.init(context)
  com.yunx.app.data.network.Pan123DeviceId.install(context)
  com.yunx.app.util.DiagnosticLog.install(context)
  com.yunx.app.data.download.DownloadDebugLog.install(context)
  val core=EngineCore(object:EngineCore.Platform {
   override fun context():Context=context
   override fun root():File=context.root
   override fun openFile(path:String):Any?=null
   override fun chooseDirectory():Any?=null
  })
  try {
   val boot=core.execute("bootstrap",JSONObject()) as JSONObject
   assertTrue(core.execute("settings.set",boot.getJSONObject("settings")) is JSONObject)
   val providers=boot.getJSONArray("providers")
   val names=mutableListOf("resolve","video")
   for(i in 0 until providers.length()){val id=providers.getJSONObject(i).getString("id");names.add("account:$id");names.add("cloud:$id")}
   for(name in names)assertTrue(name,core.execute("vm.snapshot",JSONObject().put("model",name)) is JSONObject)
   val field=EngineCore::class.java.getDeclaredField("models").apply{isAccessible=true}
   val models=field.get(core) as Map<*,*>
   assertNotSame("share and video must keep independent state",models["resolve"],models["video"])
   val video=models["video"]!!
   assertEquals("com.yunx.app.ui.viewmodel.ResolveViewModel",video.javaClass.name)
   val requests=java.util.concurrent.atomic.AtomicInteger()
   val client=OkHttpClient.Builder().addInterceptor { chain ->
    requests.incrementAndGet()
    val body=if(chain.request().url.encodedPath.endsWith("/view")) """{"code":0,"data":{"bvid":"BV1xx411c7mD","cid":123,"title":"Android parse fixture","owner":{"name":"Fixture"}}}"""
     else """{"code":0,"data":{"quality":80,"durl":[{"url":"https://example.invalid/video.mp4","size":1234}]}}"""
    Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(body.toResponseBody("application/json".toMediaType())).build()
   }.build()
   com.yunx.app.data.network.HttpClients::class.java.getDeclaredField("apiCache").apply{isAccessible=true}.set(null,client)
   fun awaitVideo():JSONObject {
    repeat(100){Shadows.shadowOf(Looper.getMainLooper()).idle();val value=core.execute("vm.snapshot",JSONObject().put("model","video")) as JSONObject;if(value.optJSONObject("uiState")?.optString("type")=="Success")return value;Thread.sleep(20)}
    throw AssertionError("Android video parsing did not reach Success")
   }
   core.execute("vm.call",JSONObject().put("model","video").put("action","resolve").put("args",JSONArray().put("https://www.bilibili.com/video/BV1xx411c7mD")))
   assertEquals("Android parse fixture",awaitVideo().getJSONObject("uiState").getJSONObject("result").getString("title"))
   assertEquals(2,requests.get())
   core.execute("vm.call",JSONObject().put("model","video").put("action","reset"))
   core.execute("vm.call",JSONObject().put("model","video").put("action","retry"))
   awaitVideo();assertEquals("retry reuses the submitted input",4,requests.get())
   val delegate=video.javaClass.getDeclaredField("uiState\$delegate").apply{isAccessible=true}.get(video) as MutableState<ResolveUiState>
   val media=VideoMedia("https://example.invalid/video.mp4",VideoMediaKind.VIDEO,fileName="contract.mp4",headers=mapOf("Referer" to "https://example.invalid/"),audioUrl="https://example.invalid/audio.m4a",audioHeaders=mapOf("Referer" to "https://example.invalid/"),motionUrl="https://example.invalid/motion.mp4")
   delegate.value=ResolveUiState.Video(VideoParseResult(VideoPlatform.BILIBILI,"Fixture",url="https://www.bilibili.com/video/BV1fixture",medias=listOf(media)))
   val snapshot=core.execute("vm.snapshot",JSONObject().put("model","video")) as JSONObject
   val state=snapshot.getJSONObject("uiState");assertEquals("Success",state.getString("type"))
   val result=state.getJSONObject("result");val item=result.getJSONArray("medias").getJSONObject(0)
   val queued=core.execute("video.download",JSONObject().put("result",result.getString("_ref")).put("media",item.getString("_ref"))) as JSONObject
   assertTrue("real Android suspend enqueue returns a task ID",queued.getLong("id")>0)
   val tasks=core.execute("downloads.list",JSONObject()) as JSONArray
   val task=(0 until tasks.length()).map{tasks.getJSONObject(it)}.first{it.getLong("id")==queued.getLong("id")}
   assertEquals("contract.mp4",task.getString("fileName"));assertEquals(media.audioUrl,task.getString("mergeAudioUrl"));assertEquals(media.motionUrl,task.getString("motionUrl"))
   core.execute("vm.call",JSONObject().put("model","video").put("action","reset"))
   assertEquals("Idle",(core.execute("vm.snapshot",JSONObject().put("model","video")) as JSONObject).getJSONObject("uiState").getString("type"))
  } finally {core.close()}
 }
}
