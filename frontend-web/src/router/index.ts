import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'visitor-records',
          name: 'VisitorRecords',
          component: () => import('@/views/visitor/VisitorRecordsView.vue'),
          meta: { title: '访客记录' },
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/user/AccountsView.vue'),
          meta: { title: '账号管理' },
        },
        {
          path: 'notify-config',
          name: 'NotifyConfig',
          component: () => import('@/views/config/NotifyConfigView.vue'),
          meta: { title: '通知配置' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
