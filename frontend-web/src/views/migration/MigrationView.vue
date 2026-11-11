<template>
  <div>
    <el-alert
      type="info"
      :closable="false"
      title="历史数据迁移"
      description="从旧系统导出 Excel 后在此导入。逐行校验，失败行会列出行号与原因，可修正后重新导入。"
      class="tip"
    />

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>访客记录</template>
          <p class="desc">列：访客姓名、手机号、证件号、被访人、事由、车牌号、到访时间、离校时间、状态</p>
          <el-button type="primary" @click="openImport('records')">Excel 导入</el-button>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>黑白名单</template>
          <p class="desc">列：姓名、手机号、车牌号、类型（黑名单/白名单）、失效时间、原因</p>
          <el-button type="primary" @click="openImport('access')">Excel 导入</el-button>
        </el-card>
      </el-col>
    </el-row>

    <ExcelImportDialog
      v-model="importVisible"
      :title="importTitle"
      :template-url="importTemplate"
      :upload-url="importUrl"
      :description="importDescription"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import ExcelImportDialog from '@/components/ExcelImportDialog.vue'

const importVisible = ref(false)
const importMode = ref<'records' | 'access'>('records')

const importTitle = computed(() => (importMode.value === 'records' ? 'Excel 导入访客记录' : 'Excel 导入黑白名单'))
const importTemplate = computed(() => (importMode.value === 'records'
  ? '/templates/visitor-record-import.xlsx'
  : '/templates/access-list-import.xlsx'))
const importUrl = computed(() => (importMode.value === 'records'
  ? '/api/web/websuper/migration/visitor-records'
  : '/api/web/websuper/migration/access-list'))
const importDescription = computed(() => (importMode.value === 'records'
  ? '表头：访客姓名、手机号、证件号、被访人、事由、车牌号、到访时间、离校时间、状态。'
  : '表头：姓名、手机号、车牌号、类型、生效时间、失效时间、原因。'))

function openImport(mode: 'records' | 'access') {
  importMode.value = mode
  importVisible.value = true
}
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.desc {
  margin: 0 0 16px;
  color: #6b6b6b;
  font-size: 13px;
  line-height: 1.7;
}
</style>
