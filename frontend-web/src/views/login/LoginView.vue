<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>访客管理系统</h1>
      <p class="subtitle">Web 管理后台</p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="账号（手机号）">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item v-if="captchaEnabled" label="验证码">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" />
            <img :src="captchaImage" class="captcha-img" alt="验证码" @click="loadCaptcha" />
          </div>
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>

      <p class="tip">演示账号：13800000000 / admin123（系统管理员）</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface LoginResult {
  token: string
  userId: number
  name: string
  currentRole: number
  schoolId: number | null
}

interface CaptchaResult {
  enabled: boolean
  captchaId?: string
  image?: string
}

const route = useRoute()
const router = useRouter()

const form = reactive({ phone: '', password: '', captchaCode: '' })
const loading = ref(false)
const captchaEnabled = ref(false)
const captchaId = ref('')
const captchaImage = ref('')

async function loadCaptcha() {
  try {
    const data = await http.get<CaptchaResult>('/auth/captcha')
    captchaEnabled.value = data.enabled
    captchaId.value = data.captchaId ?? ''
    captchaImage.value = data.image ?? ''
    form.captchaCode = ''
  } catch {
    captchaEnabled.value = false
  }
}

async function handleLogin() {
  if (!form.phone || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (captchaEnabled.value && !form.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const data = await http.post<LoginResult>('/auth/login', {
      phone: form.phone,
      password: form.password,
      captchaId: captchaEnabled.value ? captchaId.value : undefined,
      captchaCode: captchaEnabled.value ? form.captchaCode : undefined,
    })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userName', data.name ?? '')
    localStorage.setItem('roleCode', String(data.currentRole))
    if (data.schoolId != null) {
      localStorage.setItem('schoolId', String(data.schoolId))
    } else {
      localStorage.removeItem('schoolId')
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
    if (captchaEnabled.value) {
      await loadCaptcha()
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  background-image: linear-gradient(#e8e8e8 1px, transparent 1px),
    linear-gradient(90deg, #e8e8e8 1px, transparent 1px);
  background-size: 40px 40px;
}

.login-card {
  width: 400px;
  padding: 20px 16px 12px;
  border: 1px solid #0a0a0a;
  border-radius: 10px;
  box-shadow: 8px 8px 0 #0a0a0a;
}

h1 {
  margin: 4px 0 6px;
  font-size: 22px;
  letter-spacing: 0.5px;
  text-align: center;
  color: #0a0a0a;
}

.subtitle {
  margin: 0 0 24px;
  text-align: center;
  color: #6b6b6b;
  font-size: 13px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.submit {
  width: 100%;
  margin-top: 8px;
}

.tip {
  margin: 16px 0 0;
  font-size: 12px;
  color: #b1b3b8;
  text-align: center;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-img {
  height: 40px;
  width: 120px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
