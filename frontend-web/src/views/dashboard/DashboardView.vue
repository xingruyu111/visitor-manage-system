<template>
  <div class="dashboard">
    <section class="welcome">
      <div>
        <p class="welcome-kicker">今日校园运行概览</p>
        <h2>{{ greeting }}，{{ userName || '管理员' }}</h2>
        <p class="welcome-desc">访客预约、审批与在校状态，已为你汇总到这一页。</p>
      </div>
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

      <div v-if="(data.pendingTasks ?? 0) > 0" class="pending-banner">
        <el-icon><Clock /></el-icon>
        <span>当前有 <strong>{{ data.pendingTasks }}</strong> 个审批待办，建议及时处理。</span>
        <el-button link type="primary" @click="router.push('/visitor-records')">去处理</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Calendar, Clock, OfficeBuilding, Tickets } from '@element-plus/icons-vue'
import { http } from '@/api/request'

const router = useRouter()
const userName = localStorage.getItem('userName') || ''
const loading = ref(false)
const data = ref<Record<string, number>>({})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const cards = [
  { key: 'today', label: '今日预约', note: '今日计划来访', tone: 'tone-blue', icon: Calendar },
  { key: 'pending', label: '待审批', note: '等待处理', tone: 'tone-amber', icon: Clock },
  { key: 'inSchool', label: '当前在校', note: '实时在访人数', tone: 'tone-green', icon: OfficeBuilding },
  { key: 'pendingTasks', label: '待办任务', note: '审批任务', tone: 'tone-rose', icon: Tickets },
]

const metrics = [
  { key: 'total', label: '累计预约', color: '#2563eb' },
  { key: 'approved', label: '已通过', color: '#16a34a' },
  { key: 'rejected', label: '已拒绝', color: '#dc2626' },
  { key: 'inSchool', label: '已入校', color: '#0ea5e9' },
  { key: 'left', label: '已离校', color: '#8b5cf6' },
  { key: 'expired', label: '已过期', color: '#94a3b8' },
  { key: 'pendingTasks', label: '待办审批', color: '#d97706' },
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

.welcome {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 4px 2px;
}

.welcome-kicker {
  margin: 0 0 8px;
  color: var(--vst-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.8px;
}

.welcome h2 {
  margin: 0;
  color: var(--vst-text);
  font-size: 26px;
  font-weight: 650;
  letter-spacing: -0.5px;
}

.welcome-desc {
  margin: 8px 0 0;
  color: var(--vst-text-secondary);
  font-size: 13px;
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
.tone-rose { --tone: #e11d48; --tone-soft: #fff1f2; }

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

.pending-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 10px 13px;
  color: #92400e;
  font-size: 12px;
  background: #fffbeb;
  border: 1px solid #fef3c7;
  border-radius: 9px;
}

.pending-banner .el-button {
  margin-left: auto;
}

@media (max-width: 1100px) {
  .kpi-grid,
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .welcome {
    align-items: flex-start;
    flex-direction: column;
  }

  .welcome h2 {
    font-size: 22px;
  }

  .kpi-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions {
    width: 100%;
  }

  .quick-actions .el-button {
    flex: 1;
  }
}
</style>
