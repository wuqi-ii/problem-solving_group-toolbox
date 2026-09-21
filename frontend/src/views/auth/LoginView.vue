<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

import { login, register } from '@/api/auth'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const mode = ref<'login' | 'register'>('login')
const submitting = ref(false)
const form = reactive({ account: '', password: '', nickname: '' })

async function submit() {
  if (!form.account || !form.password || (mode.value === 'register' && !form.nickname)) {
    ElMessage.warning('请完整填写表单')
    return
  }
  submitting.value = true
  try {
    const auth = mode.value === 'login'
      ? await login(form.account, form.password)
      : await register(form.account, form.password, form.nickname)
    session.setAuth(auth.accessToken, auth.user)
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    await router.replace(String(route.query.redirect ?? '/'))
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message ?? '暂时无法完成操作')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-intro">
      <div class="brand-mark large">协</div>
      <span class="eyebrow">TEAM TOOLBOX</span>
      <h1>让小组协作从分工开始，<br>到成果归档结束。</h1>
      <p>成员、权限、任务和文件使用同一套协作空间，减少群聊中反复确认和资料丢失。</p>
    </section>
    <el-card class="auth-card" shadow="never">
      <el-segmented v-model="mode" :options="[{ label: '登录', value: 'login' }, { label: '注册', value: 'register' }]" />
      <div class="auth-heading">
        <h2>{{ mode === 'login' ? '欢迎回来' : '创建你的账号' }}</h2>
        <p>{{ mode === 'login' ? '登录后继续管理你的小组。' : '账号支持字母、数字和下划线。' }}</p>
      </div>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="form.account" maxlength="32" placeholder="至少 4 位" autofocus />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="昵称">
          <el-input v-model="form.nickname" maxlength="30" placeholder="组员看到的名称" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 8 位" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="submitting" class="submit-button" @click="submit">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </el-button>
      </el-form>
    </el-card>
  </main>
</template>
