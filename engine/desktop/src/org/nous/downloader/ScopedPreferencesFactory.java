package org.nous.downloader;
import java.util.prefs.*;import java.util.*;import java.io.*;import java.nio.file.*;import java.security.*;
/** Keeps upstream preferences inside this plugin profile; never reads YunX settings. */
public final class ScopedPreferencesFactory implements PreferencesFactory {
 private final Preferences root=new Node(null,"");public Preferences systemRoot(){return root;}public Preferences userRoot(){return root;}
 private static final class Node extends AbstractPreferences {
  private final Properties values=new Properties();private final File file;
  Node(Node parent,String name){super(parent,name);try{String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(absolutePath().getBytes(java.nio.charset.StandardCharsets.UTF_8)));file=new File(System.getProperty("nous.plugin.root"),"preferences/"+hash+".properties");if(file.exists())try(InputStream in=new FileInputStream(file)){values.load(in);}}catch(Exception e){throw new IllegalStateException(e);}}
  protected synchronized void putSpi(String k,String v){values.setProperty(k,v);save();}protected synchronized String getSpi(String k){return values.getProperty(k);}protected synchronized void removeSpi(String k){values.remove(k);save();}protected void removeNodeSpi(){file.delete();}protected String[] keysSpi(){return values.stringPropertyNames().toArray(new String[0]);}protected String[] childrenNamesSpi(){return new String[0];}protected AbstractPreferences childSpi(String name){return new Node(this,name);}protected void syncSpi(){}protected void flushSpi(){save();}
  private synchronized void save(){try{file.getParentFile().mkdirs();Path tmp=Path.of(file+".tmp");try(OutputStream out=Files.newOutputStream(tmp)){values.store(out,"Nous plugin preferences");}Files.move(tmp,file.toPath(),StandardCopyOption.REPLACE_EXISTING);}catch(IOException e){throw new IllegalStateException(e);}}
 }
}
