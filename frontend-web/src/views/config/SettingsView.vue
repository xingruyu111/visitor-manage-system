<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>系统参数</span>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="settingKey" label="参数" width="260" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="值" min-width="240">
          <template #default="{ row }">
            <el-input v-model="row.settingValue" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="save(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface Setting {
  settingKey: string
  settingValue?: string
  description?: string
  updatedAt?: string
}

const rows = ref<Setting[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await http.get<Setting[]>('/web/websuper/settings')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function save(row: Setting) {
  try {
    await http.put(
      `/web/websuper/settings/${encodeURIComponent(row.settingKey)}?value=${encodeURIComponent(row.settingValue ?? '')}`,
    )
    ElMessage.success('已保存')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

onMounted(load)
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
