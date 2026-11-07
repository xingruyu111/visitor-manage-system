<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="status" clearable placeholder="全部" style="width: 140px" @change="load(1)">
            <el-option v-for="(label, value) in STATUS" :key="value" :label="label" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="keyword" placeholder="姓名/手机号/被访人" clearable style="width: 220px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-button type="primary" @click="load(1)">查询</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="visitorName" label="访客" width="120" />
        <el-table-column prop="visitorPhone" label="手机号" width="140" />
        <el-table-column prop="targetName" label="被访人" width="120" />
        <el-table-column prop="visitPurpose" label="事由" min-width="160" show-overflow-tooltip />
        <el-table-column label="到访时间" width="180">
          <template #default="{ row }">{{ row.expectedTime }} ~ {{ row.leaveTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="TAG[row.status] ?? 'info'">{{ STATUS[row.status] ?? row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="drawer" title="访客详情" size="420px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="访客">{{ current.visitorName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ current.visitorPhone }}</el-descriptions-item>
        <el-descriptions-item label="证件号">{{ current.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被访人">{{ current.targetName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事由">{{ current.visitPurpose || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到访">{{ current.expectedTime }}</el-descriptions-item>
        <el-descriptions-item label="离校">{{ current.leaveTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ STATUS[current.status] }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface VisitorRecord {
  id: number
  visitorName: string
  visitorPhone: string
  idCard?: string
  targetName?: string
  visitPurpose?: string
  expectedTime: string
  leaveTime: string
  status: number
}

const STATUS: Record<number, string> = {
  0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已入校', 4: '已离校', 5: '已取消', 6: '已过期',
}
const TAG: Record<number, string> = {
  0: 'warning', 1: 'success', 2: 'danger', 3: 'primary', 4: 'info', 5: 'info', 6: 'info',
}

const rows = ref<VisitorRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const status = ref<number | undefined>()
const keyword = ref('')
const loading = ref(false)
const drawer = ref(false)
const current = ref<VisitorRecord>()

async function load(targetPage = page.value) {
  page.value = targetPage
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: VisitorRecord[] }>('/web/admin/visitor-records', {
      params: { page: page.value, size: size.value, status: status.value, keyword: keyword.value || undefined },
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openDetail(row: VisitorRecord) {
  current.value = row
  drawer.value = true
}

onMounted(() => load(1))
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
