<script setup lang="ts">
import {
  Bell,
  Briefcase,
  Collection,
  DataBoard,
  FolderOpened,
  Monitor,
  Promotion,
} from '@element-plus/icons-vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { logout } from '@/api/auth'
import { unreadCount } from '@/api/notifications'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const unread = ref(0)
async function refreshUnread() { try { unread.value = await unreadCount() } catch { /* 页面仍可正常使用 */ } }
onMounted(() => { refreshUnread(); window.addEventListener('notifications-changed', refreshUnread) })
onBeforeUnmount(() => window.removeEventListener('notifications-changed', refreshUnread))

async function signOut() {
  try { await logout() } catch { /* 本地令牌仍需清除 */ }
  session.clearAuth()
  await router.replace('/login')
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">协</div>
        <div>
          <strong>小组协作工具箱</strong>
          <span>Team Toolbox</span>
        </div>
      </div>

      <el-menu :default-active="route.path" router class="nav-menu">
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon><span>个人工作台</span>
        </el-menu-item>
        <el-menu-item index="/groups">
          <el-icon><Collection /></el-icon><span>小组与成员</span>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><Briefcase /></el-icon><span>任务与成果</span>
        </el-menu-item>
        <el-menu-item index="/files">
          <el-icon><FolderOpened /></el-icon><span>共享资料库</span>
        </el-menu-item>
        <el-menu-item index="/transfer">
          <el-icon><Promotion /></el-icon><span>临时文件中转</span>
        </el-menu-item>
        <el-menu-item index="/devices">
          <el-icon><Monitor /></el-icon><span>我的设备</span>
        </el-menu-item>
        <el-menu-item index="/notifications">
          <el-icon><Bell /></el-icon><span>通知中心 <el-badge v-if="unread" :value="unread" :max="99" /></span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-foot">第一版 · 可演示 MVP</div>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <span class="eyebrow">当前页面</span>
          <h1>{{ route.meta.title }}</h1>
        </div>
        <div class="user-actions">
          <span>{{ session.user?.nickname }}</span>
          <el-button text @click="signOut">退出</el-button>
        </div>
      </el-header>
      <el-main class="content"><RouterView /></el-main>
    </el-container>
  </el-container>
</template>
