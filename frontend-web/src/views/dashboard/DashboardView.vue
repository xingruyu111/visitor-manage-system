<template>
  <div class="dashboard">
    <section class="dashboard-top">
      <p class="welcome-kicker">今日校园运行概览</p>
      <div class="quick-actions">
        <el-button @click="router.push('/visitor-records')">查看访客记录</el-button>
        <el-button type="primary" @click="router.push('/screen')">打开数据大屏</el-button>
      </div>
    </section>

    <div class="kpi-grid">
      <article v-for="card in cards" :key="card.key" class="kpi-card" :class="card.tone">
        <div class="kpi-icon"><el-icon><component :is="card.icon" /></el-icon></div>
        <div class="kpi-copy">
          <span>{{ card.label }}</span>
          <strong>{{ data[card.key] ?? 0 }}</strong>
        </div>
        <span class="kpi-note">{{ card.note }}</span>
      </article>
    </div>

    <el-card class="overview" shadow="never">
      <template #header>
        <div class="card-head">
          <div>
            <h3>业务概览</h3>
            <p>预约与通行状态的完整分布</p>
          </div>
          <el-button size="small" :loading="loading" @click="load">刷新数据</el-button>
        </div>
      </template>

      <div class="metric-grid">
        <div v-for="metric in metrics" :key="metric.key" class="metric-item">
          <span class="metric-label">{{ metric.label }}</span>
          <strong>{{ data[metric.key] ?? 0 }}</strong>
          <span class="metric-line" :style="{ background: metric.color }"></span>
        </div>
      </div>


    </el-card>

    <section class="shortcuts">
      <div class="shortcut-head">
        <h3>常用入口</h3>
        <span>高频操作，一步直达</span>
      </div>
      <div class="shortcut-grid">
        <button v-for="item in shortcuts" :key="item.path" type="button" class="shortcut-card" @click="router.push(item.path)">
          <span class="shortcut-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span class="shortcut-copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.desc }}</small>
          </span>
          <el-icon class="shortcut-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Calendar, Clock, Files, Monitor, OfficeBuilding, Setting, Tickets } from '@element-plus/icons-vue'
import { http } from '@/api/request'

const router = useRouter()
const loading = ref(false)
const data = ref<Record<string, number>>({})


const cards = [
  { key: 'today', label: '今日预约', note: '今日计划来访', tone: 'tone-blue', icon: Calendar },
  { key: 'pending', label: '待审批', note: '等待处理', tone: 'tone-amber', icon: Clock },
  { key: 'inSchool', label: '当前在校', note: '实时在访人数', tone: 'tone-green', icon: OfficeBuilding },
  { key: 'total', label: '累计预约', note: '历史预约总量', tone: 'tone-violet', icon: Tickets },
]

const shortcuts = [
  { label: '访客记录', desc: '查询预约与通行状态', path: '/visitor-records', icon: Tickets },
  { label: '名单管理', desc: '维护黑名单与白名单', path: '/access-list', icon: Files },
  { label: '预约配置', desc: '设置访客类型与时间段', path: '/visit-config', icon: Setting },
  { label: '数据大屏', desc: '查看校园访客态势', path: '/screen', icon: Monitor },
]

const metrics = [
  { key: 'total', label: '累计预约', color: '#2563eb' },
  { key: 'approved', label: '已通过', color: '#16a34a' },
  { key: 'rejected', label: '已拒绝', color: '#dc2626' },
  { key: 'inSchool', label: '已入校', color: '#0ea5e9' },
  { key: 'left', label: '已离校', color: '#8b5cf6' },
  { key: 'expired', label: '已过期', color: '#94a3b8' },
  { key: 'activeBlacklist', label: '有效黑名单', color: '#e11d48' },
]

async function load() {
  loading.value = true
  try {
    data.value = await http.get<Record<string, number>>('/web/admin/dashboard/summary')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dashboard-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 2px;
}

.welcome-kicker {
  margin: 0;
  color: var(--vst-primary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.4px;
}

.quick-actions {
  display: flex;
  gap: 8px;
  flex: 0 0 auto;
}


.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.kpi-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  overflow: hidden;
  padding: 18px;
  background: #fff;
  border: 1px solid var(--vst-border);
  border-radius: var(--vst-radius);
  box-shadow: var(--vst-shadow);
}

.kpi-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 2px;
  background: var(--tone);
}

.kpi-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  color: var(--tone);
  background: var(--tone-soft);
  border-radius: 9px;
}

.kpi-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.kpi-copy span {
  color: var(--vst-text-secondary);
  font-size: 12px;
}

.kpi-copy strong {
  margin-top: 4px;
  color: var(--vst-text);
  font-size: 28px;
  font-weight: 650;
  line-height: 1.2;
}

.kpi-note {
  margin-left: auto;
  color: #94a3b8;
  font-size: 10px;
  white-space: nowrap;
}

.tone-blue { --tone: #2563eb; --tone-soft: #eff6ff; }
.tone-amber { --tone: #d97706; --tone-soft: #fffbeb; }
.tone-green { --tone: #16a34a; --tone-soft: #f0fdf4; }
.tone-violet { --tone: #7c3aed; --tone-soft: #f5f3ff; }

.overview :deep(.el-card__header) {
  padding: 17px 20px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.card-head h3 {
  margin: 0;
  color: var(--vst-text);
  font-size: 15px;
  font-weight: 600;
}

.card-head p {
  margin: 4px 0 0;
  color: var(--vst-text-secondary);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  position: relative;
  padding: 14px 15px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 10px;
}

.metric-label {
  display: block;
  color: var(--vst-text-secondary);
  font-size: 12px;
}

.metric-item strong {
  display: block;
  margin-top: 5px;
  color: var(--vst-text);
  font-size: 23px;
  font-weight: 650;
}

.metric-line {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 28px;
  height: 3px;
  border-radius: 0 3px 0 0;
}


@media (max-width: 1100px) {
  .kpi-grid,
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .dashboard-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .quick-actions {
    width: 100%;
  }

  .quick-actions .el-button {
    flex: 1;
  }

  .kpi-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
.shortcuts {
  padding: 2px;
}

.shortcut-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.shortcut-head h3 {
  margin: 0;
  color: var(--vst-text);
  font-size: 15px;
  font-weight: 600;
}

.shortcut-head span {
  color: var(--vst-text-secondary);
  font-size: 12px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.shortcut-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  text-align: left;
  background: #fff;
  border: 1px solid var(--vst-border);
  border-radius: var(--vst-radius);
  box-shadow: var(--vst-shadow);
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.shortcut-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.shortcut-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  color: var(--vst-primary);
  background: var(--vst-primary-soft);
  border-radius: 9px;
}

.shortcut-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 2px;
}

.shortcut-copy strong {
  color: var(--vst-text);
  font-size: 13px;
  font-weight: 600;
}

.shortcut-copy small {
  overflow: hidden;
  color: var(--vst-text-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shortcut-arrow {
  margin-left: auto;
  color: #cbd5e1;
}

@media (max-width: 1100px) {
  .shortcut-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .shortcut-grid {
    grid-template-columns: 1fr;
  }
}
</style>
