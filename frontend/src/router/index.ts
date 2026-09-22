import { createRouter, createWebHistory } from 'vue-router'

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
        { path: 'tasks', name: 'tasks', component: () => import('@/views/task/TasksView.vue'), meta: { title: '任务与成果' } },
        { path: 'files', name: 'files', component: () => import('@/views/file/FilesView.vue'), meta: { title: '共享资料库' } },
        { path: 'transfer', name: 'transfer', component: () => import('@/views/transfer/TransfersView.vue'), meta: { title: '临时文件中转' } },
        { path: 'devices', name: 'devices', component: () => import('@/views/device/DevicesView.vue'), meta: { title: '我的设备' } },
        { path: 'notifications', name: 'notifications', component: () => import('@/views/notification/NotificationsView.vue'), meta: { title: '通知中心' } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authenticated = Boolean(localStorage.getItem('team-toolbox-token'))
  if (to.matched.some((record) => record.meta.requiresAuth) && !authenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && authenticated) return { name: 'workbench' }
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? '首页')} · 小组协作工具箱`
})

export default router
