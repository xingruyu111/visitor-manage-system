<template>
  <div class="page">
    <van-nav-bar title="访客预约" left-arrow @click-left="$router.back()" />

    <van-cell-group inset title="选择被访人">
      <van-field v-model="keyword" placeholder="输入姓名或手机号搜索" @keyup.enter="search">
        <template #button>
          <van-button size="small" type="primary" @click="search">搜索</van-button>
        </template>
      </van-field>
      <van-radio-group v-model="targetId">
        <van-cell v-for="item in targets" :key="item.id" :title="item.name" :label="item.phone">
          <template #right-icon>
            <van-radio :name="item.id" />
          </template>
        </van-cell>
      </van-radio-group>
    </van-cell-group>

    <van-cell-group inset title="到访信息">
      <van-field v-model="expectedTime" label="到访时间" placeholder="yyyy-MM-dd HH:mm:ss" />
      <van-field v-model="leaveTime" label="离校时间" placeholder="yyyy-MM-dd HH:mm:ss" />
      <van-field v-model="purpose" label="事由" placeholder="请输入来访事由" />
      <van-field v-model="plate" label="车牌号" placeholder="选填" />
    </van-cell-group>

    <div class="actions">
      <van-button round block type="primary" :loading="submitting" @click="submit">提交申请</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface Staff {
  id: number
  name: string
  phone?: string
}

const router = useRouter()
const keyword = ref('')
const targets = ref<Staff[]>([])
const targetId = ref<number | null>(null)
const expectedTime = ref('')
const leaveTime = ref('')
const purpose = ref('')
const plate = ref('')
const submitting = ref(false)

function defaultTime(offsetHours: number) {
  const date = new Date(Date.now() + offsetHours * 3600 * 1000)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

async function search() {
  try {
    targets.value = await http.get<Staff[]>('/web/visitor/targets', { keyword: keyword.value || undefined })
    if (targets.value.length === 0) {
      showToast('未找到被访人')
    }
  } catch (e) {
    showToast(e instanceof Error ? e.message : '搜索失败')
  }
}

async function submit() {
  if (!targetId.value) {
    showToast('请选择被访人')
    return
  }
  submitting.value = true
  try {
    await http.post('/web/visitor/records', {
      targetId: targetId.value,
      expectedTime: expectedTime.value,
      leaveTime: leaveTime.value,
      visitPurpose: purpose.value,
      plateNumber: plate.value || undefined,
    })
    showToast('提交成功，等待审批')
    router.replace('/v/records')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  expectedTime.value = defaultTime(1)
  leaveTime.value = defaultTime(3)
  search()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 24px;
}
.actions {
  margin: 24px 16px;
}
</style>
