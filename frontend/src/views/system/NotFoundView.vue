<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const authenticated = computed(() => Boolean(localStorage.getItem('team-toolbox-token')))
</script>

<template>
  <main class="not-found-page">
    <section class="not-found-card">
      <span class="eyebrow">404 · PAGE NOT FOUND</span>
      <h1>这个页面不存在</h1>
      <p>地址可能已失效，或者页面已经移动。你可以返回系统继续操作。</p>
      <el-button type="primary" @click="router.replace(authenticated ? '/' : '/login')">
        {{ authenticated ? '返回工作台' : '返回登录' }}
      </el-button>
    </section>
  </main>
</template>
