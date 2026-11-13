import {createRequire} from 'node:module';import test from 'node:test';import assert from 'node:assert/strict';
const file=createRequire(import.meta.url)('../ui/file-model.js');
test('real upstream directory and size fields remain actionable across providers',()=>{
 const folder={fid:'real-id',fname:'Folder',isdir:true,fsize:0};assert.equal(file.isDirectory(folder),true);assert.equal(file.size({isdir:false,fsize:1024}),1024);
 for(const value of [false,0,'0','false',undefined])assert.equal(file.isDirectory({isdir:value}),false);
 assert.equal(file.isDirectory({dir:true}),true);assert.equal(file.size({size:42}),42);
});
test('shared directory fragments preserve the selected Quark folder',()=>{
 assert.equal(file.shareDirectory('https://pan.quark.cn/s/fixture#/list/share/11112222333344445555666677778888'),'11112222333344445555666677778888');assert.equal(file.shareDirectory('https://pan.quark.cn/s/fixture'),'');
});
