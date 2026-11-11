<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item v-if="!fixedAction" label="动作">
          <el-select v-model="action" clearable placeholder="全部" style="width: 180px" @change="load(1)">
            <el-option v-for="item in ACTIONS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="操作人/详情" clearable style="width: 200px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button :loading="exporting" @click="exportCsv">导出</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="userName" label="操作人" width="120" />
        <el-table-column prop="action" label="动作" width="190" />
        <el-table-column prop="bizType" label="业务类型" width="140" />
        <el-table-column prop="bizId" label="业务ID" width="90" />
        <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
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
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface AuditLog {
  id: number
  userName?: string
  action: string
  bizType?: string
  bizId?: number
  detail?: string
  ip?: string
  createdAt: string
}

const ACTIONS = [
  'VISITOR_APPLY',
  'APPROVAL_APPROVE',
  'APPROVAL_REJECT',
  'ACCESS_VERIFY',
  'BLACKLIST_HIT',
  'ACCESS_LIST_CREATE',
  'ACCESS_LIST_RELEASE',
  'GATE_PUSH_FAILED',
  'DATA_SYNC_FAILED',
]

const props = defineProps<{ fixedAction?: string }>()

const rows = ref<AuditLog[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const action = ref<string | undefined>(props.fixedAction)
const keyword = ref('')
const loading = ref(false)
const exporting = ref(false)

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: AuditLog[] }>('/web/admin/audit-logs', {
      page: page.value,
      size: size.value,
      action: action.value,
      keyword: keyword.value || undefined,
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
    if (action.value) params.set('action', action.value)
    if (keyword.value) params.set('keyword', keyword.value)
    const response = await fetch(`/api/web/admin/audit-logs/export?${params.toString()}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
    })
    if (!response.ok) throw new Error('导出失败')
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '审计日志.csv'
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

watch(
  () => props.fixedAction,
  (value) => {
    action.value = value
    keyword.value = ''
    page.value = 1
    load(1)
  },
  { immediate: true },
)
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
