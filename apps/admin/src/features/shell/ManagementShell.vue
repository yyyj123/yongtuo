<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../auth/client'
const open = ref(false), router = useRouter()
const groups = [
 {name:'控制台',items:[['工作概览','/dashboard']]},
 {name:'产品管理',items:[['产品列表','/products'],['分类管理','/categories'],['参数配置','/attributes'],['Excel 导入','/import'],['批量图片','/batch-images']]},
 {name:'内容管理',items:[['新闻文章','/articles'],['应用案例','/cases']]},
 {name:'资料管理',items:[['资质证书','/certificates'],['产品目录','/catalogs']]},
 {name:'网站设置',items:[['首页内容','/home'],['CNC 与生产能力','/pages'],['公司与 SEO','/site'],['联系方式','/contact']]},
 {name:'系统设置',items:[['操作记录','/logs'],['账号与密码','/account']]}
]
async function logout() { try { await api.logout() } finally { await router.replace('/login') } }
</script>
<template><div class="workspace">
<a href="#workspace-main" class="skip-link">跳转到主内容</a>
<header class="mobile-bar"><strong>YONGTUO</strong><button @click="open=!open" :aria-expanded="open" aria-controls="admin-navigation">{{ open ? '收起导航' : '打开导航' }}</button></header>
<aside id="admin-navigation" class="sidebar" :class="{open}"><div class="brand">YONGTUO<span>勇拓五金实业 · 内容管理</span></div>
<nav aria-label="后台导航"><section v-for="group in groups" :key="group.name"><h2>{{ group.name }}</h2><RouterLink v-for="[label,path] in group.items" :key="path" :to="path" @click="open=false">{{ label }}</RouterLink></section></nav>
<button class="logout" @click="logout">退出登录</button></aside>
<main id="workspace-main" class="workspace-main" tabindex="-1"><RouterView /></main></div></template>
