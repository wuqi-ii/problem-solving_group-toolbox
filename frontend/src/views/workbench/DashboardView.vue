<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { getSystemHealth } from '@/api/system'

const backendStatus = ref<'checking' | 'up' | 'down'>('checking')

onMounted(async () => {
  try {
    const health = await getSystemHealth()
    backendStatus.value = health.status === 'UP' ? 'up' : 'down'
  } catch {
    backendStatus.value = 'down'
  }
})

const modules = [
  { title: '小组与权限', status: '骨架已建立', tone: 'blue' },
  { title: '任务协作', status: '等待业务实现', tone: 'amber' },
  { title: '文件工具', status: '等待业务实现', tone: 'violet' },
  { title: '通知与工作台', status: '基础页面可用', tone: 'green' },
]
</script>

<template>
  <section class="hero-card">
    <div>
      <span class="eyebrow">项目基础已经就绪</span>
      <h2>从同一套框架开始协作开发</h2>
      <p>前端路由、后端分层、统一响应、权限入口、数据库迁移和业务分区已经建立。</p>
    </div>
    <div class="health-pill" :class="backendStatus">
      <span class="health-dot" />
      <span v-if="backendStatus === 'checking'">正在检查后端</span>
      <span v-else-if="backendStatus === 'up'">后端服务正常</span>
      <span v-else>后端尚未启动</span>
    </div>
  </section>

  <section class="module-grid">
    <article v-for="item in modules" :key="item.title" class="module-card">
      <div class="module-icon" :class="item.tone" />
      <h3>{{ item.title }}</h3>
      <p>{{ item.status }}</p>
    </article>
  </section>

  <section class="next-card">
    <div>
      <span class="eyebrow">下一阶段</span>
      <h3>优先完成账号、小组和统一权限</h3>
    </div>
    <ol>
      <li>实现注册、登录、退出和会话刷新。</li>
      <li>实现创建小组、邀请成员和组长转让。</li>
      <li>让任务区和文件区共同使用服务端权限组件。</li>
    </ol>
  </section>
</template>
