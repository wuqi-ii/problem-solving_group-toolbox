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
  { title: '小组与权限', status: '核心流程可用', tone: 'blue' },
  { title: '任务协作', status: '发布、提交与审核可用', tone: 'amber' },
  { title: '文件工具', status: '资料库、中转与设备同步可用', tone: 'violet' },
  { title: '通知与工作台', status: '任务通知与已读状态可用', tone: 'green' },
]
</script>

<template>
  <section class="hero-card">
    <div>
      <span class="eyebrow">核心协作流程已经打通</span>
      <h2>从组队、授权到任务验收</h2>
      <p>账号、小组、权限、任务、共享文件、临时取件、跨设备传递和通知已经组成可完整演示的第一版。</p>
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
      <span class="eyebrow">MVP 已完成</span>
      <h3>后续进入稳定性增强</h3>
    </div>
    <ol>
      <li>补充成员移除、组长转让和邀请码管理。</li>
      <li>补充容量配额、回收站和上传分片能力。</li>
      <li>进行性能、安全与部署环境专项测试。</li>
    </ol>
  </section>
</template>
