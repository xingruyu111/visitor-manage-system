<template>
  <van-popup
    :show="show"
    position="bottom"
    round
    :style="{ maxHeight: '86vh' }"
    @update:show="(value: boolean) => emit('update:show', value)"
  >
    <div class="detail-sheet">
      <div class="detail-head">
        <strong>{{ title || '预约详情' }}</strong>
        <van-icon name="cross" class="close" @click="emit('update:show', false)" />
      </div>

      <div class="detail-body">
        <div class="status-row">
          <van-tag :type="statusTag" round>{{ statusText }}</van-tag>
          <span v-if="task?.nodeName" class="node">{{ task.nodeName }}</span>
        </div>

        <section class="block">
          <h3>访客信息</h3>
          <van-cell title="访客姓名" :value="record?.visitorName || '—'" />
          <van-cell title="手机号" :value="record?.visitorPhone || '—'" />
          <van-cell title="访客类型" :value="visitorTypeText" />
        </section>

        <section class="block">
          <h3>到访信息</h3>
          <van-cell title="被访人" :value="record?.targetName || '—'" />
          <van-cell title="到访时间" :value="record?.expectedTime || '—'" />
          <van-cell title="离校时间" :value="record?.leaveTime || '—'" />
          <van-cell title="事由" :value="record?.visitPurpose || '—'" />
          <van-cell title="通行方式" :value="passModeText" />
          <van-cell v-if="record?.plateNumber" title="车牌号" :value="record.plateNumber" />
          <van-cell v-if="record?.headTeacherNo" title="班主任工号" :value="record.headTeacherNo" />
        </section>

        <section v-if="extraEntries.length" class="block">
          <h3>补充信息</h3>
          <van-cell
            v-for="entry in extraEntries"
            :key="entry.key"
            :title="entry.label"
            :value="entry.value"
          />
        </section>

        <section v-if="task?.dueAt" class="block">
          <h3>审批信息</h3>
          <van-cell title="当前节点" :value="task?.nodeName || '—'" />
          <van-cell title="截止时间" :value="task?.dueAt || '—'" />
        </section>

        <section v-if="approval" class="block">
          <h3>审批意见</h3>
          <van-cell :title="approval.nodeName || '审批结果'" :label="approval.actedAt || ''">
            <template #value>
              <van-tag :type="approval.decision === 'APPROVED' ? 'success' : 'danger'">
                {{ approval.decision === 'APPROVED' ? '通过' : '驳回' }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell title="意见" :label="approval.comment || '无意见'" />
        </section>

        <section v-if="history && history.length" class="block">
          <h3>审批进度</h3>
          <div v-for="item in history" :key="item.taskId || item.nodeName" class="history-row">
            <div class="history-title">
              <span>{{ item.nodeName }}</span>
              <van-tag
                v-if="item.decision"
                :type="item.decision === 'APPROVED' ? 'success' : 'danger'"
                plain
              >
                {{ item.decision === 'APPROVED' ? '通过' : '驳回' }}
              </van-tag>
              <van-tag v-else type="warning" plain>待处理</van-tag>
            </div>
            <div class="history-meta">
              {{ item.actedAt || '—' }}
              <span v-if="item.comment"> · {{ item.comment }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface VisitRecord {
  id?: number
  visitorName?: string
  visitorPhone?: string
  visitorType?: string
  targetName?: string
  visitPurpose?: string
  expectedTime?: string
  leaveTime?: string
  passMode?: string
  plateNumber?: string
  headTeacherNo?: string
  status?: number
  extraJson?: string
}

export interface TaskInfo {
  nodeName?: string
  dueAt?: string
  status?: string
  decision?: string
  comment?: string
}

export interface HistoryInfo {
  taskId?: number
  nodeName?: string
  status?: string
  decision?: string
  comment?: string
  actedAt?: string
}

export interface ApprovalInfo {
  nodeName?: string
  decision?: string
  comment?: string
  actedAt?: string
}

const props = defineProps<{
  show: boolean
  title?: string
  record?: VisitRecord | null
  task?: TaskInfo | null
  approval?: ApprovalInfo | null
  history?: HistoryInfo[] | null
  fieldLabels?: Record<string, string> | null
}>()

const emit = defineEmits<{ 'update:show': [value: boolean] }>()

const STATUS: Record<number, string> = {
  0: '待审批',
  1: '已通过',
  2: '已拒绝',
  3: '已入校',
  4: '已离校',
  5: '已取消',
  6: '已过期',
}

const statusText = computed(() => STATUS[props.record?.status ?? -1] ?? '—')
const statusTag = computed<'warning' | 'success' | 'danger' | 'primary' | 'default'>(() => {
  const status = props.record?.status
  if (status === 0) return 'warning'
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  if (status === 3) return 'primary'
  return 'default'
})

const visitorTypeText = computed(() => {
  const map: Record<string, string> = {
    NORMAL: '普通访客',
    ALUMNI: '校友',
    PARENT: '学生家长',
    CONSTRUCTION: '施工/合作',
    COOPERATION: '合作访客',
  }
  return map[props.record?.visitorType ?? ''] ?? props.record?.visitorType ?? '—'
})

const passModeText = computed(() => {
  const map: Record<string, string> = { WALK: '步行', EBIKE: '电动车', CAR: '机动车' }
  return map[props.record?.passMode ?? ''] ?? props.record?.passMode ?? '—'
})

const extraEntries = computed(() => {
  if (!props.record?.extraJson) return []
  try {
    const data = JSON.parse(props.record.extraJson) as Record<string, unknown>
    return Object.entries(data)
      .filter(([, value]) => value !== null && value !== undefined && String(value).trim() !== '')
      .map(([key, value]) => ({
        key,
        label: props.fieldLabels?.[key] || key,
        value: String(value),
      }))
  } catch {
    return []
  }
})
</script>

<style scoped>
.detail-sheet {
  display: flex;
  flex-direction: column;
  max-height: 86vh;
}
.detail-head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #eef3fa;
}
.detail-head strong {
  font-size: 17px;
  color: #10233f;
}
.close {
  color: #61738c;
  font-size: 18px;
  padding: 4px;
}
.detail-body {
  overflow-y: auto;
  padding: 12px 16px calc(20px + env(safe-area-inset-bottom));
}
.status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.node {
  color: #61738c;
  font-size: 13px;
}
.block {
  margin-top: 12px;
  border-radius: 14px;
  overflow: hidden;
  background: #f8fbff;
  border: 1px solid #e8f1fb;
}
.block h3 {
  margin: 0;
  padding: 12px 16px 4px;
  color: #61738c;
  font-size: 13px;
  font-weight: 600;
}
.block :deep(.van-cell) {
  background: transparent;
  padding: 10px 16px;
}
.block :deep(.van-cell__value) {
  color: #10233f;
}
.history-row {
  padding: 10px 16px;
  border-top: 1px solid #eef3fa;
}
.history-row:first-of-type {
  border-top: 0;
}
.history-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #10233f;
  font-size: 14px;
}
.history-meta {
  margin-top: 4px;
  color: #8da0b8;
  font-size: 12px;
  line-height: 1.5;
}
</style>
