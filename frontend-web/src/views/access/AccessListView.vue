<template>
  <div>
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="黑名单" name="1" />
        <el-tab-pane label="白名单" name="2" />
      </el-tabs>

      <el-alert
        v-if="isWhitelist"
        type="info"
        :closable="false"
        show-icon
        class="whitelist-tip"
        title="白名单按人管理，多人同车请分别添加每个人；有效期内不限次进出。保卫处管理员上传后直接生效，部门经办人上传后需保卫处管理员审批。"
      />

      <el-form inline class="access-toolbar">
        <el-form-item label="关键字">
          <el-input
            v-model="keyword"
            :placeholder="isWhitelist ? '姓名/手机号/车牌' : '手机号/车牌/姓名'"
            clearable
            style="width: 220px"
            @keyup.enter="load(1)"
          />
        </el-form-item>
        <el-form-item v-if="!isWhitelist" label="对象">
          <el-select v-model="targetType" clearable placeholder="全部" style="width: 150px" @change="load(1)">
            <el-option label="人员" value="PERSON" />
            <el-option label="机动车" value="PLATE_MOTOR" />
            <el-option label="电动自行车" value="PLATE_EBIKE" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="openCreate">新增</el-button>
        <el-button @click="importVisible = true">Excel 导入</el-button>
      </el-form>

      <el-table v-if="isWhitelist" v-loading="loading" :data="rows" border class="access-table">
        <el-table-column prop="personName" label="姓名" width="110" />
        <el-table-column prop="targetValue" label="手机号" width="140" />
        <el-table-column label="车牌号" width="120">
          <template #default="{ row }">{{ row.plateNumber || '—' }}</template>
        </el-table-column>
        <el-table-column prop="effectiveFrom" label="生效时间" width="170" />
        <el-table-column prop="effectiveTo" label="失效时间" width="170" />
        <el-table-column prop="reason" label="原因" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="whiteStatus(row).type">{{ whiteStatus(row).text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canApprove" link type="primary" @click="openWhiteEdit(row)">编辑</el-button>
            <template v-if="canApprove && row.status === 0">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <el-button
              v-if="canApprove && (row.status === 1 || row.status === 3)"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleWhiteStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button v-if="canApprove" link type="danger" @click="remove(row)">删除</el-button>
            <span v-if="!canApprove && row.status === 0" class="pending-text">待保卫处审批</span>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="loading" :data="rows" border class="access-table">
        <el-table-column label="对象" width="110">
          <template #default="{ row }">{{ TYPE[row.targetType] ?? row.targetType }}</template>
        </el-table-column>
        <el-table-column prop="targetValue" label="手机号/车牌" min-width="150" />
        <el-table-column prop="effectiveFrom" label="生效时间" width="170" />
        <el-table-column prop="effectiveTo" label="失效时间" width="170" />
        <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '生效中' : '已失效' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="visible" :title="dialogTitle" width="560px">
      <el-form label-width="120px">
        <template v-if="isWhitelist">
          <el-form-item label="姓名" required><el-input v-model="whiteForm.personName" maxlength="64" /></el-form-item>
          <el-form-item label="手机号" required><el-input v-model="whiteForm.phone" maxlength="20" /></el-form-item>
          <el-form-item label="车牌号">
            <el-input v-model="whiteForm.plateNumber" placeholder="选填；有车牌按机动车通行" maxlength="20" />
          </el-form-item>
          <el-form-item label="生效时间" required>
            <el-date-picker
              v-model="whiteForm.effectiveFrom"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="失效时间" required>
            <el-date-picker
              v-model="whiteForm.effectiveTo"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="原因"><el-input v-model="whiteForm.reason" maxlength="255" /></el-form-item>
          <el-alert
            v-if="!canApprove"
            type="warning"
            :closable="false"
            show-icon
            title="提交后进入待审批，由保卫处管理员审批通过后生效。"
          />
        </template>

        <template v-else>
          <el-form-item label="对象类型" required>
            <el-select v-model="form.targetType" style="width: 100%">
              <el-option label="人员" value="PERSON" />
              <el-option label="机动车" value="PLATE_MOTOR" />
              <el-option label="电动自行车" value="PLATE_EBIKE" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号/车牌" required>
            <el-input v-model="form.targetValue" placeholder="人员填手机号，车辆填车牌" />
          </el-form-item>
          <el-form-item label="生效时间">
            <el-date-picker v-model="form.effectiveFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="失效时间">
            <el-date-picker v-model="form.effectiveTo" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="原因"><el-input v-model="form.reason" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">确定</el-button>
      </template>
    </el-dialog>

    <ExcelImportDialog
      v-model="importVisible"
      :title="importTitle"
      :template-url="templateUrl"
      :upload-url="importUploadUrl"
      :description="importDescription"
      @success="load(1)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'
import ExcelImportDialog from '@/components/ExcelImportDialog.vue'

interface Row {
  id: number
  listType: number
  targetType: string
  targetValue: string
  personName?: string
  plateNumber?: string
  effectiveFrom?: string
  effectiveTo?: string
  reason?: string
  status: number
  rejectReason?: string
}

interface Me {
  currentRole?: number
  roles?: Array<{ code: number; name?: string }>
}

const TYPE: Record<string, string> = { PERSON: '人员', PLATE_MOTOR: '机动车', PLATE_EBIKE: '电动自行车' }
const STATUS_PENDING = 0
const STATUS_ACTIVE = 1
const STATUS_REJECTED = 2
const STATUS_DISABLED = 3

const activeTab = ref('1')
const rows = ref<Row[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const targetType = ref<string>()
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const importVisible = ref(false)
const editingId = ref<number>()
const me = ref<Me>({})
const form = reactive({
  listType: 1,
  targetType: 'PERSON',
  targetValue: '',
  effectiveFrom: '',
  effectiveTo: '',
  reason: '',
})
const whiteForm = reactive({
  id: undefined as number | undefined,
  personName: '',
  phone: '',
  plateNumber: '',
  effectiveFrom: '',
  effectiveTo: '',
  reason: '',
})

const isWhitelist = computed(() => activeTab.value === '2')
const canApprove = computed(() => (me.value.roles ?? []).some((role) => role.code === 2 || role.code === 1))
const dialogTitle = computed(() => (isWhitelist.value ? (whiteForm.id ? '编辑白名单' : '新增白名单') : editingId.value ? '编辑黑名单' : '新增黑名单'))
const importTitle = computed(() => (isWhitelist.value ? 'Excel 导入白名单' : 'Excel 导入黑名单'))
const templateUrl = computed(() => (isWhitelist.value ? '/templates/whitelist-import.xlsx' : '/templates/access-list-import.xlsx'))
const importUploadUrl = computed(() => (isWhitelist.value ? '/api/web/whitelist/import' : '/api/web/admin/access-list/import'))
const importDescription = computed(() => (isWhitelist.value
  ? '表头：姓名、手机号、车牌号、生效时间、失效时间、原因。多人同车请每人一行。'
  : '表头：姓名、手机号、车牌号、类型、生效时间、失效时间、原因。'))

function whiteStatus(row: Row): { text: string; type: 'success' | 'warning' | 'danger' | 'info' } {
  if (row.status === STATUS_PENDING) return { text: '待审批', type: 'warning' }
  if (row.status === STATUS_REJECTED) return { text: '已驳回', type: 'danger' }
  if (row.status === STATUS_DISABLED) return { text: '已停用', type: 'info' }
  if (row.effectiveTo && new Date(row.effectiveTo.replace(' ', 'T')) < new Date()) {
    return { text: '已失效', type: 'info' }
  }
  return { text: '生效中', type: 'success' }
}

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
    }
    if (isWhitelist.value) {
      const data = await http.get<{ total: number; records: Row[] }>('/web/whitelist', params)
      rows.value = data.records
      total.value = data.total
    } else {
      params.listType = Number(activeTab.value)
      params.targetType = targetType.value
      const data = await http.get<{ total: number; records: Row[] }>('/web/admin/access-list', params)
      rows.value = data.records
      total.value = data.total
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  keyword.value = ''
  targetType.value = undefined
  load(1)
}

function openCreate() {
  editingId.value = undefined
  whiteForm.id = undefined
  if (isWhitelist.value) {
    Object.assign(whiteForm, {
      personName: '',
      phone: '',
      plateNumber: '',
      effectiveFrom: '',
      effectiveTo: '',
      reason: '',
    })
  } else {
    form.listType = Number(activeTab.value)
    form.targetType = 'PERSON'
    form.targetValue = ''
    form.effectiveFrom = ''
    form.effectiveTo = ''
    form.reason = ''
  }
  visible.value = true
}

function openWhiteEdit(row: Row) {
  whiteForm.id = row.id
  Object.assign(whiteForm, {
    personName: row.personName || '',
    phone: row.targetValue || '',
    plateNumber: row.plateNumber || '',
    effectiveFrom: row.effectiveFrom || '',
    effectiveTo: row.effectiveTo || '',
    reason: row.reason || '',
  })
  visible.value = true
}

function openEdit(row: Row) {
  editingId.value = row.id
  form.listType = row.listType
  form.targetType = row.targetType
  form.targetValue = row.targetValue
  form.effectiveFrom = row.effectiveFrom || ''
  form.effectiveTo = row.effectiveTo || ''
  form.reason = row.reason || ''
  visible.value = true
}

async function save() {
  saving.value = true
  try {
    if (isWhitelist.value) {
      const body = {
        personName: whiteForm.personName,
        phone: whiteForm.phone,
        plateNumber: whiteForm.plateNumber || undefined,
        effectiveFrom: whiteForm.effectiveFrom,
        effectiveTo: whiteForm.effectiveTo,
        reason: whiteForm.reason || undefined,
      }
      if (whiteForm.id) {
        await http.put(`/web/whitelist/${whiteForm.id}`, body)
      } else {
        await http.post('/web/whitelist', body)
      }
      ElMessage.success(whiteForm.id
        ? '已保存'
        : canApprove.value ? '已生效并生成预约记录' : '已提交，等待保卫处审批')
    } else {
      const body = {
        listType: form.listType,
        targetType: form.targetType,
        targetValue: form.targetValue,
        effectiveFrom: form.effectiveFrom || undefined,
        effectiveTo: form.effectiveTo || undefined,
        reason: form.reason || undefined,
      }
      if (editingId.value) {
        await http.put(`/web/admin/access-list/${editingId.value}`, body)
      } else {
        await http.post('/web/admin/access-list', body)
      }
      ElMessage.success(editingId.value ? '已保存' : '已新增')
    }
    visible.value = false
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function approve(row: Row) {
  try {
    await http.post(`/web/whitelist/${row.id}/approve`)
    ElMessage.success('已通过，预约记录已生成')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '审批失败')
  }
}

async function reject(row: Row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回白名单', {
      inputValue: '',
      inputValidator: (text) => Boolean(text && text.trim()) || '请填写驳回原因',
    })
    await http.post(`/web/whitelist/${row.id}/reject?reason=${encodeURIComponent(value)}`)
    ElMessage.success('已驳回')
    await load()
  } catch {
    // 取消
  }
}

async function toggleWhiteStatus(row: Row) {
  try {
    const next = row.status === STATUS_ACTIVE ? STATUS_DISABLED : STATUS_ACTIVE
    await http.put(`/web/whitelist/${row.id}/status?status=${next}`)
    ElMessage.success(next === STATUS_ACTIVE ? '已启用' : '已停用')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function toggleStatus(row: Row) {
  try {
    const next = row.status === 1 ? 0 : 1
    await http.put(`/web/admin/access-list/${row.id}/status?status=${next}`)
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function remove(row: Row) {
  try {
    const url = isWhitelist.value ? `/web/whitelist/${row.id}` : `/web/admin/access-list/${row.id}`
    await ElMessageBox.confirm(isWhitelist.value ? '删除后该人员不能再免预约通行，确定删除？' : '确定删除该名单记录？', '提示', {
      type: 'warning',
    })
    await http.delete(url)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 取消
  }
}

onMounted(async () => {
  try {
    me.value = await http.get<Me>('/auth/me')
  } catch {
    me.value = {}
  }
  await load(1)
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.access-table {
  margin-top: 12px;
}
.access-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.access-toolbar :deep(.el-form-item) {
  margin: 0;
}
.access-toolbar :deep(.el-button) {
  margin-left: 0;
}
.whitelist-tip {
  margin-bottom: 12px;
}
.pending-text {
  color: #8a6d3b;
  font-size: 12px;
}
</style>
