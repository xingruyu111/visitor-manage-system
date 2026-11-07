<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>智慧校园访客管理系统</h1>
      <p class="subtitle">Web 管理后台</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit" @click="handleLogin">登录</el-button>
      </el-form>

      <p class="tip">骨架阶段：任意账号密码可进入，接入认证后替换为真实校验。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const form = reactive({
  username: '',
  password: '',
})

function handleLogin() {
  localStorage.setItem('token', 'dev-token')
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
  router.replace(redirect)
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
