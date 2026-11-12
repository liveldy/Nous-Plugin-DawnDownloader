/* Plugin-scoped replacement for the upstream desktop directory facade. AGPL-3.0. */
package com.yunx.app;
import java.io.File;import java.util.prefs.Preferences;
public final class AppContext {
 public static final AppContext INSTANCE=new AppContext();public static final int $stable=0;
 public static final String KEY_DB_DIR="storage_db_dir",KEY_CACHE_DIR="storage_cache_dir",KEY_DB_MIGRATED_FROM="storage_db_migrated_from",KEY_CACHE_MIGRATED_FROM="storage_cache_migrated_from";
 private final File root=new File(System.getProperty("nous.plugin.root"));
 public File getDataDir(){return root;}public File getDbDir(){return new File(root,"database");}public File getCacheDir(){return new File(root,"cache");}public File getDownloadTmpDir(){return new File(getCacheDir(),"download_tmp");}public File getMergeDir(){return new File(getCacheDir(),"merge");}public File getFilesDir(){return new File(root,"files");}public File getAppRootDir(){return new File(System.getProperty("nous.engine.home"));}
 public Preferences getMiscPrefs(){return Preferences.userRoot().node("yunx/misc");}public Preferences getSettingsPrefs(){return Preferences.userRoot().node("yunx/settings");}public Preferences getXunleiFpPrefs(){return Preferences.userRoot().node("yunx/xunlei_fp");}
 public void init(){for(File f:new File[]{root,getDbDir(),getCacheDir(),getDownloadTmpDir(),getMergeDir(),getFilesDir()})f.mkdirs();}
 public File appDataDirPreset(){return getDbDir();}public File appCacheDirPreset(){return getCacheDir();}
}
