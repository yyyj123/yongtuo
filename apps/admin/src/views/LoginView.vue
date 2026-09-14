<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../features/auth/client'
const username = ref(''), password = ref(''), error = ref(''), busy = ref(false)
const route = useRoute(), router = useRouter()
async function submit() {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    await api.login(username.value, password.value)
    const redirect = String(route?.query.redirect || '/dashboard')
    await router.replace(redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.startsWith('/login') ? redirect : '/dashboard')
  } catch (e) { error.value = (e as Error).message }
  finally { busy.value = false; password.value = '' }
}
</script>
<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="brand">YONGTUO <span>勇拓五金实业</span></div>
      <h1 id="login-title">登录内容管理后台</h1>
      <p class="muted">维护产品资料，让每一次更新准确发布。</p>
      <form aria-label="管理员登录" @submit.prevent="submit">
        <label for="username">用户名</label><el-input id="username" name="username" v-model="username" autocomplete="username" required />
        <label for="password">密码</label><el-input id="password" name="password" type="password" v-model="password" autocomplete="current-password" required />
        <p v-if="error" role="alert" class="error">{{ error }}</p>
        <el-button class="login-submit" native-type="submit" type="primary" :loading="busy">登录</el-button>
      </form>
      <p class="muted small">仅限授权管理员使用</p>
    </section>
  </main>
</template>
