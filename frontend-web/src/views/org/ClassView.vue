<template>
  <div>
    <el-card shadow="never">
      <el-form inline>
        <el-button type="primary" @click="openCreate">新增班级</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </el-form>

      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="name" label="班级名称" min-width="160" />
        <el-table-column prop="grade" label="年级" width="120" />
        <el-table-column prop="headTeacherName" label="班主任" width="140" />
        <el-table-column prop="counselorName" label="辅导员" width="140" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="form.id ? '编辑班级' : '新增班级'" width="460px">
      <el-form label-width="90px">
        <el-form-item label="班级名称"><el-input v-model="form.name" placeholder="如 计算机2201" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" placeholder="如 2022" /></el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.headTeacherId" clearable filterable style="width: 100%">
            <el-option v-for="item in teachers" :key="item.id" :label="`${item.name}（${item.phone ?? ''}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="辅导员">
          <el-select v-model="form.counselorId" clearable filterable style="width: 100%">
            <el-option v-for="item in teachers" :key="item.id" :label="`${item.name}（${item.phone ?? ''}）`" :value="item.id" />
          </el-select>
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
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface ClassRow {
  id: number
  name: string
  grade?: string
  headTeacherId?: number
  headTeacherName?: string
  counselorId?: number
  counselorName?: string
}

interface Staff {
  id: number
  name: string
  phone?: string
}

const rows = ref<ClassRow[]>([])
const teachers = ref<Staff[]>([])
const loading = ref(false)
const visible = ref(false)
const form = reactive({
  id: undefined as number | undefined,
  name: '',
  grade: '',
  headTeacherId: undefined as number | undefined,
  counselorId: undefined as number | undefined,
})

async function load() {
  loading.value = true
  try {
    rows.value = await http.get<ClassRow[]>('/web/admin/classes')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadTeachers() {
  try {
    const data = await http.get<{ records: Staff[] }>('/web/admin/staff', {
      page: 1,
      size: 100,
      personType: 1,
    })
    teachers.value = data.records
  } catch {
    teachers.value = []
  }
}

function openCreate() {
  form.id = undefined
  form.name = ''
  form.grade = ''
  form.headTeacherId = undefined
  form.counselorId = undefined
  visible.value = true
}

function openEdit(row: ClassRow) {
  form.id = row.id
  form.name = row.name
  form.grade = row.grade ?? ''
  form.headTeacherId = row.headTeacherId
  form.counselorId = row.counselorId
  visible.value = true
}

async function save() {
  try {
    const body = {
      name: form.name,
      grade: form.grade || undefined,
      headTeacherId: form.headTeacherId,
      counselorId: form.counselorId,
    }
    if (form.id) {
      await http.put(`/web/admin/classes/${form.id}`, body)
    } else {
      await http.post('/web/admin/classes', body)
    }
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

onMounted(async () => {
  await Promise.all([load(), loadTeachers()])
})
</script>
