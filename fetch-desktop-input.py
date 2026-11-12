from pathlib import Path
import urllib.request,json,hashlib,zipfile,time,concurrent.futures
root=Path(__file__).resolve().parent;asset=json.loads((root/'upstream.lock.json').read_text(encoding='utf8'))['desktopBuildInput'];directory=root/'build-input';directory.mkdir(exist_ok=True);parts=directory/'.chunks';parts.mkdir(exist_ok=True);step=1024*1024;total=asset['bytes']
def fetch(index):
 start=index*step;end=min(total,start+step)-1;file=parts/str(index)
 if file.exists() and file.stat().st_size==end-start+1:return
 for retry in range(6):
  try:
   req=urllib.request.Request(asset['url'],headers={'Range':f'bytes={start}-{end}','Accept-Encoding':'identity'})
   with urllib.request.urlopen(req,timeout=120) as response:data=response.read()
   assert len(data)==end-start+1;file.write_bytes(data);return
  except Exception:
   if retry==5:raise
   time.sleep(retry+1)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:list(pool.map(fetch,range((total+step-1)//step)))
archive=directory/'upstream-build-input.zip'
with archive.open('wb') as out:
 for i in range((total+step-1)//step):out.write((parts/str(i)).read_bytes())
assert hashlib.sha256(archive.read_bytes()).hexdigest()==asset['sha256']
with zipfile.ZipFile(archive) as zip:
 for entry in zip.infolist():
  target=(directory/entry.filename).resolve();assert target.is_relative_to(directory.resolve())
  if entry.is_dir():continue
  target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(zip.read(entry))
print('Verified upstream libraries/JVM extracted for building the plugin. They are not an end-user program distribution.')
