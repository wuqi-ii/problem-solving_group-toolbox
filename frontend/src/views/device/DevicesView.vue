<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Delete, Download, Upload } from '@element-plus/icons-vue'
import { createDeviceFile, createDeviceText, deleteDeviceItem, downloadDeviceFile, listDeviceItems, type DeviceItemView } from '@/api/devices'
const items = ref<DeviceItemView[]>([]); const type = ref<'TEXT' | 'LINK'>('TEXT'); const content = ref(''); const sourceDevice = ref('当前设备'); const fileInput = ref<HTMLInputElement>(); const loading = ref(false)
async function refresh() { try { items.value = await listDeviceItems() } catch { ElMessage.error('设备空间加载失败') } }
async function saveText() { if (!content.value.trim()) return ElMessage.warning('请输入内容'); loading.value = true; try { await createDeviceText(type.value, content.value, sourceDevice.value); content.value = ''; await refresh(); ElMessage.success('已同步到设备空间') } catch (e: any) { ElMessage.error(e.response?.data?.message ?? '保存失败') } finally { loading.value = false } }
async function saveFile(event: Event) { const file = (event.target as HTMLInputElement).files?.[0]; if (!file) return; loading.value = true; try { await createDeviceFile(file, sourceDevice.value); await refresh(); ElMessage.success('文件已同步') } catch (e: any) { ElMessage.error(e.response?.data?.message ?? '上传失败') } finally { loading.value = false; (event.target as HTMLInputElement).value = '' } }
async function copy(value: string) { await navigator.clipboard.writeText(value); ElMessage.success('已复制') }
async function remove(item: DeviceItemView) { try { await ElMessageBox.confirm('确定删除这条跨设备内容吗？', '删除内容', { type: 'warning' }); await deleteDeviceItem(item.id); await refresh() } catch { /* user cancelled */ } }
function formatSize(bytes: number | null) { if (bytes == null) return ''; return bytes < 1024 ** 2 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 ** 2).toFixed(1)} MB` }
onMounted(refresh)
</script>
<template>
  <section class="page-heading"><div><span class="eyebrow">MY DEVICES</span><h2>跨设备空间</h2><p>登录同一账号即可传递文字、链接和文件，内容仅本人可见。</p></div></section>
  <div class="tool-grid">
    <el-card class="tool-panel"><template #header><strong>发送到其他设备</strong></template>
      <el-input v-model="sourceDevice" maxlength="100" placeholder="来源设备名称" />
      <el-segmented v-model="type" :options="[{ label: '文字', value: 'TEXT' }, { label: '链接', value: 'LINK' }]" />
      <el-input v-model="content" type="textarea" :rows="5" maxlength="4000" show-word-limit :placeholder="type === 'LINK' ? '粘贴链接' : '输入要传递的文字'" />
      <div class="device-actions"><el-button type="primary" :loading="loading" @click="saveText">保存内容</el-button><input ref="fileInput" class="hidden-input" type="file" @change="saveFile"><el-button :icon="Upload" :loading="loading" @click="fileInput?.click()">上传文件</el-button></div>
    </el-card>
    <el-card class="tool-panel"><template #header><strong>我的内容</strong></template>
      <el-empty v-if="!items.length" description="还没有跨设备内容" />
      <div v-for="item in items" :key="item.id" class="utility-row device-row"><div><el-tag size="small">{{ item.type === 'TEXT' ? '文字' : item.type === 'LINK' ? '链接' : '文件' }}</el-tag><strong>{{ item.type === 'FILE' ? item.fileName : item.content }}</strong><span>{{ item.sourceDevice || '未知设备' }} · {{ new Date(item.createdAt).toLocaleString('zh-CN', { hour12: false }) }} {{ formatSize(item.sizeBytes) }}</span></div><div><el-button v-if="item.type !== 'FILE'" circle :icon="CopyDocument" @click="copy(item.content!)" /><el-button v-else circle :icon="Download" @click="downloadDeviceFile(item)" /><el-button circle type="danger" plain :icon="Delete" @click="remove(item)" /></div></div>
    </el-card>
  </div>
</template>
