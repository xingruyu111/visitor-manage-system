<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="方向">
          <el-select v-model="direction" clearable placeholder="全部" style="width: 120px" @change="load(1)">
            <el-option label="入校" value="IN" />
            <el-option label="离校" value="OUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="source" clearable placeholder="全部" style="width: 130px" @change="load(1)">
            <el-option label="本地核验" value="LOCAL" />
            <el-option label="门闸回传" value="GATE" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button :loading="exporting" @click="exportCsv">导出</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="eventTime" label="时间" width="180" />
        <el-table-column label="方向" width="90">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'info'">{{ row.direction === 'IN' ? '入校' : '离校' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="110" />
        <el-table-column prop="visitorName" label="访客" width="130" />
        <el-table-column prop="plateNumber" label="车牌" width="120" />
        <el-table-column prop="recordId" label="预约ID" width="100" />
        <el-table-column prop="externalId" label="外部记录ID" min-width="160" />
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

interface VisitLog {
  id: number
  recordId: number
  direction: string
  source: string
  visitorName?: string
  plateNumber?: string
  externalId?: string
  eventTime: string
}

const rows = ref<VisitLog[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const direction = ref<string>()
const source = ref<string>()
const loading = ref(false)
const exporting = ref(false)

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: VisitLog[] }>('/web/admin/visit-logs', {
      page: page.value,
      size: size.value,
      direction: direction.value,
      source: source.value,
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
    if (direction.value) params.set('direction', direction.value)
    if (source.value) params.set('source', source.value)
    const response = await fetch(`/api/web/admin/visit-logs/export?${params.toString()}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
    })
    if (!response.ok) throw new Error('导出失败')
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '通行记录.csv'
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
