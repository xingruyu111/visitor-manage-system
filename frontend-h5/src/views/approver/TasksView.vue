<template>
  <div class="page">
    <van-nav-bar title="我的审批" left-arrow @click-left="$router.back()">
      <template #right>
        <span class="nav-link" @click="openHistory">历史</span>
      </template>
    </van-nav-bar>

    <van-empty v-if="loaded && tasks.length === 0" description="暂无待审批任务" />

    <van-cell-group v-for="item in tasks" :key="item.id" inset class="card">
      <van-cell :title="item.nodeName" :label="`截止 ${item.dueAt}`" />
      <van-cell>
        <template #title>
          <van-button size="small" type="primary" @click="handle(item, true)">通过</van-button>
          <van-button size="small" type="danger" plain class="reject" @click="handle(item, false)">驳回</van-button>
        </template>
      </van-cell>
    </van-cell-group>

    <van-popup v-model:show="historyVisible" position="bottom" round :style="{ height: '70%' }">
      <div class="history">
        <h3>历史审批</h3>
        <van-empty v-if="history.length === 0" description="暂无历史" />
        <van-cell-group v-for="item in history" :key="item.taskId" inset class="card">
          <van-cell :title="item.nodeName" :label="`${item.actedAt}`">
            <template #value>
              <van-tag :type="item.decision === 'APPROVED' ? 'success' : 'danger'">
                {{ item.decision === 'APPROVED' ? '通过' : '驳回' }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell v-if="item.comment" :title="item.comment" />
        </van-cell-group>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface Task {
  id: number
  nodeName: string
  dueAt: string
}

interface HistoryItem {
  taskId: number
  nodeName: string
  decision: string
  comment?: string
  actedAt: string
}

const tasks = ref<Task[]>([])
const history = ref<HistoryItem[]>([])
const loaded = ref(false)
const historyVisible = ref(false)

async function load() {
  try {
    const data = await http.get<{ records: Task[] }>('/web/approval/tasks', { page: 1, size: 50 })
    tasks.value = data.records
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loaded.value = true
  }
}

async function openHistory() {
  historyVisible.value = true
  try {
    const data = await http.get<{ records: HistoryItem[] }>('/web/approval/tasks/history', { page: 1, size: 50 })
    history.value = data.records
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载失败')
  }
}

async function handle(task: Task, approve: boolean) {
  const action = approve ? 'approve' : 'reject'
  try {
    await http.post(`/web/approval/tasks/${task.id}/${action}?comment=H5处理`)
    showToast(approve ? '已通过' : '已驳回')
    await load()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 24px;
}
.card {
  margin-top: 12px;
}
.reject {
  margin-left: 8px;
}
.nav-link {
  color: #1989fa;
  font-size: 14px;
}
.history {
  padding: 16px;
}
</style>
