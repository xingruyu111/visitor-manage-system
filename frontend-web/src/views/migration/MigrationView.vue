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
          <el-upload :show-file-list="false" accept=".xlsx,.xls" :http-request="uploadRecords">
            <el-button type="primary" :loading="recordsLoading">选择文件并导入</el-button>
          </el-upload>
          <div v-if="recordsResult" class="result">
            <p>成功 {{ recordsResult.success }} 条，失败 {{ recordsResult.failed }} 条</p>
            <el-table v-if="recordsResult.errors?.length" :data="recordsResult.errors" size="small" border>
              <el-table-column prop="row" label="行号" width="80" />
              <el-table-column prop="reason" label="原因" />
            </el-table>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>黑白名单</template>
          <p class="desc">列：姓名、手机号、车牌号、类型（黑名单/白名单）、失效时间、原因</p>
          <el-upload :show-file-list="false" accept=".xlsx,.xls" :http-request="uploadAccessList">
            <el-button type="primary" :loading="accessLoading">选择文件并导入</el-button>
          </el-upload>
          <div v-if="accessResult" class="result">
            <p>成功 {{ accessResult.success }} 条，失败 {{ accessResult.failed }} 条</p>
            <el-table v-if="accessResult.errors?.length" :data="accessResult.errors" size="small" border>
              <el-table-column prop="row" label="行号" width="80" />
              <el-table-column prop="reason" label="原因" />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

interface ImportResult {
  success: number
  failed: number
  errors?: Array<{ row: number; reason: string }>
}

const recordsLoading = ref(false)
const accessLoading = ref(false)
const recordsResult = ref<ImportResult>()
const accessResult = ref<ImportResult>()

async function upload(path: string, file: File): Promise<ImportResult> {
  const body = new FormData()
  body.append('file', file)
  const response = await fetch(`/api${path}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
    body,
  })
  const result = await response.json()
  if (result.code !== 200) {
    throw new Error(result.message ?? '导入失败')
  }
  return result.data
}

async function uploadRecords(options: { file: File }) {
  recordsLoading.value = true
  try {
    recordsResult.value = await upload('/web/websuper/migration/visitor-records', options.file)
    ElMessage.success('导入完成')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  } finally {
    recordsLoading.value = false
  }
}

async function uploadAccessList(options: { file: File }) {
  accessLoading.value = true
  try {
    accessResult.value = await upload('/web/websuper/migration/access-list', options.file)
    ElMessage.success('导入完成')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导入失败')
  } finally {
    accessLoading.value = false
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 16px;
}
.desc {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
.result {
  margin-top: 12px;
  font-size: 13px;
}
</style>
