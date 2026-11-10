<template>
  <div class="page">
    <van-nav-bar title="历史审批" left-arrow @click-left="$router.back()" />

    <van-loading v-if="!loaded" class="loading" />

    <van-empty v-else-if="history.length === 0" description="暂无历史审批" />

    <div v-else class="list">
      <div v-for="item in history" :key="item.taskId" class="history-card">
        <div class="history-top">
          <span class="node">{{ item.nodeName }}</span>
          <van-tag :type="item.decision === 'APPROVED' ? 'success' : 'danger'" round>
            {{ item.decision === 'APPROVED' ? '通过' : '驳回' }}
          </van-tag>
        </div>
        <div class="time">{{ item.actedAt }}</div>
        <div v-if="item.comment" class="comment">{{ item.comment }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface HistoryItem {
  taskId: number
  nodeName: string
  decision: string
  comment?: string
  actedAt: string
}

const history = ref<HistoryItem[]>([])
const loaded = ref(false)

async function load() {
  try {
    const data = await http.get<{ records: HistoryItem[] }>('/web/approval/tasks/history', {
      page: 1,
      size: 50,
    })
    history.value = data.records
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loaded.value = true
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
.history-card {
  padding: 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(226, 236, 249, 0.9);
  box-shadow: 0 6px 20px rgba(16, 42, 80, 0.06);
}
.history-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.node {
  font-size: 16px;
  font-weight: 600;
  color: #10233f;
}
.time {
  margin-top: 8px;
  color: #61738c;
  font-size: 13px;
}
.comment {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f5f8fd;
  color: #4a5f7a;
  font-size: 13px;
  line-height: 1.6;
}
</style>
