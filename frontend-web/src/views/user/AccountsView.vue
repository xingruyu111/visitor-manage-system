<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="姓名/手机号/工号" clearable style="width: 220px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="role" clearable placeholder="全部" style="width: 180px" @change="load(1)">
            <el-option label="访客" :value="4" />
            <el-option v-for="item in assignableRoles" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="openCreate">新增独立账号</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="personNo" label="工号/学号" width="150">
          <template #default="{ row }">{{ row.personNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="130" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column label="角色" min-width="260">
          <template #default="{ row }">
            <el-tag
              v-for="item in row.roleDetails"
              :key="item.code"
              :type="item.source === 'IDENTITY' ? 'info' : 'primary'"
              class="role-tag"
              effect="plain"
            >
              {{ item.name }}<span v-if="item.isDefault === 1">（默认）</span>
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRoles(row)">角色</el-button>
            <el-button v-if="!row.personNo" link type="primary" @click="changePhone(row)">改手机号</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
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

    <el-dialog v-model="roleVisible" title="角色管理" width="520px">
      <div v-if="current" class="role-head">
        <span>{{ current.name }}</span>
        <span class="sub">{{ current.personNo || current.phone }}</span>
      </div>
      <el-tag
        v-for="item in current?.roleDetails ?? []"
        :key="item.code"
        :type="item.source === 'IDENTITY' ? 'info' : 'primary'"
        class="role-tag"
        :closable="item.source !== 'IDENTITY' && item.code !== 12"
        :disable-transitions="true"
        @close="removeRole(item)"
      >
        {{ item.name }}<span v-if="item.isDefault === 1">（默认）</span>
      </el-tag>
      <p v-if="current?.roleDetails?.some((item) => item.code === 12)" class="hint">
        辅导员角色由“人员管理 → 辅导员管理”授予和移除，不能在此手工删除。
      </p>
      <p v-if="current?.roleDetails?.some((item) => item.source === 'IDENTITY')" class="hint">
        灰色为人员类型生成的身份角色，不可删除；如需变更请修改人员类型或停用人员。
      </p>
      <div class="add-role">
        <el-select v-model="newRole" placeholder="选择附加角色" style="width: 220px">
          <el-option
            v-for="item in assignableRoles.filter((role) => !hasRole(role.code))"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
        <el-button type="primary" :disabled="!newRole" @click="addRole">添加</el-button>
      </div>
      <template #footer>
        <el-button @click="roleVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createVisible" title="新增独立账号" width="460px">
      <el-alert
        type="info"
        :closable="false"
        title="用于没有人员档案的账号（如保安公司人员）。人员请从人员管理新增，系统会自动开通账号。"
        class="tip"
      />
      <el-form label-width="80px">
        <el-form-item label="手机号" required><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色" required>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface RoleDetail {
  code: number
  name: string
  source: string
  isDefault: number
  status: number
}

interface RoleItem {
  code: number
  name: string
  assignable: boolean
}

interface Row {
  id: number
  personNo?: string
  phone: string
  name: string
  status: number
  roles: number[]
  roleDetails: RoleDetail[]
}

const rows = ref<Row[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const role = ref<number>()
const loading = ref(false)
const roleVisible = ref(false)
const createVisible = ref(false)
const current = ref<Row>()
const newRole = ref<number>()
const roles = ref<RoleItem[]>([])
const form = reactive({ phone: '', name: '', role: 5, password: '' })

const assignableRoles = ref<RoleItem[]>([])

function hasRole(code: number) {
  return current.value?.roleDetails?.some((item) => item.code === code) ?? false
}

async function loadRoles() {
  try {
    roles.value = await http.get<RoleItem[]>('/web/common/roles')
    assignableRoles.value = roles.value.filter((item) => item.assignable)
  } catch {
    assignableRoles.value = []
  }
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

function openRoles(row: Row) {
  current.value = row
  newRole.value = undefined
  roleVisible.value = true
}

async function addRole() {
  if (!current.value || !newRole.value) return
  try {
    await http.post(`/web/websuper/users/${current.value.id}/roles?role=${newRole.value}`)
    ElMessage.success('已添加')
    await refreshCurrent()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '添加失败')
  }
}

async function removeRole(item: RoleDetail) {
  if (!current.value) return
  try {
    await http.delete(`/web/websuper/users/${current.value.id}/roles/${item.code}`)
    ElMessage.success('已移除')
    await refreshCurrent()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '移除失败')
  }
}


async function refreshCurrent() {
  await load()
  if (current.value) {
    current.value = rows.value.find((item) => item.id === current.value?.id) ?? current.value
  }
  newRole.value = undefined
}

async function toggle(row: Row) {
  const next = row.status === 1 ? 0 : 1
  try {
    if (next === 0) {
      await ElMessageBox.confirm(`确定停用 ${row.name}？停用后立即无法登录。`, '提示', { type: 'warning' })
    }
    await http.put(`/web/websuper/users/${row.id}/status?status=${next}`)
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  }
}

async function changePhone(row: Row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的登录手机号', `修改 ${row.name} 的手机号`, {
      inputPattern: /^1\d{10}$/,
      inputErrorMessage: '请输入 11 位手机号',
      inputValue: row.phone,
    })
    await http.put(`/web/websuper/users/${row.id}/phone?phone=${encodeURIComponent(value)}`)
    ElMessage.success('手机号已修改')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '修改失败')
    }
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
.role-tag {
  margin: 2px 6px 2px 0;
}
.role-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
  font-weight: 600;
}
.sub {
  color: #909399;
  font-weight: 400;
  font-size: 13px;
}
.hint {
  margin: 8px 0 12px;
  color: #909399;
  font-size: 12px;
}
.add-role {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
.tip {
  margin-bottom: 16px;
}
</style>
