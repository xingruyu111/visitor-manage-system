<template>
  <div class="layout">
    <el-card shadow="never" class="tree-card">
      <template #header>
        <div class="tree-header">
          <span>组织架构</span>
          <div>
            <el-button link type="primary" @click="openDeptCreate">新增</el-button>
            <el-button link type="primary" :disabled="!currentDept" @click="openDeptEdit">编辑</el-button>
            <el-button link type="danger" :disabled="!currentDept" @click="removeDept">删除</el-button>
          </div>
        </div>
      </template>
      <el-tree
        :data="tree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        highlight-current
        default-expand-all
        :expand-on-click-node="false"
        @node-click="onDept"
      />
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-form inline class="filter-form">
          <el-form-item label="关键字">
            <el-input v-model="keyword" placeholder="姓名/手机/工号" clearable style="width: 150px" @keyup.enter="load(1)" />
          </el-form-item>
          <el-form-item label="部门/班级">
            <el-tree-select
              v-model="filterOrgId"
              :data="tree"
              :props="{ label: 'name', children: 'children' }"
              node-key="id"
              check-strictly
              clearable
              placeholder="全部"
              style="width: 150px"
              @change="load(1)"
            />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="personType" clearable placeholder="全部" style="width: 150px" @change="load(1)">
              <el-option v-for="item in TYPES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="toolbar-actions">
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="openCreate">新增</el-button>
          <el-button @click="openImport">导入</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border class="staff-table">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="personNo" label="工号/学号" width="130" />
        <el-table-column prop="phone" label="手机号" width="125" />
        <el-table-column prop="personTypeDesc" label="类型" width="100" />
        <el-table-column prop="orgName" label="部门/班级" min-width="160" />
        <el-table-column prop="position" label="职务" width="100" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeStaff(row)">删除</el-button>
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

    <el-dialog v-model="visible" :title="form.id ? '编辑人员' : '新增人员'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="工号/学号"><el-input v-model="form.personNo" placeholder="唯一，教职工填工号、学生填学号" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="必填，用于开通账号" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.personType" style="width: 100%">
            <el-option v-for="item in TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门/班级">
          <el-tree-select
            v-model="form.orgId"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="职务"><el-input v-model="form.position" placeholder="备注，不参与审批" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入人员" width="560px">
      <el-alert
        type="info"
        :closable="false"
        title="模板列：工号/学号、姓名、手机号、人员类型、部门、班级、职务。导入后自动开通账号，默认密码 123456。"
      />
      <div class="import-row">
        <input ref="fileInput" class="hidden-file" type="file" accept=".xlsx,.xls" @change="onFileChange" />
        <el-button @click="fileInput?.click()">选择文件</el-button>
        <span v-if="importFile" class="file-name">{{ importFile.name }}</span>
      </div>
      <div v-if="importResult" class="import-result">
        共 {{ importResult.total }} 条，成功 {{ importResult.success }}，失败 {{ importResult.failed }}
        <ul v-if="importResult.errors?.length">
          <li v-for="item in importResult.errors" :key="item.row">第 {{ item.row }} 行：{{ item.message }}</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deptVisible" :title="deptForm.id ? '编辑部门' : '新增部门'" width="420px">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="deptForm.name" /></el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="deptForm.parentId"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDept">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request, { http } from '@/api/request'

interface Staff {
  id: number
  name: string
  personNo?: string
  phone?: string
  personType?: number
  personTypeDesc?: string
  orgId?: number
  orgName?: string
  deptId?: number
  deptName?: string
  className?: string
  jobNo?: string
  position?: string
  status: number
}

const TYPES = [
  { value: 1, label: '教职工' },
  { value: 2, label: '学生' },
  { value: 3, label: '校内商户' },
  { value: 4, label: '食堂人员' },
  { value: 5, label: '附属学校教职工' },
  { value: 6, label: '附属学校学生' },
  { value: 7, label: '校友' },
]

const tree = ref<Array<{ id: number; name: string; children?: unknown[] }>>([])
const rows = ref<Staff[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const personType = ref<number>()
const filterOrgId = ref<number>()
const loading = ref(false)
const visible = ref(false)
const importVisible = ref(false)
const importing = ref(false)
const importFile = ref<File>()
const fileInput = ref<HTMLInputElement>()
const importResult = ref<{
  total: number
  success: number
  failed: number
  errors?: Array<{ row: number; message: string }>
}>()
const deptVisible = ref(false)
const currentDept = ref<{ id: number; name: string; parentId?: number } | null>(null)
const deptForm = reactive({ id: undefined as number | undefined, name: '', parentId: undefined as number | undefined })
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  personNo: '',
  phone: '',
  personType: 1,
  orgId: undefined as number | undefined,
  position: '',
})

async function loadTree() {
  try {
    tree.value = await http.get('/web/admin/orgs/tree')
  } catch {
    tree.value = []
  }
}

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: Staff[] }>('/web/admin/staff', {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      personType: personType.value,
      orgId: filterOrgId.value,
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function onDept(node: { id: number; name: string; parentId?: number }) {
  currentDept.value = node
  filterOrgId.value = node.id
  load(1)
}

function openDeptCreate() {
  deptForm.id = undefined
  deptForm.name = ''
  deptForm.parentId = currentDept.value?.id
  deptVisible.value = true
}

function openDeptEdit() {
  if (!currentDept.value) return
  deptForm.id = currentDept.value.id
  deptForm.name = currentDept.value.name
  deptForm.parentId = currentDept.value.parentId
  deptVisible.value = true
}

async function saveDept() {
  try {
    const params = new URLSearchParams({ name: deptForm.name })
    if (deptForm.parentId) {
      params.set('parentId', String(deptForm.parentId))
    }
    if (deptForm.id) {
      await http.put(`/web/admin/orgs/${deptForm.id}?${params.toString()}`)
    } else {
      await http.post(`/web/admin/orgs?${params.toString()}`)
    }
    ElMessage.success('已保存')
    deptVisible.value = false
    await loadTree()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function removeDept() {
  if (!currentDept.value) return
  try {
    await ElMessageBox.confirm(`确定删除部门「${currentDept.value.name}」？`, '提示', { type: 'warning' })
    await http.delete(`/web/admin/orgs/${currentDept.value.id}`)
    ElMessage.success('已删除')
    currentDept.value = null
    await loadTree()
  } catch {
    // 取消
  }
}

function openCreate() {
  form.id = undefined
  form.name = ''
  form.personNo = ''
  form.phone = ''
  form.personType = 1
  form.orgId = currentDept.value?.id
  form.position = ''
  visible.value = true
}

function openEdit(row: Staff) {
  form.id = row.id
  form.name = row.name
  form.personNo = row.personNo ?? ''
  form.phone = row.phone ?? ''
  form.personType = row.personType ?? 1
  form.orgId = row.orgId
  form.position = row.position ?? ''
  visible.value = true
}

function openImport() {
  importFile.value = undefined
  importResult.value = undefined
  importVisible.value = true
  setTimeout(() => fileInput.value?.click(), 100)
}

function onFileChange(event: Event) {
  const files = (event.target as HTMLInputElement).files
  importFile.value = files && files.length > 0 ? files[0] : undefined
  importResult.value = undefined
}

async function doImport() {
  if (!importFile.value) return
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    importResult.value = await request.post('/web/admin/staff/import', formData) as unknown as typeof importResult.value
    ElMessage.success('导入完成')
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  } finally {
    importing.value = false
  }
}

async function save() {
  if (!form.name || !form.personNo || !form.phone) {
    ElMessage.warning('姓名、工号/学号、手机号必填')
    return
  }
  try {
    const body = {
      name: form.name,
      personNo: form.personNo,
      phone: form.phone,
      personType: form.personType,
      orgId: form.orgId,
      position: form.position || undefined,
    }
    if (form.id) {
      await http.put(`/web/admin/staff/${form.id}`, body)
    } else {
      await http.post('/web/admin/staff', body)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}


async function removeStaff(row: Staff) {
  try {
    await ElMessageBox.confirm(`确定删除 ${row.name}？删除后不再显示，账号停用，历史记录保留。`, '删除人员', {
      type: 'warning',
    })
    await http.delete(`/web/admin/staff/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '删除失败')
    }
  }
}

onMounted(async () => {
  await loadTree()
  await load(1)
})
</script>

<style scoped>
.layout {
  display: flex;
  gap: 16px;
}
.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tree-card {
  width: 260px;
  flex-shrink: 0;
}
.table-card {
  flex: 1;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.staff-table {
  margin-top: 12px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 8px;
}

.filter-form {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 8px;
}

.filter-form :deep(.el-form-item__label) {
  padding-right: 8px;
}

.filter-form :deep(.el-form-item) {
  margin: 0;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.import-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0;
}

.hidden-file {
  display: none;
}

.file-name {
  color: #606266;
  font-size: 13px;
}

.import-result {
  margin-top: 12px;
  font-size: 13px;
  color: #303133;
}

.import-result ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #f56c6c;
}
</style>
