<template>
  <div class="page">
    <van-nav-bar title="批量预约" left-arrow @click-left="$router.back()" />

    <van-cell-group inset title="访客名单">
      <van-field
        v-model="text"
        type="textarea"
        rows="8"
        autosize
        placeholder="每行一条：姓名,手机号,开始时间,结束时间,事由&#10;例：张三,13900000001,2026-11-10 09:00:00,2026-11-10 11:00:00,业务洽谈"
      />
    </van-cell-group>

    <div class="hint-card">
      <van-icon name="info-o" class="hint-icon" />
      <div>
        <strong>批量预约说明</strong>
        <p>每行一条访客信息，提交后进入保卫处审批，通过后自动签发通行凭证。</p>
      </div>
    </div>

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
  background: transparent;
  padding: 12px 0 96px;
}
.hint-card {
  display: flex;
  gap: 10px;
  margin: 12px 16px 0;
  padding: 14px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #e2ecf9;
  color: #4a5f7a;
  font-size: 13px;
  line-height: 1.6;
  box-shadow: 0 4px 14px rgba(16, 42, 80, 0.04);
}
.hint-icon {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #1677ff;
  font-size: 17px;
}
.hint-card strong {
  display: block;
  margin-bottom: 2px;
  color: #10233f;
  font-size: 14px;
}
.hint-card p {
  margin: 0;
  color: #61738c;
}
.actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(242, 247, 253, 0), #f2f7fd 38%);
}
</style>
