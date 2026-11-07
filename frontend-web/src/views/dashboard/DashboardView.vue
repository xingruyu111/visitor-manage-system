<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :span="6">
        <el-card shadow="hover" class="stat">
          <div class="label">{{ card.label }}</div>
          <div class="value">{{ data[card.key] ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="detail">
      <template #header>
        <div class="header">
          <span>业务概览</span>
          <el-button size="small" :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="累计预约">{{ data.total ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="待审批">{{ data.pending ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已通过">{{ data.approved ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已拒绝">{{ data.rejected ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已入校">{{ data.inSchool ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已离校">{{ data.left ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已过期">{{ data.expired ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="待办审批任务">{{ data.pendingTasks ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="有效黑名单">{{ data.activeBlacklist ?? 0 }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

const cards = [
  { key: 'today', label: '今日预约' },
  { key: 'pending', label: '待审批' },
  { key: 'inSchool', label: '当前在校' },
  { key: 'pendingTasks', label: '待办任务' },
]

const data = ref<Record<string, number>>({})
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    data.value = await http.get<Record<string, number>>('/web/admin/dashboard/summary')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.stat {
  text-align: center;
}
.label {
  color: #909399;
  font-size: 13px;
}
.value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}
.detail {
  margin-top: 16px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
