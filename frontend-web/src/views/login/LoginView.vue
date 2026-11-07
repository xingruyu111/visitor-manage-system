<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>智慧校园访客管理系统</h1>
      <p class="subtitle">Web 管理后台</p>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="账号（手机号）">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>

      <p class="tip">演示账号：13800000000 / admin123（系统管理员）</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/api/request'

interface RoleItem {
  code: number
  name: string
}

interface LoginResult {
  token: string
  userId: number
  name: string
  currentRole: number
  schoolId: number | null
  roles: RoleItem[]
}

const route = useRoute()
const router = useRouter()

const form = reactive({ phone: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.phone || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await http.post<LoginResult>('/auth/login', {
      phone: form.phone,
      password: form.password,
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
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef4ff 0%, #f7f9fc 100%);
}

.login-card {
  width: 380px;
  padding: 8px 12px;
}

h1 {
  margin: 8px 0 4px;
  font-size: 20px;
  text-align: center;
}

.subtitle {
  margin: 0 0 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
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
</style>
