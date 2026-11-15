<template>
  <div class="page">
    <van-nav-bar title="批量预约" left-arrow @click-left="$router.back()" />

    <div class="intro-card">
      <div class="intro-title">Excel 批量预约</div>
      <p v-if="isSecurityAdmin">按人员填写，每人一行；保卫处管理员提交后直接生效并签发通行凭证。</p>
      <p v-else>按人员填写，每人一行；提交后进入保卫处审批，通过后自动签发通行凭证。</p>
      <a class="template-link" :href="`${base}templates/batch-apply-import.xlsx`" download>
        下载导入模板
      </a>
    </div>

    <div class="upload-card">
      <input
        ref="fileInput"
        class="hidden-file"
        type="file"
        accept=".xlsx,.xls"
        @change="onFileChange"
      />
      <button class="file-picker" type="button" @click="fileInput?.click()">
        <span class="file-icon">＋</span>
        <span class="file-label">{{ file ? file.name : '选择 Excel 文件' }}</span>
        <span class="file-hint">{{ file ? fileSize : '支持 .xlsx / .xls，最大 20MB' }}</span>
      </button>

      <van-button
        block
        round
        type="primary"
        class="submit"
        :loading="uploading"
        :disabled="!file"
        @click="upload"
      >
        开始导入
      </van-button>
    </div>

    <div v-if="result" class="result-card">
      <div class="result-title">导入结果</div>
      <div class="result-line">成功 {{ result.success ?? 0 }} 条，失败 {{ result.failed ?? 0 }} 条</div>
      <div v-if="result.errors?.length" class="errors">
        <div v-for="item in result.errors" :key="item.row" class="error-row">
          第 {{ item.row }} 行：{{ item.reason || '导入失败' }}
        </div>
      </div>
    </div>

    <div class="history-card">
      <div class="history-head">
        <strong>导入记录</strong>
        <span>{{ isSecurityAdmin ? '显示全部经办人和自己的记录' : '仅显示自己导入的记录' }}</span>
      </div>
      <div v-if="recordsLoading" class="history-empty">加载中…</div>
      <div v-else-if="records.length === 0" class="history-empty">暂无导入记录</div>
      <div v-else class="table-scroll">
        <table class="history-table">
          <thead>
            <tr>
              <th>姓名</th>
              <th>手机号</th>
              <th>到访时间</th>
              <th>离校时间</th>
              <th>事由</th>
              <th>状态</th>
              <th v-if="isSecurityAdmin">经办人</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in records" :key="item.id">
              <td>{{ item.visitorName }}</td>
              <td>{{ item.visitorPhone }}</td>
              <td>{{ displayTime(item.expectedTime) }}</td>
              <td>{{ displayTime(item.leaveTime) }}</td>
              <td>{{ item.visitPurpose || '—' }}</td>
              <td>
                <van-tag :type="statusTag(item.status)">{{ statusText(item.status) }}</van-tag>
              </td>
              <td v-if="isSecurityAdmin">{{ item.operatorName || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="tip-card">
      <strong>模板列</strong>
      <p>姓名、手机号、到访时间、离校时间、事由。</p>
      <p>时间格式：2026-11-12 08:00:00，也可以只写 2026-11-12。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface ImportError {
  row: number
  reason?: string
}

interface ImportResult {
  success?: number
  failed?: number
  errors?: ImportError[]
}

interface BatchRecord {
  id: number
  visitorName: string
  visitorPhone?: string
  expectedTime?: string
  leaveTime?: string
  visitPurpose?: string
  status?: number
  operatorName?: string
  createdAt?: string
}

const fileInput = ref<HTMLInputElement>()
const base = import.meta.env.BASE_URL
const file = ref<File>()
const uploading = ref(false)
const isSecurityAdmin = ref(false)
const result = ref<ImportResult>()
const records = ref<BatchRecord[]>([])
const recordsLoading = ref(false)

const fileSize = computed(() => {
  if (!file.value) return ''
  const size = file.value.size
  return size > 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(size / 1024)} KB`
})

onMounted(async () => {
  try {
    const me = await http.get<{ roles?: Array<{ code: number }> }>('/auth/me')
    isSecurityAdmin.value = (me.roles ?? []).some((role) => role.code === 1 || role.code === 2)
  } catch {
    isSecurityAdmin.value = false
  }
  await loadRecords()
})

async function loadRecords() {
  recordsLoading.value = true
  try {
    const data = await http.get<{ records: BatchRecord[] }>('/web/dept/visitor-records/batch/imported', {
      page: 1,
      size: 50,
    })
    records.value = data.records ?? []
  } catch {
    records.value = []
  } finally {
    recordsLoading.value = false
  }
}

function displayTime(value?: string) {
  return value ? value.replace('T', ' ') : '—'
}

const STATUS_TEXT: Record<number, string> = {
  0: '待审批',
  1: '已通过',
  2: '已驳回',
  3: '已入校',
  4: '已离校',
  5: '已取消',
  6: '已过期',
}

function statusText(status?: number) {
  return STATUS_TEXT[status ?? -1] ?? '未知'
}

function statusTag(status?: number): 'success' | 'warning' | 'danger' | 'default' {
  if (status === 1 || status === 3 || status === 4) return 'success'
  if (status === 0) return 'warning'
  if (status === 2) return 'danger'
  return 'default'
}

function onFileChange(event: Event) {
  const files = (event.target as HTMLInputElement).files
  const selected = files && files.length > 0 ? files[0] : undefined
  if (!selected) return
  if (!/\.(xlsx|xls)$/i.test(selected.name)) {
    showToast('请选择 Excel 文件')
    return
  }
  if (selected.size > 20 * 1024 * 1024) {
    showToast('文件不能超过 20MB')
    return
  }
  file.value = selected
  result.value = undefined
}

async function upload() {
  if (!file.value || uploading.value) return
  uploading.value = true
  result.value = undefined
  try {
    const body = new FormData()
    body.append('file', file.value)
    const response = await fetch('/api/web/dept/visitor-records/batch/import', {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('visitor_token') ?? ''}` },
      body,
    })
    const payload = await response.json()
    if (payload.code !== 200) {
      throw new Error(payload.message || '导入失败')
    }
    result.value = payload.data as ImportResult
    showToast('导入完成')
    file.value = undefined
    if (fileInput.value) fileInput.value.value = ''
    await loadRecords()
  } catch (error) {
    showToast(error instanceof Error ? error.message : '导入失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 32px;
}
.intro-card,
.upload-card,
.result-card,
.history-card,
.tip-card {
  margin: 12px 16px;
  padding: 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e8f1fb;
  box-shadow: 0 6px 20px rgba(16, 42, 80, 0.05);
}
.history-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.history-head strong {
  color: #10233f;
  font-size: 16px;
}
.history-head span,
.history-empty {
  color: #8a99ad;
  font-size: 12px;
}
.history-empty {
  padding: 18px 0;
  text-align: center;
}
.table-scroll {
  overflow-x: auto;
}
.history-table {
  width: 100%;
  min-width: 680px;
  border-collapse: collapse;
  font-size: 12px;
}
.history-table th,
.history-table td {
  padding: 8px 6px;
  border-bottom: 1px solid #edf2f8;
  text-align: left;
  white-space: nowrap;
}
.history-table th {
  color: #61738c;
  font-weight: 500;
  background: #f7faff;
}
.intro-title {
  color: #10233f;
  font-size: 16px;
  font-weight: 600;
}
.intro-card p,
.tip-card p {
  margin: 8px 0 0;
  color: #61738c;
  font-size: 13px;
  line-height: 1.7;
}
.template-link {
  display: inline-block;
  margin-top: 12px;
  color: #1677ff;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
}
.hidden-file {
  display: none;
}
.file-picker {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  min-height: 150px;
  padding: 22px 12px;
  border: 1px dashed #b9d5f7;
  border-radius: 14px;
  background: #f8fbff;
  color: #10233f;
}
.file-icon {
  width: 42px;
  height: 42px;
  margin-bottom: 10px;
  border-radius: 50%;
  background: #e8f3ff;
  color: #1677ff;
  font-size: 26px;
  line-height: 40px;
}
.file-label {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 500;
}
.file-hint {
  margin-top: 6px;
  color: #8da0b8;
  font-size: 12px;
}
.submit {
  margin-top: 16px;
  height: 44px;
}
.result-title {
  color: #10233f;
  font-size: 15px;
  font-weight: 600;
}
.result-line {
  margin-top: 8px;
  color: #1677ff;
  font-size: 14px;
}
.errors {
  margin-top: 10px;
  max-height: 220px;
  overflow-y: auto;
}
.error-row {
  margin-top: 6px;
  color: #d93026;
  font-size: 12px;
  line-height: 1.5;
}
.tip-card strong {
  color: #10233f;
  font-size: 14px;
}
</style>
