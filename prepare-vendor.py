"""Restore exact corresponding upstream sources, then apply the plugin's explicit source overrides."""
from pathlib import Path
import json,hashlib,urllib.request,tarfile,io,shutil,os
root=Path(__file__).resolve().parent;lock=json.loads((root/'upstream.lock.json').read_text(encoding='utf8'))
for name,key in [('android','android'),('desktop','windows')]:
 info=lock[key];destination=root/'vendor'/name
 if not destination.exists():
  url='https://github.com/liveldy/Nous-Plugin-YunX/releases/download/v1.1.0/'+info['source']
  with urllib.request.urlopen(url,timeout=180) as response:payload=response.read()
  assert hashlib.sha256(payload).hexdigest()==info['sourceSha256'],'Upstream source checksum mismatch'
  destination.mkdir(parents=True,exist_ok=True)
  with tarfile.open(fileobj=io.BytesIO(payload),mode='r:gz') as archive:
   for member in archive.getmembers():
    if not member.isfile():continue
    parts=Path(member.name).parts[1:]
    if not parts:continue
    target=destination.joinpath(*parts).resolve();assert target.is_relative_to(destination.resolve())
    target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(archive.extractfile(member).read())
 override=root/'vendor-overrides'/name
 if override.exists():shutil.copytree(override,destination,dirs_exist_ok=True)
 if name=='android' and os.environ.get('ANDROID_TOOLCHAIN'):(destination/'local.properties').write_text('sdk.dir='+str(Path(os.environ['ANDROID_TOOLCHAIN']).resolve()/'sdk').replace('\\','/')+'\n',encoding='utf8')
print('Exact upstream source restored and plugin source overrides applied.')
