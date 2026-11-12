from pathlib import Path
import json,hashlib
root=Path(__file__).resolve().parent;p=root/'manifest.json';m=json.loads(p.read_text(encoding='utf8'))
for platform,spec in m['runtime'].items():
 file=root/'dist'/spec['asset']['url'].rsplit('/',1)[-1];spec['asset'].update(bytes=file.stat().st_size,sha256=hashlib.sha256(file.read_bytes()).hexdigest())
p.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n',encoding='utf8');print('Module hashes updated; pack the UI next.')
