/* Shared projection of upstream ShareFile objects. AGPL-3.0-or-later. */
(function(root){
 const truth=value=>value===true||value===1||value==='1'||value==='true';
 const model={isDirectory:file=>truth(file?.isdir??file?.dir??file?.isDir),size:file=>file?.fsize??file?.size??null,
  shareDirectory(text){const match=String(text||'').match(/pan\.quark\.cn\/s\/[A-Za-z0-9]+[^\s]*#\/list\/share\/([a-f0-9]{32})(?:[/?&#]|$)/i);return match?.[1]||'';}};
 root.NousFileModel=model;if(typeof module!=='undefined')module.exports=model;
})(globalThis);
