import {localeOf,contentPath,switchLocale} from '../utils/locale'
import {localPath} from '../utils/site'
export function useLocale(){const route=useRoute();const locale=computed(()=>localeOf(route.path));return {locale,isEnglish:computed(()=>locale.value==='en'),path:computed(()=>contentPath(route.path)),t:(zh:string,en:string)=>locale.value==='en'?en:zh,link:(path:string)=>localPath(path,locale.value),switchPath:computed(()=>switchLocale(route.fullPath))}}
