<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { listGroups, type GroupView } from '@/api/groups'
import { listTasks, type TaskStatus, type TaskView } from '@/api/tasks'
import { listFiles } from '@/api/files'
import { unreadCount } from '@/api/notifications'
import { getSystemHealth } from '@/api/system'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const router = useRouter()
const backendStatus = ref<'checking' | 'up' | 'down'>('checking')
const groups = ref<GroupView[]>([])
const tasks = ref<TaskView[]>([])
const fileCount = ref(0)
const unread = ref(0)
const loading = ref(false)

const activeTasks = computed(() => tasks.value.filter(task => !['COMPLETED', 'CANCELLED'].includes(task.status)).length)
const myPendingTasks = computed(() => tasks.value.filter(task =>
  task.assignees.some(item => item.userId === session.user?.id)
  && !['COMPLETED', 'CANCELLED'].includes(task.status)).length)
const pendingReviews = computed(() => tasks.value.filter(task => task.canReview).length)
const recentTasks = computed(() => tasks.value.slice(0, 5))
const statusText: Record<TaskStatus, string> = {
  OPEN: '待开始', IN_PROGRESS: '进行中', SUBMITTED: '待审核', NEEDS_CHANGES: '需修改', COMPLETED: '已完成', CANCELLED: '已取消',
}

function groupName(groupId: string) { return groups.value.find(group => group.id === groupId)?.name ?? '未知小组' }

async function loadAll() {
  tasks.value = []
  fileCount.value = 0
  if (groups.value.length === 0) return
  loading.value = true
  try {
    const [taskGroups, fileGroups] = await Promise.all([
      Promise.all(groups.value.map(group => listTasks(group.id))),
      Promise.all(groups.value.map(group => listFiles(group.id))),
    ])
    tasks.value = taskGroups.flat()
    fileCount.value = fileGroups.reduce((sum, files) => sum + files.length, 0)
  } catch { ElMessage.error('工作台数据加载失败') }
  finally { loading.value = false }
}

onMounted(async () => {
  try {
    const [health, groupData, unreadData] = await Promise.all([getSystemHealth(), listGroups(), unreadCount()])
    backendStatus.value = health.status === 'UP' ? 'up' : 'down'
    groups.value = groupData
    if (!groups.value.some(group => group.id === session.activeGroupId)) {
      session.switchGroup(groups.value[0]?.id ?? null)
    }
    unread.value = unreadData
    await loadAll()
  } catch { backendStatus.value = 'down' }
})

</script>

<template>
  <section class="hero-card">
    <div>
      <span class="eyebrow">跨小组个人工作台</span>
      <h2>今天需要关注的协作事项</h2>
      <p v-if="groups.length">汇总你有权访问的 {{ groups.length }} 个小组中的任务、审核、文件和通知。</p>
      <p v-else>先创建或加入小组，即可查看真实协作数据。</p>
    </div>
    <div class="health-pill" :class="backendStatus">
      <span class="health-dot" />
      <span v-if="backendStatus === 'checking'">正在检查后端</span>
      <span v-else-if="backendStatus === 'up'">后端服务正常</span>
      <span v-else>后端尚未启动</span>
    </div>
  </section>

  <el-skeleton v-if="loading" :rows="4" animated class="dashboard-loading" />
  <section v-else class="dashboard-stat-grid">
    <button class="dashboard-stat" @click="router.push('/tasks')"><span>进行中任务</span><strong>{{ activeTasks }}</strong></button>
    <button class="dashboard-stat" @click="router.push('/tasks')"><span>我的待办</span><strong>{{ myPendingTasks }}</strong></button>
    <button class="dashboard-stat" @click="router.push('/tasks')"><span>等待我审核</span><strong>{{ pendingReviews }}</strong></button>
    <button class="dashboard-stat" @click="router.push('/files')"><span>资料库文件</span><strong>{{ fileCount }}</strong></button>
    <button class="dashboard-stat" @click="router.push('/notifications')"><span>未读通知</span><strong>{{ unread }}</strong></button>
  </section>

  <section class="dashboard-recent">
    <div class="dashboard-section-head"><div><span class="eyebrow">RECENT TASKS</span><h3>最近任务</h3></div><el-button text type="primary" @click="router.push('/tasks')">查看全部</el-button></div>
    <el-empty v-if="groups.length === 0" description="请先创建或加入小组" />
    <el-empty v-else-if="recentTasks.length === 0" description="当前小组还没有任务" />
    <div v-else class="dashboard-task-list">
      <button v-for="task in recentTasks" :key="task.id" @click="router.push({ path: '/tasks', query: { group: task.groupId, item: task.id } })">
        <div><strong>{{ task.title }}</strong><span>{{ groupName(task.groupId) }} · {{ task.assignees.map(item => item.nickname).join('、') || '未指派' }}</span></div>
        <el-tag :type="task.overdue ? 'danger' : 'info'">{{ task.overdue ? '已逾期' : statusText[task.status] }}</el-tag>
      </button>
    </div>
  </section>
</template>
