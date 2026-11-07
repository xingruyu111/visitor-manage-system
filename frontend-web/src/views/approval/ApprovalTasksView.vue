<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>我的待审批任务</span>
          <div>
            <el-button :disabled="selected.length === 0" type="success" @click="batch(true)">批量通过</el-button>
            <el-button :disabled="selected.length === 0" type="danger" plain @click="batch(false)">批量驳回</el-button>
            <el-button :loading="loading" @click="load">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" border @selection-change="onSelect">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="nodeName" label="审批节点" min-width="160" />
        <el-table-column prop="instanceId" label="审批实例" width="120" />
        <el-table-column prop="dueAt" label="截止时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="handle(row, true)">通过</el-button>
            <el-button link type="danger" @click="handle(row, false)">驳回</el-button>
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

interface Task {
  id: number
  nodeName: string
  instanceId: number
  dueAt: string
}

const rows = ref<Task[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const selected = ref<Task[]>([])

function onSelect(value: Task[]) {
  selected.value = value
}

async function load(target = page.value) {
  page.value = target
  loading.value = true
  try {
    const data = await http.get<{ total: number; records: Task[] }>('/web/approval/tasks', {
      page: page.value,
      size: size.value,
    })
    rows.value = data.records
    total.value = data.total
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function handle(task: Task, approve: boolean) {
  try {
    const { value } = await ElMessageBox.prompt(approve ? '审批意见（可选）' : '驳回原因', approve ? '通过' : '驳回', {
      inputPlaceholder: approve ? '同意' : '请填写原因',
      inputValidator: (input) => (approve || (input && input.length > 0) ? true : '驳回必须填写原因'),
    })
    const action = approve ? 'approve' : 'reject'
    await http.post(`/web/approval/tasks/${task.id}/${action}?comment=${encodeURIComponent(value || '')}`)
    ElMessage.success(approve ? '已通过' : '已驳回')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  }
}

async function batch(approve: boolean) {
  try {
    const { value } = await ElMessageBox.prompt(
      `将处理 ${selected.value.length} 条任务，${approve ? '审批意见（可选）' : '驳回原因'}`,
      approve ? '批量通过' : '批量驳回',
      { inputPlaceholder: approve ? '批量通过' : '请填写原因' },
    )
    const ids = selected.value.map((item) => item.id)
    const path = approve ? '/web/approval/tasks/batch-approve' : '/web/approval/tasks/batch-reject'
    await http.post(path, { taskIds: ids, comment: value || '' })
    ElMessage.success('已处理')
    await load(1)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '操作失败')
    }
  }
}

onMounted(() => load(1))
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
