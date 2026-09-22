<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { listNotifications, markAllNotificationsRead, markNotificationRead, type NotificationView } from '@/api/notifications'
const router = useRouter(); const items = ref<NotificationView[]>([]); const loading = ref(false)
async function refresh() { loading.value = true; try { items.value = await listNotifications() } catch { ElMessage.error('通知加载失败') } finally { loading.value = false } }
async function open(item: NotificationView) { if (!item.read) { await markNotificationRead(item.id); item.read = true; window.dispatchEvent(new Event('notifications-changed')) } if (item.targetPath) await router.push(item.targetPath) }
async function readAll() { await markAllNotificationsRead(); items.value.forEach(item => item.read = true); window.dispatchEvent(new Event('notifications-changed')); ElMessage.success('已全部标为已读') }
onMounted(refresh)
</script>
<template><section class="page-heading"><div><span class="eyebrow">NOTIFICATIONS</span><h2>通知中心</h2><p>任务指派和审核结果会集中出现在这里。</p></div><el-button :icon="Check" :disabled="!items.some(i => !i.read)" @click="readAll">全部已读</el-button></section><el-skeleton v-if="loading" :rows="5" animated /><el-empty v-else-if="!items.length" description="暂时没有通知" /><div v-else class="notification-list"><button v-for="item in items" :key="item.id" class="notification-item" :class="{ unread: !item.read }" @click="open(item)"><span class="notification-dot" /><div><strong>{{ item.title }}</strong><p>{{ item.content }}</p><small>{{ new Date(item.createdAt).toLocaleString('zh-CN', { hour12: false }) }}</small></div><el-icon v-if="item.read"><Check /></el-icon></button></div></template>
