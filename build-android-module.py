"""Build an Android Dex/resource plugin module. No installer APK is published."""
from pathlib import Path
import os,subprocess,zipfile,shutil
plugin=Path(__file__).resolve().parent;tool=Path(os.environ['ANDROID_TOOLCHAIN']).resolve();env=os.environ.copy();env['ANDROID_HOME']=str(tool/'sdk');env['GRADLE_USER_HOME']=str(tool/'gradle-cache')
subprocess.run([str(tool/'gradle-8.13/bin/gradle.bat'),'--no-daemon','--console=plain','assembleRelease'],cwd=plugin/'vendor/android',env=env,check=True)
apk=plugin/'vendor/android/app/build/outputs/apk/release/app-release-unsigned.apk'
if not apk.exists():apk=apk.with_name('app-release.apk')
target=plugin/'dist/downloader-android-arm64.module.zip';target.parent.mkdir(exist_ok=True)
with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as out:
 out.write(apk,'module.apk')
 with zipfile.ZipFile(apk) as source:
  for name in source.namelist():
   if name.startswith('lib/arm64-v8a/') and not name.endswith('/'):out.writestr(name,source.read(name))
print('Built host-loaded Android plugin module: '+str(target),flush=True)
