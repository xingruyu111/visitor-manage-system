<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="访客类型">
          <el-select v-model="visitorType" clearable placeholder="全部" style="width: 160px" @change="load">
            <el-option v-for="item in TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">新增字段</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column label="访客类型" width="120">
          <template #default="{ row }">{{ TYPE_LABEL[row.visitorType] ?? row.visitorType }}</template>
        </el-table-column>
        <el-table-column prop="fieldKey" label="字段编码" width="150" />
        <el-table-column prop="label" label="显示名称" min-width="140" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ TYPE_LABELS[row.fieldType] || row.fieldType }}</template>
        </el-table-column>
        <el-table-column label="必填" width="80">
          <template #default="{ row }">
            <el-tag :type="row.required === 1 ? 'danger' : 'info'">{{ row.required === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="options" label="选项" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑字段' : '新增字段'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="访客类型" required>
          <el-select v-model="form.visitorType" style="width: 100%">
            <el-option v-for="item in TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段编码" required>
          <el-input v-model="form.fieldKey" :disabled="!!form.id" placeholder="如 company" />
        </el-form-item>
        <el-form-item label="显示名称" required><el-input v-model="form.label" placeholder="如 工作单位" /></el-form-item>
        <el-form-item label="字段类型" required>
          <el-select v-model="form.fieldType" style="width: 100%">
            <el-option v-for="item in FIELD_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="form.required" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="form.fieldType === 'SELECT'" label="选项" required>
          <el-input v-model="form.options" placeholder="逗号分隔，如 A,B,C" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface Field {
  id: number
  visitorType: string
  fieldKey: string
  label: string
  fieldType: string
  required: number
  options?: string
  sortNo: number
  status: number
}

const TYPES = ref([
  { value: 'NORMAL', label: '普通访客' },
  { value: 'ALUMNI', label: '校友' },
  { value: 'PARENT', label: '学生家长' },
  { value: 'CONSTRUCTION', label: '施工/合作' },
])
const TYPE_LABEL = ref<Record<string, string>>(Object.fromEntries(TYPES.value.map((item) => [item.value, item.label])))
const FIELD_TYPES = [
  { value: 'TEXT', label: '文本' },
  { value: 'NUMBER', label: '数字' },
  { value: 'DATE', label: '日期' },
  { value: 'SELECT', label: '下拉选择' },
  { value: 'PHONE', label: '手机号' },
  { value: 'ID_CARD', label: '身份证号' },
]
const TYPE_LABELS: Record<string, string> = Object.fromEntries(FIELD_TYPES.map((item) => [item.value, item.label]))

const rows = ref<Field[]>([])
const visitorType = ref<string>()
const loading = ref(false)
const visible = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  visitorType: 'NORMAL',
  fieldKey: '',
  label: '',
  fieldType: 'TEXT',
  required: 0,
  options: '',
  status: 1,
})

async function loadVisitorTypes() {
  try {
    const data = await http.get<Array<{ code: string; name: string; status: number }>>('/web/admin/visitor-types')
    const enabled = data.filter((item) => item.status === 1)
    if (enabled.length > 0) {
      TYPES.value = enabled.map((item) => ({ value: item.code, label: item.name }))
      TYPE_LABEL.value = Object.fromEntries(TYPES.value.map((item) => [item.value, item.label]))
    }
  } catch {
    // 保留内置类型兜底
  }
}

async function load() {
  loading.value = true
  try {
    rows.value = await http.get<Field[]>('/web/admin/form-fields', {
      visitorType: visitorType.value,
      onlyActive: false,
    })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.id = undefined
  form.visitorType = visitorType.value ?? 'NORMAL'
  form.fieldKey = ''
  form.label = ''
  form.fieldType = 'TEXT'
  form.required = 0
  form.options = ''
  form.status = 1
  visible.value = true
}

function openEdit(row: Field) {
  form.id = row.id
  form.visitorType = row.visitorType
  form.fieldKey = row.fieldKey
  form.label = row.label
  form.fieldType = row.fieldType
  form.required = row.required
  form.options = row.options ?? ''
  form.status = row.status
  visible.value = true
}

async function save() {
  try {
    const body = {
      visitorType: form.visitorType,
      fieldKey: form.fieldKey,
      label: form.label,
      fieldType: form.fieldType,
      required: form.required,
      options: form.options || undefined,
      status: form.status,
    }
    if (form.id) {
      await http.put(`/web/admin/form-fields/${form.id}`, body)
    } else {
      await http.post('/web/admin/form-fields', body)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function toggleStatus(row: Field) {
  const next = row.status === 1 ? 0 : 1
  try {
    await http.put(`/web/admin/form-fields/${row.id}`, {
      visitorType: row.visitorType,
      fieldKey: row.fieldKey,
      label: row.label,
      fieldType: row.fieldType,
      required: row.required,
      options: row.options,
      sortNo: row.sortNo,
      status: next,
    })
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function remove(row: Field) {
  try {
    await ElMessageBox.confirm(`确定删除字段「${row.label}」？`, '提示', { type: 'warning' })
    await http.delete(`/web/admin/form-fields/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 取消
  }
}

onMounted(async () => {
  await loadVisitorTypes()
  await load()
})
</script>
