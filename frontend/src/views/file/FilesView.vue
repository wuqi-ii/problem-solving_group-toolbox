<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Upload } from '@element-plus/icons-vue'

import { listGroups, type GroupView } from '@/api/groups'
import { deleteFile, downloadFile, listFiles, renameFile, uploadFile, type FileView } from '@/api/files'

const groups = ref<GroupView[]>([])
const files = ref<FileView[]>([])
const activeGroupId = ref('')
const keyword = ref('')
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const fileInput = ref<HTMLInputElement>()

const filteredFiles = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return query ? files.value.filter((file) => file.name.toLowerCase().includes(query)) : files.value
})

async function initialize() {
  try {
    groups.value = await listGroups()
    activeGroupId.value = groups.value[0]?.id ?? ''
  } catch { ElMessage.error('小组列表加载失败') }
}

async function refresh(groupId = activeGroupId.value) {
  if (!groupId) return
  loading.value = true
  try { files.value = await listFiles(groupId) }
  catch { ElMessage.error('文件列表加载失败') }
  finally { loading.value = false }
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
  } catch (error: any) { ElMessage.error(error.response?.data?.message ?? '文件上传失败') }
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
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.response?.data?.message ?? '重命名失败')
  }
}

async function remove(file: FileView) {
  try {
    await ElMessageBox.confirm(`将“${file.name}”移入回收状态？`, '删除文件', { type: 'warning' })
    await deleteFile(file.id)
    ElMessage.success('文件已删除')
    await refresh()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error.response?.data?.message ?? '删除失败')
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`
}

watch(activeGroupId, refresh)
onMounted(initialize)
</script>

<template>
  <section class="page-heading file-heading">
    <div><span class="eyebrow">SHARED LIBRARY</span><h2>共享资料库</h2><p>文件按小组隔离保存，可直接作为任务成果附件使用。</p></div>
    <div class="heading-actions">
      <el-select v-model="activeGroupId" class="group-select" placeholder="选择小组"><el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" /></el-select>
      <input ref="fileInput" class="hidden-input" type="file" @change="chooseFile">
      <el-button type="primary" :icon="Upload" :loading="uploading" :disabled="!activeGroupId" @click="fileInput?.click()">上传文件</el-button>
    </div>
  </section>

  <el-progress v-if="uploading" :percentage="uploadProgress" class="upload-progress" />
  <div v-if="activeGroupId" class="library-toolbar"><el-input v-model="keyword" clearable placeholder="按文件名搜索" /></div>
  <el-empty v-if="groups.length === 0" description="请先创建或加入小组" />
  <el-skeleton v-else-if="loading" :rows="5" animated />
  <el-empty v-else-if="filteredFiles.length === 0" description="资料库中还没有文件" />
  <el-table v-else :data="filteredFiles" class="file-table">
    <el-table-column prop="name" label="文件名" min-width="260" />
    <el-table-column label="大小" width="110"><template #default="scope">{{ formatSize(scope.row.sizeBytes) }}</template></el-table-column>
    <el-table-column prop="uploaderName" label="上传者" width="130" />
    <el-table-column label="上传时间" width="190"><template #default="scope">{{ new Date(scope.row.createdAt).toLocaleString('zh-CN', { hour12: false }) }}</template></el-table-column>
    <el-table-column label="操作" width="230" fixed="right">
      <template #default="scope">
        <el-button text type="primary" :icon="Download" @click="downloadFile(scope.row)">下载</el-button>
        <el-button v-if="scope.row.canManage" text :icon="Edit" @click="editName(scope.row)">改名</el-button>
        <el-button v-if="scope.row.canManage" text type="danger" :icon="Delete" @click="remove(scope.row)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
