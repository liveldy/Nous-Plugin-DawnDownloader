import test from 'node:test';import assert from 'node:assert/strict';import fs from 'node:fs/promises';import {validateBundle,pluginDocument} from '../sdk/protocol.mjs';
const manifest=JSON.parse(await fs.readFile('manifest.json','utf8')),files={};for(const path of manifest.resources)files[path]=await fs.readFile(path,'utf8');delete manifest.resources;const bundle={format:'nous.plugin/v1',manifest,files};
test('supported platforms and explicit permissions',()=>{assert.ok(validateBundle(bundle,'windows-x64'));assert.ok(validateBundle(bundle,'android-arm64'));assert.throws(()=>validateBundle(bundle,'web'));});
test('path traversal rejected',()=>{const copy=structuredClone(bundle);copy.files['../secret']='x';assert.throws(()=>validateBundle(copy));});
test('sandbox disallows direct network',()=>{assert.match(pluginDocument(bundle,'test'),/connect-src 'none'/);});
