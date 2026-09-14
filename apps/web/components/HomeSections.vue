<script setup lang="ts">
import {computed,ref} from 'vue'
import {homeMedia} from '../utils/home-media'
import {localPath,safeUrl} from '../utils/site'
const props=defineProps<{home:Record<string,any>;locale:string}>()
const t=(zh:string,en:string)=>props.locale==='en'?en:zh,link=(path:string)=>localPath(path,props.locale)
const categories=computed(()=>(props.home.categories||[]).filter((c:any)=>c.name||c.title))
const heroMedia=computed(()=>homeMedia(props.home.hero?.imageUrl,'hero'))
const companyMedia=computed(()=>homeMedia(props.home.about?.imageUrl||props.home.capabilities?.imageUrl,'workshop'))
const cncMedia=computed(()=>homeMedia(props.home.cnc?.imageUrl,'machining'))
const categoryTrack=ref<HTMLElement>()
const homeMotion=useHomeMotion()
function slide(direction:number){const track=categoryTrack.value;if(track)track.scrollBy({left:direction*track.clientWidth,behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'})}
const entrances=[{path:'/capabilities',zh:'生产能力',en:'Capabilities',descriptionZh:'了解加工设备与制造工艺',descriptionEn:'Explore equipment and machining processes'},{path:'/cases',zh:'应用案例',en:'Applications',descriptionZh:'查看产品应用与案例资料',descriptionEn:'Browse product applications and case information'},{path:'/certificates',zh:'资质与质量',en:'Quality',descriptionZh:'查阅已公开的质量与资质资料',descriptionEn:'View published quality information and certificates'}]
</script>
<template>
<div ref="homeMotion" class="company-home">
  <section class="company-hero" data-home-section="hero">
    <SiteImage class="company-hero-image" :src="heroMedia.src" :alt="home.hero?.title||''" width="1920" height="1080" loading="eager" fetchpriority="high"/>
    <div class="wrap company-hero-copy">
      <div class="hero-word" aria-hidden="true">YONGTUO</div>
      <h1>{{home.hero?.title||home.site?.company_name||'YONGTUO'}}</h1>
      <p v-if="home.hero?.subtitle">{{home.hero.subtitle}}</p>
      <div v-if="home.hero?.content" class="prose" v-html="home.hero.content"/>
      <NuxtLink class="company-action light-action" :to="link('/products')">{{t('浏览产品','Explore products')}}<svg class="link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></NuxtLink>
    </div>
    
    <nav class="home-section-nav" :aria-label="t('首页内容导航','Home sections')"><a href="#home-about">{{t('公司简介','Company profile')}}</a><a href="#home-manufacturing">{{t('加工与制造','Manufacturing')}}</a><a href="#home-products">{{t('产品中心','Products')}}</a></nav>
  </section>

  <section id="home-about" class="company-story" data-home-section="about">
    <div class="wrap company-collage">
      <figure class="company-main-photo"><SiteImage :src="companyMedia.src" :alt="home.about?.title||t('公司简介','Company profile')" width="1000" height="800"/></figure>
      <div class="company-introduction"><h2><span class="company-outline" aria-hidden="true">ABOUT US</span>{{home.about?.title||t('关于勇拓','About YONGTUO')}}</h2><p v-if="home.about?.subtitle" class="lead">{{home.about.subtitle}}</p><div v-if="home.about?.content" class="prose" v-html="home.about.content"/><p v-else>{{t('公司介绍正在整理，欢迎查看产品或联系我们。','Our company profile is being prepared. Browse our products or contact us.')}}</p><NuxtLink class="company-text-link" :to="link('/about')">{{t('了解公司详情','More about YONGTUO')}}<svg class="link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></NuxtLink></div>
      <div id="home-manufacturing" class="company-machining" data-home-section="cnc"><h2>{{home.cnc?.title||t('CNC 定制加工','CNC machining')}}</h2><p v-if="home.cnc?.subtitle">{{home.cnc.subtitle}}</p><div v-if="home.cnc?.content" class="prose" v-html="home.cnc.content"/><p v-else>{{t('查看定制加工服务，沟通图纸与样品需求。','Explore machining services and discuss your drawing and sample requirements.')}}</p><NuxtLink class="company-action light-action" :to="link('/cnc-machining')">{{t('了解加工服务','Explore machining')}}<svg class="link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></NuxtLink></div>
      <figure class="company-detail-photo"><SiteImage :src="cncMedia.src" :alt="home.cnc?.title||t('CNC 定制加工','CNC machining')" width="1000" height="700"/></figure>
    </div>
  </section>

  <section class="company-services" data-home-section="capabilities"><div class="wrap">
    <div v-if="home.capabilities?.content||home.capabilities?.subtitle" class="company-capability-copy"><h2>{{home.capabilities.title||t('生产能力','Capabilities')}}</h2><p v-if="home.capabilities.subtitle">{{home.capabilities.subtitle}}</p><div v-if="home.capabilities.content" class="prose" v-html="home.capabilities.content"/></div>
    <div class="company-service-links"><NuxtLink v-for="(entry,index) in entrances" :key="entry.path" :to="link(entry.path)"><svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><template v-if="index===0"><path d="M7 39V21l11 6V16l11 6V9h10v30ZM13 33h4m6 0h4m6 0h2"/></template><template v-else-if="index===1"><rect x="8" y="13" width="32" height="26" rx="2"/><path d="M18 13V8h12v5M8 23h32M20 23v5h8v-5"/></template><template v-else><circle cx="24" cy="20" r="12"/><path d="m18 20 4 4 8-9M16 30l-3 11 11-5 11 5-3-11"/></template></svg><div><h3>{{t(entry.zh,entry.en)}}</h3><p>{{t(entry.descriptionZh,entry.descriptionEn)}}</p></div><span class="service-arrow" aria-hidden="true">↗</span></NuxtLink></div>
  </div></section>

  <section id="home-products" class="company-products" data-home-section="products"><div class="wrap">
    <div class="company-section-heading"><h2>{{t('产品中心','Products')}}<span aria-hidden="true">PRODUCTS</span></h2><NuxtLink class="company-text-link" :to="link('/products')">{{t('查看全部产品','All products')}}<svg class="link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></NuxtLink></div>
    <div v-if="categories.length" class="company-categories"><div ref="categoryTrack" class="company-category-track" tabindex="0" :aria-label="t('产品分类，可横向滚动','Product categories, scroll horizontally')"><NuxtLink v-for="c in categories" :key="c.slug" :to="link('/categories/'+c.slug)"><SiteImage v-if="safeUrl(c.coverImage)" :src="c.coverImage" :alt="c.name||c.title" width="400" height="300"/><h3>{{c.name||c.title}}</h3></NuxtLink></div></div>
    <div v-if="home.featuredProducts?.length" class="company-product-grid"><NuxtLink v-for="p in home.featuredProducts.slice(0,8)" :key="p.slug" :to="link('/products/'+p.slug)" class="company-product"><div class="company-product-image"><SiteImage v-if="safeUrl(p.coverImage)" :src="p.coverImage" :alt="p.name||p.title" width="600" height="450"/><span v-else>{{t('暂无产品图片','Image unavailable')}}</span></div><div class="company-product-info"><h3>{{p.name||p.title}}</h3><p v-if="p.summary">{{p.summary}}</p><span class="company-product-detail">{{t('查看产品','View product')}}<svg class="link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></span></div></NuxtLink></div>
    <div v-else-if="!categories.length" class="company-products-empty"><figure><SiteImage src="/images/ai-preview/products.png" :alt="t('五金产品组合','Hardware product assortment')" width="1200" height="900"/></figure><p>{{t('产品资料正在整理，欢迎与我们沟通您的需求。','Product information is being prepared. Contact us to discuss your requirements.')}}</p></div>
  </div></section>

  <section class="company-news" data-home-section="articles"><div class="wrap company-news-layout"><div class="company-news-heading"><h2>{{t('新闻资讯','News & insights')}}<span aria-hidden="true">NEWS</span></h2><NuxtLink class="company-action" :to="link('/articles')">{{t('查看全部资讯','All insights')}}<svg class="link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></NuxtLink></div><div class="company-news-list"><article v-for="item in (home.featuredArticles||[]).slice(0,3)" :key="item.slug||item.id"><NuxtLink :to="link('/articles/'+item.slug)"><SiteImage v-if="safeUrl(item.coverImage)" :src="item.coverImage" :alt="item.title||item.name" width="320" height="220"/><div><h3>{{item.title||item.name}}</h3><p v-if="item.summary">{{item.summary}}</p></div><svg class="link-arrow" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></NuxtLink></article><p v-if="!home.featuredArticles?.length">{{t('暂无已发布的新闻与技术文章。','No news or insights have been published yet.')}}</p></div></div></section>
</div>
</template>
<style scoped>
.company-hero{overflow:hidden}
.company-hero-image{transform:translateY(var(--hero-drift,0px)) scale(1.09);transform-origin:center center}
.company-hero-copy .hero-word{animation:hero-letter-reveal 1.1s cubic-bezier(.16,1,.3,1) both}
.company-hero-copy h1,.company-hero-copy>p,.company-hero-copy>.prose,.company-hero-copy>.company-action{animation:hero-copy-reveal .85s cubic-bezier(.16,1,.3,1) .12s both}
[data-reveal]{transition:transform 1.05s cubic-bezier(.16,1,.3,1),opacity .7s ease,clip-path 1.15s cubic-bezier(.16,1,.3,1);transition-delay:var(--reveal-delay,0ms)}
.company-home:not([data-motion-ready]) [data-reveal]{transition:none}
[data-reveal="image"]{clip-path:inset(0);transform:translateY(0)}
[data-reveal="image"][data-revealed="false"]{clip-path:inset(0 100% 0 0);transform:translateY(48px)}
[data-reveal="copy"][data-revealed="false"]{opacity:0;transform:translateY(76px)}
[data-reveal="item"][data-revealed="false"]{opacity:0;transform:translateY(56px)}
[data-revealed="true"]{opacity:1}
@keyframes hero-letter-reveal{from{clip-path:inset(0 100% 0 0);transform:translateX(-18px)}to{clip-path:inset(0);transform:translateX(0)}}
@keyframes hero-copy-reveal{from{opacity:0;transform:translateY(24px)}to{opacity:1;transform:translateY(0)}}
@media(max-width:700px){[data-reveal]{transition-duration:.6s;transition-delay:0ms}.company-hero-image{transform:scale(1.04)}}
@media(min-width:1101px){
 .company-home[data-motion-ready] [data-reveal]{opacity:var(--scroll-reveal-opacity,1);transform:translateY(var(--scroll-reveal-y,0px));transition:none}
 .company-home[data-motion-ready] [data-reveal="image"]{opacity:1;clip-path:inset(0 var(--scroll-reveal-clip,0%) 0 0)}
}
@media(prefers-reduced-motion:reduce){[data-reveal],.company-hero-image{transform:none!important;clip-path:none!important;opacity:1!important;transition:none!important}.company-hero-copy>*{animation:none!important}}
@media print{[data-reveal]{transform:none!important;clip-path:none!important;opacity:1!important}}
</style>
