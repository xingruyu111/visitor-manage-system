<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="hover" class="stat">
          <div class="label">通过率</div>
          <div class="value">{{ data.approvalRate ?? 0 }}%</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat">
          <div class="label">黑名单拦截</div>
          <div class="value">{{ data.blacklistHits ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat">
          <div class="label">接口异常</div>
          <div class="value">{{ data.integrationErrors ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="detail">
      <template #header>
        <div class="header">
          <span>月度统计</span>
          <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>
      <el-table :data="monthly" border>
        <el-table-column prop="month" label="月份" width="140" />
        <el-table-column prop="total" label="预约量" width="110" />
        <el-table-column prop="approved" label="通过" width="100" />
        <el-table-column prop="rejected" label="拒绝" width="100" />
        <el-table-column prop="left" label="离校" width="100" />
        <el-table-column prop="approvalRate" label="通过率(%)" width="120" />
      </el-table>
    </el-card>

    <el-card shadow="never" class="detail">
      <template #header>
        <div class="header">
          <span>每日预约趋势</span>
          <el-select v-model="days" style="width: 120px" @change="load">
            <el-option label="近 7 天" :value="7" />
            <el-option label="近 14 天" :value="14" />
            <el-option label="近 30 天" :value="30" />
          </el-select>
        </div>
      </template>
      <el-table :data="data.daily" border>
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="total" label="预约量" width="110" />
        <el-table-column prop="approved" label="通过" width="100" />
        <el-table-column prop="rejected" label="拒绝" width="100" />
        <el-table-column prop="left" label="离校" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface Daily {
  date: string
  total: number
  approved: number
  rejected: number
  left: number
}

interface Trend {
  days: number
  daily: Daily[]
  approvalRate: number
  blacklistHits: number
  integrationErrors: number
}

const days = ref(14)
const loading = ref(false)
const monthly = ref<Array<Record<string, unknown>>>([])
const data = ref<Trend>({ days: 14, daily: [], approvalRate: 0, blacklistHits: 0, integrationErrors: 0 })

async function load() {
  loading.value = true
  try {
    data.value = await http.get<Trend>('/web/admin/dashboard/trend', { days: days.value })
    monthly.value = await http.get<Array<Record<string, unknown>>>('/web/admin/dashboard/monthly', { months: 6 })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat {
  text-align: center;
}
.label {
  color: #909399;
  font-size: 13px;
}
.value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 600;
}
.detail {
  margin-top: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
