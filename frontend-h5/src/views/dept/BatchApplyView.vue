<template>
  <div class="page">
    <van-nav-bar title="部门批量预约" left-arrow @click-left="$router.back()" />

    <van-cell-group inset title="访客名单">
      <van-field
        v-model="text"
        type="textarea"
        rows="8"
        autosize
        placeholder="每行一条：姓名,手机号,开始时间,结束时间,事由&#10;例：张三,13900000001,2026-11-10 09:00:00,2026-11-10 11:00:00,业务洽谈"
      />
    </van-cell-group>

    <p class="hint">提交后进入保卫处审批，审批通过后自动签发通行凭证。</p>

    <div class="actions">
      <van-button block type="primary" :loading="submitting" @click="submit">提交审批</van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'

const router = useRouter()
const text = ref('')
const submitting = ref(false)

async function submit() {
  const items = text.value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [name, phone, expectedTime, leaveTime, visitPurpose] = line.split(',').map((part) => part.trim())
      return { name, phone, expectedTime, leaveTime, visitPurpose }
    })

  if (items.length === 0) {
    showToast('请至少填写一行')
    return
  }

  submitting.value = true
  try {
    const result = await http.post<{ created: number }>('/web/dept/visitor-records/batch', { items })
    showToast(`已提交 ${result.created} 条，等待保卫处审批`)
    text.value = ''
    router.replace('/v/home')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding: 12px 0 24px;
}
.hint {
  margin: 12px 16px;
  color: #969799;
  font-size: 12px;
}
.actions {
  margin: 24px 16px;
}
</style>
