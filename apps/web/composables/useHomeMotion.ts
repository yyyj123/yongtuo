import {ref,onMounted,onBeforeUnmount,nextTick} from 'vue'

export function useHomeMotion(){
 const root=ref<HTMLElement>()
 let observer:IntersectionObserver|undefined,frame=0,readyFrame=0,disposed=false
 let preference:MediaQueryList|undefined
 const targets:HTMLElement[]=[]
 const revealAll=()=>{observer?.disconnect();for(const element of targets)element.dataset.revealed='true';root.value?.style.removeProperty('--hero-drift')}
 const revealFocused=(event:FocusEvent)=>{const target=(event.target as HTMLElement)?.closest<HTMLElement>('[data-reveal]');if(target){target.dataset.revealed='true';target.style.setProperty('--scroll-reveal-y','0px');target.style.setProperty('--scroll-reveal-opacity','1');target.style.setProperty('--scroll-reveal-clip','0%')}}
 const update=()=>{
  frame=0;if(!root.value||preference?.matches)return
  const rect=root.value.getBoundingClientRect(),travel=Math.max(0,-rect.top)
  root.value.style.setProperty('--hero-drift',`${Math.min(travel*.12,72)}px`)
  if(window.innerWidth<=1100)return
  const positions=targets.map(element=>{let top=0,node:HTMLElement|null=element;while(node){top+=node.offsetTop;node=node.offsetParent as HTMLElement|null}return top-window.scrollY})
  targets.forEach((element,index)=>{
   const progress=element.contains(document.activeElement)?1:Math.max(0,Math.min(1,(window.innerHeight*.78-positions[index])/(window.innerHeight*.6)))
   const eased=1-Math.pow(1-progress,2)
   element.dataset.revealed='true'
   element.style.setProperty('--scroll-reveal-y',`${(1-eased)*76}px`)
   element.style.setProperty('--scroll-reveal-opacity',String(.12+.88*eased))
   element.style.setProperty('--scroll-reveal-clip',`${(1-eased)*100}%`)
  })
 }
 const scroll=()=>{if(!frame&&!preference?.matches)frame=requestAnimationFrame(update)}
 const preferenceChanged=()=>{if(preference?.matches)revealAll();else scroll()}
 onMounted(async()=>{
  await nextTick();if(disposed||!root.value)return
  preference=window.matchMedia('(prefers-reduced-motion: reduce)')
  if(!('IntersectionObserver' in window))return
  observer=new IntersectionObserver(entries=>{for(const entry of entries){const element=entry.target as HTMLElement;if(entry.isIntersecting)element.dataset.revealed='true';else if(entry.boundingClientRect.top>=window.innerHeight&&!element.contains(document.activeElement))element.dataset.revealed='false'}},{threshold:0,rootMargin:'0px 0px -18% 0px'})
  const groups:[string,string][]=[
   ['.company-main-photo,.company-detail-photo','image'],
   ['.company-introduction,.company-machining,.company-capability-copy,.company-section-heading,.company-news-heading','copy'],
   ['.company-service-links>a,.company-product-grid>a,.company-news-list>article','item'],
  ]
  for(const [selector,kind]of groups){
   let index=0
   for(const element of root.value.querySelectorAll<HTMLElement>(selector)){
    targets.push(element);element.dataset.reveal=kind
    element.style.setProperty('--reveal-delay',kind==='item'?`${(index++%3)*110}ms`:kind==='copy'?'100ms':'0ms')
    const rect=element.getBoundingClientRect()
    element.dataset.revealed=preference.matches||rect.top<window.innerHeight*.55?'true':'false'
   }
  }
  // Keyboard navigation must never land on a visually concealed item.
  // Commit starting poses before enabling transitions; avoid animating into hiding on load.
  readyFrame=requestAnimationFrame(()=>{readyFrame=requestAnimationFrame(()=>{if(disposed||!root.value)return;root.value.dataset.motionReady='true';if(!preference?.matches)for(const element of targets)observer?.observe(element)})})
  root.value.addEventListener('focusin',revealFocused)
  preference.addEventListener('change',preferenceChanged)
  window.addEventListener('scroll',scroll,{passive:true});window.addEventListener('resize',scroll,{passive:true});update()
 })
 onBeforeUnmount(()=>{disposed=true;observer?.disconnect();cancelAnimationFrame(frame);cancelAnimationFrame(readyFrame);window.removeEventListener('scroll',scroll);window.removeEventListener('resize',scroll);preference?.removeEventListener('change',preferenceChanged);root.value?.removeEventListener('focusin',revealFocused)})
 return root
}
