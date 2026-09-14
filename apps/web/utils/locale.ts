export type Locale='zh'|'en'
export function localeOf(path:string):Locale{return /^\/en(?:\/|$|[?#])/.test(path)?'en':'zh'}
export function contentPath(path:string){return localeOf(path)==='en'?(path.replace(/^\/en(?=\/|$|[?#])/,'')||'/'):path}
export function switchLocale(path:string){return localeOf(path)==='en'?contentPath(path):'/en'+(path.startsWith('/')?path:'/'+path)}
