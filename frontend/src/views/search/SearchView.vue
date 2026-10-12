<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

import { listGroups, type GroupView } from '@/api/groups'
import { listTasks } from '@/api/tasks'
import { listFiles } from '@/api/files'
import { listAnnouncements, listDiscussions } from '@/api/collaboration'
import { useSessionStore } from '@/stores/session'

interface SearchItem { id: string; type: '任务' | '文件' | '讨论' | '公告'; title: string; detail: string; groupId: string; path: string }
const router = useRouter()
const session = useSessionStore()
const groups = ref<GroupView[]>([])
const items = ref<SearchItem[]>([])
const keyword = ref('')
const typeFilter = ref('')
const loading = ref(false)
const results = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return items.value.filter(item => (!typeFilter.value || item.type === typeFilter.value)
    && (!query || `${item.title} ${item.detail}`.toLowerCase().includes(query)))
})
function groupName(id: string) { return groups.value.find(group => group.id === id)?.name ?? '未知小组' }

onMounted(async () => {
  loading.value = true
  try {
    groups.value = await listGroups()
    const bundles = await Promise.all(groups.value.map(async group => {
      const [tasks, files, discussions, announcements] = await Promise.all([
        listTasks(group.id), listFiles(group.id), listDiscussions(group.id), listAnnouncements(group.id),
      ])
      return [
        ...tasks.map(task => ({ id: task.id, type: '任务' as const, title: task.title, detail: task.description ?? '', groupId: group.id, path: '/tasks' })),
        ...files.map(file => ({ id: file.id, type: '文件' as const, title: file.name, detail: `v${file.versionNo}`, groupId: group.id, path: '/files' })),
        ...discussions.map(post => ({ id: post.id, type: '讨论' as const, title: post.content.slice(0, 60), detail: post.authorName, groupId: group.id, path: '/collaboration' })),
        ...announcements.map(post => ({ id: post.id, type: '公告' as const, title: post.content.slice(0, 60), detail: post.authorName, groupId: group.id, path: '/collaboration' })),
      ]
    }))
    items.value = bundles.flat()
  } catch { ElMessage.error('统一搜索数据加载失败') }
  finally { loading.value = false }
})

async function open(item: SearchItem) {
  session.switchGroup(item.groupId)
  await router.push(item.path)
}
</script>

<template>
  <section class="page-heading"><div><span class="eyebrow">UNIFIED SEARCH</span><h2>统一搜索</h2><p>只搜索当前账号有权访问的小组内容。</p></div></section>
  <div class="search-toolbar"><el-input v-model="keyword" size="large" clearable :prefix-icon="Search" placeholder="搜索任务、文件、讨论或公告" aria-label="统一搜索关键词" /><el-select v-model="typeFilter" clearable placeholder="全部类型" aria-label="搜索类型筛选"><el-option v-for="type in ['任务','文件','讨论','公告']" :key="type" :label="type" :value="type" /></el-select></div>
  <el-skeleton v-if="loading" :rows="6" animated />
  <el-empty v-else-if="results.length === 0" description="没有找到匹配内容" />
  <section v-else class="search-results">
    <button v-for="item in results" :key="`${item.type}-${item.id}`" :aria-label="`打开${item.type}：${item.title}`" @click="open(item)"><el-tag effect="plain">{{ item.type }}</el-tag><div><strong>{{ item.title }}</strong><span>{{ groupName(item.groupId) }} · {{ item.detail || '无补充说明' }}</span></div></button>
  </section>
</template>
