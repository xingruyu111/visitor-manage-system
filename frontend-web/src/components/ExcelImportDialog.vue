<template>
  <el-dialog
    :model-value="modelValue"
    :title="title || 'Excel 导入'"
    width="580px"
    destroy-on-close
    @update:model-value="(value: boolean) => emit('update:modelValue', value)"
    @closed="reset"
  >
    <div class="import-template">
      <div>
        <strong>第一步：下载模板</strong>
        <p>{{ description || '请按模板表头填写，不要修改列名。' }}</p>
      </div>
      <a :href="templateUrl" download>
        <el-button>下载模板</el-button>
      </a>
    </div>

    <div class="import-upload-title">第二步：选择文件</div>
    <el-upload
      drag
      :auto-upload="false"
      :show-file-list="false"
      :accept="accept"
      :disabled="uploading"
      :on-change="onFileChange"
    >
      <div class="drop-zone">
        <div class="drop-icon">＋</div>
        <div class="drop-text">将 Excel 拖到此处，或<em>点击选择文件</em></div>
        <div class="drop-hint">支持 {{ accept.split(',').join('、') }}，单个文件不超过 20MB</div>
      </div>
    </el-upload>

    <div v-if="file" class="selected-file">
      <span class="file-name">{{ file.name }}</span>
      <span class="file-size">{{ fileSize }}</span>
      <el-button link type="danger" :disabled="uploading" @click="file = undefined">移除</el-button>
    </div>

    <div v-if="result" class="import-result">
      <div class="result-summary">{{ summary }}</div>
      <el-table v-if="result.errors?.length" :data="result.errors" size="small" border max-height="220">
        <el-table-column prop="row" label="行号" width="80" />
        <el-table-column label="原因">
          <template #default="{ row }">{{ row.reason || row.message || '导入失败' }}</template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button :disabled="uploading" @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" :loading="uploading" :disabled="!file" @click="upload">
        开始导入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

interface ImportError {
  row: number
  reason?: string
  message?: string
}

interface ImportResult {
  total?: number
  success?: number
  failed?: number
  created?: number
  skipped?: number
  errors?: ImportError[]
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  templateUrl: string
  uploadUrl: string
  description?: string
  accept?: string
  successMessage?: string
}>(), {
  title: 'Excel 导入',
  description: '',
  accept: '.xlsx,.xls',
  successMessage: '导入完成',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [result: ImportResult]
}>()

const file = ref<File>()
const uploading = ref(false)
const result = ref<ImportResult>()

const fileSize = computed(() => {
  if (!file.value) return ''
  const size = file.value.size
  return size > 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(size / 1024)} KB`
})

const summary = computed(() => {
  if (!result.value) return ''
  const parts: string[] = []
  if (result.value.total !== undefined) parts.push(`共 ${result.value.total} 条`)
  if (result.value.success !== undefined) parts.push(`成功 ${result.value.success} 条`)
  if (result.value.created !== undefined) parts.push(`新增 ${result.value.created} 条`)
  if (result.value.skipped !== undefined) parts.push(`跳过 ${result.value.skipped} 条`)
  if (result.value.failed !== undefined) parts.push(`失败 ${result.value.failed} 条`)
  return parts.join('，') || '导入完成'
})

function onFileChange(uploadFile: { raw?: File }) {
  const selected = uploadFile.raw
  if (!selected) return
  if (!/\.(xlsx|xls)$/i.test(selected.name)) {
    ElMessage.warning('请选择 .xlsx 或 .xls 文件')
    return
  }
  if (selected.size > 20 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 20MB')
    return
  }
  file.value = selected
  result.value = undefined
}

async function upload() {
  if (!file.value || uploading.value) return
  uploading.value = true
  result.value = undefined
  try {
    const body = new FormData()
    body.append('file', file.value)
    const response = await fetch(props.uploadUrl, {
      method: 'POST',
      headers: { Authorization: `Bearer ${localStorage.getItem('token') ?? ''}` },
      body,
    })
    const payload = await response.json()
    if (payload.code !== 200) {
      throw new Error(payload.message || '导入失败')
    }
    result.value = payload.data as ImportResult
    ElMessage.success(props.successMessage)
    file.value = undefined
    emit('success', result.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    uploading.value = false
  }
}

function reset() {
  file.value = undefined
  result.value = undefined
  uploading.value = false
}
</script>

<style scoped>
.import-template {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
}

.import-template strong {
  color: #111;
  font-size: 14px;
}

.import-template p {
  margin: 4px 0 0;
  color: #6b6b6b;
  font-size: 13px;
  line-height: 1.5;
}

.import-upload-title {
  margin: 18px 0 8px;
  color: #333;
  font-size: 14px;
  font-weight: 500;
}

.drop-zone {
  padding: 22px 12px;
}

.drop-icon {
  width: 42px;
  height: 42px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #555;
  font-size: 26px;
  line-height: 40px;
}

.drop-text {
  color: #333;
  font-size: 14px;
}

.drop-text em {
  color: #0a0a0a;
  font-style: normal;
  font-weight: 600;
}

.drop-hint {
  margin-top: 6px;
  color: #999;
  font-size: 12px;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f7f7f7;
  font-size: 13px;
}

.selected-file .file-name {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #111;
}

.selected-file .file-size {
  color: #999;
}

.import-result {
  margin-top: 14px;
  padding: 12px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
}

.result-summary {
  margin-bottom: 8px;
  color: #111;
  font-size: 14px;
  font-weight: 500;
}
</style>
