<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">访客管理系统</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <div class="menu-group">概览</div>
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/screen">
          <el-icon><Monitor /></el-icon>
          <span>数据大屏</span>
        </el-menu-item>
        <div class="menu-group">业务</div>
        <el-menu-item index="/visitor-records">
          <el-icon><Tickets /></el-icon>
          <span>访客记录</span>
        </el-menu-item>
        <el-menu-item index="/access-list">
          <el-icon><Files /></el-icon>
          <span>名单管理</span>
        </el-menu-item>
        <el-menu-item index="/approval-tasks">
          <el-icon><Checked /></el-icon>
          <span>审批任务</span>
        </el-menu-item>
        <el-menu-item index="/alarms">
          <el-icon><Warning /></el-icon>
          <span>通行告警</span>
        </el-menu-item>
        <el-menu-item index="/visit-logs">
          <el-icon><Position /></el-icon>
          <span>通行记录</span>
        </el-menu-item>
        <div class="menu-group">人员</div>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>账号管理</span>
        </el-menu-item>
        <el-menu-item index="/staff">
          <el-icon><UserFilled /></el-icon>
          <span>人员管理</span>
        </el-menu-item>
        <el-menu-item index="/classes">
          <el-icon><School /></el-icon>
          <span>班级管理</span>
        </el-menu-item>
        <el-menu-item index="/approval-flows">
          <el-icon><Share /></el-icon>
          <span>审批流程</span>
        </el-menu-item>
        <div class="menu-group">配置</div>
        <el-menu-item index="/notify-config">
          <el-icon><Bell /></el-icon>
          <span>通知配置</span>
        </el-menu-item>
        <el-menu-item index="/form-fields">
          <el-icon><EditPen /></el-icon>
          <span>表单字段</span>
        </el-menu-item>
        <el-menu-item index="/parent-slots">
          <el-icon><Calendar /></el-icon>
          <span>家长时段</span>
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
        <div class="menu-group">数据与审计</div>
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
        <span class="title">{{ pageTitle }}</span>
        <el-button link type="primary" @click="logout">退出登录</el-button>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Odometer, Monitor, Tickets, User, Bell, Share, Connection, Files, Checked, UserFilled, EditPen, Calendar, School, Warning, Upload, Position, TrendCharts, Document, Link, CircleClose, Lock, Setting, Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

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
  background: #ffffff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.brand {
  height: 64px;
  line-height: 64px;
  padding: 0 22px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #0a0a0a;
  border-bottom: 1px solid #f0f0f0;
}

.menu {
  border-right: none;
  background: transparent;
  padding: 10px;
  overflow-y: auto;
  flex: 1;
}

.menu-group {
  margin: 14px 10px 6px;
  font-size: 11px;
  letter-spacing: 1.5px;
  color: #9a9a9a;
  font-weight: 600;
}

.menu-group:first-child {
  margin-top: 4px;
}

.menu :deep(.el-menu-item) {
  height: 42px;
  line-height: 42px;
  margin-bottom: 2px;
  border-radius: 6px;
  color: #4d4d4d;
  font-weight: 500;
}

.menu :deep(.el-menu-item:hover) {
  background: #f2f2f2;
  color: #0a0a0a;
}

.menu :deep(.el-menu-item.is-active) {
  background: #0a0a0a;
  color: #ffffff;
}

.menu :deep(.el-menu-item.is-active .el-icon) {
  color: #ffffff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 24px;
}

.title {
  font-size: 17px;
  font-weight: 650;
  letter-spacing: 0.2px;
  color: #0a0a0a;
}

.main {
  padding: 20px;
  background: #f5f5f5;
}
</style>
