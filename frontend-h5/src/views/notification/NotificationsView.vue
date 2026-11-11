<template>
  <div class="page">
    <van-nav-bar title="站内通知" left-arrow @click-left="$router.back()">
      <template #right>
        <span class="nav-link" @click="readAll">全部已读</span>
      </template>
    </van-nav-bar>

    <van-loading v-if="!loaded" class="loading" />

    <van-empty v-else-if="records.length === 0" description="暂无通知" />

    <div v-else class="list">
      <div
        v-for="item in records"
        :key="item.id"
        class="notice-card"
        :class="{ unread: item.isRead === 0 }"
        @click="open(item)"
      >
        <div class="notice-head">
          <span class="dot" v-if="item.isRead === 0" />
          <span class="notice-title">{{ item.title }}</span>
          <van-tag :type="tagType(item.type)" plain>{{ typeLabel(item.type) }}</van-tag>
        </div>
        <p class="notice-content">{{ item.content }}</p>
        <div class="notice-time">{{ item.createdAt }}</div>
      </div>
    </div>

    <div v-if="records.length > 0" class="read-tip">点击通知可标记为已读</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface Notice {
  id: number
  title: string
  content: string
  type?: string
  isRead: number
  bizType?: string
  bizId?: number
  createdAt: string
}

const router = useRouter()
const records = ref<Notice[]>([])
const loaded = ref(false)

async function load() {
  try {
    const data = await http.get<{ records: Notice[] }>('/web/notifications', { page: 1, size: 50 })
    records.value = data.records
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loaded.value = true
  }
}

function typeLabel(type?: string) {
  if (type === 'APPROVAL' || type === 'APPROVAL_RESULT') return '审批'
  if (type === 'ACCESS_ALERT') return '告警'
  return '系统'
}

function tagType(type?: string): 'primary' | 'warning' | 'default' {
  if (type === 'APPROVAL' || type === 'APPROVAL_RESULT') return 'primary'
  if (type === 'ACCESS_ALERT') return 'warning'
  return 'default'
}

async function open(item: Notice) {
  if (item.isRead === 0) {
    try {
      await http.post(`/web/notifications/${item.id}/read`)
      item.isRead = 1
    } catch (e) {
      showToast(e instanceof Error ? e.message : '操作失败')
    }
  }
  if (item.bizType === 'APPROVAL_TASK' && item.bizId) {
    router.push({ path: '/a/tasks', query: { taskId: String(item.bizId) } })
  } else if (item.title.includes('新的访客申请')) {
    router.push('/a/tasks')
  } else if (item.bizType === 'VISITOR_RECORD' && item.bizId) {
    router.push({ path: '/v/records', query: { recordId: String(item.bizId) } })
  }
}

async function readAll() {
  try {
    await http.post('/web/notifications/read-all')
    records.value.forEach((item) => {
      item.isRead = 1
    })
    showToast('已全部标记为已读')
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
.notice-card {
  padding: 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid rgba(226, 236, 249, 0.9);
  box-shadow: 0 6px 20px rgba(16, 42, 80, 0.06);
  cursor: pointer;
}
.notice-card.unread {
  border-left: 3px solid #1677ff;
}
.notice-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f04438;
  flex: 0 0 auto;
}
.notice-title {
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  color: #10233f;
}
.notice-content {
  margin: 8px 0 0;
  color: #4a5f7a;
  font-size: 13px;
  line-height: 1.65;
}
.notice-time {
  margin-top: 10px;
  color: #8da0b8;
  font-size: 12px;
}
.nav-link {
  color: #14508f;
  font-size: 14px;
  font-weight: 500;
}
.read-tip {
  margin-top: 8px;
  text-align: center;
  color: #8da0b8;
  font-size: 12px;
}
</style>
