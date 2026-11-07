<template>
  <div class="layout">
    <el-card shadow="never" class="tree-card">
      <template #header>组织架构</template>
      <el-tree
        :data="tree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        highlight-current
        default-expand-all
        @node-click="onDept"
      />
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="姓名/手机号/工号" clearable style="width: 200px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="personType" clearable placeholder="全部" style="width: 160px" @change="load(1)">
            <el-option v-for="item in TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="openCreate">新增人员</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="personTypeDesc" label="类型" width="120" />
        <el-table-column prop="deptName" label="部门" min-width="140" />
        <el-table-column prop="className" label="班级" width="140" />
        <el-table-column prop="position" label="职务" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在职' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 1" link type="danger" @click="deactivate(row)">停用</el-button>
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
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.personType" style="width: 100%">
            <el-option v-for="item in TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="form.deptId"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="工号"><el-input v-model="form.jobNo" /></el-form-item>
        <el-form-item label="职务"><el-input v-model="form.position" /></el-form-item>
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

interface Staff {
  id: number
  name: string
  phone?: string
  personType?: number
  personTypeDesc?: string
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
const deptId = ref<number>()
const loading = ref(false)
const visible = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  phone: '',
  personType: 1,
  deptId: undefined as number | undefined,
  jobNo: '',
  position: '',
})

async function loadTree() {
  try {
    tree.value = await http.get('/web/admin/departments')
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
      deptId: deptId.value,
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function onDept(node: { id: number }) {
  deptId.value = node.id
  load(1)
}

function openCreate() {
  form.id = undefined
  form.name = ''
  form.phone = ''
  form.personType = 1
  form.deptId = deptId.value
  form.jobNo = ''
  form.position = ''
  visible.value = true
}

function openEdit(row: Staff) {
  form.id = row.id
  form.name = row.name
  form.phone = row.phone ?? ''
  form.personType = row.personType ?? 1
  form.deptId = row.deptId
  form.jobNo = row.jobNo ?? ''
  form.position = row.position ?? ''
  visible.value = true
}

async function save() {
  try {
    const body = {
      name: form.name,
      phone: form.phone || undefined,
      personType: form.personType,
      deptId: form.deptId,
      jobNo: form.jobNo || undefined,
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

async function deactivate(row: Staff) {
  try {
    await ElMessageBox.confirm(`确定停用 ${row.name}？其账号权限会同时回收。`, '提示', { type: 'warning' })
    await http.delete(`/web/admin/staff/${row.id}`)
    ElMessage.success('已停用')
    await load()
  } catch {
    // 取消
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
</style>
