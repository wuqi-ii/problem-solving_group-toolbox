<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Plus, Upload } from '@element-plus/icons-vue'

import { listGroups, listMembers, type GroupView, type MemberView } from '@/api/groups'
import { downloadFile, listFiles, type FileView } from '@/api/files'
import {
  createTask, listTasks, reviewTask, startTask, submitTask,
  type ReviewDecision, type TaskPriority, type TaskStatus, type TaskView,
} from '@/api/tasks'

const groups = ref<GroupView[]>([])
const members = ref<MemberView[]>([])
const tasks = ref<TaskView[]>([])
const files = ref<FileView[]>([])
const activeGroupId = ref('')
const loading = ref(false)
const submitting = ref(false)
const createVisible = ref(false)
const submitVisible = ref(false)
const reviewVisible = ref(false)
const activeTask = ref<TaskView | null>(null)

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

async function initialize() {
  try {
    groups.value = await listGroups()
    activeGroupId.value = groups.value[0]?.id ?? ''
  } catch { ElMessage.error('小组列表加载失败') }
}

async function loadGroupData(groupId: string) {
  if (!groupId) return
  loading.value = true
  try {
    ;[tasks.value, members.value, files.value] = await Promise.all([listTasks(groupId), listMembers(groupId), listFiles(groupId)])
  } catch { ElMessage.error('任务数据加载失败') }
  finally { loading.value = false }
}

async function submitCreate() {
  if (!createForm.title.trim() || createForm.assigneeIds.length === 0) return ElMessage.warning('请填写标题并选择执行人')
  submitting.value = true
  try {
    await createTask(activeGroupId.value, {
      title: createForm.title,
      description: createForm.description,
      priority: createForm.priority,
      dueAt: createForm.dueAt ? new Date(createForm.dueAt).toISOString() : null,
      assigneeIds: createForm.assigneeIds,
    })
    Object.assign(createForm, { title: '', description: '', priority: 'NORMAL', dueAt: '', assigneeIds: [] })
    createVisible.value = false
    ElMessage.success('任务已发布')
    await loadGroupData(activeGroupId.value)
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '任务发布失败') }
  finally { submitting.value = false }
}

async function begin(task: TaskView) {
  try { await startTask(task.id); ElMessage.success('任务已开始'); await loadGroupData(activeGroupId.value) }
  catch (error: any) { ElMessage.error(error.response?.data?.message ?? '操作失败') }
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
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '提交失败') }
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
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '审核失败') }
  finally { submitting.value = false }
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '未设置'
}

function downloadAttachment(task: TaskView) {
  const submission = task.latestSubmission
  if (!submission?.attachmentFileId || !submission.attachmentName) return
  return downloadFile({ id: submission.attachmentFileId, name: submission.attachmentName })
}

watch(activeGroupId, loadGroupData)
onMounted(initialize)
</script>

<template>
  <section class="page-heading task-heading">
    <div><span class="eyebrow">TASK WORKFLOW</span><h2>任务与成果</h2><p>从任务指派、版本提交到审核归档，保留完整协作过程。</p></div>
    <div class="heading-actions">
      <el-select v-model="activeGroupId" placeholder="选择小组" class="group-select">
        <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
      </el-select>
      <el-button type="primary" :icon="Plus" :disabled="!activeGroupId" @click="createVisible = true">发布任务</el-button>
    </div>
  </section>

  <el-empty v-if="groups.length === 0" description="请先创建或加入一个小组" />
  <el-skeleton v-else-if="loading" :rows="5" animated />
  <el-empty v-else-if="tasks.length === 0" description="该小组还没有任务" />
  <section v-else class="task-list">
    <article v-for="task in tasks" :key="task.id" class="task-card">
      <div class="task-card-head">
        <div>
          <div class="task-tags"><el-tag :type="statusType[task.status]">{{ statusText[task.status] }}</el-tag><el-tag effect="plain">{{ priorityText[task.priority] }}优先级</el-tag></div>
          <h3>{{ task.title }}</h3>
          <p>{{ task.description || '暂无任务说明' }}</p>
        </div>
        <div class="task-actions">
          <el-button v-if="task.canStart" @click="begin(task)">开始任务</el-button>
          <el-button v-if="task.canSubmit" type="primary" :icon="Upload" @click="openSubmit(task)">提交成果</el-button>
          <el-button v-if="task.canReview" type="success" :icon="Check" @click="openReview(task)">审核</el-button>
        </div>
      </div>
      <div class="task-meta">
        <span>执行人：{{ task.assignees.map((item) => item.nickname).join('、') }}</span>
        <span>截止：{{ formatTime(task.dueAt) }}</span>
      </div>
      <div v-if="task.latestSubmission" class="submission-box">
        <strong>最新成果 v{{ task.latestSubmission.versionNo }}</strong>
        <p>{{ task.latestSubmission.content }}</p>
        <el-button v-if="task.latestSubmission.attachmentFileId" text type="primary" @click="downloadAttachment(task)">下载附件：{{ task.latestSubmission.attachmentName }}</el-button>
        <div v-if="task.latestSubmission.reviewDecision" class="review-note">
          {{ task.latestSubmission.reviewDecision === 'APPROVED' ? '审核通过' : '退回修改' }}：{{ task.latestSubmission.reviewComment || '无审核说明' }}
        </div>
      </div>
    </article>
  </section>

  <el-dialog v-model="createVisible" :title="`在 ${activeGroup?.name ?? ''} 发布任务`" width="560px">
    <el-form label-position="top">
      <el-form-item label="任务标题"><el-input v-model="createForm.title" maxlength="100" show-word-limit /></el-form-item>
      <el-form-item label="任务说明"><el-input v-model="createForm.description" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
      <div class="form-grid">
        <el-form-item label="优先级"><el-select v-model="createForm.priority"><el-option label="低" value="LOW" /><el-option label="普通" value="NORMAL" /><el-option label="高" value="HIGH" /><el-option label="紧急" value="URGENT" /></el-select></el-form-item>
        <el-form-item label="截止时间"><el-date-picker v-model="createForm.dueAt" type="datetime" placeholder="可选" /></el-form-item>
      </div>
      <el-form-item label="执行人"><el-select v-model="createForm.assigneeIds" multiple placeholder="可选择多人"><el-option v-for="member in members" :key="member.userId" :label="member.nickname" :value="member.userId" /></el-select></el-form-item>
    </el-form>
    <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitCreate">发布</el-button></template>
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
</template>
