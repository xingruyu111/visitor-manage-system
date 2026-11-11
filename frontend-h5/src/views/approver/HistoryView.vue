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
        <div class="history-actions">
          <van-button size="small" round plain @click="openDetail(item)">查看详情</van-button>
        </div>
      </div>
    </div>

    <VisitDetailSheet
      v-model:show="detailVisible"
      title="审批详情"
      :record="detailRecord"
      :task="detailTask"
      :history="detailHistory"
      :field-labels="detailLabels"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { http } from '@/api/request'
import VisitDetailSheet, {
  type HistoryInfo,
  type TaskInfo,
  type VisitRecord,
} from '@/components/VisitDetailSheet.vue'

interface HistoryItem {
  taskId: number
  nodeName: string
  decision: string
  comment?: string
  actedAt: string
}

const history = ref<HistoryItem[]>([])
const loaded = ref(false)
const detailVisible = ref(false)
const detailRecord = ref<VisitRecord | null>(null)
const detailTask = ref<TaskInfo | null>(null)
const detailHistory = ref<HistoryInfo[]>([])
const detailLabels = ref<Record<string, string>>({})

async function loadFieldLabels(visitorType?: string): Promise<Record<string, string>> {
  try {
    const fields = await http.get<Array<{ fieldKey: string; label: string }>>('/web/visitor/form-fields', {
      visitorType: visitorType || 'NORMAL',
    })
    return Object.fromEntries(fields.map((field) => [field.fieldKey, field.label]))
  } catch {
    return {}
  }
}

async function openDetail(item: HistoryItem) {
  try {
    const data = await http.get<{
      task: TaskInfo
      record: VisitRecord | null
      history: HistoryInfo[]
    }>(`/web/approval/tasks/${item.taskId}/detail`)
    detailTask.value = data.task
    detailRecord.value = data.record
    detailLabels.value = await loadFieldLabels(data.record?.visitorType)
    detailHistory.value = data.history ?? []
    detailVisible.value = true
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载详情失败')
  }
}

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
.history-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eff4fb;
}
</style>
