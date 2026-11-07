import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/v/home' },
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
