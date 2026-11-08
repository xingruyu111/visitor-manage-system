<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>数据同步任务</span>
          <el-button :loading="loadingLogs" @click="loadLogs">刷新日志</el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        title="每日 02:00 自动同步；也可在此手动触发。同步失败会自动重试 3 次并告警。"
        class="tip"
      />

      <el-row :gutter="12">
        <el-col v-for="item in TYPES" :key="item.value" :span="6">
          <el-card shadow="hover" class="type-card">
            <div class="type-name">{{ item.label }}</div>
            <el-button type="primary" size="small" :loading="running === item.value" @click="run(item)">
              立即同步
            </el-button>
            <p v-if="results[item.value]" class="result">{{ results[item.value] }}</p>
          </el-card>
        </el-col>
      </el-row>

      <el-table v-loading="loadingLogs" :data="logs" border class="logs">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="integrationKey" label="接口" width="120" />
        <el-table-column prop="action" label="动作" width="180" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface SyncResult {
  type: string
  created: number
  updated: number
  deactivated: number
  failed: number
}

interface LogRow {
  id: number
  createdAt: string
  integrationKey: string
  action: string
  success: number
  errorMsg?: string
}

const TYPES = [
  { value: 'org', label: '组织机构' },
  { value: 'staff', label: '教职工' },
  { value: 'student', label: '学生' },
  { value: 'alumni', label: '校友' },
]

const running = ref('')
const results = reactive<Record<string, string>>({})
const logs = ref<LogRow[]>([])
const loadingLogs = ref(false)

async function run(item: { value: string; label: string }) {
  running.value = item.value
  try {
    const data = await http.post<SyncResult>(`/web/admin/org-sync/${item.value}`)
    results[item.value] = `新增 ${data.created}，更新 ${data.updated}，停用 ${data.deactivated}，失败 ${data.failed}`
    ElMessage.success(`${item.label}同步完成`)
    await loadLogs()
  } catch (e) {
    results[item.value] = '同步失败'
    ElMessage.error(e instanceof Error ? e.message : '同步失败')
  } finally {
    running.value = ''
  }
}

async function loadLogs() {
  loadingLogs.value = true
  try {
    const data = await http.get<{ records: LogRow[] }>('/web/admin/integration-logs', {
      integrationKey: 'DATA_SYNC',
      page: 1,
      size: 20,
    })
    logs.value = data.records
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loadingLogs.value = false
  }
}

onMounted(loadLogs)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tip {
  margin-bottom: 12px;
}
.type-card {
  text-align: center;
}
.type-name {
  font-weight: 600;
  margin-bottom: 8px;
}
.result {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
}
.logs {
  margin-top: 16px;
}
</style>
