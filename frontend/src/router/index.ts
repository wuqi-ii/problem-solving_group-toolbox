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
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'workbench', component: () => import('@/views/workbench/DashboardView.vue'), meta: { title: '工作台' } },
        { path: 'groups', name: 'groups', component: () => import('@/views/group/GroupsView.vue'), meta: { title: '小组与成员' } },
        { path: 'tasks', name: 'tasks', component: () => import('@/views/shared/ModulePlaceholder.vue'), meta: { title: '任务与成果', description: '任务发布、进度、提交版本、审核和归档。' } },
        { path: 'files', name: 'files', component: () => import('@/views/shared/ModulePlaceholder.vue'), meta: { title: '共享资料库', description: '小组文件上传、分类、搜索、预览和下载。' } },
        { path: 'transfer', name: 'transfer', component: () => import('@/views/shared/ModulePlaceholder.vue'), meta: { title: '临时文件中转', description: '创建取件码、二维码、有效期和分享记录。' } },
        { path: 'devices', name: 'devices', component: () => import('@/views/shared/ModulePlaceholder.vue'), meta: { title: '我的设备', description: '在本人不同设备之间传递文字、链接和文件。' } },
        { path: 'notifications', name: 'notifications', component: () => import('@/views/shared/ModulePlaceholder.vue'), meta: { title: '通知中心', description: '查看任务、审核、公告和文件相关通知。' } },
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
