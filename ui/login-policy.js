/* DawnDownloader platform login policy. AGPL-3.0. */
(function(root){
 const desktopUA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36';
 function embedded(config,platform,provider){const value=JSON.parse(JSON.stringify(config));value.urls={'windows-x64':config.url,'android-arm64':({baidu:'https://wappass.baidu.com/passport/?login&tpl=netdisk&u=https%3A%2F%2Fpan.baidu.com%2Fwap%2Fhome',quark:'https://pan.quark.cn/',uc:'https://drive.uc.cn/',pan115:'https://115.com/',c139:'https://yun.139.com/m/#/login'})[provider]||config.url};value.userAgents={'windows-x64':config.userAgent||desktopUA};delete value.userAgent;if(provider==='baidu'){value.cookieUrls.push('https://pan.baidu.com/wap/');value.validation.userAgent=desktopUA;}return value;}
 // An external button requires an actual platform adapter, not just an HTTPS login URL.
 function modes(provider,platform,methods={}){const result=['embedded'];const external=provider?.externalAuthorization;if(external?.platforms?.includes(platform)&&external.credentialExchange===true&&methods['auth.external.begin']&&methods['auth.external.status']&&methods['auth.external.cancel'])result.push('external');return result;}
 const api={embedded,modes};if(typeof module!=='undefined'&&module.exports)module.exports=api;root.DawnLoginPolicy=api;
})(typeof globalThis!=='undefined'?globalThis:this);
