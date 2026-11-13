<template>
  <div class="screen">
    <div class="ambient ambient-a"></div>
    <div class="ambient ambient-b"></div>
    <div class="screen-grid"></div>

    <header class="topbar">
      <div class="brand-block">
        <div class="brand-mark">访</div>
        <div class="brand-copy">
          <div class="eyebrow">SMART CAMPUS · VISITOR INTELLIGENCE</div>
          <h1>校园访客态势感知</h1>
        </div>
      </div>

      <div class="top-actions">
        <div class="live-chip"><span class="live-dot"></span>实时数据</div>
        <div class="clock-block">
          <span>{{ dateText }}</span>
          <strong>{{ timeText }}</strong>
        </div>
        <button class="icon-button" title="全屏" @click="toggleFullscreen">
          <el-icon><FullScreen /></el-icon>
        </button>
        <button class="exit-button" @click="router.push('/dashboard')">
          <el-icon><Back /></el-icon>
          退出大屏
        </button>
      </div>
    </header>

    <section class="kpi-grid">
      <article v-for="(item, index) in kpis" :key="item.label" class="kpi-card" :class="item.tone">
        <div class="kpi-top">
          <span class="kpi-no">{{ String(index + 1).padStart(2, '0') }}</span>
          <el-icon class="kpi-icon"><component :is="item.icon" /></el-icon>
        </div>
        <div class="kpi-value">
          {{ formatNumber(item.value) }}<small v-if="item.unit">{{ item.unit }}</small>
        </div>
        <div class="kpi-label">{{ item.label }}</div>
        <div class="kpi-meta">{{ item.meta }}</div>
      </article>
    </section>

    <div v-if="error" class="load-error">{{ error }}</div>
    <div v-if="loading" class="loading-mask">
      <span class="loader"></span>
      <span>正在汇聚校园访客数据…</span>
    </div>

    <main class="main-grid">
      <section class="panel status-panel">
        <div class="panel-head">
          <div>
            <span class="panel-index">01</span>
            <h2>预约状态分布</h2>
          </div>
          <span class="panel-sub">LIFECYCLE</span>
        </div>
        <div class="status-body">
          <div class="donut-wrap">
            <VChart class="chart" :option="statusOption" autoresize />
            <div class="donut-center">
              <strong>{{ formatNumber(summary.total) }}</strong>
              <span>累计预约</span>
            </div>
          </div>
          <div class="status-legend">
            <div v-for="item in statusRows" :key="item.name" class="legend-row">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span class="legend-name">{{ item.name }}</span>
              <strong>{{ formatNumber(item.value) }}</strong>
            </div>
          </div>
        </div>
      </section>

      <section class="panel trend-panel">
        <div class="panel-head">
          <div>
            <span class="panel-index">02</span>
            <h2>近 14 天预约趋势</h2>
          </div>
          <span class="panel-sub">TRAFFIC OVERVIEW</span>
        </div>
        <VChart class="chart trend-chart" :option="trendOption" autoresize />
      </section>

      <section class="panel health-panel">
        <div class="panel-head">
          <div>
            <span class="panel-index">03</span>
            <h2>运行健康度</h2>
          </div>
          <span class="panel-sub">OPERATIONS</span>
        </div>
        <div class="health-body">
          <div class="ring-wrap">
            <div class="ring" :style="ringStyle">
              <div class="ring-inner">
                <strong>{{ approvalRate }}<small>%</small></strong>
                <span>审批通过率</span>
              </div>
            </div>
          </div>
          <div class="health-metrics">
            <div class="health-row">
              <span>待处理任务</span>
              <strong>{{ formatNumber(summary.pendingTasks) }}</strong>
            </div>
            <div class="health-row">
              <span>黑名单拦截</span>
              <strong class="warn">{{ formatNumber(trend.blacklistHits) }}</strong>
            </div>
            <div class="health-row">
              <span>接口异常</span>
              <strong :class="trend.integrationErrors > 0 ? 'danger' : 'ok'">
                {{ formatNumber(trend.integrationErrors) }}
              </strong>
            </div>
          </div>
        </div>
      </section>

      <section class="panel hourly-panel">
        <div class="panel-head">
          <div>
            <span class="panel-index">04</span>
            <h2>今日到访时段</h2>
          </div>
          <span class="panel-sub">HOURLY ARRIVALS</span>
        </div>
        <VChart class="chart hourly-chart" :option="hourlyOption" autoresize />
      </section>

      <section class="panel monthly-panel">
        <div class="panel-head">
          <div>
            <span class="panel-index">05</span>
            <h2>月度预约与通过率</h2>
          </div>
          <span class="panel-sub">6 MONTHS</span>
        </div>
        <VChart class="chart monthly-chart" :option="monthlyOption" autoresize />
      </section>

      <section class="panel composition-panel">
        <div class="panel-head">
          <div>
            <span class="panel-index">06</span>
            <h2>访客构成</h2>
          </div>
          <span class="panel-sub">VISITOR MIX</span>
        </div>
        <div class="composition-body">
          <div class="composition-block">
            <div class="mini-title">访客类型</div>
            <div v-for="item in visitorTypeRows" :key="item.code" class="bar-row">
              <div class="bar-label"><span>{{ item.label }}</span><strong>{{ item.total }}</strong></div>
              <div class="bar-track"><i :style="{ width: item.width, background: item.color }"></i></div>
            </div>
            <div v-if="visitorTypeRows.length === 0" class="empty-line">暂无访客数据</div>
          </div>
          <div class="composition-block">
            <div class="mini-title">热门被访人</div>
            <div v-for="(item, index) in topTargets" :key="item.name" class="rank-row">
              <span class="rank-no">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="rank-name">{{ item.name }}</span>
              <strong>{{ formatNumber(item.total) }}</strong>
            </div>
            <div v-if="topTargets.length === 0" class="empty-line">暂无被访人数据</div>
          </div>
        </div>
      </section>

      <section class="panel recent-panel">
        <div class="panel-head recent-head">
          <div>
            <span class="panel-index">07</span>
            <h2>最新访客动态</h2>
          </div>
          <span class="panel-sub">LATEST ACTIVITY</span>
        </div>
        <div v-if="recent.length === 0" class="empty-line recent-empty">暂无最新访客记录</div>
        <div v-else class="recent-list">
          <div v-for="item in recent" :key="item.id" class="recent-row">
            <span class="recent-avatar">{{ (item.visitorName || '访').slice(0, 1) }}</span>
            <div class="recent-main">
              <strong>{{ item.visitorName || '访客' }}</strong>
              <span>拜访 {{ item.targetName || '—' }}</span>
            </div>
            <span class="recent-type">{{ visitorTypeLabel(item.visitorType) }}</span>
            <span class="recent-time">{{ formatTime(item.expectedTime) }}</span>
            <span class="recent-status" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
          </div>
        </div>
      </section>
    </main>

    <footer class="screen-footer">
      <span>数据更新于 {{ updatedText }}</span>
      <span class="footer-sep">·</span>
      <span>每 60 秒自动刷新</span>
      <span class="footer-right">SECURITY OPERATIONS CENTER / 01</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Back,
  Calendar,
  CircleCheck,
  Clock,
  FullScreen,
  OfficeBuilding,
  Position,
  Warning,
} from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { http } from '@/api/request'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
])

interface Summary {
  total: number
  today: number
  pending: number
  approved: number
  rejected: number
  inSchool: number
  left: number
  expired: number
  pendingTasks: number
  activeBlacklist: number
  generatedAt?: string
}

interface TrendDay {
  date: string
  total: number
  approved: number
  rejected: number
  left: number
}

interface TrendData {
  daily: TrendDay[]
  approvalRate: number
  blacklistHits: number
  integrationErrors: number
}

interface MonthlyData {
  month: string
  total: number
  approved: number
  rejected: number
  left: number
  approvalRate: number
}

interface DistributionRow {
  type?: string
  name?: string
  total: number
  value?: number
}

interface RecentItem {
  id: number
  visitorName?: string
  targetName?: string
  visitorType?: string
  expectedTime?: string
  status?: number
}

interface ScreenData {
  summary: Summary
  trend: TrendData
  monthly: MonthlyData[]
  statusDistribution: Array<{ name: string; value: number }>
  visitorTypeDistribution: DistributionRow[]
  targetTypeDistribution: DistributionRow[]
  hourly: Array<{ hour: number; total: number }>
  topTargets: Array<{ name: string; total: number }>
  recent: RecentItem[]
}

const router = useRouter()
const emptySummary: Summary = {
  total: 0, today: 0, pending: 0, approved: 0, rejected: 0,
  inSchool: 0, left: 0, expired: 0, pendingTasks: 0, activeBlacklist: 0,
}
const data = ref<ScreenData>({
  summary: emptySummary,
  trend: { daily: [], approvalRate: 0, blacklistHits: 0, integrationErrors: 0 },
  monthly: [],
  statusDistribution: [],
  visitorTypeDistribution: [],
  targetTypeDistribution: [],
  hourly: [],
  topTargets: [],
  recent: [],
})
const loading = ref(true)
const error = ref('')
const timeText = ref('')
const now = ref(new Date())
const updatedText = ref('—')
let clockTimer: number | undefined
let refreshTimer: number | undefined

const summary = computed(() => data.value.summary ?? emptySummary)
const trend = computed(() => data.value.trend ?? {
  daily: [], approvalRate: 0, blacklistHits: 0, integrationErrors: 0,
})
const approvalRate = computed(() => Number(trend.value.approvalRate ?? 0).toFixed(1))
const recent = computed(() => data.value.recent ?? [])
const topTargets = computed(() => data.value.topTargets ?? [])

const dateText = computed(() => {
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${now.value.getFullYear()}年${now.value.getMonth() + 1}月${now.value.getDate()}日 ${weekdays[now.value.getDay()]}`
})

const todayLeft = computed(() => {
  const daily = trend.value.daily ?? []
  return daily.length === 0 ? 0 : daily[daily.length - 1]?.left ?? 0
})

const kpis = computed(() => [
  {
    label: '今日预约', value: summary.value.today, unit: '次',
    meta: 'TODAY BOOKINGS', tone: 'ambient-cyan', icon: markRaw(Calendar),
  },
  {
    label: '待审批', value: summary.value.pending, unit: '条',
    meta: `${summary.value.pendingTasks ?? 0} 个待办任务`, tone: 'ambient-amber', icon: markRaw(Clock),
  },
  {
    label: '当前在校', value: summary.value.inSchool, unit: '人',
    meta: 'ON CAMPUS NOW', tone: 'ambient-green', icon: markRaw(OfficeBuilding),
  },
  {
    label: '今日离校', value: todayLeft.value, unit: '人',
    meta: 'CHECKED OUT TODAY', tone: 'ambient-blue', icon: markRaw(Position),
  },
  {
    label: '审批通过率', value: Number(approvalRate.value), unit: '%',
    meta: '14 DAY APPROVAL RATE', tone: 'ambient-green', icon: markRaw(CircleCheck),
  },
  {
    label: '有效黑名单', value: summary.value.activeBlacklist, unit: '人',
    meta: 'ACTIVE WATCHLIST', tone: 'ambient-rose', icon: markRaw(Warning),
  },
])

const statusColors = ['#fbbf24', '#34d399', '#fb7185', '#38bdf8', '#a78bfa', '#64748b']
const statusRows = computed(() => {
  const rows = data.value.statusDistribution ?? []
  return rows.map((item, index) => ({ ...item, color: statusColors[index % statusColors.length] }))
})

const statusOption = computed(() => ({
  backgroundColor: 'transparent',
  color: statusColors,
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(8, 20, 36, .96)',
    borderColor: 'rgba(96, 165, 250, .35)',
    textStyle: { color: '#dbeafe' },
    formatter: '{b}<br/>{c} 条 · {d}%',
  },
  series: [{
    type: 'pie',
    radius: ['58%', '78%'],
    center: ['50%', '50%'],
    avoidLabelOverlap: true,
    itemStyle: { borderColor: '#07111f', borderWidth: 4 },
    label: { show: false },
    labelLine: { show: false },
    data: statusRows.value.map((item) => ({ name: item.name, value: item.value })),
  }],
}))

const trendOption = computed(() => {
  const daily = trend.value.daily ?? []
  return {
    backgroundColor: 'transparent',
    animationDuration: 700,
    color: ['#38bdf8', '#34d399', '#fb7185'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(8, 20, 36, .96)',
      borderColor: 'rgba(96, 165, 250, .35)',
      textStyle: { color: '#dbeafe' },
    },
    legend: {
      right: 6,
      top: 0,
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#94a3b8', fontSize: 11 },
      data: ['预约总量', '已通过', '已驳回'],
    },
    grid: { left: 42, right: 18, top: 38, bottom: 24 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: daily.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, .25)' } },
      axisLabel: { color: '#64748b', fontSize: 10 },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, .12)', type: 'dashed' } },
      axisLabel: { color: '#64748b', fontSize: 10 },
    },
    series: [
      {
        name: '预约总量',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 2.5, color: '#38bdf8' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(56, 189, 248, .32)' },
              { offset: 1, color: 'rgba(56, 189, 248, .01)' },
            ],
          },
        },
        data: daily.map((item) => item.total),
      },
      {
        name: '已通过',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.6, color: '#34d399' },
        data: daily.map((item) => item.approved),
      },
      {
        name: '已驳回',
        type: 'line',
        smooth: true,
        symbol: 'none',
        lineStyle: { width: 1.4, color: '#fb7185', type: 'dashed' },
        data: daily.map((item) => item.rejected),
      },
    ],
  }
})

const hourlyOption = computed(() => {
  const hourly = data.value.hourly ?? []
  return {
    backgroundColor: 'transparent',
    animationDuration: 700,
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(8, 20, 36, .96)',
      borderColor: 'rgba(96, 165, 250, .35)',
      textStyle: { color: '#dbeafe' },
      formatter: (params: Array<{ name: string; value: number }>) => `${params[0]?.name}:00<br/>${params[0]?.value ?? 0} 人`,
    },
    grid: { left: 34, right: 12, top: 16, bottom: 24 },
    xAxis: {
      type: 'category',
      data: hourly.map((item) => `${String(item.hour).padStart(2, '0')}`),
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, .22)' } },
      axisLabel: {
        color: '#64748b',
        fontSize: 10,
        interval: 2,
        formatter: (value: string) => `${value}:00`,
      },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(148, 163, 184, .1)', type: 'dashed' } },
      axisLabel: { color: '#64748b', fontSize: 10 },
    },
    series: [{
      type: 'bar',
      barWidth: '44%',
      data: hourly.map((item) => item.total),
      itemStyle: {
        borderRadius: [3, 3, 0, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#67e8f9' },
            { offset: 1, color: 'rgba(14, 116, 144, .18)' },
          ],
        },
      },
    }],
  }
})

const monthlyOption = computed(() => {
  const monthly = data.value.monthly ?? []
  return {
    backgroundColor: 'transparent',
    animationDuration: 700,
    color: ['#6366f1', '#fbbf24'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(8, 20, 36, .96)',
      borderColor: 'rgba(96, 165, 250, .35)',
      textStyle: { color: '#dbeafe' },
    },
    legend: {
      right: 4,
      top: 0,
      itemWidth: 8,
      itemHeight: 8,
      textStyle: { color: '#94a3b8', fontSize: 10 },
      data: ['预约量', '通过率'],
    },
    grid: { left: 38, right: 42, top: 32, bottom: 24 },
    xAxis: {
      type: 'category',
      data: monthly.map((item) => item.month.slice(5)),
      axisLine: { lineStyle: { color: 'rgba(148, 163, 184, .22)' } },
      axisLabel: { color: '#64748b', fontSize: 10 },
      axisTick: { show: false },
    },
    yAxis: [
      {
        type: 'value',
        splitLine: { lineStyle: { color: 'rgba(148, 163, 184, .1)', type: 'dashed' } },
        axisLabel: { color: '#64748b', fontSize: 10 },
      },
      {
        type: 'value',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: { color: '#64748b', fontSize: 10, formatter: '{value}%' },
      },
    ],
    series: [
      {
        name: '预约量',
        type: 'bar',
        barWidth: '38%',
        data: monthly.map((item) => item.total),
        itemStyle: { borderRadius: [3, 3, 0, 0], color: 'rgba(99, 102, 241, .72)' },
      },
      {
        name: '通过率',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { width: 2, color: '#fbbf24' },
        itemStyle: { color: '#fbbf24' },
        data: monthly.map((item) => item.approvalRate),
      },
    ],
  }
})

const ringStyle = computed(() => {
  const degree = Math.min(360, Math.max(0, Number(approvalRate.value) * 3.6))
  return {
    background: `conic-gradient(#34d399 0deg, #34d399 ${degree}deg, rgba(52, 211, 153, .08) ${degree}deg, rgba(52, 211, 153, .08) 360deg)`,
  }
})

const VISITOR_TYPE_LABELS: Record<string, string> = {
  NORMAL: '普通访客',
  PARENT: '学生家长',
  ALUMNI: '校友',
  CONSTRUCTION: '施工/合作',
}
const visitorTypeRows = computed(() => {
  const rows = data.value.visitorTypeDistribution ?? []
  const max = Math.max(1, ...rows.map((item) => Number(item.total) || 0))
  return rows.slice(0, 5).map((item, index) => ({
    code: item.type || item.name || '—',
    label: VISITOR_TYPE_LABELS[item.type || ''] || item.type || '其他',
    total: item.total ?? 0,
    width: `${Math.max(4, (Number(item.total) / max) * 100)}%`,
    color: ['#38bdf8', '#818cf8', '#34d399', '#fbbf24', '#fb7185'][index % 5],
  }))
})

function formatNumber(value: number | undefined) {
  return new Intl.NumberFormat('zh-CN').format(Number(value ?? 0))
}

function visitorTypeLabel(type?: string) {
  return type ? (VISITOR_TYPE_LABELS[type] || type) : '访客'
}

function formatTime(value?: string) {
  if (!value) return '—'
  return value.replace('T', ' ').slice(5, 16)
}

const STATUS_TEXT: Record<number, string> = {
  0: '待审批', 1: '已通过', 2: '已驳回', 3: '在校中', 4: '已离校', 5: '已取消', 6: '已过期',
}
function statusText(status?: number) {
  return STATUS_TEXT[status ?? -1] ?? '未知'
}
function statusClass(status?: number) {
  if (status === 1) return 's-green'
  if (status === 2) return 's-red'
  if (status === 3) return 's-blue'
  if (status === 0) return 's-amber'
  return 's-muted'
}

function updateClock() {
  now.value = new Date()
  timeText.value = now.value.toLocaleTimeString('zh-CN', { hour12: false })
}

async function load() {
  try {
    const payload = await http.get<ScreenData>('/web/admin/dashboard/screen')
    data.value = payload
    updatedText.value = payload.summary?.generatedAt
      ? payload.summary.generatedAt.replace('T', ' ').slice(11, 19)
      : new Date().toLocaleTimeString('zh-CN', { hour12: false })
    error.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : '数据加载失败，稍后自动重试'
  } finally {
    loading.value = false
  }
}

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen()
    } else {
      await document.documentElement.requestFullscreen()
    }
  } catch {
    // 浏览器拒绝全屏时保持页面状态
  }
}

onMounted(() => {
  updateClock()
  load()
  clockTimer = window.setInterval(updateClock, 1000)
  refreshTimer = window.setInterval(load, 60_000)
})

onBeforeUnmount(() => {
  if (clockTimer) window.clearInterval(clockTimer)
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<style scoped>
.screen {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  padding: 22px 30px 16px;
  box-sizing: border-box;
  color: #dbeafe;
  background:
    radial-gradient(1200px 700px at 12% -10%, rgba(37, 99, 235, .23), transparent 62%),
    radial-gradient(1000px 620px at 95% 4%, rgba(16, 185, 129, .12), transparent 58%),
    linear-gradient(135deg, #050b14 0%, #071525 50%, #050d18 100%);
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.screen-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: .34;
  background-image:
    linear-gradient(rgba(148, 163, 184, .06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, .06) 1px, transparent 1px);
  background-size: 64px 64px;
  mask-image: linear-gradient(to bottom, black, transparent 85%);
}
.ambient {
  position: absolute;
  width: 500px;
  height: 500px;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
  opacity: .13;
}
.ambient-a { left: 25%; top: -260px; background: #38bdf8; }
.ambient-b { right: -180px; bottom: -260px; background: #34d399; }
.topbar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, .14);
}
.brand-block { display: flex; align-items: center; gap: 14px; }
.brand-mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: #06111f;
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, #67e8f9, #38bdf8 52%, #818cf8);
  box-shadow: 0 0 28px rgba(56, 189, 248, .3);
}
.eyebrow {
  color: #38bdf8;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 2.6px;
}
h1 {
  margin: 4px 0 0;
  color: #f8fafc;
  font-size: 27px;
  line-height: 1.2;
  letter-spacing: 3px;
  font-weight: 700;
}
.top-actions { display: flex; align-items: center; gap: 15px; }
.live-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 11px;
  border: 1px solid rgba(52, 211, 153, .25);
  border-radius: 999px;
  color: #86efac;
  font-size: 11px;
  letter-spacing: .8px;
  background: rgba(52, 211, 153, .06);
}
.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, .15);
  animation: pulse 2s ease-in-out infinite;
}
.clock-block { display: flex; flex-direction: column; text-align: right; line-height: 1.5; }
.clock-block span { color: #64748b; font-size: 11px; }
.clock-block strong {
  color: #e2e8f0;
  font-family: 'Cascadia Mono', 'Consolas', monospace;
  font-size: 17px;
  letter-spacing: 1.4px;
  font-weight: 500;
}
.icon-button,
.exit-button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 11px;
  border: 1px solid rgba(148, 163, 184, .2);
  border-radius: 7px;
  background: rgba(15, 31, 50, .55);
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  transition: border-color .2s, color .2s, background .2s;
}
.icon-button { width: 32px; justify-content: center; padding: 0; }
.icon-button:hover,
.exit-button:hover { color: #e2e8f0; border-color: rgba(56, 189, 248, .45); background: rgba(56, 189, 248, .08); }
.kpi-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}
.kpi-card {
  position: relative;
  overflow: hidden;
  padding: 14px 15px 12px;
  border: 1px solid rgba(148, 163, 184, .13);
  border-radius: 10px;
  background: linear-gradient(150deg, rgba(16, 35, 56, .92), rgba(8, 19, 33, .82));
  box-shadow: 0 8px 24px rgba(0, 0, 0, .14);
}
.kpi-card::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 2px;
  background: var(--accent, #38bdf8);
  opacity: .8;
}
.kpi-card.ambient-amber { --accent: #fbbf24; }
.kpi-card.ambient-green { --accent: #34d399; }
.kpi-card.ambient-blue { --accent: #818cf8; }
.kpi-card.ambient-rose { --accent: #fb7185; }
.kpi-top { display: flex; justify-content: space-between; align-items: center; }
.kpi-no { color: #475569; font-family: 'Cascadia Mono', monospace; font-size: 10px; letter-spacing: 1px; }
.kpi-icon { color: var(--accent, #38bdf8); font-size: 17px; }
.kpi-value {
  margin-top: 6px;
  color: var(--accent, #67e8f9);
  font-family: 'Cascadia Mono', 'Consolas', monospace;
  font-size: 30px;
  line-height: 1.1;
  letter-spacing: -1px;
  font-weight: 600;
  text-shadow: 0 0 24px color-mix(in srgb, var(--accent, #38bdf8) 25%, transparent);
}
.kpi-value small { margin-left: 4px; color: #64748b; font-family: inherit; font-size: 12px; font-weight: 400; }
.kpi-label { margin-top: 5px; color: #cbd5e1; font-size: 13px; }
.kpi-meta { margin-top: 3px; color: #475569; font-size: 9px; letter-spacing: 1px; white-space: nowrap; }
.main-grid {
  position: relative;
  display: grid;
  flex: 1;
  min-height: 0;
  grid-template-columns: .92fr 1.72fr 1.04fr;
  grid-template-rows: minmax(0, 1.08fr) minmax(0, .86fr) 158px;
  gap: 16px;
  margin-top: 13px;
}
.panel {
  min-width: 0;
  overflow: hidden;
  padding: 15px 17px;
  border: 1px solid rgba(96, 165, 250, .13);
  border-radius: 10px;
  background: linear-gradient(145deg, rgba(13, 30, 50, .9), rgba(7, 17, 30, .85));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .02), 0 12px 30px rgba(0, 0, 0, .12);
}
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.panel-head > div { display: flex; align-items: baseline; gap: 8px; }
.panel-index { color: #38bdf8; font-family: 'Cascadia Mono', monospace; font-size: 10px; letter-spacing: 1px; }
.panel h2 { margin: 0; color: #e2e8f0; font-size: 15px; font-weight: 500; letter-spacing: .8px; }
.panel-sub { color: #475569; font-size: 10px; letter-spacing: 1.2px; white-space: nowrap; }
.chart { width: 100%; height: 100%; min-height: 0; }
.status-body { display: flex; align-items: center; height: calc(100% - 30px); min-height: 180px; }
.donut-wrap { position: relative; flex: 1.12; height: 100%; min-height: 180px; }
.donut-wrap .chart { position: absolute; inset: 0; }
.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.donut-center strong { color: #f8fafc; font-family: 'Cascadia Mono', monospace; font-size: 27px; font-weight: 600; }
.donut-center span { margin-top: 2px; color: #64748b; font-size: 10px; letter-spacing: 1px; }
.status-legend { flex: 1; display: flex; flex-direction: column; gap: 8px; padding-left: 6px; }
.legend-row { display: flex; align-items: center; gap: 8px; color: #94a3b8; font-size: 12px; }
.legend-dot { width: 6px; height: 6px; border-radius: 50%; flex: 0 0 auto; }
.legend-name { flex: 1; }
.legend-row strong { color: #cbd5e1; font-family: 'Cascadia Mono', monospace; font-size: 11px; font-weight: 500; }
.trend-chart { height: calc(100% - 30px); min-height: 195px; }
.health-body { display: flex; align-items: center; gap: 18px; height: calc(100% - 30px); min-height: 180px; }
.ring-wrap { flex: 0 0 118px; display: grid; place-items: center; }
.ring {
  width: 112px;
  height: 112px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  box-shadow: 0 0 25px rgba(52, 211, 153, .12);
}
.ring-inner {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #091727;
}
.ring-inner strong { color: #f8fafc; font-family: 'Cascadia Mono', monospace; font-size: 23px; font-weight: 600; }
.ring-inner strong small { color: #34d399; font-size: 11px; }
.ring-inner span { margin-top: 2px; color: #64748b; font-size: 10px; }
.health-metrics { flex: 1; display: flex; flex-direction: column; gap: 10px; }
.health-row { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; color: #94a3b8; font-size: 12px; }
.health-row strong { color: #e2e8f0; font-family: 'Cascadia Mono', monospace; font-size: 16px; font-weight: 500; }
.health-row .warn { color: #fbbf24; }
.health-row .danger { color: #fb7185; }
.health-row .ok { color: #34d399; }
.hourly-chart { height: calc(100% - 28px); min-height: 165px; }
.monthly-chart { height: calc(100% - 28px); min-height: 155px; }
.composition-body { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; height: calc(100% - 28px); min-height: 165px; }
.composition-block { min-width: 0; }
.mini-title { margin-bottom: 10px; color: #64748b; font-size: 11px; letter-spacing: 1.2px; }
.bar-row { margin-bottom: 9px; }
.bar-label { display: flex; justify-content: space-between; color: #94a3b8; font-size: 11px; }
.bar-label strong { color: #cbd5e1; font-family: 'Cascadia Mono', monospace; font-weight: 500; }
.bar-track { height: 4px; margin-top: 4px; overflow: hidden; border-radius: 4px; background: rgba(148, 163, 184, .12); }
.bar-track i { display: block; height: 100%; border-radius: 4px; }
.rank-row { display: flex; align-items: center; gap: 7px; padding: 5px 0; border-bottom: 1px solid rgba(148, 163, 184, .08); font-size: 10px; }
.rank-no { color: #38bdf8; font-family: 'Cascadia Mono', monospace; font-size: 9px; }
.rank-name { flex: 1; overflow: hidden; color: #cbd5e1; text-overflow: ellipsis; white-space: nowrap; }
.rank-row strong { color: #94a3b8; font-family: 'Cascadia Mono', monospace; font-size: 10px; font-weight: 500; }
.recent-panel { grid-column: 1 / -1; min-height: 0; }
.recent-head { align-items: center; }
.recent-list { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 7px 18px; margin-top: 10px; }
.recent-row { display: flex; align-items: center; gap: 8px; min-width: 0; padding: 6px 0; border-bottom: 1px solid rgba(148, 163, 184, .08); }
.recent-avatar {
  display: grid;
  place-items: center;
  width: 25px;
  height: 25px;
  flex: 0 0 25px;
  border-radius: 50%;
  color: #67e8f9;
  font-size: 11px;
  background: rgba(56, 189, 248, .1);
  border: 1px solid rgba(56, 189, 248, .18);
}
.recent-main { display: flex; flex-direction: column; min-width: 0; flex: 1; line-height: 1.35; }
.recent-main strong { overflow: hidden; color: #cbd5e1; font-size: 11px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.recent-main span { overflow: hidden; color: #64748b; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }
.recent-type { color: #64748b; font-size: 9px; white-space: nowrap; }
.recent-time { color: #64748b; font-family: 'Cascadia Mono', monospace; font-size: 9px; white-space: nowrap; }
.recent-status { padding: 2px 5px; border-radius: 3px; font-size: 9px; white-space: nowrap; }
.s-green { color: #86efac; background: rgba(52, 211, 153, .1); }
.s-red { color: #fda4af; background: rgba(251, 113, 133, .1); }
.s-blue { color: #7dd3fc; background: rgba(56, 189, 248, .1); }
.s-amber { color: #fcd34d; background: rgba(251, 191, 36, .1); }
.s-muted { color: #94a3b8; background: rgba(148, 163, 184, .08); }
.empty-line { padding: 14px 0; color: #475569; font-size: 11px; text-align: center; }
.recent-empty { margin-top: 10px; }
.screen-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 11px;
  color: #334155;
  font-family: 'Cascadia Mono', monospace;
  font-size: 9px;
  letter-spacing: .8px;
}
.footer-sep { color: #1e293b; }
.footer-right { margin-left: auto; color: #1e3a5f; letter-spacing: 1.4px; }
.loading-mask {
  position: absolute;
  z-index: 5;
  inset: 180px 30px auto;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 40px;
  border: 1px solid rgba(56, 189, 248, .16);
  border-radius: 8px;
  color: #7dd3fc;
  font-size: 11px;
  background: rgba(8, 20, 36, .8);
  backdrop-filter: blur(8px);
}
.loader {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(56, 189, 248, .2);
  border-top-color: #38bdf8;
  border-radius: 50%;
  animation: spin .8s linear infinite;
}
.load-error {
  position: relative;
  margin-top: 10px;
  padding: 8px 12px;
  border: 1px solid rgba(251, 113, 133, .2);
  border-radius: 6px;
  color: #fda4af;
  font-size: 11px;
  background: rgba(251, 113, 133, .06);
}
@keyframes pulse {
  0%, 100% { opacity: 1; box-shadow: 0 0 0 4px rgba(52, 211, 153, .15); }
  50% { opacity: .55; box-shadow: 0 0 0 7px rgba(52, 211, 153, .04); }
}
@keyframes spin { to { transform: rotate(360deg); } }
@media (min-width: 1281px) and (max-height: 980px) {
  .screen { height: auto; min-height: 100vh; overflow: auto; }
  .main-grid { flex: none; grid-template-rows: 310px 245px 178px; }
}
@media (max-width: 1280px) {
  .screen { height: auto; min-height: 100vh; overflow: auto; padding: 18px 20px 14px; }
  .kpi-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .main-grid { flex: none; grid-template-columns: 1fr 1.6fr; grid-template-rows: minmax(240px, auto) minmax(200px, auto) auto auto; }
  .health-panel { grid-column: 1 / -1; }
  .composition-panel { grid-column: 1 / -1; }
  .recent-panel { grid-column: 1 / -1; }
  .recent-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 820px) {
  .screen { height: auto; min-height: 100vh; padding: 14px; overflow: auto; }
  .topbar { align-items: flex-start; flex-direction: column; }
  .top-actions { width: 100%; justify-content: flex-end; }
  h1 { font-size: 21px; letter-spacing: 1.5px; }
  .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .main-grid { display: flex; flex-direction: column; }
  .panel { min-height: 240px; }
  .recent-list { grid-template-columns: 1fr; }
  .screen-footer { flex-wrap: wrap; }
  .footer-right { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .live-dot, .loader { animation: none; }
}
</style>
