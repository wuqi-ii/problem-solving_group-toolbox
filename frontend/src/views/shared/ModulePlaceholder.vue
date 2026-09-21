<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '功能模块'))
const description = computed(() => String(route.meta.description ?? '该模块等待实现。'))
</script>

<template>
  <section class="placeholder-card">
    <div class="placeholder-index">{{ title.slice(0, 1) }}</div>
    <span class="eyebrow">业务分区入口</span>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <el-alert
      title="当前页面是可替换的模块骨架"
      description="开发时保留路由和布局，在本目录下拆分列表、详情、表单与权限组件。"
      type="info"
      :closable="false"
      show-icon
    />
  </section>
</template>
