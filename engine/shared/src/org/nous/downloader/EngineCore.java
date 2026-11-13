/* Nous Downloader plugin. AGPL-3.0-or-later. Reuses the upstream YunX business
 * repositories and view-model state machines; no upstream application UI runs. */
package org.nous.downloader;

import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import kotlin.coroutines.*;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.*;

public final class EngineCore {
 public interface Platform { Object context(); File root(); Object openFile(String path)throws Exception; Object chooseDirectory()throws Exception; default void prepareVideo(String text)throws Exception{} }
 private final Platform platform;
 private final Map<Class<?>,Object> objects=new ConcurrentHashMap<>();
 private final Map<String,Object> models=new ConcurrentHashMap<>(), references=new ConcurrentHashMap<>();
 private final Map<Object,String> referenceIds=Collections.synchronizedMap(new IdentityHashMap<>());
 private final Map<String,String> drives=new LinkedHashMap<>();
 private final Map<String,String> labels=new LinkedHashMap<>();
 private final Map<String,Set<String>> allowed=new HashMap<>();
 private final Set<String> properties=new LinkedHashSet<>(Arrays.asList("downloadThreads","maxConcurrentDownloads","downloadDirUri","downloadSpeedLimit","downloadRetryCount","quarkNoSaveDownload","multiAccountDownload","multiSourceDownload","directWriteSingleThread","directWriteMultiThread","http2Enabled","downloadEngine","keepAwakeWhileDownloading","keepDownloadWhenLocked","notificationShowSpeed","githubMirrorPrefix"));
 private volatile long proxyChecked;private String proxySignature="";
 private String lastVideoInput="";
 private boolean android(){return platform.context()!=null;}

 public EngineCore(Platform platform)throws Exception{
  this.platform=platform;
  String[][] providers={{"quark","Quark","夸克网盘"},{"uc","UC","UC 网盘"},{"xunlei","Xunlei","迅雷云盘"},{"baidu","Baidu","百度网盘"},{"c139","C139","中国移动云盘"},{"pan123","Pan123","123 云盘"},{"pan115","Pan115","115 网盘"},{"guangya","GuangYa","光鸭云盘"},{"ilanzou","ILanzou","蓝奏优享"},{"lanzou","Lanzou","蓝奏云"}};
  for(String[] p:providers){drives.put(p[0],p[1]);labels.put(p[0],p[2]);}
  allowed.put("resolve",set("chooseDownloadDir requestSave dismissSave consumeSaveMessage saveToCloud cancelBatch enterMultiSelect toggleSelect toggleSelectAll exitMultiSelect batchSaveToCloud batchDownload consumeDownloadStarted consumeDownloadError consumePrefill startResolve refreshGitHubCurrentNode openGitHubParentRepo openFolder goBack navigateBack backToInput addCurrentToBookmark deleteLinkHistory clearLinkHistory navigateToLevel fetchDownloadLink dismissDownloadDialog startDownload"));
  allowed.put("video",set("resolve selectQuality retry reset toggleSelect selectAll clearSelection"));
  allowed.put("cloud",set("cancelDownload consumeDownloadTriggered openMoveRoot openMoveFolder moveBack moveNavigateToLevel loadRoot openFolder back navigateToLevel enterMultiSelect toggleSelect toggleSelectAll exitMultiSelect openActions dismissActions consumeMessage dismissShareResult downloadFolder downloadFile downloadFileTo startDownload dismissDownloadDialog renameFile createFolder moveFile deleteFile shareFile downloadSelected shareSelected moveSelected deleteSelected refresh"));
  allowed.put("account",set("consumeLoginError beginAddAccount cancelAddAccount saveQuarkAccount saveUCAccount saveBaiduAccount saveC139Account saveCookie saveWebCredential login retryLoginAfterVerify sendSms loginWithSms resetLoginStep logout sendCode smsLogin"));
  platform.root().mkdirs();syncProxy();
 }
 private static Set<String> set(String words){return new HashSet<>(Arrays.asList(words.split(" ")));}
 private Class<?> cls(String name)throws Exception{return Class.forName(name);}
 private Object get(String name)throws Exception{return get(cls(name));}
 private Object get(Class<?> type)throws Exception{
  if(objects.containsKey(type))return objects.get(type);
  if(type.getName().equals("android.content.Context"))return platform.context();
  if(type.getName().equals("kotlinx.coroutines.flow.Flow"))return kotlinx.coroutines.flow.FlowKt.flowOf(true);
  try{Object singleton=type.getField("INSTANCE").get(null);objects.put(type,singleton);return singleton;}catch(NoSuchFieldException ignored){}
  if(type.getSimpleName().equals("AppDatabase")){
   Object companion=type.getField("Companion").get(null);Object result=platform.context()==null?call(companion,"get",new JSONArray()):call(companion,"get",new Object[]{platform.context()});objects.put(type,result);return result;
  }
  if(type.getSimpleName().endsWith("Dao")){Object db=get("com.yunx.app.data.db.AppDatabase");String n=type.getSimpleName();String method=Character.toLowerCase(n.charAt(0))+n.substring(1);if(n.equals("UCAccountDao"))method="ucAccountDao";if(n.equals("GuangYaAccountDao"))method="guangyaAccountDao";if(n.equals("ILanzouAccountDao"))method="ilanzouAccountDao";Object dao=call(db,method,new JSONArray());objects.put(type,dao);return dao;}
  Object value=construct(type,null);objects.put(type,value);return value;
 }
 private Object construct(Class<?> type,String model)throws Exception{
  Constructor<?> constructor=null;int original=0;
  for(Constructor<?> c:type.getConstructors()){
   Class<?>[] ts=c.getParameterTypes();
   if(ts.length>0&&ts[ts.length-1].getName().endsWith("DefaultConstructorMarker")){constructor=c;original=originalCount(ts.length,false);break;}
   if(constructor==null||c.getParameterCount()>constructor.getParameterCount()){constructor=c;original=ts.length;}
  }
  if(constructor==null)throw new IOException("无法构建插件业务模块："+type.getSimpleName());
  Class<?>[] ts=constructor.getParameterTypes();Object[] args=new Object[ts.length];boolean defaults=ts.length>original;
  for(int i=0;i<original;i++){
   Class<?> t=ts[i];Object function=function(type.getSimpleName(),model,i,t);
   if(function!=null){if(!t.isInstance(function))throw new IOException("业务回调类型不兼容："+type.getSimpleName()+" #"+i+" 需要 "+t.getSimpleName());args[i]=function;continue;}
   if(t.getName().startsWith("kotlin.jvm.functions.")){args[i]=null;if(defaults)setMask(args,original,i);else throw new IOException("缺少业务回调："+type.getSimpleName()+" #"+i);}
   else if(t.isPrimitive()||t==String.class){args[i]=zero(t);if(defaults)setMask(args,original,i);}
   else args[i]=get(t);
  }
  if(defaults)for(int i=original;i<args.length-1;i++)if(args[i]==null)args[i]=0;
  try{return constructor.newInstance(args);}catch(InvocationTargetException e){throw problem(e.getCause());}
 }
 private Object function(String type,String model,int index,Class<?> parameter)throws Exception{
  if(!parameter.getName().startsWith("kotlin.jvm.functions."))return null;
  if(type.equals("ChunkDownloader"))return (Function0<Object>)()->uncheckedGet("com.yunx.app.data.network.HttpClients","downloadClient");
  if(type.equals("VideoParseRepository"))return (Function0<Object>)()->uncheckedGet("com.yunx.app.data.network.HttpClients","apiClient");
  if(type.equals("XunleiResolveRepository")&&parameter==Function2.class)return (Function2<String,Object,Object>)(token,continuation)->{try{for(Object account:(Iterable<?>)call(get("com.yunx.app.data.repository.XunleiAccountRepository"),"getAccounts",new JSONArray()))if(Objects.equals(token,call(account,"getAccessToken",new JSONArray())))return account;return null;}catch(Exception e){throw new RuntimeException(e);}};
  if((type.equals("XunleiResolveRepository")||type.equals("XunleiCloudViewModel"))&&parameter==Function1.class)return (Function1<Object,Object>)(continuation)->{try{if(index==4&&type.equals("XunleiResolveRepository"))return refreshXunlei();return accountField("xunlei",index==1?"accessToken":index==2?"deviceId":"captchaToken");}catch(Exception e){throw new RuntimeException(e);}};
  if(type.equals("Pan123ResolveRepository")||type.equals("GuangYaResolveRepository"))return (Function1<Object,Object>)(continuation)->{try{return credential(type.startsWith("Pan123")?"pan123":"guangya");}catch(Exception e){throw new RuntimeException(e);}};
  if(type.equals("ResolveViewModel")&&parameter==Function0.class){int n=index+(android()?1:0);if(n==24)return (Function0<String>)()->String.valueOf(setting("githubMirrorPrefix",""));if(n==25)return (Function0<Boolean>)()->(Boolean)setting("quarkNoSaveDownload",true);if(n==26)return (Function0<Boolean>)()->(Boolean)setting("multiAccountDownload",false);if(n==27)return (Function0<Boolean>)()->(Boolean)setting("multiSourceDownload",false);}
  if(type.endsWith("CloudViewModel")||type.endsWith("CoudViewModel")){
   if(parameter.getSimpleName().equals("Function1"))return (Function1<Object,Object>)(continuation)->{try{return credential(model.split(":")[1]);}catch(Exception e){throw new RuntimeException(e);}};
  }
  if(type.equals("DownloadManager")){
   int n=index-(platform.context()==null?0:1);
   if(n==2)return (Function1<String,Integer>)(drive)->((Number)setting("downloadThreads",32)).intValue();
   if(n==3)return (Function0<String>)()->(String)setting("downloadDirUri",platform.context()==null?new File(platform.root(),"downloads").getAbsolutePath():null);
   if(n==4)return (Function0<Integer>)()->((Number)setting("maxConcurrentDownloads",3)).intValue();
   if(n==5)return (Function0<Long>)()->((Number)setting("downloadSpeedLimit",0L)).longValue();
   if(n==6)return (Function0<Integer>)()->((Number)setting("downloadRetryCount",3)).intValue();
   if(n==7)return (Function0<Boolean>)()->(Boolean)setting(platform.context()==null?"keepAwakeWhileDownloading":"keepDownloadWhenLocked",true);
   if(n==8)return (Function0<Boolean>)()->(Boolean)setting("notificationShowSpeed",true);
   if(n==9)return (Function0<Boolean>)()->(Boolean)setting("directWriteSingleThread",false);
   if(n==10)return (Function0<Boolean>)()->(Boolean)setting("directWriteMultiThread",false);
   if(n==11)return (Function0<Boolean>)()->"gopeed".equals(setting("downloadEngine","builtin"));
  }
  return null;
 }
 private Object uncheckedGet(String name,String method){try{return call(get(name),method,new JSONArray());}catch(Exception e){throw new RuntimeException(e);}}
 private Object setting(String name,Object fallback){try{Object settings=get("com.yunx.app.data.prefs.SettingsRepository");Object result=call(settings,"get"+capitalize(name),new JSONArray());return result==null||result instanceof String&&((String)result).isEmpty()?fallback:result;}catch(Exception e){return fallback;}}
 private Object credential(String drive)throws Exception{
  Object repo=get("com.yunx.app.data.repository."+prefix(drive)+"AccountRepository");if(drive.equals("quark"))return call(repo,"getFreshCookie",new JSONArray());if(drive.equals("guangya"))return call(repo,"ensureAccessToken",new JSONArray());Object account=call(repo,"getAccount",new JSONArray());if(account==null)return null;
  for(String property:new String[]{"cookie","accessToken","authorization"})try{Object v=call(account,"get"+capitalize(property),new JSONArray());if(v!=null&&!v.toString().isEmpty())return v;}catch(Exception ignored){}
  return null;
 }
 private Object accountField(String drive,String property)throws Exception{Object account=call(get("com.yunx.app.data.repository."+prefix(drive)+"AccountRepository"),"getAccount",new JSONArray());return account==null?null:call(account,"get"+capitalize(property),new JSONArray());}
 private Object refreshXunlei()throws Exception{Object refresh=accountField("xunlei","refreshToken");if(refresh==null)return null;Object result=call(get("com.yunx.app.data.network.XunleiApi"),"refreshToken",new Object[]{refresh,accountField("xunlei","deviceId"),accountField("xunlei","authType")});if(result!=null)call(get("com.yunx.app.data.repository.XunleiAccountRepository"),"updateTokens",new Object[]{call(result,"getFirst",new JSONArray()),call(result,"getSecond",new JSONArray())});return result;}
 private String prefix(String drive)throws IOException{String p=drives.get(drive);if(p==null)throw new IOException("未知网盘");return p;}
 private Object model(String name)throws Exception{
  if(models.containsKey(name))return models.get(name);String className;
  if(name.equals("resolve"))className="ResolveViewModel";
  else if(name.equals("video"))className=android()?"ResolveViewModel":"VideoResolveViewModel";
  else{String[] parts=name.split(":");if(parts.length!=2)throw new IOException("未知业务页面");String p=prefix(parts[1]);className=p+(parts[0].equals("cloud")?(p.equals("UC")?"CoudViewModel":"CloudViewModel"):"AccountViewModel");}
  Object m=construct(cls("com.yunx.app.ui.viewmodel."+className),name);models.put(name,m);return m;
 }
 public synchronized Object execute(String method,JSONObject p)throws Exception{
  syncProxy();
  switch(method){
   case "bootstrap":{JSONArray providers=new JSONArray();for(String drive:drives.keySet())providers.put(new JSONObject().put("id",drive).put("name",labels.get(drive)).put("accounts",accounts(drive)).put("login",loginType(drive)));return new JSONObject().put("providers",providers).put("settings",settings()).put("downloads",downloads()).put("protocol","1.1");}
   case "vm.snapshot":return snapshotFor(p.getString("model"),model(p.getString("model")));
   case "vm.call":{
    String name=p.getString("model"),kind=name.contains(":")?name.split(":")[0]:name,action=p.getString("action");
    if(!allowed.getOrDefault(kind,Collections.emptySet()).contains(action))throw new IOException("未声明的插件业务操作");
    JSONArray args=p.optJSONArray("args")==null?new JSONArray():p.getJSONArray("args");
    if(name.equals("video")&&(action.equals("resolve")||action.equals("retry"))){if(action.equals("resolve"))lastVideoInput=args.optString(0);platform.prepareVideo(lastVideoInput);if(android()){action="startVideoResolve";args=new JSONArray().put(lastVideoInput);}}
    if(name.equals("video")&&android()&&action.equals("reset"))action="backToInput";
    if(name.equals("account:baidu")&&(action.equals("saveBaiduAccount")||action.equals("logout"))){clearBaiduSession();if(action.equals("saveBaiduAccount")){Object token=call(get("com.yunx.app.data.network.BaiduApi"),"getBdstoken",new Object[]{args.getString(0)});if(token==null||token.toString().isEmpty())throw new IOException("百度登录尚未完成或凭证已失效，请在官方网页完成登录后重试");}}
    Object m=model(name),result=call(m,action,args);return new JSONObject().put("result",json(result,0)).put("state",snapshotFor(name,m));
   }
   case "accounts.list":return accounts(p.getString("provider"));
   case "accounts.action":{String drive=p.getString("provider"),action=p.getString("action");if(!Arrays.asList("setPrimary","updateRemark","deleteById").contains(action))throw new IOException("未知账户操作");if(drive.equals("baidu")&&!action.equals("updateRemark"))clearBaiduSession();Object dao=get("com.yunx.app.data.db."+prefix(drive)+"AccountDao");JSONArray args=new JSONArray().put(p.getString("id"));if(action.equals("updateRemark"))args.put(p.optString("remark"));call(dao,action,args);return accounts(drive);}
   case "settings.get":return settings();
   case "settings.set":{Object settings=get("com.yunx.app.data.prefs.SettingsRepository");for(Iterator<String> it=p.keys();it.hasNext();){String key=it.next();if(!properties.contains(key))throw new IOException("未知下载设置："+key);call(settings,"set"+capitalize(key),new JSONArray().put(p.get(key)));}try{call(get("com.yunx.app.data.network.HttpClients"),"setHttp2Enabled",new JSONArray().put(setting("http2Enabled",false)));}catch(Exception ignored){}return settings();}
   case "settings.directory":return platform.chooseDirectory();
   case "downloads.list":return downloads();
   case "downloads.add":{String url=p.getString("url");if(!url.matches("(?is)^(https?://|magnet:|ed2k:).+"))throw new IOException("请输入 HTTP、HTTPS、磁力或 ed2k 链接");Object manager=get("com.yunx.app.data.download.DownloadManager");Object id=call(manager,"enqueue",new JSONArray().put(url).put(p.optString("name")));return new JSONObject().put("id",id);}
   case "downloads.action":{String action=p.getString("action");if(!Arrays.asList("start","pause","remove","redownload").contains(action))throw new IOException("未知下载操作");JSONArray args=new JSONArray().put(p.getLong("id"));if(action.equals("remove"))args.put(p.optBoolean("deleteFile"));call(get("com.yunx.app.data.download.DownloadManager"),action,args);if(action.equals("remove"))for(int attempt=0;attempt<40&&findTask(p.getLong("id"))!=null;attempt++)Thread.sleep(50);return downloads();}
   case "downloads.open":{Object task=findTask(p.getLong("id"));if(task==null)throw new IOException("下载记录不存在");String location=String.valueOf(call(task,"getSavePath",new JSONArray()));return platform.openFile(location);}
   case "cloud.share":{Object cloud=model("cloud:"+p.getString("provider"));for(Method m:cloud.getClass().getMethods())if(m.getName().equals("shareFile")){Object[] values=new Object[m.getParameterCount()];call(cloud,"shareFile",values);return snapshot(cloud);}throw new IOException("此网盘不支持分享");}
   case "video.accounts":return json(first(call(get("com.yunx.app.data.repository.VideoAccountRepository"),"observeAccounts",new JSONArray())),0);
   case "video.account.action":{Object repo=get("com.yunx.app.data.repository.VideoAccountRepository");if(p.getString("action").equals("save"))return json(call(repo,"save",new JSONArray().put(p.getString("platform")).put(p.getString("cookie")).put(true)),0);if(p.getString("action").equals("remove")){call(repo,"remove",new JSONArray().put(p.getString("id")));return true;}throw new IOException("未知视频账户操作");}
   case "video.download":{
    Object result=reference(p.getString("result")),media=reference(p.getString("media"));
    if(!((List<?>)call(result,"getMedias",new JSONArray())).contains(media))throw new IOException("媒体不属于当前解析结果，请重新解析");
    Object url=call(media,"getUrl",new JSONArray()),headers=call(media,"getHeaders",new JSONArray());String file;
    if(android()){
     file=String.valueOf(call(media,"getFileName",new JSONArray()));
     if(file.trim().isEmpty()){String title=String.valueOf(call(result,"getTitle",new JSONArray())).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]","_");if(title.length()>80)title=title.substring(0,80);String kind=String.valueOf(call(media,"getKind",new JSONArray()));file=title+"_"+(p.optInt("index")+1)+(kind.equals("IMAGE")?".jpg":kind.equals("AUDIO")?".m4a":".mp4");}
     Object id=call(get("com.yunx.app.data.download.DownloadManager"),"enqueue",new Object[]{url,file,headers,call(media,"getSize",new JSONArray()),"video","",Collections.emptyList(),call(media,"getAudioUrl",new JSONArray()),call(media,"getAudioHeaders",new JSONArray()),call(media,"getMotionUrl",new JSONArray())});
     return new JSONObject().put("id",id);
    }
    Object helper=get("com.yunx.app.data.download.VideoDownloads");file=String.valueOf(call(helper,"fileNameFor",new Object[]{result,media,p.optInt("index"),p.optInt("total",1)}));Object id=call(get("com.yunx.app.data.download.DownloadManager"),"enqueue",new Object[]{url,file,headers,call(media,"getSize",new JSONArray()),"video",call(result,"getUrl",new JSONArray()),"",Collections.emptyList(),"",call(media,"getAudioUrl",new JSONArray()),call(media,"getAudioHeaders",new JSONArray()),call(media,"getMotionUrl",new JSONArray())});return new JSONObject().put("id",id);
   }
   case "history.list":return json(first(call(get("com.yunx.app.data.db.LinkHistoryDao"),"observeAll",new JSONArray())),0);
   case "cache.info":return new JSONObject().put("bytes",sizeOf(new File(platform.root(),"cache"))).put("path",new File(platform.root(),"cache").getAbsolutePath());
   default:throw new IOException("未知插件引擎方法："+method);
  }
 }
 private void clearBaiduSession()throws Exception{Object api=get("com.yunx.app.data.network.BaiduApi");try{call(api,"clearSessionCache",new JSONArray());}catch(NoSuchMethodException e){try{Field cache=api.getClass().getDeclaredField("cachedBdstoken");cache.setAccessible(true);cache.set(api,null);}catch(NoSuchFieldException ignored){}}}
 private String loginType(String p){return Arrays.asList("pan123","ilanzou","guangya").contains(p)?"password":p.equals("xunlei")?"both":"cookie";}
 private JSONArray accounts(String drive)throws Exception{
  Object dao=get("com.yunx.app.data.db."+prefix(drive)+"AccountDao");return (JSONArray)json(call(dao,"getAccounts",new JSONArray()),0);
 }
 private JSONObject settings()throws Exception{JSONObject out=new JSONObject();Object settings=get("com.yunx.app.data.prefs.SettingsRepository");for(String key:properties)try{out.put(key,json(call(settings,"get"+capitalize(key),new JSONArray()),0));}catch(NoSuchMethodException ignored){}return out;}
 private JSONArray downloads()throws Exception{
  Object manager=get("com.yunx.app.data.download.DownloadManager"),tasks=first(call(manager,"getTasks",new JSONArray()));Object stats=first(call(manager,"getStats",new JSONArray()));JSONArray out=new JSONArray();if(tasks instanceof Iterable)for(Object t:(Iterable<?>)tasks){JSONObject row=(JSONObject)json(t,0);long id=((Number)call(t,"getId",new JSONArray())).longValue();Object info=stats instanceof Map?((Map<?,?>)stats).get(id):null;row.put("stats",json(info,0));out.put(row);}return out;
 }
 private Object findTask(long id)throws Exception{Object tasks=first(call(get("com.yunx.app.data.download.DownloadManager"),"getTasks",new JSONArray()));if(tasks instanceof Iterable)for(Object t:(Iterable<?>)tasks)if(((Number)call(t,"getId",new JSONArray())).longValue()==id)return t;return null;}
 private Object first(Object flow)throws Exception{
  if(flow==null)return null;
  try{return flow.getClass().getMethod("getValue").invoke(flow);}catch(NoSuchMethodException|IllegalAccessException ignored){}
  for(Method m:cls("kotlinx.coroutines.flow.FlowKt").getMethods())if(m.getName().equals("first")&&m.getParameterCount()==2)return suspendInvoke(m,null,new Object[]{flow,null},1);
  throw new IOException("无法读取下载状态");
 }
 private JSONObject snapshotFor(String name,Object vm)throws Exception{JSONObject out=snapshot(vm);if(android()&&name.equals("video")){JSONObject state=out.optJSONObject("uiState");if(state!=null&&state.optString("type").equals("Video"))state.put("type","Success");}return out;}
 private JSONObject snapshot(Object vm)throws Exception{JSONObject out=new JSONObject();for(Method m:vm.getClass().getMethods()){String n=m.getName();if(m.getParameterCount()!=0||(!n.startsWith("get")&&!n.startsWith("is"))||n.equals("getClass")||n.equals("getViewModelScope"))continue;String key=n.startsWith("is")?Character.toLowerCase(n.charAt(2))+n.substring(3):Character.toLowerCase(n.charAt(3))+n.substring(4);try{Object v=m.invoke(vm);if(v!=null&&implementsFlow(v.getClass()))v=first(v);out.put(key,json(v,0));}catch(Exception ignored){}}return out;}
 private boolean implementsFlow(Class<?> t){if(t==null)return false;for(Class<?> i:t.getInterfaces())if(i.getName().startsWith("kotlinx.coroutines.flow.")||implementsFlow(i))return true;return implementsFlow(t.getSuperclass());}
 private Object reference(String id)throws IOException{Object v=references.get(id);if(v==null)throw new IOException("文件列表已过期，请刷新后重试");return v;}
 private Object json(Object v,int depth)throws Exception{
  if(v==null||v==JSONObject.NULL||v.getClass().getName().equals("kotlin.Unit"))return JSONObject.NULL;
  if(v instanceof String||v instanceof Number||v instanceof Boolean||v instanceof JSONObject||v instanceof JSONArray)return v;
  if(v instanceof Enum)return ((Enum<?>)v).name();if(depth>7)return JSONObject.NULL;
  if(v instanceof Map){JSONObject out=new JSONObject();for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet())if(!secret(String.valueOf(e.getKey())))out.put(String.valueOf(e.getKey()),json(e.getValue(),depth+1));return out;}
  if(v instanceof Iterable){JSONArray out=new JSONArray();for(Object item:(Iterable<?>)v)out.put(json(item,depth+1));return out;}
  if(v.getClass().isArray()){JSONArray out=new JSONArray();for(int i=0;i<Array.getLength(v);i++)out.put(json(Array.get(v,i),depth+1));return out;}
  if(!v.getClass().getName().startsWith("com.yunx.app."))return String.valueOf(v);
  JSONObject out=new JSONObject().put("type",v.getClass().getSimpleName());String ref=referenceIds.get(v);if(ref==null){ref=UUID.randomUUID().toString();referenceIds.put(v,ref);references.put(ref,v);}out.put("_ref",ref);
  for(Method m:v.getClass().getMethods()){String n=m.getName();if(m.getParameterCount()!=0||(!n.startsWith("get")&&!n.startsWith("is"))||n.equals("getClass")||n.contains("$"))continue;String key=n.startsWith("is")?Character.toLowerCase(n.charAt(2))+n.substring(3):Character.toLowerCase(n.charAt(3))+n.substring(4);if(secret(key))continue;try{out.put(key,json(m.invoke(v),depth+1));}catch(Exception ignored){}}
  return out;
 }
 private static boolean secret(String key){return key.matches("(?i).*(cookie|password|authorization|accessToken|refreshToken|captchaToken|requestHeadersJson|altSourcesJson|mergeAudioHeadersJson).* ".trim());}
 private Object call(Object target,String action,JSONArray args)throws Exception{Object[] values=new Object[args.length()];for(int i=0;i<values.length;i++)values[i]=args.get(i);return call(target,action,values);}
 private Object call(Object target,String action,Object[] values)throws Exception{
  Exception last=null;
  for(Method m:target.getClass().getMethods())if(!Modifier.isStatic(m.getModifiers())&&(m.getName().equals(action)||m.getName().startsWith(action+"-"))){Class<?>[] ts=m.getParameterTypes();boolean suspend=ts.length>0&&Continuation.class.isAssignableFrom(ts[ts.length-1]);if(ts.length!=values.length+(suspend?1:0))continue;try{Object[] converted=new Object[ts.length];for(int i=0;i<values.length;i++)converted[i]=convert(values[i],ts[i]);m.setAccessible(true);return suspend?suspendInvoke(m,target,converted,ts.length-1):m.invoke(target,converted);}catch(InvocationTargetException e){throw problem(e.getCause());}catch(IllegalArgumentException e){last=e;}}
  for(Method m:target.getClass().getMethods())if(Modifier.isStatic(m.getModifiers())&&(m.getName().equals(action+"$default")||m.getName().startsWith(action+"-" )&&m.getName().endsWith("$default"))){Class<?>[] ts=m.getParameterTypes();int continuation=-1;for(int i=1;i<ts.length;i++)if(Continuation.class.isAssignableFrom(ts[i]))continuation=i;int count=continuation<0?originalCount(ts.length,true):continuation-1;if(values.length>count)continue;try{Object[] args=new Object[ts.length];args[0]=target;int masksStart=continuation<0?count+1:continuation+1;for(int i=masksStart;i<args.length-1;i++)args[i]=0;for(int i=0;i<count;i++){if(i<values.length)args[i+1]=convert(values[i],ts[i+1]);else{args[i+1]=zero(ts[i+1]);int slot=masksStart+i/32;args[slot]=((Integer)args[slot])|(1<<(i%32));}}m.setAccessible(true);return continuation<0?m.invoke(null,args):suspendInvoke(m,null,args,continuation);}catch(InvocationTargetException e){throw problem(e.getCause());}catch(IllegalArgumentException e){last=e;}}
  throw new NoSuchMethodException(target.getClass().getSimpleName()+"."+action+"("+values.length+")"+(last==null?"":": "+last.getMessage()));
 }
 private Object convert(Object value,Class<?> type)throws Exception{
  if(value==JSONObject.NULL)value=null;if(value==null)return zero(type);if(type.isInstance(value))return value;
  if(value instanceof JSONObject&&((JSONObject)value).has("_ref")){Object ref=reference(((JSONObject)value).getString("_ref"));if(type.isInstance(ref))return ref;}
  if(type==String.class)return value.toString();if(type==int.class||type==Integer.class)return ((Number)value).intValue();if(type==long.class||type==Long.class)return ((Number)value).longValue();if(type==float.class||type==Float.class)return ((Number)value).floatValue();if(type==double.class||type==Double.class)return ((Number)value).doubleValue();if(type==boolean.class||type==Boolean.class)return value;
  if(type.isEnum())return Enum.valueOf((Class)type,value.toString().equals("xiaohongshu")?"XHS":value.toString().toUpperCase(Locale.ROOT));
  if(Map.class.isAssignableFrom(type)&&value instanceof JSONObject){Map<String,Object> out=new LinkedHashMap<>();JSONObject o=(JSONObject)value;for(Iterator<String> it=o.keys();it.hasNext();){String key=it.next();out.put(key,o.get(key));}return out;}
  if(List.class.isAssignableFrom(type)&&value instanceof JSONArray){List<Object> out=new ArrayList<>();JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++){Object item=a.get(i);out.add(item instanceof JSONObject&&((JSONObject)item).has("_ref")?reference(((JSONObject)item).getString("_ref")):item);}return out;}
  throw new IllegalArgumentException("不支持的参数类型 "+type.getSimpleName());
 }
 private Object suspendInvoke(Method method,Object target,Object[] args,int index)throws Exception{
  CompletableFuture<Object> future=new CompletableFuture<>();args[index]=new Continuation<Object>(){public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}public void resumeWith(Object value){try{kotlin.ResultKt.throwOnFailure(value);future.complete(value);}catch(Throwable e){future.completeExceptionally(e);}}};Object result;
  try{method.setAccessible(true);result=method.invoke(target,args);}catch(InvocationTargetException e){throw problem(e.getCause());}
  if(result!=IntrinsicsKt.getCOROUTINE_SUSPENDED())return result;
  try{return future.get(120,TimeUnit.SECONDS);}catch(ExecutionException e){throw problem(e.getCause());}
 }
 private static int originalCount(int total,boolean self){for(int n=0;n<128;n++)if(n+(self?1:0)+(n+31)/32+1==total)return n;throw new IllegalArgumentException("业务默认参数形状不兼容");}
 private static void setMask(Object[] args,int start,int bit){int i=start+bit/32;args[i]=(args[i]==null?0:(Integer)args[i])|(1<<(bit%32));}
 private static Object zero(Class<?> t){if(t==boolean.class)return false;if(t==long.class)return 0L;if(t==float.class)return 0F;if(t==double.class)return 0D;if(t.isPrimitive())return 0;return null;}
 private static String capitalize(String value){return Character.toUpperCase(value.charAt(0))+value.substring(1);}
 private static Exception problem(Throwable e){return e instanceof Exception?(Exception)e:new IOException(e.toString(),e);}
 private static long sizeOf(File file){if(!file.exists())return 0;if(file.isFile())return file.length();long n=0;File[] children=file.listFiles();if(children!=null)for(File f:children)n+=sizeOf(f);return n;}
 public synchronized void close()throws Exception{for(Object vm:models.values())try{Object scope=cls("androidx.lifecycle.ViewModelKt").getMethod("getViewModelScope",cls("androidx.lifecycle.ViewModel")).invoke(null,vm);cls("kotlinx.coroutines.CoroutineScopeKt").getMethod("cancel",cls("kotlinx.coroutines.CoroutineScope"),java.util.concurrent.CancellationException.class).invoke(null,scope,null);}catch(Exception ignored){}Object manager=objects.get(cls("com.yunx.app.data.download.DownloadManager"));if(manager!=null){Object tasks=first(call(manager,"getTasks",new JSONArray()));if(tasks instanceof Iterable)for(Object task:(Iterable<?>)tasks)call(manager,"pause",new Object[]{call(task,"getId",new JSONArray())});}for(Object value:objects.values())for(Field field:value.getClass().getDeclaredFields())if(field.getType().getName().equals("kotlinx.coroutines.CoroutineScope")){try{field.setAccessible(true);Object scope=field.get(value);if(scope!=null)for(Method method:cls("kotlinx.coroutines.CoroutineScopeKt").getMethods())if(method.getName().equals("cancel")&&method.getParameterCount()==2){method.invoke(null,scope,null);break;}}catch(Exception ignored){}}Object db=objects.get(cls("com.yunx.app.data.db.AppDatabase"));if(db!=null)try{call(db,"close",new JSONArray());}catch(Exception ignored){}references.clear();referenceIds.clear();}
 private void syncProxy(){if(platform.context()!=null||System.currentTimeMillis()-proxyChecked<5000)return;proxyChecked=System.currentTimeMillis();try{Object system=get("com.yunx.app.data.network.SystemProxy"),info=call(system,"inspect",new JSONArray());String host="";int port=0;if(info.getClass().getSimpleName().equals("Proxy")){host=String.valueOf(call(info,"getHost",new JSONArray()));port=((Number)call(info,"getPort",new JSONArray())).intValue();}String next=host+":"+port;if(!next.equals(proxySignature)){call(get("com.yunx.app.data.network.HttpClients"),"setProxy",new JSONArray().put(host.isEmpty()?JSONObject.NULL:host).put(port));proxySignature=next;}}catch(Exception ignored){}}
}
