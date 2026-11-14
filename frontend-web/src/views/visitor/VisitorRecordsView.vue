<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="status" clearable placeholder="全部" style="width: 140px" @change="load(1)">
            <el-option v-for="(label, value) in STATUS" :key="value" :label="label" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="姓名/手机号/被访人" clearable style="width: 220px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="openBatch">批量直录</el-button>
        <el-button :loading="exporting" @click="exportExcel">导出</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="visitorName" label="访客" width="120" />
        <el-table-column prop="visitorPhone" label="手机号" width="140" />
        <el-table-column prop="targetName" label="被访人" width="120" />
        <el-table-column prop="visitPurpose" label="事由" min-width="160" show-overflow-tooltip />
        <el-table-column label="到访时间" width="180">
          <template #default="{ row }">{{ row.expectedTime }} ~ {{ row.leaveTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="TAG[row.status] ?? 'info'">{{ STATUS[row.status] ?? row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="batchVisible" title="批量直录（无需审批，直接发证）" width="560px">
      <p class="hint">每行一条：姓名,手机号,开始时间,结束时间,事由（时间格式 yyyy-MM-dd HH:mm:ss）</p>
      <el-input v-model="batchText" type="textarea" :rows="8" placeholder="张三,13900000001,2026-11-10 09:00:00,2026-11-10 11:00:00,业务洽谈" />
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBatch">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawer" title="访客详情" size="420px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="访客">{{ current.visitorName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ current.visitorPhone }}</el-descriptions-item>
        <el-descriptions-item label="证件号">{{ current.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被访人">{{ current.targetName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事由">{{ current.visitPurpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到访">{{ current.expectedTime }}</el-descriptions-item>
        <el-descriptions-item label="离校">{{ current.leaveTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ STATUS[current.status] }}</el-descriptions-item>
        <el-descriptions-item
          v-for="entry in extraEntries"
          :key="entry.key"
          :label="entry.label"
        >
          {{ entry.value }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-if="current && current.status === 0" class="emergency">
        <el-button type="success" @click="emergency(true)">代通过</el-button>
        <el-button type="danger" @click="emergency(false)">代驳回</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface VisitorRecord {
  id: number
  visitorName: string
  visitorPhone: string
  idCard?: string
  targetName?: string
  visitPurpose?: string
  expectedTime: string
  leaveTime: string
  status: number
  visitorType?: string
  extraJson?: string
}

const STATUS: Record<number, string> = {
  0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已入校', 4: '已离校', 5: '已取消', 6: '已过期',
}
const TAG: Record<number, string> = {
  0: 'warning', 1: 'success', 2: 'danger', 3: 'primary', 4: 'info', 5: 'info', 6: 'info',
}

const rows = ref<VisitorRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const status = ref<number | undefined>()
const keyword = ref('')
const loading = ref(false)
const drawer = ref(false)
const batchVisible = ref(false)
const batchText = ref('')
const submitting = ref(false)
const exporting = ref(false)
const current = ref<VisitorRecord>()
const extraEntries = ref<Array<{ key: string; label: string; value: string }>>([])

async function load(targetPage = page.value) {
  page.value = targetPage
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: VisitorRecord[] }>('/web/admin/visitor-records', {
      page: page.value,
      size: size.value,
      status: status.value,
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

async function exportExcel() {
  exporting.value = true
  try {
    const params = new URLSearchParams()
    if (status.value !== undefined && status.value !== null) params.set('status', String(status.value))
    if (keyword.value) params.set('keyword', keyword.value)
    const response = await fetch(`/api/web/admin/visitor-records/export?${params.toString()}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
    })
    if (!response.ok) throw new Error('导出失败')
    const blob = await response.blob()
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '访客记录.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

function openBatch() {
  batchText.value = ''
  batchVisible.value = true
}

async function submitBatch() {
  const items = batchText.value
    .split('\\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [name, phone, expectedTime, leaveTime, purpose] = line.split(',').map((part) => part.trim())
      return { name, phone, expectedTime, leaveTime, visitPurpose: purpose }
    })
  if (items.length === 0) {
    ElMessage.warning('请至少填写一行')
    return
  }
  submitting.value = true
  try {
    const result = await http.post<{ created: number }>('/web/admin/visitor-records/batch', { items })
    ElMessage.success(`已直录 ${result.created} 人`)
    batchVisible.value = false
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

async function emergency(approve: boolean) {
  if (!current.value) return
  try {
    const { value } = await ElMessageBox.prompt(
      approve ? '代通过意见（可选）' : '代驳回原因',
      approve ? '代通过' : '代驳回',
    )
    await http.post(
      `/web/admin/visitor-records/${current.value.id}/emergency?approve=${approve}&comment=${encodeURIComponent(value || '')}`,
    )
    ElMessage.success(approve ? '已代通过' : '已代驳回')
    drawer.value = false
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  }
}

function openDetail(row: VisitorRecord) {
  current.value = row
  drawer.value = true
  loadExtraEntries(row)
}

async function loadExtraEntries(row: VisitorRecord) {
  extraEntries.value = []
  if (!row.extraJson) return
  let values: Record<string, unknown> = {}
  try {
    values = JSON.parse(row.extraJson)
  } catch {
    return
  }
  let labels: Record<string, string> = {}
  try {
    const fields = await http.get<Array<{ fieldKey: string; label: string }>>('/web/visitor/form-fields', {
      visitorType: row.visitorType || 'NORMAL',
    })
    labels = Object.fromEntries(fields.map((field) => [field.fieldKey, field.label]))
  } catch {
    // 字段配置读取失败时使用字段编码
  }
  extraEntries.value = Object.entries(values)
    .filter(([, value]) => value !== null && value !== undefined && String(value).trim() !== '')
    .map(([key, value]) => ({
      key,
      label: labels[key] || key,
      value: String(value),
    }))
}

onMounted(() => load(1))
</script>

<style scoped>
.hint {
  color: #909399;
  font-size: 12px;
}
.emergency {
  margin-top: 16px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
