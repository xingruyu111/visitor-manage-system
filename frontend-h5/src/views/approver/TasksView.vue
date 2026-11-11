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
          <van-button size="small" round plain @click="openDetail(item.id)">详情</van-button>
          <van-button size="small" round type="primary" @click="openComment(item, true)">通过</van-button>
          <van-button size="small" round plain type="danger" @click="openComment(item, false)">驳回</van-button>
        </div>
      </div>
    </div>

    <van-popup v-model:show="commentVisible" position="bottom" round>
      <div class="comment-sheet">
        <div class="comment-head">
          <button class="comment-cancel" @click="commentVisible = false">取消</button>
          <strong>{{ commentApprove ? '通过审批' : '驳回审批' }}</strong>
          <button class="comment-submit" :disabled="commentSubmitting" @click="submitComment">
            {{ commentSubmitting ? '提交中' : '提交' }}
          </button>
        </div>
        <van-field
          v-model="comment"
          type="textarea"
          rows="4"
          maxlength="200"
          show-word-limit
          :placeholder="commentApprove ? '请填写审批意见（选填）' : '请填写驳回原因'"
        />
      </div>
    </van-popup>

    <VisitDetailSheet
      v-model:show="detailVisible"
      title="审批详情"
      :record="detailRecord"
      :task="detailTask"
      :history="detailHistory"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'
import VisitDetailSheet, {
  type HistoryInfo,
  type TaskInfo,
  type VisitRecord,
} from '@/components/VisitDetailSheet.vue'

interface Task {
  id: number
  nodeName: string
  dueAt: string
}

const route = useRoute()
const tasks = ref<Task[]>([])
const loaded = ref(false)
const commentVisible = ref(false)
const comment = ref('')
const commentApprove = ref(true)
const commentSubmitting = ref(false)
const currentTask = ref<Task>()
const detailVisible = ref(false)
const detailRecord = ref<VisitRecord | null>(null)
const detailTask = ref<TaskInfo | null>(null)
const detailHistory = ref<HistoryInfo[]>([])

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

async function openDetail(taskId: number) {
  try {
    const data = await http.get<{
      task: TaskInfo
      record: VisitRecord | null
      history: HistoryInfo[]
    }>(`/web/approval/tasks/${taskId}/detail`)
    detailTask.value = data.task
    detailRecord.value = data.record
    detailHistory.value = data.history ?? []
    detailVisible.value = true
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载详情失败')
  }
}

function openComment(task: Task, approve: boolean) {
  currentTask.value = task
  commentApprove.value = approve
  comment.value = ''
  commentVisible.value = true
}

async function submitComment() {
  const task = currentTask.value
  if (!task) return
  if (!commentApprove.value && !comment.value.trim()) {
    showToast('驳回请填写审批意见')
    return
  }
  const action = commentApprove.value ? 'approve' : 'reject'
  commentSubmitting.value = true
  try {
    await http.post(`/web/approval/tasks/${task.id}/${action}?comment=${encodeURIComponent(comment.value.trim())}`)
    showToast(commentApprove.value ? '已通过' : '已驳回')
    commentVisible.value = false
    await load()
  } catch (e) {
    showToast(e instanceof Error ? e.message : '操作失败')
  } finally {
    commentSubmitting.value = false
  }
}

onMounted(async () => {
  await load()
  const taskId = Number(route.query.taskId)
  if (taskId) {
    await openDetail(taskId)
  }
})
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
.comment-sheet {
  padding-bottom: env(safe-area-inset-bottom);
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eef3fa;
}

.comment-head strong {
  font-size: 16px;
  color: #10233f;
}

.comment-cancel,
.comment-submit {
  border: 0;
  background: transparent;
  padding: 6px 2px;
  font-size: 15px;
}

.comment-cancel {
  color: #61738c;
}

.comment-submit {
  color: #1677ff;
  font-weight: 600;
}

.nav-link {
  color: #14508f;
  font-size: 14px;
  font-weight: 500;
}
</style>
