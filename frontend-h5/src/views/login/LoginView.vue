<template>
  <div class="page">
    <h2 class="title">访客登录</h2>
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field v-model="phone" label="手机号" placeholder="请输入手机号" :rules="[{ required: true, message: '请输入手机号' }]" />
        <van-field v-model="password" type="password" label="密码" placeholder="请输入密码" :rules="[{ required: true, message: '请输入密码' }]" />
      </van-cell-group>
      <div class="actions">
        <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'

const router = useRouter()
const phone = ref('')
const password = ref('')
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    const data = await http.post<{ token: string }>('/auth/login', {
      phone: phone.value,
      password: password.value,
    })
    localStorage.setItem('visitor_token', data.token)
    router.replace('/v/home')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  padding: 40px 0;
}
.title {
  text-align: center;
  margin-bottom: 24px;
}
.actions {
  margin: 24px 16px;
}
</style>
