<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Promotion, Top } from '@element-plus/icons-vue'

import { listGroups, type GroupView } from '@/api/groups'
import {
  createAnnouncement, createDiscussion, deletePost, listAnnouncements, listDiscussions, setPostPinned,
  type CollaborationPostView,
} from '@/api/collaboration'
import { useSessionStore } from '@/stores/session'
import { apiErrorMessage, isDialogDismissal } from '@/api/errors'

const session = useSessionStore()
const groups = ref<GroupView[]>([])
const route = useRoute()
const linkedGroupId = computed(() => typeof route.query.group === 'string' ? route.query.group : '')
const linkedItemId = computed(() => typeof route.query.item === 'string' ? route.query.item : '')
const linkedGroupUnavailable = computed(() => Boolean(linkedGroupId.value && !groups.value.some(group => group.id === linkedGroupId.value)))
const focusedId = computed(() => linkedGroupId.value === activeGroupId.value ? linkedItemId.value : '')
function selectLinkedGroup() {
  if (linkedGroupId.value && !linkedGroupUnavailable.value) activeGroupId.value = linkedGroupId.value
}
watch(linkedGroupId, selectLinkedGroup)

const discussions = ref<CollaborationPostView[]>([])
const announcements = ref<CollaborationPostView[]>([])
const activeTab = ref<'discussion' | 'announcement'>('discussion')
const content = ref('')
const loading = ref(false)
const initializing = ref(true)
const submitting = ref(false)
const activeGroupId = computed({
  get: () => session.activeGroupId ?? '',
  set: (value: string) => session.switchGroup(value || null),
})
const activeGroup = computed(() => groups.value.find(group => group.id === activeGroupId.value))
const canAnnounce = computed(() => activeGroup.value?.myRole === 'LEADER'
  || activeGroup.value?.myPermissions.includes('GROUP_ANNOUNCE'))
const visiblePosts = computed(() => {
  const posts = activeTab.value === 'discussion' ? discussions.value : announcements.value
  return focusedId.value ? posts.filter(post => post.id === focusedId.value) : posts
})
watch(() => route.query.tab, tab => {
  if (tab === 'discussion' || tab === 'announcement') activeTab.value = tab
}, { immediate: true })

async function initialize() {
  try {
    groups.value = await listGroups()
    selectLinkedGroup()
    if (!groups.value.some(group => group.id === activeGroupId.value)) {
      activeGroupId.value = groups.value[0]?.id ?? ''
    } else await loadPosts(activeGroupId.value)
  } catch { ElMessage.error('协作空间加载失败') }
  finally { initializing.value = false }
}

let loadVersion = 0

async function loadPosts(groupId: string) {
  if (groupId !== activeGroupId.value) return
  const version = ++loadVersion
  loading.value = Boolean(groupId)
  if (!groupId) return
  try {
    const result = await Promise.all([listDiscussions(groupId), listAnnouncements(groupId)])
    if (version !== loadVersion || groupId !== activeGroupId.value) return
    discussions.value = result[0]
    announcements.value = result[1]
  } catch {
    if (version === loadVersion && groupId === activeGroupId.value) ElMessage.error('讨论和公告加载失败')
  } finally {
    if (version === loadVersion && groupId === activeGroupId.value) loading.value = false
  }
}

async function publish() {
  if (!content.value.trim()) return ElMessage.warning('请输入内容')
  if (activeTab.value === 'announcement' && !canAnnounce.value) return
  const groupId = activeGroupId.value
  const tab = activeTab.value
  const draft = content.value
  submitting.value = true
  try {
    if (tab === 'discussion') await createDiscussion(groupId, draft)
    else await createAnnouncement(groupId, draft)
    if (activeGroupId.value === groupId && activeTab.value === tab && content.value === draft) content.value = ''
    await loadPosts(groupId)
    ElMessage.success(tab === 'discussion' ? '讨论已发布' : '公告已发布')
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '发布失败')) }
  finally { submitting.value = false }
}

async function togglePin(post: CollaborationPostView) {
  try {
    await setPostPinned(post.id, !post.pinned)
    await loadPosts(activeGroupId.value)
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '操作失败')) }
}

async function remove(post: CollaborationPostView) {
  try {
    await ElMessageBox.confirm('删除后其他成员将无法查看，确定继续吗？', '删除协作内容', { type: 'warning' })
    await deletePost(post.id)
    await loadPosts(activeGroupId.value)
    ElMessage.success('内容已删除')
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '删除失败'))
  }
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

watch(activeGroupId, groupId => {
  discussions.value = []
  announcements.value = []
  content.value = ''
  void loadPosts(groupId)
}, { flush: 'sync' })
watch(activeTab, () => { content.value = '' })
onMounted(initialize)
</script>

<template>
  <section class="page-heading">
    <div><span class="eyebrow">TEAM COLLABORATION</span><h2>讨论与公告</h2><p>集中记录小组讨论、决定和重要通知，后续 AI 总结只处理明确授权的内容。</p></div>
  </section>

  <el-alert v-if="!initializing && linkedGroupUnavailable" title="链接对应的小组已无法访问" type="warning" :closable="false" />
  <router-link v-if="linkedItemId" :to="$route.path">返回完整列表</router-link>
  <el-skeleton v-if="initializing" :rows="5" animated />
  <el-empty v-else-if="groups.length === 0" description="请先创建或加入小组" />
  <template v-else>
    <el-tabs v-model="activeTab" class="collaboration-tabs">
      <el-tab-pane label="小组讨论" name="discussion" />
      <el-tab-pane label="公告" name="announcement" />
    </el-tabs>

    <section v-if="activeTab === 'discussion' || canAnnounce" class="collaboration-editor">
      <el-input v-model="content" type="textarea" :rows="4" maxlength="2000" show-word-limit
        :placeholder="activeTab === 'discussion' ? '分享进展、问题或需要讨论的事项' : '发布需要所有成员关注的公告'"
        :aria-label="activeTab === 'discussion' ? '讨论内容' : '公告内容'" />
      <div><span>{{ activeTab === 'announcement' ? '公告发布后会通知全部小组成员' : '小组成员均可参与讨论' }}</span><el-button type="primary" :icon="Promotion" :loading="submitting" @click="publish">发布到 {{ activeGroup?.name }}</el-button></div>
    </section>
    <el-alert v-else type="info" :closable="false" title="你可以查看公告，但没有公告发布权限。" />

    <el-skeleton v-if="loading" :rows="5" animated />
    <el-empty v-else-if="visiblePosts.length === 0" :description="focusedId ? '该内容不存在或已无法访问' : activeTab === 'discussion' ? '还没有讨论内容' : '还没有公告'" />
    <section v-else class="collaboration-list">
      <article v-for="post in visiblePosts" :key="post.id" class="collaboration-post" :class="{ pinned: post.pinned }">
        <header><div><router-link :to="{ name: 'member-profile', params: { groupId: post.groupId, userId: post.createdBy } }">{{ post.authorName }}</router-link><span>{{ formatTime(post.createdAt) }}</span></div><el-tag v-if="post.pinned" type="warning" effect="light">置顶</el-tag></header>
        <p>{{ post.content }}</p>
        <footer>
          <el-button v-if="post.canPin" text :icon="Top" @click="togglePin(post)">{{ post.pinned ? '取消置顶' : '置顶' }}</el-button>
          <el-button v-if="post.canDelete" text type="danger" :icon="Delete" @click="remove(post)">删除</el-button>
        </footer>
      </article>
    </section>
  </template>
</template>
