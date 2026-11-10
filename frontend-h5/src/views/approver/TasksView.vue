<template>
  <div class="page">
    <van-nav-bar title="我的审批" left-arrow @click-left="$router.back()">
      <template #right>
        <span class="nav-link" @click="$router.push('/a/history')">历史</span>
      </template>
    </van-nav-bar>

    <van-loading v-if="!loaded" class="loading" />

    <van-empty v-else-if="tasks.length === 0" description="暂无待审批任务" />

    <div v-else class="list">
      <div v-for="item in tasks" :key="item.id" class="task-card">
        <div class="task-top">
          <span class="task-node">{{ item.nodeName }}</span>
          <van-tag type="warning" round>待审批</van-tag>
        </div>
        <div class="task-due"><van-icon name="clock-o" /> 截止 {{ item.dueAt }}</div>
        <div class="task-actions">
          <van-button size="small" round type="primary" @click="handle(item, true)">通过</van-button>
          <van-button size="small" round plain type="danger" @click="handle(item, false)">驳回</van-button>
        </div>
      </div>
    </div>
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

const tasks = ref<Task[]>([])
const loaded = ref(false)

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
  background: transparent;
  padding-bottom: 28px;
}
.loading {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
}
.task-card {
  padding: 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(226, 236, 249, 0.9);
  box-shadow: 0 6px 20px rgba(16, 42, 80, 0.06);
}
.task-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.task-node {
  font-size: 16px;
  font-weight: 600;
  color: #10233f;
}
.task-due {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: #61738c;
  font-size: 13px;
}
.task-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eff4fb;
}
.nav-link {
  color: #14508f;
  font-size: 14px;
  font-weight: 500;
}
</style>
