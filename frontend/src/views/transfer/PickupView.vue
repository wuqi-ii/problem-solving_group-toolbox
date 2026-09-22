<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { downloadPickup, inspectPickup, type PickupView } from '@/api/transfers'
const route = useRoute(); const code = ref(String(route.params.code ?? '')); const item = ref<PickupView>(); const loading = ref(false)
async function inspect() { if (!code.value.trim()) return; loading.value = true; try { item.value = await inspectPickup(code.value.trim()); } catch (e: any) { item.value = undefined; ElMessage.error(e.response?.data?.message ?? '取件码不可用') } finally { loading.value = false } }
async function download() { if (!item.value) return; try { await downloadPickup(code.value.trim(), item.value.fileName); item.value = await inspectPickup(code.value.trim()).catch(() => undefined) } catch (e: any) { ElMessage.error(e.response?.data?.message ?? '下载失败') } }
onMounted(() => { if (code.value) inspect() })
</script>
<template><main class="pickup-page"><div class="pickup-shell"><div class="brand-mark">取</div><span class="eyebrow">TEAM TOOLBOX</span><h1>输入取件码</h1><p>无需登录，只显示完成取件所需的最少信息。</p><div class="pickup-search"><el-input v-model="code" maxlength="10" size="large" placeholder="10 位取件码" @keyup.enter="inspect" /><el-button type="primary" size="large" :loading="loading" @click="inspect">查询</el-button></div><el-card v-if="item" class="pickup-result"><strong>{{ item.fileName }}</strong><span>剩余 {{ item.remainingDownloads }} 次 · {{ new Date(item.expiresAt).toLocaleString('zh-CN', { hour12: false }) }} 到期</span><el-button type="primary" :icon="Download" @click="download">下载文件</el-button></el-card><el-button text class="back-login" @click="$router.push('/')">返回系统</el-button></div></main></template>
