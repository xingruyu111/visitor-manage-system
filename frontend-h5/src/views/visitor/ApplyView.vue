<template>
  <div class="page">
    <van-nav-bar title="访客预约" left-arrow @click-left="$router.back()" />

    <van-notice-bar
      v-if="checkMessage"
      :type="checkStatus === 'BLACKLIST' ? 'error' : 'success'"
      :text="checkMessage"
    />

    <van-cell-group inset class="target-group" title="选择被访人">
      <div class="target-search">
        <van-field
          v-model="keyword"
          placeholder="输入完整姓名或手机号"
          @keyup.enter="search"
        >
          <template #button>
            <van-button size="small" type="primary" @click="search">搜索</van-button>
          </template>
        </van-field>
        <div v-if="showTargets" class="target-dropdown">
          <van-cell
            v-for="item in targets"
            :key="item.id"
            :title="item.name"
            :label="targetMeta(item)"
            clickable
            @click="selectTarget(item)"
          />
          <van-empty v-if="targets.length === 0" description="未找到匹配人员" image-size="60" />
        </div>
      </div>
      <van-cell
        v-if="selectedTarget"
        title="已选被访人"
        :label="`${selectedTarget.name} · ${targetMeta(selectedTarget)}`"
      />
    </van-cell-group>

    <van-cell-group inset title="访客类型">
      <van-field
        v-if="isAlumni"
        label="访客类型"
        :model-value="visitorTypeLabel"
        readonly
      />
      <van-field
        v-else
        label="访客类型"
        :model-value="visitorTypeLabel"
        readonly
        is-link
        @click="showTypePicker = true"
      />
    </van-cell-group>

    <van-cell-group inset title="到访信息">
      <van-field
        readonly
        clickable
        label="到访时间"
        :model-value="displayTime(expectedTime)"
        placeholder="选择到访时间"
        @click="openTimePicker('expectedTime')"
      />
      <van-field
        readonly
        clickable
        label="离校时间"
        :model-value="displayTime(leaveTime)"
        placeholder="选择离校时间"
        @click="openTimePicker('leaveTime')"
      />
      <van-field v-model="purpose" label="事由" placeholder="请输入来访事由" />
      <van-field label="通行方式">
        <template #input>
          <van-radio-group v-model="passMode" direction="horizontal">
            <van-radio name="WALK">步行</van-radio>
            <van-radio name="EBIKE">电动车</van-radio>
            <van-radio name="CAR">机动车</van-radio>
          </van-radio-group>
        </template>
      </van-field>
      <van-field v-if="passMode !== 'WALK'" v-model="plate" label="车牌号" placeholder="机动车必填" />
      <van-field
        v-if="isAffiliatedStudent"
        v-model="headTeacherNo"
        label="班主任工号"
        placeholder="附属学生预约必填"
      />
    </van-cell-group>

    <van-cell-group v-if="fields.length" inset title="补充信息">
      <template v-for="field in fields" :key="field.fieldKey">
        <van-field
          v-if="field.fieldType === 'SELECT'"
          readonly
          clickable
          :label="field.label"
          :model-value="extra[field.fieldKey]"
          :placeholder="field.required === 1 ? '请选择（必填）' : '请选择'"
          @click="openFieldSelect(field)"
        />
        <van-field
          v-else-if="field.fieldType === 'DATE'"
          readonly
          clickable
          :label="field.label"
          :model-value="extra[field.fieldKey]"
          :placeholder="field.required === 1 ? '请选择日期（必填）' : '请选择日期'"
          @click="openFieldDate(field)"
        />
        <van-field
          v-else
          v-model="extra[field.fieldKey]"
          :label="field.label"
          :type="fieldInputType(field)"
          :maxlength="fieldMaxLength(field)"
          :placeholder="field.required === 1 ? '必填' : '选填'"
        />
      </template>
    </van-cell-group>

    <div class="actions">
      <van-button round block type="primary" :loading="submitting" @click="submit">提交申请</van-button>
    </div>

    <van-popup v-model:show="showTypePicker" position="bottom">
      <van-picker
        :columns="visitorTypeOptions"
        @confirm="onTypeConfirm"
        @cancel="showTypePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="timePickerVisible" position="bottom" round>
      <div class="time-picker">
        <div class="time-picker-head">
          <button class="time-cancel" @click="timePickerVisible = false">取消</button>
          <strong>{{ timeTitle }}</strong>
          <button class="time-confirm" @click="confirmTime">确定</button>
        </div>
        <van-tabs v-model:active="timeTab" shrink>
          <van-tab title="日期">
            <van-date-picker
              v-model="dateValues"
              :min-date="minDate"
              :max-date="maxDate"
              :show-toolbar="false"
            />
          </van-tab>
          <van-tab title="时间">
            <van-time-picker v-model="timeValues" :show-toolbar="false" />
          </van-tab>
        </van-tabs>
      </div>
    </van-popup>

    <van-popup v-model:show="fieldPickerVisible" position="bottom" round>
      <van-picker
        :columns="fieldOptions.map((item) => ({ text: item, value: item }))"
        @confirm="onFieldOptionConfirm"
        @cancel="fieldPickerVisible = false"
      />
    </van-popup>

    <van-popup v-model:show="fieldDateVisible" position="bottom" round>
      <div class="time-picker">
        <div class="time-picker-head">
          <button class="time-cancel" @click="fieldDateVisible = false">取消</button>
          <strong>{{ fieldDateTitle }}</strong>
          <button class="time-confirm" @click="confirmFieldDate">确定</button>
        </div>
        <van-date-picker
          v-model="fieldDateValues"
          :min-date="new Date(1900, 0, 1)"
          :max-date="new Date(2100, 11, 31)"
          :show-toolbar="false"
        />
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface FormField {
  fieldKey: string
  label: string
  fieldType?: string
  options?: string
  required?: number
}

interface Staff {
  id: number
  name: string
  phone?: string
  orgName?: string
  personType?: number
}

interface Profile {
  currentRole: number
  personType?: number
  roles?: Array<{ code: number }>
}

interface PickerConfirm {
  selectedOptions: Array<{ text: string; value: string }>
}

const VISITOR_TYPES: Record<string, string> = {
  NORMAL: '普通',
  PARENT: '家长',
  CONSTRUCTION: '施工合作',
  ALUMNI: '校友',
}

const router = useRouter()
const keyword = ref('')
const targets = ref<Staff[]>([])
const targetId = ref<number | null>(null)
const selectedTarget = ref<Staff>()
const showTargets = ref(false)
const expectedTime = ref('')
const leaveTime = ref('')
const purpose = ref('')
const plate = ref('')
const passMode = ref('WALK')
const headTeacherNo = ref('')
const checkStatus = ref('NONE')
const checkMessage = ref('')
const visitorType = ref('NORMAL')
const fields = ref<FormField[]>([])
const extra = reactive<Record<string, string>>({})
const fieldPickerVisible = ref(false)
const fieldDateVisible = ref(false)
const selectedField = ref<FormField>()
const fieldOptions = ref<string[]>([])
const fieldDateValues = ref<string[]>(['2026', '01', '01'])
const fieldDateTitle = computed(() => selectedField.value?.label || '选择日期')
const submitting = ref(false)
const profile = ref<Profile>()
const showTypePicker = ref(false)
const timePickerVisible = ref(false)
const timeTarget = ref<'expectedTime' | 'leaveTime'>('expectedTime')
const timeTab = ref(0)
const dateValues = ref<string[]>([])
const timeValues = ref<string[]>([])
const minDate = new Date()
const maxDate = new Date(Date.now() + 365 * 24 * 3600 * 1000)
const timeTitle = computed(() => (timeTarget.value === 'expectedTime' ? '选择到访时间' : '选择离校时间'))

const isAlumni = computed(() => {
  const current = profile.value
  if (!current) return false
  return current.personType === 7 || current.roles?.some((role) => role.code === 9) === true
})

const isAffiliatedStudent = computed(() => selectedTarget.value?.personType === 6)

const visitorTypeOptions = computed(() => {
  const codes = isAlumni.value ? ['ALUMNI'] : ['NORMAL', 'PARENT', 'CONSTRUCTION']
  return codes.map((code) => ({ text: VISITOR_TYPES[code], value: code }))
})

const visitorTypeLabel = computed(() => VISITOR_TYPES[visitorType.value] ?? '普通')

function localTime(offsetHours: number) {
  const date = new Date(Date.now() + offsetHours * 3600 * 1000)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function backendTime(value: string) {
  if (!value) return ''
  const normalized = value.replace('T', ' ')
  return normalized.length === 16 ? `${normalized}:00` : normalized
}

function displayTime(value: string) {
  return value ? value.replace('T', ' ') : ''
}

function openTimePicker(target: 'expectedTime' | 'leaveTime') {
  timeTarget.value = target
  const raw = target === 'expectedTime' ? expectedTime.value : leaveTime.value
  const date = raw ? new Date(raw) : new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  dateValues.value = [String(date.getFullYear()), pad(date.getMonth() + 1), pad(date.getDate())]
  timeValues.value = [pad(date.getHours()), pad(date.getMinutes())]
  timeTab.value = 0
  timePickerVisible.value = true
}

function confirmTime() {
  const date = dateValues.value.join('-')
  const time = timeValues.value.join(':')
  const value = `${date}T${time}`
  const picked = new Date(value)
  if (Number.isNaN(picked.getTime())) {
    showToast('时间格式不正确')
    return
  }
  if (picked.getTime() < Date.now() - 60 * 1000) {
    showToast('不能选择过去的时间')
    return
  }
  if (timeTarget.value === 'expectedTime') {
    expectedTime.value = value
    if (leaveTime.value && leaveTime.value <= value) {
      leaveTime.value = localTime(3)
    }
  } else {
    if (expectedTime.value && value <= expectedTime.value) {
      showToast('离校时间需晚于到访时间')
      return
    }
    leaveTime.value = value
  }
  timePickerVisible.value = false
}

async function loadProfile() {
  try {
    profile.value = await http.get<Profile>('/auth/me')
    if (isAlumni.value) {
      visitorType.value = 'ALUMNI'
    } else if (!['NORMAL', 'PARENT', 'CONSTRUCTION'].includes(visitorType.value)) {
      visitorType.value = 'NORMAL'
    }
    await loadFields()
  } catch {
    // 获取身份失败时按普通访客处理
  }
}

async function search() {
  if (!keyword.value.trim()) {
    showToast('请输入完整姓名或手机号')
    return
  }
  try {
    targets.value = await http.get<Staff[]>('/web/visitor/targets', {
      keyword: keyword.value.trim(),
    })
    targetId.value = null
    selectedTarget.value = undefined
    showTargets.value = true
    if (targets.value.length === 0) {
      showToast('未找到匹配人员')
    }
  } catch (e) {
    showToast(e instanceof Error ? e.message : '搜索失败')
  }
}

function targetMeta(target: Staff) {
  return [target.phone, target.orgName].filter(Boolean).join(' · ')
}

function selectTarget(target: Staff) {
  targetId.value = target.id
  selectedTarget.value = target
  showTargets.value = false
  if (target.personType !== 6) {
    headTeacherNo.value = ''
  }
}

function onTypeConfirm({ selectedOptions }: PickerConfirm) {
  visitorType.value = selectedOptions[0]?.value ?? 'NORMAL'
  showTypePicker.value = false
  loadFields()
}

async function loadFields() {
  try {
    fields.value = await http.get<FormField[]>('/web/visitor/form-fields', {
      visitorType: visitorType.value,
    })
  } catch {
    fields.value = []
  }
}

async function loadPreCheck() {
  try {
    const data = await http.get<{ status: string; message?: string }>('/web/visitor/records/pre-check', {
      plate: plate.value || undefined,
    })
    checkStatus.value = data.status
    checkMessage.value = data.message ?? ''
  } catch {
    checkStatus.value = 'NONE'
    checkMessage.value = ''
  }
}

function fieldInputType(field: FormField) {
  if (field.fieldType === 'NUMBER') return 'number'
  if (field.fieldType === 'PHONE') return 'tel'
  return 'text'
}

function fieldMaxLength(field: FormField) {
  if (field.fieldType === 'PHONE') return 11
  if (field.fieldType === 'ID_CARD') return 18
  return undefined
}

function openFieldSelect(field: FormField) {
  selectedField.value = field
  fieldOptions.value = (field.options || '')
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
  if (fieldOptions.value.length === 0) {
    showToast('该字段暂无可选项')
    return
  }
  fieldPickerVisible.value = true
}

function onFieldOptionConfirm({ selectedValues }: { selectedValues: string[] }) {
  if (!selectedField.value) return
  extra[selectedField.value.fieldKey] = selectedValues[0] ?? ''
  fieldPickerVisible.value = false
}

function openFieldDate(field: FormField) {
  selectedField.value = field
  const current = extra[field.fieldKey]
  if (current && /^\d{4}-\d{2}-\d{2}$/.test(current)) {
    fieldDateValues.value = current.split('-')
  } else {
    const now = new Date()
    fieldDateValues.value = [
      String(now.getFullYear()),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0'),
    ]
  }
  fieldDateVisible.value = true
}

function confirmFieldDate() {
  if (!selectedField.value) return
  extra[selectedField.value.fieldKey] = fieldDateValues.value.join('-')
  fieldDateVisible.value = false
}

async function submit() {
  if (checkStatus.value === 'BLACKLIST') {
    showToast('您在黑名单中，无法提交预约')
    return
  }
  if (!targetId.value) {
    showToast('请先搜索并选择被访人')
    return
  }
  if (!expectedTime.value || !leaveTime.value) {
    showToast('请选择到访时间和离校时间')
    return
  }
  if (passMode.value === 'CAR' && !plate.value) {
    showToast('机动车通行请填写车牌号')
    return
  }
  if (isAffiliatedStudent.value && !headTeacherNo.value) {
    showToast('附属学生预约请填写班主任工号')
    return
  }
  const missing = fields.value.find((field) => field.required === 1
    && !String(extra[field.fieldKey] ?? '').trim())
  if (missing) {
    showToast(`请填写${missing.label}`)
    return
  }
  submitting.value = true
  try {
    await http.post('/web/visitor/records', {
      targetId: targetId.value,
      visitorType: visitorType.value,
      expectedTime: backendTime(expectedTime.value),
      leaveTime: backendTime(leaveTime.value),
      visitPurpose: purpose.value,
      passMode: passMode.value,
      plateNumber: plate.value || undefined,
      headTeacherNo: isAffiliatedStudent.value ? headTeacherNo.value : undefined,
      extra,
    })
    router.push('/v/records')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  expectedTime.value = localTime(1)
  leaveTime.value = localTime(3)
  loadProfile()
  loadPreCheck()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 96px;
}
.target-group {
  overflow: visible;
}

.target-search {
  position: relative;
  overflow: visible;
}
.target-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 20;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 6px 18px rgba(0, 32, 64, 0.16);
  max-height: 300px;
  overflow-y: auto;
}
.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  margin: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(242, 247, 253, 0), #f2f7fd 38%);
}
.time-picker {
  padding-bottom: env(safe-area-inset-bottom);
}
.time-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #eef3fa;
}
.time-picker-head strong {
  font-size: 16px;
  color: #10233f;
}
.time-cancel,
.time-confirm {
  border: 0;
  background: transparent;
  padding: 6px 2px;
  font-size: 15px;
}
.time-cancel {
  color: #61738c;
}
.time-confirm {
  color: #1677ff;
  font-weight: 600;
}
.time-picker :deep(.van-tabs__wrap) {
  padding: 4px 0;
}
.time-picker :deep(.van-picker__columns) {
  min-height: 220px;
}
</style>
