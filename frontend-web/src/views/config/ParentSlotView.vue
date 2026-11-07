<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-form-item label="家长手机号">
          <el-input v-model="phone" placeholder="精确查询" clearable style="width: 180px" @keyup.enter="load" />
        </el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="openCreate">新增时段</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="visitorPhone" label="家长手机号" width="150" />
        <el-table-column label="星期" width="100">
          <template #default="{ row }">{{ WEEKDAYS[row.weekday] ?? '每天' }}</template>
        </el-table-column>
        <el-table-column label="时段" width="160">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="生效区间" min-width="220">
          <template #default="{ row }">{{ row.effectiveFrom || '不限' }} ~ {{ row.effectiveTo || '不限' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑时段' : '新增时段'" width="460px">
      <el-form label-width="100px">
        <el-form-item label="家长手机号"><el-input v-model="form.visitorPhone" /></el-form-item>
        <el-form-item label="星期">
          <el-select v-model="form.weekday" clearable placeholder="每天" style="width: 100%">
            <el-option v-for="(label, value) in WEEKDAYS" :key="value" :label="label" :value="Number(value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" format="HH:mm" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" format="HH:mm" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效开始">
          <el-date-picker v-model="form.effectiveFrom" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效结束">
          <el-date-picker v-model="form.effectiveTo" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
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

interface Slot {
  id: number
  visitorPhone: string
  weekday?: number
  startTime: string
  endTime: string
  effectiveFrom?: string
  effectiveTo?: string
  status: number
}

const WEEKDAYS: Record<number, string> = {
  1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日',
}

const rows = ref<Slot[]>([])
const phone = ref('')
const loading = ref(false)
const visible = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  visitorPhone: '',
  weekday: undefined as number | undefined,
  startTime: '08:00:00',
  endTime: '10:00:00',
  effectiveFrom: '',
  effectiveTo: '',
  status: 1,
})

async function load() {
  loading.value = true
  try {
    rows.value = await http.get<Slot[]>('/web/admin/parent-slots', {
      visitorPhone: phone.value || undefined,
    })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.id = undefined
  form.visitorPhone = phone.value
  form.weekday = undefined
  form.startTime = '08:00:00'
  form.endTime = '10:00:00'
  form.effectiveFrom = ''
  form.effectiveTo = ''
  form.status = 1
  visible.value = true
}

function openEdit(row: Slot) {
  form.id = row.id
  form.visitorPhone = row.visitorPhone
  form.weekday = row.weekday
  form.startTime = row.startTime
  form.endTime = row.endTime
  form.effectiveFrom = row.effectiveFrom ?? ''
  form.effectiveTo = row.effectiveTo ?? ''
  form.status = row.status
  visible.value = true
}

async function save() {
  try {
    const body = {
      visitorPhone: form.visitorPhone,
      weekday: form.weekday,
      startTime: form.startTime,
      endTime: form.endTime,
      effectiveFrom: form.effectiveFrom || undefined,
      effectiveTo: form.effectiveTo || undefined,
      status: form.status,
    }
    if (form.id) {
      await http.put(`/web/admin/parent-slots/${form.id}`, body)
    } else {
      await http.post('/web/admin/parent-slots', body)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function remove(row: Slot) {
  try {
    await ElMessageBox.confirm(`确定删除 ${row.visitorPhone} 的时段？`, '提示', { type: 'warning' })
    await http.delete(`/web/admin/parent-slots/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch {
    // 取消
  }
}

onMounted(load)
</script>
