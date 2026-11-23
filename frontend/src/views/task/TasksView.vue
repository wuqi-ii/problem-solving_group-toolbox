<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Check, Delete, Edit, Plus, Upload } from '@element-plus/icons-vue'

import { listGroups, listMembers, type GroupView, type MemberView } from '@/api/groups'
import { downloadFile, listFiles, type FileView } from '@/api/files'
import { useSessionStore } from '@/stores/session'
import { apiErrorMessage, isDialogDismissal } from '@/api/errors'
import { createTaskComment, deletePost, listTaskComments, type CollaborationPostView } from '@/api/collaboration'
import {
  cancelTask, createTask, listTasks, reviewTask, startTask, submitTask, updateTask,
  type ReviewDecision, type TaskPriority, type TaskStatus, type TaskSubmission, type TaskView,
} from '@/api/tasks'

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

const members = ref<MemberView[]>([])
const tasks = ref<TaskView[]>([])
const files = ref<FileView[]>([])
const session = useSessionStore()
const activeGroupId = computed({
  get: () => session.activeGroupId ?? '',
  set: (value: string) => session.switchGroup(value || null),
})
const loading = ref(false)
const initializing = ref(true)
const submitting = ref(false)
const createVisible = ref(false)
const submitVisible = ref(false)
const reviewVisible = ref(false)
const commentsVisible = ref(false)
const comments = ref<CollaborationPostView[]>([])
const commentContent = ref('')
const activeTask = ref<TaskView | null>(null)
const editingTask = ref<TaskView | null>(null)
const keyword = ref('')
const statusFilter = ref<TaskStatus | ''>('')

const createForm = reactive({
  title: '', description: '', priority: 'NORMAL' as TaskPriority, dueAt: '' as string | Date, assigneeIds: [] as string[],
})
const submitForm = reactive({ content: '', attachmentFileId: '' })
const reviewForm = reactive({ decision: 'APPROVED' as ReviewDecision, comment: '' })

const statusText: Record<TaskStatus, string> = {
  OPEN: '待开始', IN_PROGRESS: '进行中', SUBMITTED: '待审核', NEEDS_CHANGES: '需修改', COMPLETED: '已完成', CANCELLED: '已取消',
}
const statusType: Record<TaskStatus, 'info' | 'primary' | 'warning' | 'danger' | 'success'> = {
  OPEN: 'info', IN_PROGRESS: 'primary', SUBMITTED: 'warning', NEEDS_CHANGES: 'danger', COMPLETED: 'success', CANCELLED: 'info',
}
const priorityText: Record<TaskPriority, string> = { LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' }
const activeGroup = computed(() => groups.value.find((group) => group.id === activeGroupId.value))
const canCreateTask = computed(() => activeGroup.value?.myRole === 'LEADER'
  || activeGroup.value?.myPermissions.includes('TASK_CREATE'))
const visibleTasks = computed(() => tasks.value.filter((task) => {
  if (focusedId.value) return task.id === focusedId.value
  const matchesStatus = !statusFilter.value || task.status === statusFilter.value
  const query = keyword.value.trim().toLowerCase()
  return matchesStatus && (!query || task.title.toLowerCase().includes(query) || task.description?.toLowerCase().includes(query))
}))

async function initialize() {
  try {
    groups.value = await listGroups()
    selectLinkedGroup()
    if (!groups.value.some(group => group.id === activeGroupId.value)) {
      activeGroupId.value = groups.value[0]?.id ?? ''
    } else if (activeGroupId.value) {
      await loadGroupData(activeGroupId.value)
    }
  } catch { ElMessage.error('小组列表加载失败') }
  finally { initializing.value = false }
}

let loadVersion = 0

async function loadGroupData(groupId: string) {
  if (groupId !== activeGroupId.value) return
  const version = ++loadVersion
  loading.value = Boolean(groupId)
  if (!groupId) return
  try {
    const result = await Promise.all([listTasks(groupId), listMembers(groupId), listFiles(groupId)])
    if (version !== loadVersion || groupId !== activeGroupId.value) return
    tasks.value = result[0]
    members.value = result[1]
    files.value = result[2]
  } catch {
    if (version === loadVersion && groupId === activeGroupId.value) ElMessage.error('任务数据加载失败')
  } finally {
    if (version === loadVersion && groupId === activeGroupId.value) loading.value = false
  }
}

async function submitCreate() {
  if (!createForm.title.trim() || createForm.assigneeIds.length === 0) return ElMessage.warning('请填写标题并选择执行人')
  submitting.value = true
  try {
    const payload = {
      title: createForm.title,
      description: createForm.description,
      priority: createForm.priority,
      dueAt: createForm.dueAt ? new Date(createForm.dueAt).toISOString() : null,
      assigneeIds: createForm.assigneeIds,
    }
    if (editingTask.value) await updateTask(editingTask.value.id, payload)
    else await createTask(activeGroupId.value, payload)
    Object.assign(createForm, { title: '', description: '', priority: 'NORMAL', dueAt: '', assigneeIds: [] })
    createVisible.value = false
    ElMessage.success(editingTask.value ? '任务已更新' : '任务已发布')
    editingTask.value = null
    await loadGroupData(activeGroupId.value)
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '任务发布失败')) }
  finally { submitting.value = false }
}

function openCreate() {
  editingTask.value = null
  Object.assign(createForm, { title: '', description: '', priority: 'NORMAL', dueAt: '', assigneeIds: [] })
  createVisible.value = true
}

function openEdit(task: TaskView) {
  editingTask.value = task
  Object.assign(createForm, {
    title: task.title, description: task.description ?? '', priority: task.priority,
    dueAt: task.dueAt ? new Date(task.dueAt) : '', assigneeIds: task.assignees.map(item => item.userId),
  })
  createVisible.value = true
}

async function cancel(task: TaskView) {
  try {
    await ElMessageBox.confirm(`确定取消任务“${task.title}”？`, '取消任务', { type: 'warning' })
    await cancelTask(task.id)
    await loadGroupData(activeGroupId.value)
    ElMessage.success('任务已取消')
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '取消失败'))
  }
}

async function begin(task: TaskView) {
  try { await startTask(task.id); ElMessage.success('任务已开始'); await loadGroupData(activeGroupId.value) }
  catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '操作失败')) }
}

function openSubmit(task: TaskView) {
  activeTask.value = task
  Object.assign(submitForm, { content: '', attachmentFileId: '' })
  submitVisible.value = true
}

async function saveSubmission() {
  if (!activeTask.value || !submitForm.content.trim()) return ElMessage.warning('请填写成果说明')
  submitting.value = true
  try {
    await submitTask(activeTask.value.id, submitForm.content, submitForm.attachmentFileId)
    submitVisible.value = false
    ElMessage.success('成果已提交，等待审核')
    await loadGroupData(activeGroupId.value)
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '提交失败')) }
  finally { submitting.value = false }
}

function openReview(task: TaskView) {
  activeTask.value = task
  Object.assign(reviewForm, { decision: 'APPROVED', comment: '' })
  reviewVisible.value = true
}

async function saveReview() {
  if (!activeTask.value) return
  submitting.value = true
  try {
    await reviewTask(activeTask.value.id, reviewForm.decision, reviewForm.comment)
    reviewVisible.value = false
    ElMessage.success(reviewForm.decision === 'APPROVED' ? '任务已验收完成' : '已退回修改')
    await loadGroupData(activeGroupId.value)
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '审核失败')) }
  finally { submitting.value = false }
}

async function openComments(task: TaskView) {
  activeTask.value = task
  commentContent.value = ''
  commentsVisible.value = true
  try { comments.value = await listTaskComments(task.id) }
  catch { ElMessage.error('任务留言加载失败') }
}

async function saveComment() {
  if (!activeTask.value || !commentContent.value.trim()) return ElMessage.warning('请输入留言内容')
  submitting.value = true
  try {
    await createTaskComment(activeTask.value.id, commentContent.value)
    commentContent.value = ''
    comments.value = await listTaskComments(activeTask.value.id)
    ElMessage.success('留言已发布')
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '留言发布失败')) }
  finally { submitting.value = false }
}

async function removeComment(comment: CollaborationPostView) {
  try {
    await ElMessageBox.confirm('确定删除这条任务留言？', '删除留言', { type: 'warning' })
    await deletePost(comment.id)
    if (activeTask.value) comments.value = await listTaskComments(activeTask.value.id)
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '删除失败'))
  }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '未设置'
}

function downloadAttachment(submission: TaskSubmission) {
  if (!submission?.attachmentFileId || !submission.attachmentName) return
  return downloadFile({ id: submission.attachmentFileId, name: submission.attachmentName })
}

watch(activeGroupId, groupId => {
  createVisible.value = submitVisible.value = reviewVisible.value = commentsVisible.value = false
  tasks.value = []
  members.value = []
  files.value = []
  void loadGroupData(groupId)
}, { flush: 'sync' })
onMounted(initialize)
</script>

<template>
  <section class="page-heading task-heading">
    <div><span class="eyebrow">TASK WORKFLOW</span><h2>任务与成果</h2><p>从任务指派、版本提交到审核归档，保留完整协作过程。</p></div>
    <div class="heading-actions">
      <el-button v-if="canCreateTask" type="primary" :icon="Plus" :disabled="!activeGroupId" @click="openCreate">发布任务</el-button>
    </div>
  </section>

  <el-alert v-if="!initializing && linkedGroupUnavailable" title="链接对应的小组已无法访问" type="warning" :closable="false" />
  <router-link v-if="linkedItemId" :to="$route.path">返回完整列表</router-link>
  <el-skeleton v-if="initializing || loading" :rows="5" animated />
  <el-empty v-else-if="groups.length === 0" description="请先创建或加入一个小组" />
  <el-empty v-else-if="tasks.length === 0"  :description="focusedId ? '该任务不存在或已无法访问' : '该小组还没有任务'" />
  <template v-else>
    <div class="task-toolbar"><el-input v-model="keyword" clearable placeholder="搜索任务标题或说明" aria-label="搜索任务标题或说明" /><el-select v-model="statusFilter" clearable placeholder="全部状态" aria-label="任务状态筛选"><el-option v-for="(label, value) in statusText" :key="value" :label="label" :value="value" /></el-select></div>
    <el-empty v-if="visibleTasks.length === 0" :description="focusedId ? '该任务不存在或已无法访问' : '没有符合条件的任务'" />
  <section v-else class="task-list">
    <article v-for="task in visibleTasks" :key="task.id" class="task-card">
      <div class="task-card-head">
        <div>
          <div class="task-tags"><el-tag :type="statusType[task.status]">{{ statusText[task.status] }}</el-tag><el-tag effect="plain">{{ priorityText[task.priority] }}优先级</el-tag><el-tag v-if="task.overdue" type="danger">已逾期</el-tag></div>
          <h3>{{ task.title }}</h3>
          <p>{{ task.description || '暂无任务说明' }}</p>
        </div>
        <div class="task-actions">
          <el-button :icon="ChatDotRound" @click="openComments(task)">留言</el-button>
          <el-button v-if="task.canStart" @click="begin(task)">开始任务</el-button>
          <el-button v-if="task.canSubmit" type="primary" :icon="Upload" @click="openSubmit(task)">提交成果</el-button>
          <el-button v-if="task.canReview" type="success" :icon="Check" @click="openReview(task)">审核</el-button>
          <el-button v-if="task.canManage" :icon="Edit" @click="openEdit(task)">编辑</el-button>
          <el-button v-if="task.canManage" type="danger" plain :icon="Delete" @click="cancel(task)">取消</el-button>
        </div>
      </div>
      <div class="task-meta">
        <span>执行人：<template v-for="(item, index) in task.assignees" :key="item.userId">{{ index ? '、' : '' }}<router-link :to="{ name: 'member-profile', params: { groupId: activeGroupId, userId: item.userId } }">{{ members.find(member => member.userId === item.userId)?.groupNickname || item.nickname }}</router-link></template><template v-if="!task.assignees.length">暂无当前执行人</template></span>
        <span v-if="task.historicalAssignees.length">历史执行人：{{ task.historicalAssignees.map((item) => item.nickname).join('、') }}</span>
        <span>截止：{{ formatTime(task.dueAt) }}</span>
      </div>
      <div v-if="task.latestSubmission" class="submission-box">
        <strong>最新成果 v{{ task.latestSubmission.versionNo }}</strong>
        <p>{{ task.latestSubmission.content }}</p>
        <el-button v-if="task.latestSubmission.attachmentFileId" text type="primary" @click="downloadAttachment(task.latestSubmission)">下载附件：{{ task.latestSubmission.attachmentName }}</el-button>
        <div v-if="task.latestSubmission.reviewDecision" class="review-note">
          {{ task.latestSubmission.reviewDecision === 'APPROVED' ? '审核通过' : '退回修改' }}：{{ task.latestSubmission.reviewComment || '无审核说明' }}
        </div>
        <el-collapse v-if="task.submissions.length > 1" class="submission-history">
          <el-collapse-item :title="`查看全部 ${task.submissions.length} 个提交版本`">
            <div v-for="submission in task.submissions" :key="submission.id" class="submission-history-item">
              <div><strong>v{{ submission.versionNo }}</strong><span>{{ formatTime(submission.submittedAt) }}</span></div>
              <p>{{ submission.content }}</p>
              <el-button v-if="submission.attachmentFileId" text type="primary" @click="downloadAttachment(submission)">下载附件：{{ submission.attachmentName }}</el-button>
              <small v-if="submission.reviewDecision">{{ submission.reviewDecision === 'APPROVED' ? '审核通过' : '退回修改' }} · {{ submission.reviewComment || '无审核说明' }}</small>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </article>
  </section>
  </template>

  <el-dialog v-model="createVisible" :title="editingTask ? '编辑任务' : `在 ${activeGroup?.name ?? ''} 发布任务`" width="560px" @closed="editingTask = null">
    <el-form label-position="top">
      <el-form-item label="任务标题"><el-input v-model="createForm.title" maxlength="100" show-word-limit /></el-form-item>
      <el-form-item label="任务说明"><el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
      <div class="form-grid">
        <el-form-item label="优先级"><el-select v-model="createForm.priority"><el-option label="低" value="LOW" /><el-option label="普通" value="NORMAL" /><el-option label="高" value="HIGH" /><el-option label="紧急" value="URGENT" /></el-select></el-form-item>
        <el-form-item label="截止时间"><el-date-picker v-model="createForm.dueAt" type="datetime" placeholder="可选" /></el-form-item>
      </div>
      <el-form-item label="执行人"><el-select v-model="createForm.assigneeIds" multiple placeholder="可选择多人"><el-option v-for="member in members" :key="member.userId" :label="member.groupNickname || member.nickname" :value="member.userId" /></el-select><div class="form-help">多人任务采用共同完成模式：任一执行人提交成果后，整个任务进入审核。</div></el-form-item>
    </el-form>
    <template #footer><el-button @click="createVisible = false">关闭</el-button><el-button type="primary" :loading="submitting" @click="submitCreate">{{ editingTask ? '保存修改' : '发布' }}</el-button></template>
  </el-dialog>

  <el-dialog v-model="submitVisible" title="提交任务成果" width="520px">
    <el-form label-position="top"><el-form-item label="成果说明"><el-input v-model="submitForm.content" type="textarea" :rows="5" maxlength="2000" show-word-limit /></el-form-item><el-form-item label="资料库附件（可选）"><el-select v-model="submitForm.attachmentFileId" clearable placeholder="选择已上传的文件"><el-option v-for="file in files" :key="file.id" :label="file.name" :value="file.id" /></el-select></el-form-item></el-form>
    <template #footer><el-button @click="submitVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="saveSubmission">提交审核</el-button></template>
  </el-dialog>

  <el-dialog v-model="reviewVisible" title="审核任务成果" width="520px">
    <div v-if="activeTask?.latestSubmission" class="review-target">v{{ activeTask.latestSubmission.versionNo }} · {{ activeTask.latestSubmission.content }}</div>
    <el-form label-position="top"><el-form-item label="审核结果"><el-radio-group v-model="reviewForm.decision"><el-radio value="APPROVED">通过</el-radio><el-radio value="CHANGES_REQUESTED">退回修改</el-radio></el-radio-group></el-form-item><el-form-item label="审核意见"><el-input v-model="reviewForm.comment" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item></el-form>
    <template #footer><el-button @click="reviewVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="saveReview">确认审核</el-button></template>
  </el-dialog>

  <el-dialog v-model="commentsVisible" :title="`任务留言 · ${activeTask?.title ?? ''}`" width="600px">
    <div class="task-comment-editor"><el-input v-model="commentContent" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="记录问题、进展或协作说明" /><el-button type="primary" :loading="submitting" @click="saveComment">发布留言</el-button></div>
    <el-empty v-if="comments.length === 0" description="还没有任务留言" />
    <div v-else class="task-comment-list">
      <article v-for="comment in comments" :key="comment.id">
        <header><router-link :to="{ name: 'member-profile', params: { groupId: comment.groupId, userId: comment.createdBy } }">{{ members.find(member => member.userId === comment.createdBy)?.groupNickname || comment.authorName }}</router-link><span>{{ formatTime(comment.createdAt) }}</span></header>
        <p>{{ comment.content }}</p>
        <el-button v-if="comment.canDelete" text type="danger" @click="removeComment(comment)">删除</el-button>
      </article>
    </div>
    <template #footer><el-button @click="commentsVisible = false">关闭</el-button></template>
  </el-dialog>
</template>
