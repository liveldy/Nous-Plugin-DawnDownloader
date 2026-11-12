"""Compile the plugin adapter and package its JVM runtime; no executable installer."""
from pathlib import Path
import os,subprocess,zipfile,tempfile,json,hashlib
plugin=Path(__file__).resolve().parent;source=Path(os.environ['YUNX_BUILD_INPUT']).resolve();jdk=Path(os.environ['JAVA_HOME']);output=plugin/'dist';output.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='nous-downloader-build-',dir=output) as temp:
 classes=Path(temp)/'classes';classes.mkdir();files=[str(p) for group in ['shared','desktop'] for p in (plugin/'engine'/group).rglob('*.java')]
 subprocess.run([str(jdk/'bin/javac.exe'),'--release','17','-encoding','UTF-8','-cp',str(source/'app/*'),'-d',str(classes),*files],check=True)
 adapter=Path(temp)/'Nous-Engine.jar'
 with zipfile.ZipFile(adapter,'w',zipfile.ZIP_DEFLATED) as jar:
  for p in classes.rglob('*.class'):jar.write(p,p.relative_to(classes).as_posix())
 target=output/'downloader-windows-x64.module.zip'
 with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as out:
  out.write(adapter,'app/Nous-Engine.jar')
  for folder in ['app','runtime']:
   for p in (source/folder).rglob('*'):
    if p.is_file() and not p.name.lower().startswith('yunx-desktop'):out.write(p,p.relative_to(source).as_posix())
 print('Built host-loaded JVM plugin module: '+str(target),flush=True)
