import { createRouter, createWebHistory } from 'vue-router'
import { getSessionToken } from '@/utils/browserSession'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { title: '登录' },
    },
    { path: '/pickup/:code?', name: 'pickup', component: () => import('@/views/transfer/PickupView.vue'), meta: { title: '匿名取件' } },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'workbench', component: () => import('@/views/workbench/DashboardView.vue'), meta: { title: '工作台' } },
        { path: 'groups', name: 'groups', component: () => import('@/views/group/GroupsView.vue'), meta: { title: '小组与成员' } },
        { path: 'groups/:groupId/members/:userId', name: 'member-profile', component: () => import('@/views/group/MemberProfileView.vue'), meta: { title: '成员主页' } },
        { path: 'tasks', name: 'tasks', component: () => import('@/views/task/TasksView.vue'), meta: { title: '任务与成果' } },
        { path: 'collaboration', name: 'collaboration', component: () => import('@/views/collaboration/CollaborationView.vue'), meta: { title: '讨论与公告' } },
        { path: 'search', name: 'search', component: () => import('@/views/search/SearchView.vue'), meta: { title: '统一搜索' } },
        { path: 'files', name: 'files', component: () => import('@/views/file/FilesView.vue'), meta: { title: '共享资料库' } },
        { path: 'transfer', name: 'transfer', component: () => import('@/views/transfer/TransfersView.vue'), meta: { title: '临时文件中转' } },
        { path: 'devices', redirect: '/transfer' },
        { path: 'notifications', name: 'notifications', component: () => import('@/views/notification/NotificationsView.vue'), meta: { title: '通知中心' } },
        { path: 'audit', name: 'audit', component: () => import('@/views/audit/AuditView.vue'), meta: { title: '操作记录' } },
        { path: 'agent', name: 'agent', component: () => import('@/views/agent/AgentReservedView.vue'), meta: { title: 'AI 协作助手' } },
      ],
    },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/system/NotFoundView.vue'), meta: { title: '页面不存在' } },
  ],
})

router.beforeEach((to) => {
  const authenticated = Boolean(getSessionToken())
  if (to.matched.some((record) => record.meta.requiresAuth) && !authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && authenticated) return { name: 'workbench' }
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '首页')} · 小组协作工具箱`
})

export default router
