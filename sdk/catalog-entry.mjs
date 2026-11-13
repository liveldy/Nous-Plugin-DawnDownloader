#!/usr/bin/env node
// MIT. Generate an unsigned catalog submission; never asks developers for a signing key.
import fs from 'node:fs/promises';import path from 'node:path';import {validateBundle,githubURL} from './protocol.mjs';import {digest} from './project.mjs';
const [file,url,out='catalog-entry.json']=process.argv.slice(2);if(!file||!url)throw Error('Usage: node sdk/catalog-entry.mjs bundle.nous-plugin.json GitHubReleaseURL [entry.json]');const raw=await fs.readFile(file),b=validateBundle(JSON.parse(raw));githubURL(url,b.manifest.repository);const entry={...b.manifest,package:{url,sha256:digest(raw),bytes:raw.length}};await fs.writeFile(path.resolve(out),JSON.stringify(entry,null,2)+'\n');console.log('Wrote unsigned catalog submission '+path.resolve(out));
