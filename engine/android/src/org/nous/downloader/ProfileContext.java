package org.nous.downloader;
import android.content.*;import android.database.DatabaseErrorHandler;import android.database.sqlite.SQLiteDatabase;import java.io.File;
/** Room, preferences, cache and credentials are scoped to one host account. */
final class ProfileContext extends ContextWrapper {
 final String profile;final File root;
 ProfileContext(Context base,String profile){super(base);this.profile=profile;root=new File(base.getFilesDir(),"profiles/"+profile);root.mkdirs();}
 @Override public Context getApplicationContext(){return this;}
 @Override public File getFilesDir(){File f=new File(root,"files");f.mkdirs();return f;}
 @Override public File getCacheDir(){File f=new File(root,"cache");f.mkdirs();return f;}
 @Override public File getNoBackupFilesDir(){File f=new File(root,"private");f.mkdirs();return f;}
 @Override public File getDatabasePath(String name){File f=new File(root,"databases/"+name);f.getParentFile().mkdirs();return f;}
 @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
 @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler handler){return SQLiteDatabase.openDatabase(getDatabasePath(name).getPath(),factory,SQLiteDatabase.CREATE_IF_NECESSARY,handler);}
 @Override public SharedPreferences getSharedPreferences(String name,int mode){return getBaseContext().getSharedPreferences("plugin_"+profile+"_"+name,mode);}
}
