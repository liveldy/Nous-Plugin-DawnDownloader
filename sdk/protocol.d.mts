export interface PluginManifest {id:string;name:string;version:string;api:string;description:string;platforms:string[];permissions:string[];entry:string;icon?:string;repository:string;license:string;native?:Record<string,any>;runtime?:Record<string,any>;navigation?:{id:string;label:string;icon?:string}[];defaultView?:string;requiresHost?:string;}
export interface PluginBundle {format:'nous.plugin/v1';manifest:PluginManifest;files:Record<string,string>}
export const HOST_API:string;export const PLATFORMS:string[];export const PERMISSIONS:string[];
export function versionParts(value:string):number[];export function compareVersions(a:string,b:string):number;
export function validPath(path:string):boolean;export function validateBundle(bundle:any,platform?:string):PluginBundle;
export function githubURL(value:string,repo?:string):URL;export function b64bytes(value:string):Uint8Array;
export function sha256(value:string|Uint8Array):Promise<string>;export function verifyEnvelope(envelope:any,publicKey:string):Promise<any>;
export function pluginDocument(bundle:PluginBundle,nonce:string):string;
