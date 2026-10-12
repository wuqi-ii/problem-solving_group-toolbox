<script setup lang="ts">
import {
  Bell,
  Briefcase,
  Collection,
  DataBoard,
  FolderOpened,
  Monitor,
  Promotion,
  MagicStick,
  Menu as MenuIcon,
  ChatLineRound,
  Search,
  DocumentChecked,
} from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { logout } from '@/api/auth'
import { unreadCount } from '@/api/notifications'
import { listGroups, type GroupView } from '@/api/groups'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const unread = ref(0)
const groups = ref<GroupView[]>([])
const mobileMenuVisible = ref(false)
const activeGroupId = computed({
  get: () => session.activeGroupId ?? '',
  set: (value: string) => session.switchGroup(value || null),
})
async function refreshUnread() { try { unread.value = await unreadCount() } catch { /* 页面仍可正常使用 */ } }
async function refreshGroups() {
  try {
    groups.value = await listGroups()
    if (!groups.value.some(group => group.id === session.activeGroupId)) {
      session.switchGroup(groups.value[0]?.id ?? null)
    }
  } catch { /* 子页面会显示具体错误 */ }
}
onMounted(() => {
  refreshUnread()
  refreshGroups()
  window.addEventListener('notifications-changed', refreshUnread)
  window.addEventListener('groups-changed', refreshGroups)
})
onBeforeUnmount(() => {
  window.removeEventListener('notifications-changed', refreshUnread)
  window.removeEventListener('groups-changed', refreshGroups)
})

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
        <el-menu-item index="/collaboration">
          <el-icon><ChatLineRound /></el-icon><span>讨论与公告</span>
        </el-menu-item>
        <el-menu-item index="/search"><el-icon><Search /></el-icon><span>统一搜索</span></el-menu-item>
        <el-menu-item index="/audit"><el-icon><DocumentChecked /></el-icon><span>操作记录</span></el-menu-item>
        <el-menu-item index="/agent">
          <el-icon><MagicStick /></el-icon><span>AI 协作助手</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-foot">第一版 · 可演示 MVP</div>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <el-button class="mobile-menu-button" text :icon="MenuIcon" aria-label="打开导航" @click="mobileMenuVisible = true" />
        <div>
          <span class="eyebrow">当前页面</span>
          <h1>{{ route.meta.title }}</h1>
        </div>
        <div class="user-actions">
          <el-select v-model="activeGroupId" class="topbar-group-select" placeholder="暂无小组" :disabled="groups.length === 0">
            <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
          <span>{{ session.user?.nickname }}</span>
          <el-button text @click="signOut">退出</el-button>
        </div>
      </el-header>
      <el-main class="content"><RouterView /></el-main>
    </el-container>

    <el-drawer v-model="mobileMenuVisible" direction="ltr" size="280px" :with-header="false" class="mobile-nav-drawer">
      <div class="brand mobile-brand">
        <div class="brand-mark">协</div><div><strong>小组协作工具箱</strong><span>Team Toolbox</span></div>
      </div>
      <el-menu :default-active="route.path" router class="mobile-nav-menu" @select="mobileMenuVisible = false">
        <el-menu-item index="/"><el-icon><DataBoard /></el-icon><span>个人工作台</span></el-menu-item>
        <el-menu-item index="/groups"><el-icon><Collection /></el-icon><span>小组与成员</span></el-menu-item>
        <el-menu-item index="/tasks"><el-icon><Briefcase /></el-icon><span>任务与成果</span></el-menu-item>
        <el-menu-item index="/collaboration"><el-icon><ChatLineRound /></el-icon><span>讨论与公告</span></el-menu-item>
        <el-menu-item index="/search"><el-icon><Search /></el-icon><span>统一搜索</span></el-menu-item>
        <el-menu-item index="/files"><el-icon><FolderOpened /></el-icon><span>共享资料库</span></el-menu-item>
        <el-menu-item index="/transfer"><el-icon><Promotion /></el-icon><span>临时文件中转</span></el-menu-item>
        <el-menu-item index="/devices"><el-icon><Monitor /></el-icon><span>我的设备</span></el-menu-item>
        <el-menu-item index="/notifications"><el-icon><Bell /></el-icon><span>通知中心</span></el-menu-item>
        <el-menu-item index="/audit"><el-icon><DocumentChecked /></el-icon><span>操作记录</span></el-menu-item>
        <el-menu-item index="/agent"><el-icon><MagicStick /></el-icon><span>AI 协作助手</span></el-menu-item>
      </el-menu>
    </el-drawer>
  </el-container>
</template>
