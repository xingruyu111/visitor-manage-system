<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="IP">
          <el-input v-model="keyword" placeholder="模糊查询" clearable style="width: 180px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
        <el-button @click="openAdd">新增</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="ip" label="IP" width="160" />
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="expireAt" label="失效时间" width="180">
          <template #default="{ row }">{{ row.expireAt || '永久' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
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

    <el-dialog v-model="visible" title="新增 IP 黑名单" width="420px">
      <el-form label-width="90px">
        <el-form-item label="IP"><el-input v-model="form.ip" placeholder="如 1.2.3.4" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" /></el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker v-model="form.expireAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" placeholder="留空为永久" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface Entry {
  id: number
  ip: string
  reason?: string
  expireAt?: string
  createdAt?: string
}

const rows = ref<Entry[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const loading = ref(false)
const visible = ref(false)
const form = reactive({ ip: '', reason: '', expireAt: '' })

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: Entry[] }>('/web/admin/ip-blacklist', {
      page: page.value,
      size: size.value,
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

function openAdd() {
  form.ip = ''
  form.reason = ''
  form.expireAt = ''
  visible.value = true
}

async function save() {
  try {
    const params = new URLSearchParams({ ip: form.ip })
    if (form.reason) params.set('reason', form.reason)
    if (form.expireAt) params.set('expireAt', form.expireAt)
    await http.post(`/web/admin/ip-blacklist?${params.toString()}`)
    ElMessage.success('已新增')
    visible.value = false
    await load(1)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '新增失败')
  }
}

async function remove(row: Entry) {
  try {
    await ElMessageBox.confirm(`确定移除 ${row.ip}？`, '提示', { type: 'warning' })
    await http.delete(`/web/admin/ip-blacklist/${row.id}`)
    ElMessage.success('已移除')
    await load()
  } catch {
    // 取消
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
