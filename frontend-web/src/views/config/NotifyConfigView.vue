<template>
  <el-card shadow="never">
    <template #header>消息通知配置</template>

    <el-table v-loading="loading" :data="rows" border>
      <el-table-column label="通知类型" width="220">
        <template #default="{ row }">{{ TYPE[row.notifyType] ?? row.notifyType }}</template>
      </el-table-column>
      <el-table-column label="启用" width="120">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" @change="save(row)" />
        </template>
      </el-table-column>
      <el-table-column label="渠道">
        <template #default="{ row }">
          <el-checkbox-group :model-value="channelsOf(row)" @change="(val: string[]) => onChannels(row, val)">
            <el-checkbox label="SITE">站内</el-checkbox>
            <el-checkbox label="MP">公众号</el-checkbox>
            <el-checkbox label="TODO">统一待办</el-checkbox>
          </el-checkbox-group>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface Row {
  id: number
  notifyType: string
  enabled: number
  channels: string
  updatedAt?: string
}

const TYPE: Record<string, string> = {
  APPROVAL_RESULT: '审批结果通知',
  EXPIRE_REMINDER: '预约到期提醒',
  ACCESS_ALERT: '通行预警',
  BLACKLIST_ALERT: '黑名单拦截告警',
}

const rows = ref<Row[]>([])
const loading = ref(false)
const editing: Record<number, string[]> = {}

function channelsOf(row: Row) {
  if (!editing[row.id]) {
    editing[row.id] = row.channels ? row.channels.split(',').filter(Boolean) : []
  }
  return editing[row.id]
}

async function load() {
  loading.value = true
  try {
    rows.value = await http.get<Row[]>('/web/websuper/notify-config')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function onChannels(row: Row, val: string[]) {
  editing[row.id] = val
  save(row)
}

async function save(row: Row) {
  try {
    const channels = channelsOf(row).join(',')
    await http.put(
      `/web/websuper/notify-config/${row.notifyType}?enabled=${row.enabled}&channels=${encodeURIComponent(channels)}`,
    )
    ElMessage.success('已保存')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

onMounted(load)
</script>
