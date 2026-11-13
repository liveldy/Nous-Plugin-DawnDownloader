"""Compile the plugin-owned Kotlin API adaptations before original upstream jars."""
from pathlib import Path
import os,json,hashlib,subprocess,urllib.request
def compile_overrides(plugin,classes,source,jdk):
 cache=plugin/'build-input/kotlin-compiler';cache.mkdir(parents=True,exist_ok=True);jars=[]
 gradle=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')))/'caches/modules-2/files-2.1'
 for item in json.loads((plugin/'kotlin-compiler.lock.json').read_text(encoding='utf8')):
  file=cache/item['file']
  candidates=[file]+list((gradle/item['group']/item['artifact']/item['version']).glob('*/'+item['file']))
  found=next((p for p in candidates if p.is_file() and hashlib.sha256(p.read_bytes()).hexdigest()==item['sha256']),None)
  if found is None:
   with urllib.request.urlopen(item['url'],timeout=120) as response:file.write_bytes(response.read())
   assert hashlib.sha256(file.read_bytes()).hexdigest()==item['sha256'],'Compiler input digest mismatch';found=file
  jars.append(str(found))
 dependencies=[str(p) for p in (source/'app').glob('*.jar')]
 inputs=[str(p) for p in (plugin/'engine/desktop-kotlin').rglob('*.kt')]
 subprocess.run([str(jdk/'bin/java.exe'),'-Xmx1024m','-cp',os.pathsep.join(jars+dependencies),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-jvm-target','17','-module-name','nous_downloader_overrides','-classpath',os.pathsep.join([str(classes)]+dependencies),'-d',str(classes),*inputs],check=True)
