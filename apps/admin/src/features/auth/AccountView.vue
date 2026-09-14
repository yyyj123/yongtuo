<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from './client'
const router = useRouter(), name = ref(''), currentPassword = ref(''), newPassword = ref(''), confirm = ref(''), error = ref(''), busy = ref(false)
onMounted(async () => { try { name.value = (await api.get('/auth/me')).username } catch(e) { error.value = (e as Error).message } })
async function change() {
  error.value = ''
  if (newPassword.value !== confirm.value) { error.value = '两次新密码不一致。'; return }
  busy.value = true
  try { await api.put('/auth/password', { currentPassword: currentPassword.value, newPassword: newPassword.value }); api.clear(); await router.replace('/login') }
  catch(e) { error.value = (e as Error).message } finally { busy.value = false }
}
</script>
<template><section><h1>账号与密码</h1><p>当前账号：{{ name || '正在加载' }}</p><form class="narrow form-stack" @submit.prevent="change">
<label>当前密码<el-input v-model="currentPassword" type="password" autocomplete="current-password" required /></label>
<label>新密码<el-input v-model="newPassword" type="password" autocomplete="new-password" minlength="12" maxlength="72" required /></label>
<label>再次输入新密码<el-input v-model="confirm" type="password" autocomplete="new-password" required /></label>
<p class="muted">至少 12 个字符。修改后所有旧会话失效，需要重新登录。</p><p v-if="error" role="alert" class="error">{{ error }}</p>
<el-button native-type="submit" type="primary" :loading="busy">修改密码并重新登录</el-button></form></section></template>
