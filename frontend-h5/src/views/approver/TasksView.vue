<template>
  <div class="page">
    <van-nav-bar title="我的审批" left-arrow @click-left="$router.back()" />

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
  background: #f7f8fa;
  padding-bottom: 24px;
}
.card {
  margin-top: 12px;
}
.reject {
  margin-left: 8px;
}
</style>
