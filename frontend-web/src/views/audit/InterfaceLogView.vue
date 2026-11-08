<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="接口">
          <el-select v-model="integrationKey" clearable placeholder="全部" style="width: 160px" @change="load(1)">
            <el-option v-for="item in KEYS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="success" clearable placeholder="全部" style="width: 120px" @change="load(1)">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button :loading="exporting" @click="exportCsv">导出</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="integrationKey" label="接口" width="130" />
        <el-table-column prop="action" label="动作" width="160" />
        <el-table-column prop="direction" label="方向" width="80" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="httpStatus" label="HTTP" width="80" />
        <el-table-column prop="errorCode" label="错误码" width="120" />
        <el-table-column prop="errorMsg" label="错误信息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="costMs" label="耗时(ms)" width="90" />
      </el-table>

      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface InterfaceLog {
  id: number
  createdAt: string
  integrationKey: string
  action: string
  direction?: string
  success: number
  httpStatus?: number
  errorCode?: string
  errorMsg?: string
  costMs?: number
}

const KEYS = ['SSO', 'DATA_SYNC', 'GATE', 'FACE', 'PORTAL', 'TODO_MSG', 'ACCESS_SYNC']

const rows = ref<InterfaceLog[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const integrationKey = ref<string>()
const success = ref<number>()
const loading = ref(false)
const exporting = ref(false)

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: InterfaceLog[] }>('/web/admin/integration-logs', {
      page: page.value,
      size: size.value,
      integrationKey: integrationKey.value,
      success: success.value,
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function exportCsv() {
  exporting.value = true
  try {
    const params = new URLSearchParams()
    if (integrationKey.value) params.set('integrationKey', integrationKey.value)
    if (success.value !== undefined && success.value !== null) params.set('success', String(success.value))
    const response = await fetch(`/api/web/admin/integration-logs/export?${params.toString()}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
    })
    if (!response.ok) throw new Error('导出失败')
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '接口日志.csv'
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
