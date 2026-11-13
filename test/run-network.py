"""Run actual desktop and Android repository paging contracts against fake HTTP responses."""
from pathlib import Path
import os,subprocess,tempfile,zipfile
plugin=Path(__file__).resolve().parent.parent;jdk=Path(os.environ['JAVA_HOME']);android=plugin/'vendor/android/app/build/tmp/kotlin-classes/release'
assert android.is_dir(),'Build the Android module first; its real repository bytecode is tested without a device.'
with tempfile.TemporaryDirectory(prefix='network-contract-',dir=plugin/'dist') as temp:
 root=Path(temp);module=root/'module'
 with zipfile.ZipFile(plugin/'dist/downloader-windows-x64.module.zip') as archive:archive.extractall(module)
 classes=root/'test-classes';classes.mkdir();classpath=os.pathsep.join([str(classes),str(module/'app/Nous-Engine.jar'),str(module/'app/*')])
 source=plugin/'test/java/org/nous/downloader/NetworkContract.java'
 subprocess.run([str(jdk/'bin/javac.exe'),'--release','17','-encoding','UTF-8','-cp',classpath,'-d',str(classes),str(source)],check=True)
 subprocess.run([str(module/'runtime/bin/java.exe'),'-cp',classpath,'org.nous.downloader.NetworkContract',str(root/'data'),str(android)],check=True,timeout=90)
