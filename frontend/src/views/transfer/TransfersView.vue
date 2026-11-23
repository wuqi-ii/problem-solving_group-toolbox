<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Delete, Upload } from '@element-plus/icons-vue'
import { cancelTransfer, createTransfer, listTransfers, type TransferView } from '@/api/transfers'
import { apiErrorMessage, isDialogDismissal } from '@/api/errors'

const items = ref<TransferView[]>([]); const fileInput = ref<HTMLInputElement>(); const file = ref<File>()
const validHours = ref(24); const maxDownloads = ref(1); const loading = ref(false); const newCode = ref('')
const newTransferId = ref('')
const isLocalAddress = ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname)
const pickupLink = computed(() => newCode.value ? `${window.location.origin}/pickup/${newCode.value}` : '')
async function refresh() { items.value = await listTransfers() }
function choose(event: Event) { file.value = (event.target as HTMLInputElement).files?.[0] }
async function create() {
  if (!file.value) return ElMessage.warning('请先选择文件')
  loading.value = true
  try { const result = await createTransfer(file.value, validHours.value, maxDownloads.value); newTransferId.value = result.id; newCode.value = result.pickupCode ?? ''; ElMessage.success('取件码已生成'); file.value = undefined; if (fileInput.value) fileInput.value.value = ''; await refresh() }
  catch (error: unknown) { ElMessage.error(apiErrorMessage(error, '创建失败')) } finally { loading.value = false }
}
async function copy(value: string, label: string) {
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(label + '已复制')
  } catch {
    await ElMessageBox.alert(value, '请手动复制' + label)
  }
}

async function cancel(item: TransferView) {
  try {
    await ElMessageBox.confirm('取消后取件码将立即失效，是否继续？', '取消中转', { type: 'warning' })
    await cancelTransfer(item.id)
    if (newTransferId.value === item.id) {
      newTransferId.value = ''
      newCode.value = ''
    }
    ElMessage.success('中转已取消')
    await refresh().catch(() => ElMessage.warning('中转已取消，记录刷新失败'))
  } catch (error: unknown) {
    if (!isDialogDismissal(error)) ElMessage.error(apiErrorMessage(error, '取消中转失败'))
  }
}
const statusText: Record<string, string> = { ACTIVE: '可取件', CANCELLED: '已取消', EXPIRED: '已过期', EXHAUSTED: '次数用尽', CLEANED: '已清理' }
refresh().catch(() => ElMessage.error('中转记录加载失败'))
</script>

<template>
  <section class="page-heading"><div><span class="eyebrow">QUICK TRANSFER</span><h2>临时文件中转</h2><p>设置有效期和下载次数，再把取件码发给接收方。持码者均可下载，请私下分享。</p></div><el-button @click="$router.push('/pickup')">前往取件页</el-button></section>
  <div class="tool-grid">
    <el-card class="tool-panel"><template #header><strong>创建中转</strong></template>
      <input ref="fileInput" type="file" class="hidden-input" @change="choose"><el-button :icon="Upload" @click="fileInput?.click()">{{ file?.name ?? '选择文件' }}</el-button>
      <div class="compact-form"><label>有效小时<el-input-number v-model="validHours" :min="1" :max="168" /></label><label>最多下载<el-input-number v-model="maxDownloads" :min="1" :max="100" /></label></div>
      <el-button type="primary" :loading="loading" class="full-button" @click="create">生成取件码</el-button>
      <div v-if="newCode" class="pickup-code"><span>本次取件码（仅展示一次）</span><strong>{{ newCode }}</strong><el-button :icon="CopyDocument" circle @click="copy(newCode, '取件码')" /><img class="pickup-qr" :src="`/api/v1/pickup/${newCode}/qr`" alt="取件二维码"><el-button text type="primary" @click="copy(pickupLink, '取件链接')">复制取件链接</el-button><p v-if="isLocalAddress" class="muted">给其他设备发送链接前，请通过本机局域网地址打开中转页。</p></div>
    </el-card>
    <el-card class="tool-panel"><template #header><strong>中转记录</strong></template>
      <el-empty v-if="!items.length" description="还没有中转记录" />
      <div v-for="item in items" :key="item.id" class="utility-row"><div><strong>{{ item.fileName }}</strong><span>{{ statusText[item.status] ?? item.status }} · 已下载 {{ item.downloadCount }}/{{ item.maxDownloads }} · {{ new Date(item.expiresAt).toLocaleString('zh-CN', { hour12: false }) }} 到期</span></div><el-button v-if="item.status === 'ACTIVE'" text type="danger" :icon="Delete" @click="cancel(item)">取消</el-button></div>
    </el-card>
  </div>
</template>
