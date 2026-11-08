<template>
  <div class="screen">
    <header class="head">
      <div class="title">访客管理系统 · 数据大屏</div>
      <div class="time">{{ now }}</div>
      <el-button size="small" plain @click="$router.push('/dashboard')">退出大屏</el-button>
    </header>

    <div class="grid">
      <div v-for="item in cards" :key="item.label" class="card">
        <div class="label">{{ item.label }}</div>
        <div class="value">{{ item.value }}</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title">近 7 天预约趋势</div>
      <div class="bars">
        <div v-for="day in trend" :key="day.date" class="bar-item">
          <div class="bar-wrap">
            <div class="bar" :style="{ height: barHeight(day.total) }"></div>
          </div>
          <div class="bar-value">{{ day.total }}</div>
          <div class="bar-date">{{ day.date.slice(5) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { http } from '@/api/request'

interface Summary {
  total: number
  today: number
  pending: number
  inSchool: number
  left: number
  pendingTasks: number
  activeBlacklist: number
}

interface TrendDay {
  date: string
  total: number
}

const summary = ref<Summary>({ total: 0, today: 0, pending: 0, inSchool: 0, left: 0, pendingTasks: 0, activeBlacklist: 0 })
const trend = ref<TrendDay[]>([])
const now = ref('')
const timer = ref<number>()

const cards = computed(() => [
  { label: '今日预约', value: summary.value.today },
  { label: '待审批', value: summary.value.pending },
  { label: '当前在校', value: summary.value.inSchool },
  { label: '今日离校', value: summary.value.left },
  { label: '待办任务', value: summary.value.pendingTasks },
  { label: '有效黑名单', value: summary.value.activeBlacklist },
])

function barHeight(total: number) {
  const max = Math.max(1, ...trend.value.map((item) => item.total))
  return `${Math.max(6, Math.round((total / max) * 180))}px`
}

function updateTime() {
  now.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

async function load() {
  try {
    summary.value = await http.get<Summary>('/web/admin/dashboard/summary')
    const data = await http.get<{ daily: TrendDay[] }>('/web/admin/dashboard/trend', { days: 7 })
    trend.value = data.daily
  } catch {
    // 大屏静默重试
  }
}

onMounted(() => {
  updateTime()
  load()
  timer.value = window.setInterval(() => {
    updateTime()
    load()
  }, 60_000)
})

onBeforeUnmount(() => {
  if (timer.value) window.clearInterval(timer.value)
})
</script>

<style scoped>
.screen {
  min-height: 100vh;
  background: #0b1a2b;
  color: #e6f1ff;
  padding: 20px 28px;
  box-sizing: border-box;
}
.head {
  display: flex;
  align-items: center;
  gap: 20px;
}
.title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 2px;
  flex: 1;
}
.time {
  font-size: 14px;
  color: #8fb3d9;
}
.grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-top: 22px;
}
.card {
  background: linear-gradient(180deg, #12263d, #0e1f33);
  border: 1px solid #1f3a57;
  border-radius: 10px;
  padding: 20px 16px;
  text-align: center;
}
.label {
  color: #8fb3d9;
  font-size: 14px;
}
.value {
  margin-top: 10px;
  font-size: 40px;
  font-weight: 700;
  color: #4fc3f7;
}
.panel {
  margin-top: 22px;
  background: #0e1f33;
  border: 1px solid #1f3a57;
  border-radius: 10px;
  padding: 18px 20px 8px;
}
.panel-title {
  font-size: 16px;
  margin-bottom: 12px;
  color: #cfe6ff;
}
.bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 260px;
}
.bar-item {
  text-align: center;
  width: 10%;
}
.bar-wrap {
  height: 190px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.bar {
  width: 38px;
  background: linear-gradient(180deg, #4fc3f7, #1976d2);
  border-radius: 6px 6px 0 0;
}
.bar-value {
  margin-top: 6px;
  color: #4fc3f7;
  font-size: 14px;
}
.bar-date {
  color: #8fb3d9;
  font-size: 12px;
}
</style>
