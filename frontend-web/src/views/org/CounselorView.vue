<template>
  <div class="page">
    <el-card shadow="never">
      <div class="toolbar">
        <div class="filters">
          <el-input
            v-model="keyword"
            placeholder="辅导员姓名/工号/组织"
            clearable
            style="width: 260px"
            @keyup.enter="load(1)"
          />
          <el-button type="primary" @click="load(1)">查询</el-button>
        </div>
        <el-button type="primary" @click="openCreate">新增辅导员</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" border class="table">
        <el-table-column prop="counselorName" label="辅导员" width="110" />
        <el-table-column prop="counselorPersonNo" label="工号" width="120" />
        <el-table-column prop="counselorPhone" label="手机号" width="140" />
        <el-table-column prop="orgName" label="负责组织" min-width="220" show-overflow-tooltip />
        <el-table-column prop="studentCount" label="覆盖学生" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">移除</el-button>
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

    <el-dialog v-model="visible" :title="editingId ? '编辑辅导员' : '新增辅导员'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="辅导员">
          <el-select
            v-model="form.counselorStaffId"
            filterable
            remote
            :remote-method="searchCandidates"
            :loading="candidateLoading"
            :disabled="!!editingId"
            placeholder="搜索教职工姓名/工号/手机号"
            style="width: 100%"
          >
            <el-option
              v-for="item in candidates"
              :key="item.id"
              :label="`${item.name}（${item.personNo || item.phone}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="负责组织">
          <el-tree-select
            v-model="form.orgId"
            :data="tree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            clearable
            placeholder="选择组织，含所有下级"
            style="width: 100%"
            @change="previewOrg"
          />
        </el-form-item>
        <el-alert
          v-if="preview"
          type="info"
          :closable="false"
          :title="`该组织下共 ${preview.studentCount} 名学生、${preview.staffCount} 名人员`"
        />
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

interface CounselorRow {
  id: number
  counselorStaffId: number
  counselorName: string
  counselorPersonNo?: string
  counselorPhone?: string
  orgId: number
  orgName?: string
  studentCount: number
  createdAt?: string
}

interface Candidate {
  id: number
  name: string
  personNo?: string
  phone?: string
}

interface OrgNode {
  id: number
  name: string
  children?: OrgNode[]
}

const keyword = ref('')
const rows = ref<CounselorRow[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)
const tree = ref<OrgNode[]>([])

const visible = ref(false)
const editingId = ref<number>()
const saving = ref(false)
const candidates = ref<Candidate[]>([])
const candidateLoading = ref(false)
const preview = ref<{ studentCount: number; staffCount: number }>()
const form = reactive({ counselorStaffId: undefined as number | undefined, orgId: undefined as number | undefined })

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: CounselorRow[] }>('/web/admin/counselors', {
      keyword: keyword.value || undefined,
      page: target,
      size,
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadTree() {
  try {
    tree.value = await http.get<OrgNode[]>('/web/admin/orgs/tree')
  } catch {
    tree.value = []
  }
}

async function searchCandidates(query: string) {
  candidateLoading.value = true
  try {
    candidates.value = await http.get<Candidate[]>('/web/admin/counselors/candidates', {
      keyword: query || undefined,
    })
  } catch {
    candidates.value = []
  } finally {
    candidateLoading.value = false
  }
}

function openCreate() {
  editingId.value = undefined
  form.counselorStaffId = undefined
  form.orgId = undefined
  preview.value = undefined
  candidates.value = []
  visible.value = true
  searchCandidates('')
}

function openEdit(row: CounselorRow) {
  editingId.value = row.id
  form.counselorStaffId = row.counselorStaffId
  form.orgId = row.orgId
  preview.value = undefined
  candidates.value = [{
    id: row.counselorStaffId,
    name: row.counselorName,
    personNo: row.counselorPersonNo,
    phone: row.counselorPhone,
  }]
  visible.value = true
  previewOrg(row.orgId)
}

async function previewOrg(orgId?: number) {
  preview.value = undefined
  if (!orgId) return
  try {
    preview.value = await http.get<{ studentCount: number; staffCount: number }>(
      '/web/admin/counselors/preview',
      { orgId },
    )
  } catch {
    preview.value = undefined
  }
}

async function save() {
  if (!form.counselorStaffId || !form.orgId) {
    ElMessage.warning('请选择辅导员和负责组织')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await http.put(`/web/admin/counselors/${editingId.value}?orgId=${form.orgId}`)
      ElMessage.success('已修改负责组织')
    } else {
      await http.post('/web/admin/counselors', {
        counselorStaffId: form.counselorStaffId,
        orgId: form.orgId,
      })
      ElMessage.success('已新增辅导员')
    }
    visible.value = false
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row: CounselorRow) {
  try {
    await ElMessageBox.confirm(`确定移除 ${row.counselorName} 对 ${row.orgName} 的辅导员关联？`, '移除辅导员', {
      type: 'warning',
    })
    await http.delete(`/web/admin/counselors/${row.id}`)
    ElMessage.success('已移除')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '移除失败')
    }
  }
}

onMounted(async () => {
  await loadTree()
  await load(1)
})
</script>

<style scoped>
.page {
  padding: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.filters {
  display: flex;
  gap: 8px;
}
.table {
  margin-top: 4px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
