<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Delete, Upload } from '@element-plus/icons-vue'
import { cancelTransfer, createTransfer, listTransfers, type TransferView } from '@/api/transfers'

const items = ref<TransferView[]>([]); const fileInput = ref<HTMLInputElement>(); const file = ref<File>()
const validHours = ref(24); const maxDownloads = ref(5); const loading = ref(false); const newCode = ref('')
async function refresh() { items.value = await listTransfers() }
function choose(event: Event) { file.value = (event.target as HTMLInputElement).files?.[0] }
async function create() {
  if (!file.value) return ElMessage.warning('请先选择文件')
  loading.value = true
  try { const result = await createTransfer(file.value, validHours.value, maxDownloads.value); newCode.value = result.pickupCode ?? ''; ElMessage.success('取件码已生成'); file.value = undefined; if (fileInput.value) fileInput.value.value = ''; await refresh() }
  catch (e: any) { ElMessage.error(e.response?.data?.message ?? '创建失败') } finally { loading.value = false }
}
async function copy(value: string) { await navigator.clipboard.writeText(value); ElMessage.success('取件码已复制') }
async function cancel(item: TransferView) { try { await ElMessageBox.confirm('取消后取件码将立即失效，是否继续？', '取消中转', { type: 'warning' }); await cancelTransfer(item.id); await refresh() } catch (e) { /* user cancelled */ } }
const statusText: Record<string, string> = { ACTIVE: '可取件', CANCELLED: '已取消', EXPIRED: '已过期', EXHAUSTED: '次数用尽', CLEANED: '已清理' }
refresh().catch(() => ElMessage.error('中转记录加载失败'))
</script>

<template>
  <section class="page-heading"><div><span class="eyebrow">QUICK TRANSFER</span><h2>临时文件中转</h2><p>生成一次性取件码，在未登录设备上也能安全下载。</p></div><el-button @click="$router.push('/pickup')">前往取件页</el-button></section>
  <div class="tool-grid">
    <el-card class="tool-panel"><template #header><strong>创建中转</strong></template>
      <input ref="fileInput" type="file" class="hidden-input" @change="choose"><el-button :icon="Upload" @click="fileInput?.click()">{{ file?.name ?? '选择文件' }}</el-button>
      <div class="compact-form"><label>有效小时<el-input-number v-model="validHours" :min="1" :max="168" /></label><label>最多下载<el-input-number v-model="maxDownloads" :min="1" :max="100" /></label></div>
      <el-button type="primary" :loading="loading" class="full-button" @click="create">生成取件码</el-button>
      <div v-if="newCode" class="pickup-code"><span>本次取件码（仅展示一次）</span><strong>{{ newCode }}</strong><el-button :icon="CopyDocument" circle @click="copy(newCode)" /></div>
    </el-card>
    <el-card class="tool-panel"><template #header><strong>中转记录</strong></template>
      <el-empty v-if="!items.length" description="还没有中转记录" />
      <div v-for="item in items" :key="item.id" class="utility-row"><div><strong>{{ item.fileName }}</strong><span>{{ statusText[item.status] ?? item.status }} · 已下载 {{ item.downloadCount }}/{{ item.maxDownloads }} · {{ new Date(item.expiresAt).toLocaleString('zh-CN', { hour12: false }) }} 到期</span></div><el-button v-if="item.status === 'ACTIVE'" text type="danger" :icon="Delete" @click="cancel(item)">取消</el-button></div>
    </el-card>
  </div>
</template>
