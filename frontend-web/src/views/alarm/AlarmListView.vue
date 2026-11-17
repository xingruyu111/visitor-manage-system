<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="handled" clearable placeholder="全部" style="width: 140px" @change="load(1)">
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-button :loading="loading" @click="load(1)">刷新</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="alarmType" label="类型" width="120">
          <template #default="{ row }">{{ TYPE[row.alarmType] ?? row.alarmType }}</template>
        </el-table-column>
        <el-table-column prop="recordId" label="预约记录" width="110" />
        <el-table-column prop="message" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="产生时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.handled === 1 ? 'success' : 'danger'">{{ row.handled === 1 ? '已处理' : '待处理' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handledAt" label="处理时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.handled === 0" link type="success" @click="handle(row)">标记处理</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface Alarm {
  id: number
  recordId: number
  alarmType: string
  message?: string
  handled: number
  createdAt?: string
  handledAt?: string
}

const TYPE: Record<string, string> = { OVERSTAY: '超期滞留', BLACKLIST: '黑名单拦截' }

const rows = ref<Alarm[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const handled = ref<number>()
const loading = ref(false)

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: Alarm[] }>('/web/admin/alarms', {
      page: page.value,
      size: size.value,
      handled: handled.value,
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handle(row: Alarm) {
  try {
    await ElMessageBox.confirm('确认该告警已处理？', '提示', { type: 'warning' })
    await http.post(`/web/admin/alarms/${row.id}/handle`)
    ElMessage.success('已处理')
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
