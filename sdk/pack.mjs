#!/usr/bin/env node
// Nous Plugin Interface packer. MIT. node sdk/pack.mjs <project-directory> [output]
import fs from 'node:fs/promises';import path from 'node:path';import crypto from 'node:crypto';
import {validateBundle,validPath} from './protocol.mjs';
const root=path.resolve(process.argv[2]||'.'),manifest=JSON.parse(await fs.readFile(path.join(root,'manifest.json'),'utf8')),files={};
const declared=manifest.resources;if(!Array.isArray(declared)||!declared.length)throw Error('manifest.resources 必须列出所有页面资源');
for(const name of declared){if(!validPath(name))throw Error('无效资源路径：'+name);const target=path.join(root,name),real=await fs.realpath(target);if(!real.startsWith(root+path.sep))throw Error('资源不可指向项目目录外部');files[name]=await fs.readFile(real,'utf8');}
delete manifest.resources;const bundle=validateBundle({format:'nous.plugin/v1',manifest,files});const text=JSON.stringify(bundle),out=path.resolve(process.argv[3]||path.join(root,'dist',manifest.id+'-'+manifest.version+'.nous-plugin.json'));await fs.mkdir(path.dirname(out),{recursive:true});await fs.writeFile(out,text);console.log(JSON.stringify({file:out,bytes:Buffer.byteLength(text),sha256:crypto.createHash('sha256').update(text).digest('hex')}));
