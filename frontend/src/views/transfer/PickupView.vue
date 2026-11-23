<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { downloadPickup, inspectPickup, type PickupView } from '@/api/transfers'
import { apiErrorMessage } from '@/api/errors'
const route = useRoute()
const code = ref('')
const result = ref<{ code: string; item: PickupView }>()
const loading = ref(false)
const downloading = ref(false)
let requestId = 0

watch(code, () => {
  requestId++
  result.value = undefined
  loading.value = false
}, { flush: 'sync' })

async function inspect() {
  const submittedCode = code.value.trim()
  if (!submittedCode || downloading.value) return
  const id = ++requestId
  loading.value = true
  result.value = undefined
  try {
    const item = await inspectPickup(submittedCode)
    if (id === requestId) result.value = { code: submittedCode, item }
  } catch (error: unknown) {
    if (id === requestId) ElMessage.error(apiErrorMessage(error, '取件码不可用'))
  } finally {
    if (id === requestId) loading.value = false
  }
}

async function download() {
  const selected = result.value
  if (!selected || downloading.value) return
  downloading.value = true
  try {
    await downloadPickup(selected.code, selected.item.fileName)
    const remaining = selected.item.remainingDownloads - 1
    if (result.value === selected) {
      if (remaining > 0) selected.item.remainingDownloads = remaining
      else result.value = undefined
    }
  } catch (error: unknown) {
    ElMessage.error(apiErrorMessage(error, '下载失败'))
  } finally {
    downloading.value = false
  }
}

watch(() => route.params.code, value => {
  code.value = String(value ?? '')
  if (code.value) void inspect()
}, { immediate: true })
</script>
<template><main class="pickup-page"><div class="pickup-shell"><div class="brand-mark">取</div><span class="eyebrow">TEAM TOOLBOX</span><h1>输入取件码</h1><p>输入发送方提供的取件码，在有效期内下载文件。</p><div class="pickup-search"><el-input v-model="code" maxlength="10" size="large" placeholder="10 位取件码" aria-label="10 位取件码" @keyup.enter="inspect" /><el-button type="primary" size="large" :loading="loading" @click="inspect">查询</el-button></div><el-card v-if="result" class="pickup-result"><strong>{{ result.item.fileName }}</strong><span>剩余 {{ result.item.remainingDownloads }} 次 · {{ new Date(result.item.expiresAt).toLocaleString('zh-CN', { hour12: false }) }} 到期</span><el-button type="primary"  :loading="downloading" :icon="Download" @click="download">下载文件</el-button></el-card><el-button text class="back-login" @click="$router.push('/')">返回系统</el-button></div></main></template>
