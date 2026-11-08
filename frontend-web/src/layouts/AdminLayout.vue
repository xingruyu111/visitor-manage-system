<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">智慧校园访客</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>工作台</span>
        </el-menu-item>
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
import { Odometer, Tickets, User, Bell, Share, Connection, Files, Checked, UserFilled, EditPen, Calendar, School, Warning, Upload, Position, TrendCharts, Document, Link, CircleClose } from '@element-plus/icons-vue'

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
  background: #1f2d3d;
  color: #fff;
}

.brand {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.menu {
  border-right: none;
  background: transparent;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.main {
  padding: 20px;
}
</style>
