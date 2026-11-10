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
          path: 'screen',
          name: 'Screen',
          component: () => import('@/views/screen/ScreenView.vue'),
          meta: { title: '数据大屏' },
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
          path: 'staff',
          name: 'Staff',
          component: () => import('@/views/org/StaffView.vue'),
          meta: { title: '人员管理' },
        },
        {
          path: 'counselors',
          name: 'Counselors',
          component: () => import('@/views/org/CounselorView.vue'),
          meta: { title: '辅导员管理' },
        },
        {
          path: 'notify-config',
          name: 'NotifyConfig',
          component: () => import('@/views/config/NotifyConfigView.vue'),
          meta: { title: '通知配置' },
        },
        {
          path: 'approval-flows',
          name: 'ApprovalFlows',
          component: () => import('@/views/flow/FlowDesignerView.vue'),
          meta: { title: '审批流程' },
        },
        {
          path: 'integration-config',
          name: 'IntegrationConfig',
          component: () => import('@/views/config/IntegrationConfigView.vue'),
          meta: { title: '接口配置' },
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/config/SettingsView.vue'),
          meta: { title: '系统参数' },
        },
        {
          path: 'sync-tasks',
          name: 'SyncTasks',
          component: () => import('@/views/config/SyncTaskView.vue'),
          meta: { title: '同步任务' },
        },
        {
          path: 'migration',
          name: 'Migration',
          component: () => import('@/views/migration/MigrationView.vue'),
          meta: { title: '数据迁移' },
        },
        {
          path: 'trend',
          name: 'Trend',
          component: () => import('@/views/dashboard/TrendView.vue'),
          meta: { title: '统计趋势' },
        },
        {
          path: 'audit-logs',
          name: 'AuditLogs',
          component: () => import('@/views/audit/AuditLogView.vue'),
          meta: { title: '审计日志' },
        },
        {
          path: 'blacklist-ledger',
          name: 'BlacklistLedger',
          component: () => import('@/views/audit/AuditLogView.vue'),
          props: { fixedAction: 'BLACKLIST_HIT' },
          meta: { title: '拦截台账' },
        },
        {
          path: 'interface-logs',
          name: 'InterfaceLogs',
          component: () => import('@/views/audit/InterfaceLogView.vue'),
          meta: { title: '接口日志' },
        },
        {
          path: 'ip-blacklist',
          name: 'IpBlacklist',
          component: () => import('@/views/security/IpBlacklistView.vue'),
          meta: { title: 'IP 黑名单' },
        },
        {
          path: 'form-fields',
          name: 'FormFields',
          component: () => import('@/views/config/FormFieldView.vue'),
          meta: { title: '表单字段' },
        },
        {
          path: 'parent-slots',
          name: 'ParentSlots',
          component: () => import('@/views/config/ParentSlotView.vue'),
          meta: { title: '家长时段' },
        },
        {
          path: 'access-list',
          name: 'AccessList',
          component: () => import('@/views/access/AccessListView.vue'),
          meta: { title: '名单管理' },
        },
        {
          path: 'approval-tasks',
          name: 'ApprovalTasks',
          component: () => import('@/views/approval/ApprovalTasksView.vue'),
          meta: { title: '审批任务' },
        },
        {
          path: 'alarms',
          name: 'Alarms',
          component: () => import('@/views/alarm/AlarmListView.vue'),
          meta: { title: '通行告警' },
        },
        {
          path: 'visit-logs',
          name: 'VisitLogs',
          component: () => import('@/views/visitlog/VisitLogView.vue'),
          meta: { title: '通行记录' },
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
