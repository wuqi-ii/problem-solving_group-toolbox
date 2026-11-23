<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Refresh, Upload, View } from '@element-plus/icons-vue'

import { listGroups, type GroupView } from '@/api/groups'
import {
  deleteFile, downloadFile, downloadTaskResults, getFileQuota, listFiles, listTrash, previewFile, purgeFile, renameFile,
  restoreFile, updateFileCategory, uploadFile, type FileCategory, type FileQuotaView, type FileView,
} from '@/api/files'
import { useSessionStore } from '@/stores/session'
import { apiErrorMessage, isDialogDismissal } from '@/api/errors'

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

const files = ref<FileView[]>([])
const session = useSessionStore()
const activeGroupId = computed({
  get: () => session.activeGroupId ?? '',
  set: (value: string) => session.switchGroup(value || null),
})
const keyword = ref('')
const loading = ref(false)
const initializing = ref(true)
const uploading = ref(false)
const trashVisible = ref(false)
const trash = ref<FileView[]>([])
const quota = ref<FileQuotaView | null>(null)
const categoryFilter = ref<FileCategory | ''>('')
const uploadProgress = ref(0)
const fileInput = ref<HTMLInputElement>()
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewName = ref('')
const previewUrl = ref('')
const previewText = ref('')
const previewKind = ref<'text' | 'image' | 'pdf' | 'other'>('other')

const filteredFiles = computed(() => {
  if (focusedId.value) return files.value.filter(file => file.id === focusedId.value)
  const query = keyword.value.trim().toLowerCase()
  return files.value.filter(file => (!categoryFilter.value || file.category === categoryFilter.value)
    && (!query || file.name.toLowerCase().includes(query)))
})
const quotaPercent = computed(() => quota.value ? Math.round(quota.value.usedBytes * 100 / quota.value.limitBytes) : 0)
const categoryText: Record<FileCategory, string> = { GENERAL: '通用', DOCUMENT: '文档', IMAGE: '图片', DATA: '数据', ARCHIVE: '压缩包', TASK_RESULT: '任务成果' }

async function initialize() {
  try {
    groups.value = await listGroups()
    selectLinkedGroup()
    if (!groups.value.some(group => group.id === activeGroupId.value)) {
      activeGroupId.value = groups.value[0]?.id ?? ''
    } else if (activeGroupId.value) {
      await refresh(activeGroupId.value)
    }
  } catch { ElMessage.error('小组列表加载失败') }
  finally { initializing.value = false }
}

let loadVersion = 0

async function refresh(groupId = activeGroupId.value) {
  if (groupId !== activeGroupId.value) return
  const version = ++loadVersion
  loading.value = Boolean(groupId)
  if (!groupId) return
  try {
    const result = await Promise.all([listFiles(groupId), getFileQuota(groupId)])
    if (version !== loadVersion || groupId !== activeGroupId.value) return
    files.value = result[0]
    quota.value = result[1]
  } catch {
    if (version === loadVersion && groupId === activeGroupId.value) ElMessage.error('文件列表加载失败')
  } finally {
    if (version === loadVersion && groupId === activeGroupId.value) loading.value = false
  }
}

async function downloadResults() {
  if (!activeGroupId.value) return
  try { await downloadTaskResults(activeGroupId.value); ElMessage.success('已验收成果已打包下载') }
  catch { ElMessage.error('任务成果打包失败') }
}

async function chooseFile(event: Event) {
  const input = event.target as HTMLInputElement
  const selected = input.files?.[0]
  if (!selected) return
  uploading.value = true
  uploadProgress.value = 0
  try {
    await uploadFile(activeGroupId.value, selected, (percent) => uploadProgress.value = percent)
    ElMessage.success('文件上传成功')
    await refresh()
  } catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '文件上传失败')) }
  finally { uploading.value = false; input.value = '' }
}

async function editName(file: FileView) {
  try {
    const result = await ElMessageBox.prompt('输入新的显示名称', '重命名文件', {
      inputValue: file.name, inputValidator: (value) => Boolean(value?.trim()) || '文件名不能为空',
    })
    await renameFile(file.id, result.value)
    ElMessage.success('文件名已更新')
    await refresh()
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '重命名失败'))
  }
}

async function remove(file: FileView) {
  try {
    await ElMessageBox.confirm(`将“${file.name}”移入回收状态？`, '删除文件', { type: 'warning' })
    await deleteFile(file.id)
    ElMessage.success('文件已删除')
    await refresh()
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '删除失败'))
  }
}

async function changeCategory(file: FileView, category: FileCategory) {
  try { await updateFileCategory(file.id, category); await refresh() }
  catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '分类更新失败')) }
}

function clearPreviewUrl() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

let previewVersion = 0

async function openPreview(file: FileView) {
  const version = ++previewVersion
  const current = () => version === previewVersion && previewVisible.value && activeGroupId.value === file.groupId
  clearPreviewUrl()
  previewName.value = file.name
  previewText.value = ''
  previewKind.value = file.contentType?.startsWith('image/') ? 'image'
    : file.contentType === 'application/pdf' ? 'pdf'
      : file.contentType?.startsWith('text/') || /\.(txt|md|csv)$/i.test(file.name) ? 'text' : 'other'
  previewVisible.value = true
  previewLoading.value = true
  try {
    const blob = await previewFile(file)
    if (!current()) return
    if (previewKind.value === 'text') {
      const content = await blob.text()
      if (current()) previewText.value = content
    } else if (previewKind.value === 'image' || previewKind.value === 'pdf') {
      previewUrl.value = URL.createObjectURL(blob)
    }
  } catch (error: unknown) {
    if (current()) {
      previewVisible.value = false
      ElMessage.error(apiErrorMessage(error, '文件预览失败'))
    }
  } finally {
    if (version === previewVersion) previewLoading.value = false
  }
}

function closePreview() {
  clearPreviewUrl()
  previewText.value = ''
}

async function openTrash() {
  if (!activeGroupId.value) return
  const groupId = activeGroupId.value
  try {
    const result = await listTrash(groupId)
    if (groupId !== activeGroupId.value) return
    trash.value = result
    trashVisible.value = true
  } catch {
    if (groupId === activeGroupId.value) ElMessage.error('回收站加载失败')
  }
}

async function restore(item: FileView) {
  try { await restoreFile(item.id); trash.value = await listTrash(activeGroupId.value); await refresh(); ElMessage.success('文件已恢复') }
  catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '恢复失败')) }
}

async function purge(item: FileView) {
  try { await ElMessageBox.confirm(`永久删除“${item.name}”？此操作不可恢复。`, '永久删除', { type: 'error' }); await purgeFile(item.id); trash.value = await listTrash(activeGroupId.value); ElMessage.success('文件已永久删除') }
  catch (error: unknown) { if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '永久删除失败')) }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`
}

watch(activeGroupId, groupId => {
  trashVisible.value = previewVisible.value = false
  previewVersion++
  closePreview()
  files.value = []
  quota.value = null
  void refresh(groupId)
}, { flush: 'sync' })
onMounted(initialize)
</script>

<template>
  <section class="page-heading file-heading">
    <div><span class="eyebrow">SHARED LIBRARY</span><h2>共享资料库</h2><p>文件按小组隔离保存，可直接作为任务成果附件使用。</p></div>
    <div class="heading-actions">
      <input ref="fileInput" class="hidden-input" type="file" @change="chooseFile">
      <el-button :icon="Download" :disabled="!activeGroupId" @click="downloadResults">下载已验收成果</el-button>
      <el-button :icon="Delete" :disabled="!activeGroupId" @click="openTrash">回收站</el-button>
      <el-button type="primary" :icon="Upload" :loading="uploading" :disabled="!activeGroupId" @click="fileInput?.click()">上传文件</el-button>
    </div>
  </section>

  <el-alert v-if="!initializing && linkedGroupUnavailable" title="链接对应的小组已无法访问" type="warning" :closable="false" />
  <router-link v-if="linkedItemId" :to="$route.path">返回完整列表</router-link>
  <el-progress v-if="uploading" :percentage="uploadProgress" class="upload-progress" />
  <div v-if="!initializing && activeGroupId" class="file-quota"><span>资料库容量 {{ formatSize(quota?.usedBytes ?? 0) }} / {{ formatSize(quota?.limitBytes ?? 0) }}</span><el-progress :percentage="quotaPercent" :stroke-width="8" /></div>
  <div v-if="!initializing && activeGroupId" class="library-toolbar"><el-select v-model="categoryFilter" clearable placeholder="全部分类" aria-label="文件分类筛选"><el-option v-for="(label, value) in categoryText" :key="value" :label="label" :value="value" /></el-select><el-input v-model="keyword" clearable placeholder="按文件名搜索" aria-label="按文件名搜索" /></div>
  <el-skeleton v-if="initializing || loading" :rows="5" animated />
  <el-empty v-else-if="groups.length === 0" description="请先创建或加入小组" />
  <el-empty v-else-if="filteredFiles.length === 0" :description="focusedId ? '该文件不存在或已无法访问' : '资料库中还没有文件'" />
  <el-table v-else :data="filteredFiles" class="file-table desktop-file-table">
    <el-table-column prop="name" label="文件名" min-width="260" />
    <el-table-column label="版本" width="75"><template #default="scope">v{{ scope.row.versionNo }}</template></el-table-column>
    <el-table-column label="分类" width="135"><template #default="scope"><el-select v-if="scope.row.canManage" :model-value="scope.row.category" size="small" @change="changeCategory(scope.row, $event)"><el-option v-for="(label, value) in categoryText" :key="value" :label="label" :value="value" /></el-select><span v-else>{{ categoryText[scope.row.category as FileCategory] }}</span></template></el-table-column>
    <el-table-column label="大小" width="110"><template #default="scope">{{ formatSize(scope.row.sizeBytes) }}</template></el-table-column>
    <el-table-column label="上传者" width="130">
      <template #default="{ row }"><router-link :to="{ name: 'member-profile', params: { groupId: row.groupId, userId: row.uploadedBy } }">{{ row.uploaderName }}</router-link></template>
    </el-table-column>
    <el-table-column label="上传时间" width="190"><template #default="scope">{{ new Date(scope.row.createdAt).toLocaleString('zh-CN', { hour12: false }) }}</template></el-table-column>
    <el-table-column label="操作" width="300" fixed="right">
      <template #default="scope">
        <el-button v-if="scope.row.previewable" text :icon="View" @click="openPreview(scope.row)">预览</el-button>
        <el-button text type="primary" :icon="Download" @click="downloadFile(scope.row)">下载</el-button>
        <el-button v-if="scope.row.canManage" text :icon="Edit" @click="editName(scope.row)">改名</el-button>
        <el-button v-if="scope.row.canManage" text type="danger" :icon="Delete" @click="remove(scope.row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>

  <section v-if="!initializing && !loading && filteredFiles.length" class="mobile-file-list">
    <article v-for="file in filteredFiles" :key="file.id" class="mobile-file-card">
      <div class="mobile-file-title"><strong>{{ file.name }}</strong><el-tag size="small" effect="plain">v{{ file.versionNo }}</el-tag></div>
      <dl><div><dt>分类</dt><dd>{{ categoryText[file.category] }}</dd></div><div><dt>大小</dt><dd>{{ formatSize(file.sizeBytes) }}</dd></div><div><dt>上传者</dt><dd><router-link :to="{ name: 'member-profile', params: { groupId: file.groupId, userId: file.uploadedBy } }">{{ file.uploaderName }}</router-link></dd></div><div><dt>上传时间</dt><dd>{{ new Date(file.createdAt).toLocaleString('zh-CN', { hour12: false }) }}</dd></div></dl>
      <div class="mobile-file-actions"><el-button v-if="file.previewable" :icon="View" @click="openPreview(file)">预览</el-button><el-button type="primary" plain :icon="Download" @click="downloadFile(file)">下载</el-button><el-button v-if="file.canManage" :icon="Edit" @click="editName(file)">改名</el-button><el-button v-if="file.canManage" type="danger" plain :icon="Delete" @click="remove(file)">删除</el-button></div>
    </article>
  </section>

  <el-dialog v-model="previewVisible" :title="`预览：${previewName}`" width="min(900px, 92vw)" destroy-on-close @closed="closePreview">
    <el-skeleton v-if="previewLoading" :rows="8" animated />
    <pre v-else-if="previewKind === 'text'" class="text-preview">{{ previewText }}</pre>
    <img v-else-if="previewKind === 'image' && previewUrl" class="image-preview" :src="previewUrl" :alt="previewName">
    <iframe v-else-if="previewKind === 'pdf' && previewUrl" class="pdf-preview" :src="previewUrl" :title="previewName" />
    <el-empty v-else description="该格式暂不支持在线预览，请下载后查看" />
    <template #footer><el-button @click="previewVisible = false">关闭</el-button></template>
  </el-dialog>

  <el-dialog v-model="trashVisible" title="资料库回收站" width="760px">
    <el-empty v-if="trash.length === 0" description="回收站为空" />
    <el-table v-else :data="trash" max-height="420">
      <el-table-column prop="name" label="文件名" min-width="220" />
      <el-table-column label="删除时间" width="190"><template #default="scope">{{ scope.row.deletedAt ? new Date(scope.row.deletedAt).toLocaleString('zh-CN', { hour12: false }) : '-' }}</template></el-table-column>
      <el-table-column label="操作" width="190"><template #default="scope"><el-button text type="primary" :icon="Refresh" @click="restore(scope.row)">恢复</el-button><el-button text type="danger" :icon="Delete" @click="purge(scope.row)">永久删除</el-button></template></el-table-column>
    </el-table>
    <template #footer><el-button @click="trashVisible = false">关闭</el-button></template>
  </el-dialog>
</template>
