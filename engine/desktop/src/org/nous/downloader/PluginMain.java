/* The host starts this plugin worker; it has no installer, launcher or public server. */
package org.nous.downloader;
import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;
public final class PluginMain {
 public static void main(String[] args)throws Exception{
  File root=new File(System.getProperty("nous.plugin.root"));root.mkdirs();System.setProperty("java.util.prefs.PreferencesFactory",ScopedPreferencesFactory.class.getName());PrintStream output=new PrintStream(new FileOutputStream(FileDescriptor.out),true,StandardCharsets.UTF_8);System.setOut(System.err);com.yunx.app.AppContext.INSTANCE.init();
  EngineCore core=new EngineCore(new EngineCore.Platform(){public Object context(){return null;}public File root(){return root;}public Object openFile(String path)throws Exception{File target=new File(path);if(!target.exists())throw new IOException("文件不存在");java.awt.Desktop.getDesktop().open(target);return true;}public Object chooseDirectory()throws Exception{final String[] chosen={null};javax.swing.SwingUtilities.invokeAndWait(()->{javax.swing.JFileChooser c=new javax.swing.JFileChooser();c.setDialogTitle("选择下载目录");c.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);if(c.showOpenDialog(null)==javax.swing.JFileChooser.APPROVE_OPTION)chosen[0]=c.getSelectedFile().getAbsolutePath();});return chosen[0];}});
  BufferedReader in=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));String line;while((line=in.readLine())!=null){JSONObject reply=new JSONObject();try{JSONObject p=new JSONObject(line);reply.put("value",core.execute(p.getString("method"),p.optJSONObject("params")==null?new JSONObject():p.getJSONObject("params")));}catch(Throwable e){e.printStackTrace(System.err);while(e.getCause()!=null)e=e.getCause();reply.put("error",e.getMessage()==null?e.toString():e.getMessage());}output.println(reply);output.flush();}System.exit(0);
 }
}
