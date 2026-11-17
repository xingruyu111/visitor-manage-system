<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span>学校接口配置</span>
      </template>
      <p class="tip">所有接口留空即自动降级，不影响主流程；密钥保存后只回显是否已配置。</p>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="description" label="接口" min-width="160" />
        <el-table-column prop="integrationKey" label="标识" width="150" />
        <el-table-column prop="protocol" label="协议" width="100" />
        <el-table-column label="启用" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column label="密钥" width="90">
          <template #default="{ row }">
            <el-tag :type="row.secretConfigured ? 'success' : 'info'">{{ row.secretConfigured ? '已配置' : '未配置' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">配置</el-button>
            <el-button link @click="test(row)">测试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="`配置：${current?.description ?? ''}`" width="640px">
      <el-form label-width="100px">
        <el-form-item label="协议">
          <el-select v-model="form.protocol" style="width: 100%">
            <el-option label="不启用（降级）" value="NONE" />
            <el-option label="Mock（联调）" value="MOCK" />
            <el-option label="REST" value="REST" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="接口地址"><el-input v-model="form.baseUrl" placeholder="https://..." /></el-form-item>
        <el-form-item label="认证方式">
          <el-select v-model="form.authType" style="width: 100%">
            <el-option label="无" value="NONE" />
            <el-option label="AppId/Secret" value="APP_SECRET" />
            <el-option label="Token" value="TOKEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="AppId"><el-input v-model="form.appId" /></el-form-item>
        <el-form-item label="AppSecret">
          <el-input v-model="form.appSecret" placeholder="留空表示不修改" show-password />
        </el-form-item>
        <el-form-item label="端点配置">
          <el-input v-model="form.endpoints" type="textarea" :rows="3" placeholder="JSON，如 {&quot;login&quot;:&quot;/sso/login&quot;}" />
        </el-form-item>
        <el-form-item label="超时(ms)">
          <el-input-number v-model="form.timeoutMs" :min="100" :max="60000" />
        </el-form-item>
        <el-form-item label="重试次数">
          <el-input-number v-model="form.retryTimes" :min="0" :max="5" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface Config {
  integrationKey: string
  description: string
  protocol: string
  enabled: number
  baseUrl?: string
  authType?: string
  appId?: string
  appSecret?: string
  secretConfigured?: boolean
  endpoints?: string
  timeoutMs?: number
  retryTimes?: number
  status?: number
  remark?: string
  updatedAt?: string
}

const rows = ref<Config[]>([])
const loading = ref(false)
const visible = ref(false)
const current = ref<Config>()
const form = reactive({
  protocol: 'NONE',
  enabled: false,
  baseUrl: '',
  authType: 'NONE',
  appId: '',
  appSecret: '',
  endpoints: '',
  timeoutMs: 5000,
  retryTimes: 0,
  remark: '',
})

async function load() {
  loading.value = true
  try {
    rows.value = await http.get<Config[]>('/web/websuper/integration-configs')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openEdit(row: Config) {
  current.value = row
  form.protocol = row.protocol ?? 'NONE'
  form.enabled = row.enabled === 1
  form.baseUrl = row.baseUrl ?? ''
  form.authType = row.authType ?? 'NONE'
  form.appId = row.appId ?? ''
  form.appSecret = ''
  form.endpoints = row.endpoints ?? ''
  form.timeoutMs = row.timeoutMs ?? 5000
  form.retryTimes = row.retryTimes ?? 0
  form.remark = row.remark ?? ''
  visible.value = true
}

async function save() {
  if (!current.value) return
  try {
    await http.put(`/web/websuper/integration-configs/${current.value.integrationKey}`, {
      protocol: form.protocol,
      enabled: form.enabled,
      baseUrl: form.baseUrl || undefined,
      authType: form.authType,
      appId: form.appId || undefined,
      appSecret: form.appSecret || undefined,
      endpoints: form.endpoints || undefined,
      timeoutMs: form.timeoutMs,
      retryTimes: form.retryTimes,
      remark: form.remark || undefined,
    })
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function test(row: Config) {
  try {
    const result = await http.post<Record<string, unknown>>(
      `/web/websuper/integration-configs/${row.integrationKey}/test`,
    )
    await ElMessageBox.alert(JSON.stringify(result, null, 2), '连通性测试', { type: 'info' })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '测试失败')
  }
}

onMounted(load)
</script>

<style scoped>
.tip {
  color: #909399;
  font-size: 12px;
  margin: 0 0 12px;
}
</style>
