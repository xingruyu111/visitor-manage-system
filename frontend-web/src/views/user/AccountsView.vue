<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="姓名/手机号" clearable style="width: 200px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="role" clearable placeholder="全部" style="width: 160px" @change="load(1)">
            <el-option v-for="item in assignableRoles" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="openCreate">新增账号</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">{{ roleText(row.roles) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="primary" @click="openRole(row)">分配角色</el-button>
            <el-button link type="primary" @click="resetPassword(row)">重置密码</el-button>
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

    <el-dialog v-model="createVisible" title="新增账号" width="420px">
      <el-form label-width="80px">
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="item in assignableRoles" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="form.password" placeholder="留空则默认 123456" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="create">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配角色" width="360px">
      <el-select v-model="newRole" style="width: 100%">
        <el-option v-for="item in assignableRoles" :key="item.code" :label="item.name" :value="item.code" />
      </el-select>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="assignRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface RoleItem {
  code: number
  name: string
  assignable: boolean
}

interface Row {
  id: number
  phone: string
  name: string
  status: number
  roles: number[]
}


const roles = ref<RoleItem[]>([])
const assignableRoles = computed(() => roles.value.filter((item) => item.assignable))
const rows = ref<Row[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const role = ref<number | undefined>()
const loading = ref(false)
const createVisible = ref(false)
const roleVisible = ref(false)
const newRole = ref<number>()
const currentId = ref<number>()
const form = reactive({ phone: '', name: '', role: 5, password: '' })


function roleText(codes: number[]) {
  return codes.map((code) => roles.value.find((item) => item.code === code)?.name ?? String(code)).join('、')
}

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: Row[] }>('/web/websuper/users', {
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      role: role.value,
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.phone = ''
  form.name = ''
  form.role = 5
  form.password = ''
  createVisible.value = true
}

async function create() {
  try {
    await http.post('/web/websuper/users', form)
    ElMessage.success('已创建')
    createVisible.value = false
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function toggle(row: Row) {
  try {
    await http.put(`/web/websuper/users/${row.id}/status?status=${row.status === 1 ? 0 : 1}`)
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

function openRole(row: Row) {
  currentId.value = row.id
  newRole.value = row.roles[0]
  roleVisible.value = true
}

async function assignRole() {
  if (!newRole.value) return
  try {
    await http.put(`/web/websuper/users/${currentId.value}/role?role=${newRole.value}`)
    ElMessage.success('已更新')
    roleVisible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function resetPassword(row: Row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码（至少 6 位）', '重置密码', {
      inputPattern: /^.{6,}$/,
      inputErrorMessage: '密码至少 6 位',
    })
    await http.put(`/web/websuper/users/${row.id}/password?password=${encodeURIComponent(value)}`)
    ElMessage.success('密码已重置')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  }
}

async function loadRoles() {
  try {
    roles.value = await http.get<RoleItem[]>('/web/common/roles')
  } catch {
    roles.value = []
  }
}

onMounted(async () => {
  await loadRoles()
  await load(1)
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
