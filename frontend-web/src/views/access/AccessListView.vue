<template>
  <div>
    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="load(1)">
        <el-tab-pane label="黑名单" name="1" />
        <el-tab-pane label="白名单" name="2" />
      </el-tabs>

      <el-form inline>
        <el-form-item label="对象">
          <el-select v-model="targetType" clearable placeholder="全部" style="width: 150px" @change="load(1)">
            <el-option label="人员" value="PERSON" />
            <el-option label="机动车" value="PLATE_MOTOR" />
            <el-option label="电动自行车" value="PLATE_EBIKE" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="手机号/车牌/姓名" clearable style="width: 200px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="openCreate">新增</el-button>
        <el-upload
          class="upload"
          :show-file-list="false"
          accept=".xlsx,.xls"
          :http-request="uploadExcel"
        >
          <el-button :loading="uploading">Excel 导入</el-button>
        </el-upload>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column label="对象" width="110">
          <template #default="{ row }">{{ TYPE[row.targetType] ?? row.targetType }}</template>
        </el-table-column>
        <el-table-column prop="targetValue" label="手机号/车牌" min-width="150" />
        <el-table-column prop="personName" label="姓名" width="120" />
        <el-table-column prop="plateNumber" label="车牌" width="120" />
        <el-table-column prop="effectiveFrom" label="生效时间" width="170" />
        <el-table-column prop="effectiveTo" label="失效时间" width="170" />
        <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '生效中' : '已失效' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="warning" @click="release(row)">解除</el-button>
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

    <el-dialog v-model="visible" title="新增名单" width="480px">
      <el-form label-width="90px">
        <el-form-item label="名单类型">
          <el-radio-group v-model="form.listType">
            <el-radio :value="1">黑名单</el-radio>
            <el-radio :value="2">白名单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="对象类型">
          <el-select v-model="form.targetType" style="width: 100%">
            <el-option label="人员" value="PERSON" />
            <el-option label="机动车" value="PLATE_MOTOR" />
            <el-option label="电动自行车" value="PLATE_EBIKE" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号/车牌">
          <el-input v-model="form.targetValue" placeholder="人员填手机号，车辆填车牌" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.personName" /></el-form-item>
        <el-form-item label="车牌"><el-input v-model="form.plateNumber" /></el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker v-model="form.effectiveTo" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="create">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface Row {
  id: number
  listType: number
  targetType: string
  targetValue: string
  personName?: string
  plateNumber?: string
  effectiveTo?: string
  reason?: string
  status: number
}

const TYPE: Record<string, string> = { PERSON: '人员', PLATE_MOTOR: '机动车', PLATE_EBIKE: '电动自行车' }

const activeTab = ref('1')
const rows = ref<Row[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const targetType = ref<string>()
const loading = ref(false)
const uploading = ref(false)
const visible = ref(false)
const form = reactive({
  listType: 1,
  targetType: 'PERSON',
  targetValue: '',
  personName: '',
  plateNumber: '',
  effectiveTo: '',
  reason: '',
})

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: Row[] }>('/web/admin/access-list', {
      page: page.value,
      size: size.value,
      listType: Number(activeTab.value),
      targetType: targetType.value,
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

function openCreate() {
  form.listType = Number(activeTab.value)
  form.targetType = 'PERSON'
  form.targetValue = ''
  form.personName = ''
  form.plateNumber = ''
  form.effectiveTo = ''
  form.reason = ''
  visible.value = true
}

async function create() {
  try {
    await http.post('/web/admin/access-list', {
      listType: form.listType,
      targetType: form.targetType,
      targetValue: form.targetValue,
      personName: form.personName || undefined,
      plateNumber: form.plateNumber || undefined,
      effectiveTo: form.effectiveTo || undefined,
      reason: form.reason || undefined,
    })
    ElMessage.success('已新增')
    visible.value = false
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '新增失败')
  }
}

async function release(row: Row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入解除原因', '提前解除', { inputPlaceholder: '如：家长申诉' })
    await http.post(`/web/admin/access-list/${row.id}/release?reason=${encodeURIComponent(value || '')}`)
    ElMessage.success('已解除')
    await load()
  } catch {
    // 取消
  }
}

async function remove(row: Row) {
  try {
    await ElMessageBox.confirm('确定删除该名单记录？', '提示', { type: 'warning' })
    await http.delete(`/web/admin/access-list/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 取消
  }
}

async function uploadExcel(options: { file: File }) {
  uploading.value = true
  try {
    const body = new FormData()
    body.append('file', options.file)
    const response = await fetch('/api/web/admin/access-list/import', {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
      body,
    })
    const result = await response.json()
    if (result.code !== 200) {
      throw new Error(result.message ?? '导入失败')
    }
    const summary = `成功 ${result.data.success} 条，失败 ${result.data.failed} 条`
    const errors = (result.data.errors ?? []).map((item: { row: number; reason: string }) => `第 ${item.row} 行：${item.reason}`)
    await ElMessageBox.alert([summary, ...errors].join('\n'), '导入结果', { type: errors.length ? 'warning' : 'success' })
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  } finally {
    uploading.value = false
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.upload {
  display: inline-block;
  margin-left: 12px;
}
</style>
