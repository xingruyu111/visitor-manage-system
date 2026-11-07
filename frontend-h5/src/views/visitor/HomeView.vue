<template>
  <div class="page">
    <van-nav-bar title="访客入口" />

    <div class="content">
      <div class="hero">
        <h2>访客预约</h2>
        <p>H5 骨架已就绪，后续在此接入微信授权、差异化预约表单与通行码。</p>
      </div>

      <van-cell-group inset>
        <van-cell title="预约申请" is-link />
        <van-cell title="我的预约" is-link />
        <van-cell title="常用信息" is-link />
      </van-cell-group>

      <div class="actions">
        <van-button type="primary" block :loading="loading" @click="checkHealth">
          测试后端连通
        </van-button>
        <p v-if="message" class="result">{{ message }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getHealth } from '@/api/modules/health'

const loading = ref(false)
const message = ref('')

async function checkHealth() {
  loading.value = true
  try {
    const info = await getHealth()
    message.value = `后端状态：${info.status}（${info.service}）`
  } catch {
    message.value = '调用失败，请确认后端已启动：http://localhost:8080/api'
  } finally {
    loading.value = false
  }
}
</script>
