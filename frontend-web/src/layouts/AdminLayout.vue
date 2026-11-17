<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '76px' : '228px'" class="aside">
      <div class="brand" :class="{ collapsed }">
        <div class="brand-mark">访</div>
        <div v-if="!collapsed" class="brand-text">
          <strong>访客管理</strong>
          <span>校园安全运营台</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="menu"
      >
        <div v-if="!collapsed" class="menu-group">概览</div>
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/screen">
          <el-icon><Monitor /></el-icon>
          <span>数据大屏</span>
        </el-menu-item>
        <div v-if="!collapsed" class="menu-group">业务</div>
        <el-menu-item index="/visitor-records">
          <el-icon><Tickets /></el-icon>
          <span>访客记录</span>
        </el-menu-item>
        <el-menu-item index="/access-list">
          <el-icon><Files /></el-icon>
          <span>名单管理</span>
        </el-menu-item>
        <el-menu-item index="/alarms">
          <el-icon><Warning /></el-icon>
          <span>通行告警</span>
        </el-menu-item>
        <el-menu-item index="/visit-logs">
          <el-icon><Position /></el-icon>
          <span>通行记录</span>
        </el-menu-item>
        <div v-if="!collapsed" class="menu-group">人员</div>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>账号管理</span>
        </el-menu-item>
        <el-menu-item index="/staff">
          <el-icon><UserFilled /></el-icon>
          <span>人员管理</span>
        </el-menu-item>
        <el-menu-item index="/counselors">
          <el-icon><Avatar /></el-icon>
          <span>辅导员管理</span>
        </el-menu-item>
        <el-menu-item index="/approval-flows">
          <el-icon><Share /></el-icon>
          <span>审批流程</span>
        </el-menu-item>
        <div v-if="!collapsed" class="menu-group">配置</div>
        <el-menu-item index="/notify-config">
          <el-icon><Bell /></el-icon>
          <span>通知配置</span>
        </el-menu-item>
        <el-menu-item index="/form-fields">
          <el-icon><EditPen /></el-icon>
          <span>表单字段</span>
        </el-menu-item>
        <el-menu-item index="/visit-config">
          <el-icon><Calendar /></el-icon>
          <span>预约配置</span>
        </el-menu-item>
        <el-menu-item index="/integration-config">
          <el-icon><Connection /></el-icon>
          <span>接口配置</span>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统参数</span>
        </el-menu-item>
        <el-menu-item index="/sync-tasks">
          <el-icon><Refresh /></el-icon>
          <span>同步任务</span>
        </el-menu-item>
        <div v-if="!collapsed" class="menu-group">数据与审计</div>
        <el-menu-item index="/migration">
          <el-icon><Upload /></el-icon>
          <span>数据迁移</span>
        </el-menu-item>
        <el-menu-item index="/trend">
          <el-icon><TrendCharts /></el-icon>
          <span>统计趋势</span>
        </el-menu-item>
        <el-menu-item index="/audit-logs">
          <el-icon><Document /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/blacklist-ledger">
          <el-icon><CircleClose /></el-icon>
          <span>拦截台账</span>
        </el-menu-item>
        <el-menu-item index="/interface-logs">
          <el-icon><Link /></el-icon>
          <span>接口日志</span>
        </el-menu-item>
        <el-menu-item index="/ip-blacklist">
          <el-icon><Lock /></el-icon>
          <span>IP 黑名单</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <button class="collapse-btn" type="button" :title="collapsed ? '展开菜单' : '收起菜单'" @click="collapsed = !collapsed">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </button>
          <div class="page-heading">
            <span>校园访客管理</span>
            <strong>{{ pageTitle }}</strong>
          </div>
        </div>
        <div class="user-box">
          <el-dropdown v-if="roles.length > 1" @command="switchRole">
            <span class="role-badge">
              {{ currentRoleName }}<el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in roles"
                  :key="item.code"
                  :command="item.code"
                  :disabled="item.code === me?.currentRole"
                >
                  {{ item.name }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span v-else class="role-badge">{{ currentRoleName }}</span>
          <div class="user-chip">
            <span class="user-avatar">{{ (me?.name || '管').slice(0, 1) }}</span>
            <span class="user-name">{{ me?.name || '管理员' }}</span>
          </div>
          <el-button link @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import { http } from '@/api/request'
import { Odometer, Monitor, Tickets, User, Bell, Share, Connection, Files, UserFilled, EditPen, Calendar, Avatar, Warning, Upload, Position, TrendCharts, Document, Link, CircleClose, Lock, Setting, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

interface RoleItem {
  code: number
  name: string
}

interface Me {
  name?: string
  currentRole?: number
  roles: RoleItem[]
}

const collapsed = ref(false)
const me = ref<Me>()
const roles = ref<RoleItem[]>([])

async function loadMe() {
  try {
    const data = await http.get<Me>('/auth/me')
    me.value = data
    roles.value = data.roles ?? []
  } catch {
    roles.value = []
  }
}

function switchRole(code: number) {
  localStorage.setItem('roleCode', String(code))
  window.location.reload()
}

const currentRoleName = computed(() => {
  const current = roles.value.find((item) => item.code === me.value?.currentRole)
  return current?.name ?? ''
})

onMounted(loadMe)

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string | undefined) ?? '工作台')

function logout() {
  localStorage.removeItem('token')
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background:
    radial-gradient(380px 260px at 30% 0%, rgba(37, 99, 235, 0.2), transparent 70%),
    linear-gradient(180deg, #0b1c2c 0%, #081522 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  transition: width 0.22s ease;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 64px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  white-space: nowrap;
}

.brand.collapsed {
  justify-content: center;
  padding: 0;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  border-radius: 9px;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.32);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
}

.brand-text strong {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.4px;
}

.brand-text span {
  color: #7f9ab4;
  font-size: 10px;
  letter-spacing: 0.6px;
}

.menu {
  flex: 1;
  overflow-y: auto;
  border-right: none;
  padding: 10px 8px;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--vst-sidebar-text);
  --el-menu-hover-bg-color: rgba(51, 93, 128, 0.36);
  --el-menu-active-color: #fff;
}

.menu-group {
  margin: 16px 12px 6px;
  color: #58738c;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1.2px;
}

.menu-group:first-child {
  margin-top: 2px;
}

.menu :deep(.el-menu-item) {
  height: 40px;
  margin: 2px 0;
  border-radius: 8px;
  color: var(--vst-sidebar-text);
  font-size: 13px;
  font-weight: 500;
  transition: background 0.18s ease, color 0.18s ease;
}

.menu :deep(.el-menu-item .el-icon) {
  color: #7893aa;
  font-size: 17px;
}

.menu :deep(.el-menu-item:hover) {
  color: #eaf3ff;
  background: rgba(51, 93, 128, 0.28);
}

.menu :deep(.el-menu-item:hover .el-icon) {
  color: #cfe6ff;
}

.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: linear-gradient(90deg, #1d4ed8, #2563eb);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);
}

.menu :deep(.el-menu-item.is-active .el-icon) {
  color: #fff;
}

.menu.el-menu--collapse :deep(.el-menu-item) {
  justify-content: center;
  padding: 0 !important;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 22px;
  background: #fff;
  border-bottom: 1px solid var(--vst-border);
  box-shadow: 0 1px 0 rgba(15, 35, 65, 0.02);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.collapse-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: #64748b;
  background: #fff;
  border: 1px solid var(--vst-border);
  border-radius: 8px;
  cursor: pointer;
  transition: color 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.collapse-btn:hover {
  color: var(--vst-primary);
  border-color: #bfdbfe;
  background: #f5f9ff;
}

.page-heading {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.35;
}

.page-heading span {
  color: #94a3b8;
  font-size: 10px;
  letter-spacing: 0.4px;
}

.page-heading strong {
  overflow: hidden;
  color: var(--vst-text);
  font-size: 17px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: #475569;
  font-size: 13px;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  color: var(--vst-primary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  background: var(--vst-primary-soft);
  border: 1px solid #dbeafe;
  border-radius: 999px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 8px 3px 3px;
  border-radius: 999px;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-radius: 50%;
}

.main {
  padding: 24px;
  overflow-y: auto;
  background: var(--vst-bg);
}

@media (max-width: 820px) {
  .header {
    padding: 0 14px;
  }

  .user-name {
    display: none;
  }

  .main {
    padding: 16px;
  }
}
</style>
