package org.nous.downloader;
import java.io.*;import java.net.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.*;
import kotlin.jvm.functions.*;import okhttp3.*;import org.json.*;
import com.yunx.app.data.network.*;import com.yunx.app.data.network.model.*;import com.yunx.app.data.repository.*;

/** Real provider classes against an in-process HTTP transport; no account credentials required. */
public final class NetworkContract {
 static final PrintStream status=System.out;
 static EngineCore core;static Method invoke,snapshot;static AtomicInteger calls=new AtomicInteger();static String mode="normal",lastDirectory="";
 static Object call(Object target,String name,Object...args)throws Exception{try{return invoke.invoke(core,target,name,args);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw e;}}
 static String jsonBody(Request request)throws Exception{
  String pageValue=request.url().queryParameter("_page");if(pageValue==null)pageValue=request.url().queryParameter("page");int page=pageValue==null?1:Integer.parseInt(pageValue);calls.incrementAndGet();
  if(request.url().host().contains("baidu")){
   String cookie=request.header("Cookie"),token=cookie!=null&&cookie.contains("BDUSS=A")?"token-A":"token-B";
   if(cookie!=null&&cookie.contains("BDUSS=BAD"))return "{\"errno\":-6}";
   if(request.url().encodedPath().contains("gettemplatevariable"))return new JSONObject().put("errno",0).put("result",new JSONObject().put("bdstoken",token).put("username","fixture")).toString();
   if(!token.equals(request.url().queryParameter("bdstoken")))return "{\"errno\":-6}";
   JSONArray files=new JSONArray();if(page<=3)for(int n=(page-1)*50;n<Math.min(page*50,127);n++)files.put(new JSONObject().put("fs_id",String.valueOf(n)).put("server_filename","File "+n).put("path","/file-"+n).put("size",1024).put("isdir",0));
   return new JSONObject().put("errno",0).put("list",files).toString();
  }
  lastDirectory=request.url().queryParameter("pdir_fid");if(mode.equals("failed")&&page==2)return "{\"status\":500,\"code\":500,\"message\":\"fixture failure\"}";
  if(mode.equals("repeat")&&page>1)page=1;
  JSONArray files=new JSONArray();if(page<=3)for(int n=(page-1)*50;n<Math.min(page*50,127);n++)files.put(new JSONObject().put("fid","f"+n).put("file_name","File "+n).put("size",1024+n).put("dir",n==0).put("pdir_fid",lastDirectory).put("share_fid_token","fixture"));
  return new JSONObject().put("status",200).put("code",0).put("data",new JSONObject().put("list",files)).toString();
 }
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 static Object result(Object value)throws Exception{if(value instanceof kotlin.Result){Method unbox=value.getClass().getDeclaredMethod("unbox-impl");unbox.setAccessible(true);value=unbox.invoke(value);}kotlin.ResultKt.throwOnFailure(value);return value;}
 static void paging(Object repo,String label)throws Exception{
  mode="normal";calls.set(0);Object result=result(call(repo,"listFiles",new ShareSession("share","fixture","fixture"),"0",""));check(((List<?>)result).size()==127,label+" must retain capped pages");check(calls.get()==4,label+" must continue through a short nonterminal page");
  mode="repeat";calls.set(0);result=result(call(repo,"listFiles",new ShareSession("share","fixture","fixture"),"0",""));check(((List<?>)result).size()==50&&calls.get()==2,label+" duplicate-page termination");
  mode="failed";result=call(repo,"listFiles",new ShareSession("share","fixture","fixture"),"0","");boolean failed=false;try{result(result);}catch(Throwable expected){failed=true;}check(failed,label+" must not silently report a partial page as complete");mode="normal";
  status.println("PASS "+label+" capped 127-item paging, duplicate termination and failed-page propagation");
 }
 public static void main(String[] args)throws Exception{
  System.setOut(System.err);
  File root=new File(args[0]);root.mkdirs();System.setProperty("nous.plugin.root",root.getAbsolutePath());System.setProperty("java.util.prefs.PreferencesFactory",ScopedPreferencesFactory.class.getName());com.yunx.app.AppContext.INSTANCE.init();
  core=new EngineCore(new EngineCore.Platform(){public Object context(){return null;}public File root(){return root;}public Object openFile(String p){return null;}public Object chooseDirectory(){return null;}});
  invoke=EngineCore.class.getDeclaredMethod("call",Object.class,String.class,Object[].class);invoke.setAccessible(true);snapshot=EngineCore.class.getDeclaredMethod("snapshot",Object.class);snapshot.setAccessible(true);
  OkHttpClient client=new OkHttpClient.Builder().addInterceptor(chain->{try{return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(ResponseBody.create(jsonBody(chain.request()),MediaType.get("application/json"))).build();}catch(Exception e){throw new IOException(e);}}).build();
  QuarkApi quark=new QuarkApi((Function0<OkHttpClient>)()->client);paging(new QuarkResolveRepository(quark),"Desktop Quark");
  try(URLClassLoader android=new URLClassLoader(new URL[]{new File(args[1]).toURI().toURL()},NetworkContract.class.getClassLoader()){@Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(name)){if(name.startsWith("com.yunx.app.data.repository.QuarkResolveRepository")){Class<?> cls=findLoadedClass(name);if(cls==null)cls=findClass(name);if(resolve)resolveClass(cls);return cls;}return super.loadClass(name,resolve);}}}){Object repository=android.loadClass("com.yunx.app.data.repository.QuarkResolveRepository").getConstructor(QuarkApi.class).newInstance(quark);paging(repository,"Actual Android Quark bytecode");}
  BaiduApi baidu=new BaiduApi((Function0<OkHttpClient>)()->client);check(call(baidu,"getBdstoken","BDUSS=A").equals("token-A"),"account A token");check(call(baidu,"getBdstoken","BDUSS=B").equals("token-B"),"account B cannot reuse A token");check(((List<?>)call(baidu,"listCloudFiles","/","BDUSS=A")).size()==127,"Baidu all pages");check(((List<?>)call(baidu,"listDir","/","BDUSS=A")).size()==127,"Baidu temporary-directory lookup all pages");status.println("PASS Baidu account token isolation, cloud and directory-lookup paging");
  Field objects=EngineCore.class.getDeclaredField("objects");objects.setAccessible(true);((Map<Class<?>,Object>)objects.get(core)).put(BaiduApi.class,baidu);
  JSONArray before=(JSONArray)core.execute("accounts.list",new JSONObject().put("provider","baidu"));boolean rejected=false;try{core.execute("vm.call",new JSONObject().put("model","account:baidu").put("action","saveBaiduAccount").put("args",new JSONArray().put("BDUSS=BAD")));}catch(IOException expected){rejected=true;}
  check(rejected&&((JSONArray)core.execute("accounts.list",new JSONObject().put("provider","baidu"))).length()==before.length(),"invalid Baidu credentials must not create an account");status.println("PASS errno=-6 credential rejection before persistence");
  Method get=EngineCore.class.getDeclaredMethod("get",String.class);get.setAccessible(true);Object manager=get.invoke(core,"com.yunx.app.data.download.DownloadManager");Object vm=new com.yunx.app.ui.viewmodel.QuarkCloudViewModel(quark,(Function1<Object,Object>)continuation->"fixture",(com.yunx.app.data.download.DownloadManager)manager);
  Field models=EngineCore.class.getDeclaredField("models");models.setAccessible(true);((Map<String,Object>)models.get(core)).put("cloud:quark",vm);
  JSONObject state=null;for(int n=0;n<100;n++){state=(JSONObject)snapshot.invoke(core,vm);if("Loaded".equals(state.getJSONObject("uiState").optString("type")))break;Thread.sleep(50);}
  JSONObject file=state.getJSONObject("uiState").getJSONArray("files").getJSONObject(0);check(file.getBoolean("isdir")&&file.getLong("fsize")==1024,"upstream field projection");core.execute("vm.call",new JSONObject().put("model","cloud:quark").put("action","openFolder").put("args",new JSONArray().put(file)));
  for(int n=0;n<100&&!"f0".equals(lastDirectory);n++)Thread.sleep(50);check("f0".equals(lastDirectory),"reference-backed folder navigation");call(vm,"back");for(int n=0;n<100&&!"0".equals(lastDirectory);n++)Thread.sleep(50);check("0".equals(lastDirectory),"back to root");core.close();status.println("PASS real folder reference round trip, subdirectory request and back navigation");
  client.dispatcher().executorService().shutdown();client.connectionPool().evictAll();System.exit(0);
 }
}
