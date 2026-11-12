/* Loaded by Nous' generic Dex runtime. This is a plugin entry, never an Android application. */
package org.nous.downloader;
import android.content.Context;import org.json.*;import java.io.File;import java.util.function.BiFunction;
public final class AndroidPlugin {
 private EngineCore core;static BiFunction<String,JSONObject,Object> host;
 public void initialize(Context context,BiFunction<String,JSONObject,Object> callback)throws Exception{
  host=callback;android.app.Activity current=(android.app.Activity)callback.apply("activity.current",new JSONObject());com.yunx.app.data.network.video.HeadlessWebView.INSTANCE.install(current.getApplication());com.yunx.app.data.network.video.HeadlessWebView.INSTANCE.attach(current);ProfileContext profile=new ProfileContext(context,"current");
  com.yunx.app.util.AppStrings.INSTANCE.install(profile);com.yunx.app.data.network.XunleiDeviceFingerprint.INSTANCE.init(profile);com.yunx.app.data.network.Pan123DeviceId.INSTANCE.install(profile);com.yunx.app.util.DiagnosticLog.INSTANCE.install(profile);com.yunx.app.data.download.DownloadDebugLog.INSTANCE.install(profile);
  core=new EngineCore(new EngineCore.Platform(){public Object context(){return profile;}public File root(){return profile.root;}public Object openFile(String path)throws Exception{return callback.apply("file.open",new JSONObject().put("path",path));}public Object chooseDirectory(){return callback.apply("directory.choose",new JSONObject());}});
 }
 public Object execute(String method,JSONObject params)throws Exception{return core.execute(method,params);}
 public void close()throws Exception{core.close();}
 public static void foreground(String text){if(host!=null)try{host.apply("foreground.update",new JSONObject().put("text",text));}catch(Exception ignored){}}
}
