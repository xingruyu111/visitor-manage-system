<template>
  <div class="page">
    <van-nav-bar title="历史审批" left-arrow @click-left="$router.back()" />

    <van-empty v-if="loaded && history.length === 0" description="暂无历史审批" />

    <van-cell-group v-for="item in history" :key="item.taskId" inset class="card">
      <van-cell :title="item.nodeName" :label="item.actedAt">
        <template #value>
          <van-tag :type="item.decision === 'APPROVED' ? 'success' : 'danger'">
            {{ item.decision === 'APPROVED' ? '通过' : '驳回' }}
          </van-tag>
        </template>
      </van-cell>
      <van-cell v-if="item.comment" :title="item.comment" />
    </van-cell-group>
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
  background: #f7f8fa;
  padding-bottom: 24px;
}
.card {
  margin-top: 12px;
}
</style>
