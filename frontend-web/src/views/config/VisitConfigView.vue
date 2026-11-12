<template>
  <div class="visit-config-page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="访客类型按人配置通行次数和可预约时间段；校友通行月份在下方单独配置。表单字段和审批流程仍使用各自页面。"
    />

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-head">
          <span>访客类型</span>
          <el-button type="primary" size="small" @click="openCreate">新增类型</el-button>
        </div>
      </template>
      <el-table v-loading="loading" :data="types" border>
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="name" label="名称" width="150" />
        <el-table-column label="可通行次数" width="120">
          <template #default="{ row }">{{ row.maxEntries === 0 ? '不限' : row.maxEntries }}</template>
        </el-table-column>
        <el-table-column label="可预约时间段" min-width="320">
          <template #default="{ row }">{{ ruleSummary(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleType(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" :disabled="isBuiltin(row)" @click="removeType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-head">
          <span>校友自然月通行</span>
          <el-button type="primary" size="small" :loading="savingMonths" @click="saveMonths">保存</el-button>
        </div>
      </template>
      <div class="month-row">
        <span class="month-label">通行月份</span>
        <el-checkbox-group v-model="alumniMonths">
          <el-checkbox v-for="month in 12" :key="month" :value="month" :label="month">{{ month }}月</el-checkbox>
        </el-checkbox-group>
      </div>
      <p class="month-hint">
        选中的月份内，校友无需预约，可直接获得当月通行码；未选月份校友仍可正常预约。
      </p>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑访客类型' : '新增访客类型'" width="760px">
      <el-form label-width="110px">
        <el-form-item label="类型编码">
          <el-input v-model="form.code" :disabled="!!form.id" placeholder="如 NORMAL，创建后不可修改" />
        </el-form-item>
        <el-form-item label="显示名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="可通行次数">
          <el-input-number v-model="form.maxEntries" :min="0" :max="999999" />
          <span class="field-hint">0 表示不限次数</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-form-item label="可预约时间段">
          <div class="rules">
            <div v-for="(rule, index) in form.rules" :key="index" class="rule-row">
              <el-select v-model="rule.mode" style="width: 130px" @change="onModeChange(rule)">
                <el-option v-for="item in MODES" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>

              <el-select
                v-if="rule.mode === 'WEEKLY'"
                v-model="rule.values"
                multiple
                collapse-tags
                style="width: 190px"
                placeholder="选择星期"
              >
                <el-option v-for="item in WEEKDAYS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-select
                v-else-if="rule.mode === 'MONTHLY'"
                v-model="rule.values"
                multiple
                collapse-tags
                style="width: 190px"
                placeholder="选择月份"
              >
                <el-option v-for="month in 12" :key="month" :label="`${month}月`" :value="String(month)" />
              </el-select>
              <el-input
                v-else-if="rule.mode !== 'UNLIMITED'"
                v-model="rule.valuesText"
                style="width: 220px"
                :placeholder="rule.mode === 'DAILY' ? '每月几号，如 1,15' : '月-日，如 9-1,10-1'"
              />

              <el-time-picker
                v-model="rule.startTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="开始时间"
                clearable
                style="width: 125px"
              />
              <span class="time-sep">至</span>
              <el-time-picker
                v-model="rule.endTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="结束时间"
                clearable
                style="width: 125px"
              />
              <el-button link type="danger" :disabled="form.rules.length <= 1" @click="removeRule(index)">
                删除
              </el-button>
            </div>
            <el-button link type="primary" @click="addRule">+ 添加时间段</el-button>
            <div class="rule-hint">同一类型多条规则按“或”生效；不填时间表示全天。</div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface BookingRule {
  mode: string
  values: string[]
  valuesText?: string
  startTime?: string
  endTime?: string
}

interface VisitorType {
  id: number
  code: string
  name: string
  maxEntries: number
  bookingRulesJson?: string
  status: number
  sortNo: number
}

const MODES = [
  { value: 'DAILY', label: '按日' },
  { value: 'WEEKLY', label: '按周' },
  { value: 'MONTHLY', label: '按月' },
  { value: 'MONTH_DAY', label: '按月+日' },
  { value: 'UNLIMITED', label: '不限' },
]
const BUILTIN_CODES = new Set(['NORMAL', 'PARENT', 'ALUMNI', 'CONSTRUCTION'])

const WEEKDAYS = [
  { value: '1', label: '周一' },
  { value: '2', label: '周二' },
  { value: '3', label: '周三' },
  { value: '4', label: '周四' },
  { value: '5', label: '周五' },
  { value: '6', label: '周六' },
  { value: '7', label: '周日' },
]

const types = ref<VisitorType[]>([])
const loading = ref(false)
const saving = ref(false)
const savingMonths = ref(false)
const visible = ref(false)
const alumniMonths = ref<number[]>([])
const form = reactive({
  id: undefined as number | undefined,
  code: '',
  name: '',
  maxEntries: 1,
  status: 1,
  sortNo: 0,
  rules: [] as BookingRule[],
})

function parseRules(json?: string): BookingRule[] {
  if (!json) return []
  try {
    const parsed = JSON.parse(json) as BookingRule[]
    return parsed.map((rule) => ({
      mode: rule.mode || 'UNLIMITED',
      values: Array.isArray(rule.values) ? rule.values.map(String) : [],
      valuesText: Array.isArray(rule.values) ? rule.values.join(',') : '',
      startTime: rule.startTime || '',
      endTime: rule.endTime || '',
    }))
  } catch {
    return []
  }
}

function ruleSummary(row: VisitorType) {
  const rules = parseRules(row.bookingRulesJson)
  if (rules.length === 0) return '不限'
  return rules.map((rule) => {
    const mode = MODES.find((item) => item.value === rule.mode)?.label || rule.mode
    let values = ''
    if (rule.mode === 'WEEKLY') {
      values = rule.values.map((value) => WEEKDAYS.find((item) => item.value === value)?.label || value).join('、')
    } else if (rule.mode === 'MONTHLY') {
      values = rule.values.map((value) => `${value}月`).join('、')
    } else if (rule.mode !== 'UNLIMITED') {
      values = rule.values.join('、')
    }
    const time = rule.startTime && rule.endTime ? ` ${rule.startTime}-${rule.endTime}` : ' 全天'
    return rule.mode === 'UNLIMITED' && !rule.startTime ? '不限' : `${mode} ${values}${time}`.trim()
  }).join('；')
}

function addRule() {
  form.rules.push({ mode: 'WEEKLY', values: [], valuesText: '', startTime: '', endTime: '' })
}

function removeRule(index: number) {
  form.rules.splice(index, 1)
}

function onModeChange(rule: BookingRule) {
  rule.values = []
  rule.valuesText = ''
}

function openCreate() {
  Object.assign(form, {
    id: undefined,
    code: '',
    name: '',
    maxEntries: 1,
    status: 1,
    sortNo: 0,
    rules: [{ mode: 'UNLIMITED', values: [], valuesText: '', startTime: '', endTime: '' }],
  })
  visible.value = true
}

function openEdit(row: VisitorType) {
  Object.assign(form, {
    id: row.id,
    code: row.code,
    name: row.name,
    maxEntries: row.maxEntries,
    status: row.status,
    sortNo: row.sortNo,
    rules: parseRules(row.bookingRulesJson),
  })
  if (form.rules.length === 0) {
    form.rules.push({ mode: 'UNLIMITED', values: [], valuesText: '', startTime: '', endTime: '' })
  }
  visible.value = true
}

async function load() {
  loading.value = true
  try {
    types.value = await http.get<VisitorType[]>('/web/admin/visitor-types')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const rules = form.rules.map((rule) => {
      const values = rule.mode === 'WEEKLY' || rule.mode === 'MONTHLY'
        ? rule.values
        : (rule.valuesText || '').split(/[,，]/).map((item) => item.trim()).filter(Boolean)
      return {
        mode: rule.mode,
        values: rule.mode === 'UNLIMITED' ? [] : values,
        startTime: rule.startTime || null,
        endTime: rule.endTime || null,
      }
    })
    const body = {
      code: form.code,
      name: form.name,
      maxEntries: form.maxEntries,
      bookingRules: rules,
      status: form.status,
      sortNo: form.sortNo,
    }
    if (form.id) {
      await http.put(`/web/admin/visitor-types/${form.id}`, body)
    } else {
      await http.post('/web/admin/visitor-types', body)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function isBuiltin(row: VisitorType) {
  return BUILTIN_CODES.has(row.code)
}

async function removeType(row: VisitorType) {
  try {
    await ElMessageBox.confirm(`确定删除访客类型「${row.name}」？`, '提示', { type: 'warning' })
    await http.delete(`/web/admin/visitor-types/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '删除失败')
    }
  }
}

async function toggleType(row: VisitorType) {
  try {
    await http.put(`/web/admin/visitor-types/${row.id}/status?status=${row.status === 1 ? 0 : 1}`)
    ElMessage.success(row.status === 1 ? '已停用' : '已启用')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function loadMonths() {
  try {
    const data = await http.get<{ months: number[] }>('/web/admin/visit-config/alumni-months')
    alumniMonths.value = data.months ?? []
  } catch {
    alumniMonths.value = []
  }
}

async function saveMonths() {
  savingMonths.value = true
  try {
    await http.put('/web/admin/visit-config/alumni-months', { months: alumniMonths.value })
    ElMessage.success('校友通行月份已保存')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    savingMonths.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadMonths()])
})
</script>

<style scoped>
.visit-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.section-card :deep(.el-card__header) {
  padding: 12px 16px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.month-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.month-label {
  flex: 0 0 80px;
  color: #333;
  font-weight: 500;
  line-height: 32px;
}
.month-hint {
  margin: 10px 0 0 96px;
  color: #8a8a8a;
  font-size: 12px;
}
.rules {
  width: 100%;
}
.rule-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.time-sep {
  color: #999;
}
.rule-hint,
.field-hint {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}
</style>
