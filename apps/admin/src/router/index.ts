import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import { api, requireSession } from '../features/auth/client'
const router = createRouter({ history: createWebHistory(import.meta.env.BASE_URL), routes: [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: LoginView },
  { path: '/products/:id', component: () => import('../features/products/ProductEditor.vue') },
  { path: '/batch-images', component: () => import('../features/importer/BatchImages.vue') },
  {path:'/articles',meta:{kind:'articles'},component:()=>import('../features/content/ContentList.vue')},
  {path:'/articles/:id',meta:{kind:'articles'},component:()=>import('../features/content/ContentEditor.vue')},
  {path:'/cases',meta:{kind:'cases'},component:()=>import('../features/content/ContentList.vue')},
  {path:'/cases/:id',meta:{kind:'cases'},component:()=>import('../features/content/ContentEditor.vue')},
  {path:'/certificates',meta:{kind:'certificates'},component:()=>import('../features/content/ContentList.vue')},
  {path:'/certificates/:id',meta:{kind:'certificates'},component:()=>import('../features/content/ContentEditor.vue')},
  {path:'/catalogs',meta:{kind:'catalogs'},component:()=>import('../features/content/ContentList.vue')},
  {path:'/catalogs/:id',meta:{kind:'catalogs'},component:()=>import('../features/content/ContentEditor.vue')},
  {path:'/home',component:()=>import('../features/content/HomeView.vue')},
  {path:'/pages',component:()=>import('../features/content/HomeView.vue')},
  {path:'/site',component:()=>import('../features/content/SiteView.vue')},
  {path:'/contact',component:()=>import('../features/content/ContactView.vue')},
  {path:'/logs',component:()=>import('../features/content/LogsView.vue')},
  {path:'/article-categories',component:()=>import('../features/content/ArticleCategories.vue')},
  { path: '/categories', component: () => import('../features/categories/CategoryView.vue') },
  { path: '/categories/:id/attributes', component: () => import('../features/categories/CategoryBindings.vue') },
  { path: '/attributes', component: () => import('../features/categories/AttributeView.vue') },
  { path: '/import', component: () => import('../features/importer/ImportView.vue') },
  { path: '/products', component: () => import('../features/products/ProductList.vue') },
  { path: '/dashboard', component: () => import('../views/DashboardView.vue') },
  { path: '/account', component: () => import('../features/auth/AccountView.vue') }
] })
router.beforeEach(to => to.path === '/login' ? true : requireSession(api, to.fullPath))
window.addEventListener('auth-expired', () => { if (router.currentRoute.value.path !== '/login') void router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } }) })
export default router
