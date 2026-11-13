package org.nous.downloader;
import java.io.File;import java.lang.reflect.*;import kotlin.jvm.functions.*;
/** Regression for the six-argument Android constructor, executed against the real adapter. */
public final class CallbackContract {
 public static final class XunleiResolveRepository {
  final Function2<String,Object,Object> byToken;
  public XunleiResolveRepository(Object api,Function1<Object,Object> account,Function1<Object,Object> device,Function1<Object,Object> captcha,Function1<Object,Object> refresh,Function2<String,Object,Object> byToken){this.byToken=byToken;}
 }
 public static void main(String[] args)throws Exception{
  File root=new File(args[0]);root.mkdirs();System.setProperty("nous.plugin.root",root.getAbsolutePath());System.setProperty("java.util.prefs.PreferencesFactory",ScopedPreferencesFactory.class.getName());com.yunx.app.AppContext.INSTANCE.init();
  EngineCore core=new EngineCore(new EngineCore.Platform(){public Object context(){return null;}public File root(){return root;}public Object openFile(String value){return null;}public Object chooseDirectory(){return null;}});
  Method construct=EngineCore.class.getDeclaredMethod("construct",Class.class,String.class);construct.setAccessible(true);
  XunleiResolveRepository repository=(XunleiResolveRepository)construct.invoke(core,XunleiResolveRepository.class,null);
  if(repository.byToken==null||repository.byToken.invoke("unknown-fixture-token",null)!=null)throw new AssertionError("Android account-by-token callback contract failed");
  Method fn=EngineCore.class.getDeclaredMethod("function",String.class,String.class,int.class,Class.class);fn.setAccessible(true);
  for(int i=1;i<=4;i++)if(!(fn.invoke(core,"XunleiResolveRepository",null,i,Function1.class) instanceof Function1))throw new AssertionError("Desktop callback regressed");
  core.close();System.out.println("PASS Android six-argument construction and Function2 invocation; desktop Function1 callbacks retained");
 }
}
