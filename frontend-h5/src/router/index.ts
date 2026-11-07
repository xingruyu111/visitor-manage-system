import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/v/home' },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/v/apply',
      name: 'VisitorApply',
      component: () => import('@/views/visitor/ApplyView.vue'),
      meta: { title: '访客预约' },
    },
    {
      path: '/d/batch',
      name: 'DeptBatch',
      component: () => import('@/views/dept/BatchApplyView.vue'),
      meta: { title: '部门批量预约' },
    },
    {
      path: '/a/tasks',
      name: 'ApproverTasks',
      component: () => import('@/views/approver/TasksView.vue'),
      meta: { title: '我的审批' },
    },
    {
      path: '/g/verify',
      name: 'GuardVerify',
      component: () => import('@/views/guard/VerifyView.vue'),
      meta: { title: '门卫核验' },
    },
    {
      path: '/v/records',
      name: 'VisitorRecords',
      component: () => import('@/views/visitor/RecordsView.vue'),
      meta: { title: '我的预约' },
    },
    {
      path: '/v/home',
      name: 'VisitorHome',
      component: () => import('@/views/visitor/HomeView.vue'),
      meta: { title: '访客' },
    },
    {
      path: '/a/home',
      name: 'ApproverHome',
      component: () => import('@/views/approver/HomeView.vue'),
      meta: { title: '审批' },
    },
    {
      path: '/g/home',
      name: 'GuardHome',
      component: () => import('@/views/guard/HomeView.vue'),
      meta: { title: '核验' },
    },
    { path: '/:pathMatch(.*)*', redirect: '/v/home' },
  ],
})

export default router
