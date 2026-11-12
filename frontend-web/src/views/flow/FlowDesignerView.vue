<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="header">
          <span>审批流程</span>
          <el-button type="primary" @click="openCreate">新建流程</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="flowCode" label="流程编码" width="200" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="适用被访人类型" min-width="180">
          <template #default="{ row }">{{ targetTypesText(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ STATUS[row.status] ?? row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" :disabled="row.status === 1" @click="publish(row)">发布</el-button>
            <el-button link type="primary" :disabled="row.status !== 1" @click="offline(row)">停用</el-button>
            <el-button link type="primary" @click="openVersions(row)">历史版本</el-button>
            <el-button link type="primary" @click="openPreview(row)">预览</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" :title="creating ? '新建流程' : `编辑：${form.code}`" width="760px">
      <el-form label-width="90px">
        <el-form-item label="流程编码">
          <el-input v-model="form.code" :disabled="!creating" placeholder="如 VISIT_DEFAULT" />
        </el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="超时(小时)"><el-input-number v-model="form.timeoutHours" :min="1" :max="240" /></el-form-item>
        <el-form-item label="适用被访人类型">
          <el-select v-model="form.targetTypes" multiple collapse-tags style="width: 100%" placeholder="不选表示未关联，按审批规则兜底">
            <el-option v-for="item in TARGET_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <div class="hint">一个被访人类型只能关联一条已发布流程；不选则按原审批规则兜底。</div>
        </el-form-item>
        <el-form-item label="审批节点">
          <el-table :data="form.nodes" border size="small" style="width: 100%">
            <el-table-column label="序号" width="80">
              <template #default="{ row }"><el-input-number v-model="row.seq" :min="1" size="small" /></template>
            </el-table-column>
            <el-table-column label="节点名称">
              <template #default="{ row }"><el-input v-model="row.name" size="small" /></template>
            </el-table-column>
            <el-table-column label="审批人" width="180">
              <template #default="{ row }">
                <el-select v-model="row.assignee" size="small">
                  <el-option v-for="item in ASSIGNEES" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ $index }">
                <el-button link type="danger" :disabled="form.nodes.length === 1" @click="removeNode($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="add-node" size="small" @click="addNode">添加节点</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" @click="saveAndPublish">保存并发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="versionsVisible" title="历史版本" width="820px">
      <el-table :data="versions" border size="small">
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column label="适用被访人类型" min-width="160">
          <template #default="{ row }">{{ targetTypesText(row) }}</template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success" size="small">当前使用</el-tag>
            <span v-else>{{ STATUS[row.status] }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="165" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewVersion(row)">查看详情</el-button>
            <el-button link type="primary" :disabled="row.status === 1" @click="publishVersion(row)">发布</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="hint">发布历史版本后，当前正在使用的版本会自动停用。</p>
    </el-dialog>

    <el-dialog v-model="versionDetailVisible" title="历史版本详情" width="680px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="流程编码">{{ versionDetail?.flowCode }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ versionDetail?.version }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ versionDetail?.name }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ STATUS[versionDetail?.status ?? -1] ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="适用被访人类型">
          {{ versionDetail ? targetTypesText(versionDetail) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="超时(小时)">{{ versionDefinition.timeoutHours ?? '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-title">审批节点</div>
      <el-table :data="versionDefinition.nodes ?? []" border size="small">
        <el-table-column prop="seq" label="序号" width="70" />
        <el-table-column prop="name" label="节点名称" />
        <el-table-column label="审批人" width="140">
          <template #default="{ row }">{{ assigneeLabel(row.assignee) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="预览预计审批人" width="520px">
      <el-table :data="preview" border size="small">
        <el-table-column prop="seq" label="序号" width="70" />
        <el-table-column prop="name" label="节点" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ assigneeLabel(row.assigneeType) }}</template>
        </el-table-column>
        <el-table-column label="预计审批人" width="140">
          <template #default="{ row }">{{ row.assigneeName || '管理员处理' }}</template>
        </el-table-column>
      </el-table>
      <p class="hint">预览需要被访人 ID（staffId），可在人员列表获取。</p>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/request'

interface Flow {
  id: number
  flowCode: string
  name: string
  version: number
  status: number
  definitionJson: string
  updatedAt?: string
  publishedAt?: string
}

interface Node {
  seq: number
  name: string
  assignee: string
  optional?: boolean
}

const STATUS: Record<number, string> = { 0: '草稿', 1: '已发布', 2: '已停用' }
const STATUS_TAG: Record<number, 'success' | 'info' | 'warning'> = { 0: 'warning', 1: 'success', 2: 'info' }
const ASSIGNEES = [
  { value: 'TARGET', label: '被访人' },
  { value: 'COUNSELOR', label: '辅导员' },
  { value: 'HEAD_TEACHER', label: '班主任' },
  { value: 'SECURITY', label: '保卫处' },
  { value: 'DEFAULT', label: '默认审核人' },
]
const TARGET_TYPES = [
  { value: 'TEACHER', label: '教职工' },
  { value: 'STUDENT', label: '学生' },
  { value: 'AFFILIATED_TEACHER', label: '附属学校教职工' },
  { value: 'AFFILIATED_STUDENT', label: '附属学校学生' },
  { value: 'MERCHANT', label: '校内商户' },
  { value: 'CANTEEN', label: '食堂人员' },
  { value: 'ALUMNI', label: '校友' },
]

const rows = ref<Flow[]>([])
const loading = ref(false)
const editVisible = ref(false)
const creating = ref(false)
const versionsVisible = ref(false)
const versionDetailVisible = ref(false)
const versionDetail = ref<Flow>()
const versionDefinition = ref<{ timeoutHours?: number; targetTypes?: string[]; nodes?: Node[] }>({})
const previewVisible = ref(false)
const versions = ref<Flow[]>([])
const preview = ref<Array<{ seq: number; name: string; assigneeType: string; assigneeName?: string }>>([])
const form = reactive({ code: '', name: '', timeoutHours: 24, targetTypes: [] as string[], nodes: [] as Node[] })

async function load() {
  loading.value = true
  try {
    rows.value = await http.get<Flow[]>('/web/websuper/approval-flows')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  creating.value = true
  form.code = ''
  form.name = ''
  form.timeoutHours = 24
  form.targetTypes = []
  form.nodes = [{ seq: 1, name: '被访人审批', assignee: 'TARGET' }]
  editVisible.value = true
}

function openEdit(row: Flow) {
  creating.value = false
  form.code = row.flowCode
  form.name = row.name
  let definition: { timeoutHours?: number; targetTypes?: string[]; nodes?: Node[] } = {}
  try {
    definition = JSON.parse(row.definitionJson)
  } catch {
    definition = {}
  }
  form.timeoutHours = definition.timeoutHours ?? 24
  form.targetTypes = definition.targetTypes ?? []
  form.nodes = (definition.nodes ?? []).map((node) => ({ ...node }))
  editVisible.value = true
}

function addNode() {
  const next = form.nodes.length === 0 ? 1 : Math.max(...form.nodes.map((n) => n.seq)) + 1
  form.nodes.push({ seq: next, name: '审批节点', assignee: 'TARGET' })
}

function removeNode(index: number) {
  form.nodes.splice(index, 1)
}

function assigneeLabel(value: string) {
  return ASSIGNEES.find((item) => item.value === value)?.label || value
}

function parseDefinition(row: Flow): { timeoutHours?: number; targetTypes?: string[]; nodes?: Node[] } {
  try {
    return JSON.parse(row.definitionJson)
  } catch {
    return {}
  }
}

function targetTypesText(row: Flow) {
  const definition = parseDefinition(row)
  const values = definition.targetTypes ?? []
  if (values.length === 0) return '未关联'
  return values.map((value) => TARGET_TYPES.find((item) => item.value === value)?.label || value).join('、')
}

function definition() {
  return {
    code: form.code,
    name: form.name,
    timeoutHours: form.timeoutHours,
    targetTypes: form.targetTypes,
    nodes: form.nodes.map((node) => ({
      seq: node.seq,
      name: node.name,
      assignee: node.assignee,
      optional: false,
    })),
  }
}

async function saveDraft() {
  if (!form.code) {
    ElMessage.warning('请填写流程编码')
    return
  }
  await http.put(`/web/websuper/approval-flows/${form.code}/draft`, definition())
  ElMessage.success('草稿已保存')
  editVisible.value = false
  await load()
}

async function publishFlow(code: string) {
  const errors = await http.post<string[]>(`/web/websuper/approval-flows/${code}/validate`)
  if (errors.length > 0) {
    await ElMessageBox.alert(errors.join('\n'), '校验未通过', { type: 'warning' })
    return false
  }
  await http.post(`/web/websuper/approval-flows/${code}/publish`)
  ElMessage.success('已发布')
  return true
}

async function saveAndPublish() {
  try {
    await saveDraft()
    await publishFlow(form.code)
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发布失败')
  }
}

async function publish(row: Flow) {
  try {
    if (await publishFlow(row.flowCode)) {
      await load()
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '发布失败')
  }
}

async function offline(row: Flow) {
  try {
    await http.post(`/web/websuper/approval-flows/${row.flowCode}/offline`)
    ElMessage.success('已停用')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '停用失败')
  }
}

async function loadVersions(code: string) {
  versions.value = await http.get<Flow[]>(`/web/websuper/approval-flows/${code}/versions`)
}

async function openVersions(row: Flow) {
  await loadVersions(row.flowCode)
  versionsVisible.value = true
}

async function viewVersion(row: Flow) {
  versionDetail.value = await http.get<Flow>(
    `/web/websuper/approval-flows/${row.flowCode}?version=${row.version}`,
  )
  versionDefinition.value = parseDefinition(versionDetail.value)
  versionDetailVisible.value = true
}

async function publishVersion(row: Flow) {
  try {
    await ElMessageBox.confirm(
      `发布版本 v${row.version} 后，当前正在使用的版本会自动停用，确定发布？`,
      '发布历史版本',
      { type: 'warning' },
    )
    await http.post(`/web/websuper/approval-flows/${row.flowCode}/versions/${row.version}/publish`)
    ElMessage.success(`版本 v${row.version} 已发布`)
    await Promise.all([load(), loadVersions(row.flowCode)])
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e instanceof Error ? e.message : '发布失败')
    }
  }
}

async function openPreview(row: Flow) {
  try {
    const { value } = await ElMessageBox.prompt('请输入被访人 staffId（用于解析审批人）', '预览', {
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入数字 ID',
    })
    preview.value = await http.post(
      `/web/websuper/approval-flows/${row.flowCode}/preview?targetId=${value}`,
    )
    previewVisible.value = true
  } catch {
    // 取消
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
.add-node {
  margin-top: 8px;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.detail-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
