<template>
  <div class="login-page">
    <div class="login-glow login-glow-a"></div>
    <div class="login-glow login-glow-b"></div>
    <div class="login-grid"></div>

    <section class="login-intro">
      <div class="intro-mark">访</div>
      <p class="intro-kicker">智慧校园 · 安全运营</p>
      <h1>让每一次来访<br /><span>都有序可循</span></h1>
      <p class="intro-desc">访客预约、审批流转、通行核验与安全数据，在同一处完成。</p>
      <div class="intro-points">
        <span>预约审批</span>
        <span>通行核验</span>
        <span>安全态势</span>
      </div>
    </section>

    <el-card class="login-card" shadow="never">
      <div class="login-head">
        <p class="card-kicker">管理后台</p>
        <h2>登录工作台</h2>
        <p>使用管理员账号继续</p>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="账号（手机号）" required>
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item v-if="captchaEnabled" label="验证码" required>
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
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 460px);
  align-items: center;
  gap: 7vw;
  height: 100%;
  overflow: hidden;
  padding: 7vh 8vw;
  box-sizing: border-box;
  background:
    radial-gradient(900px 600px at 15% 0%, rgba(37, 99, 235, 0.32), transparent 65%),
    linear-gradient(135deg, #071421 0%, #0b2137 48%, #0b1a2b 100%);
}

.login-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.3;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.08) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: linear-gradient(90deg, black, transparent 76%);
}

.login-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  pointer-events: none;
}

.login-glow-a {
  top: -180px;
  left: 12%;
  width: 480px;
  height: 480px;
  background: rgba(59, 130, 246, 0.25);
}

.login-glow-b {
  right: -160px;
  bottom: -220px;
  width: 520px;
  height: 520px;
  background: rgba(14, 165, 233, 0.12);
}

.login-intro {
  position: relative;
  z-index: 1;
  max-width: 560px;
  color: #dbeafe;
}

.intro-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin-bottom: 32px;
  color: #fff;
  font-size: 21px;
  font-weight: 700;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-radius: 13px;
  box-shadow: 0 8px 26px rgba(37, 99, 235, 0.35);
}

.intro-kicker {
  margin: 0 0 16px;
  color: #7dd3fc;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 1.4px;
}

.login-intro h1 {
  margin: 0;
  color: #f8fafc;
  font-size: clamp(38px, 4.2vw, 58px);
  font-weight: 650;
  line-height: 1.15;
  letter-spacing: -1.5px;
}

.login-intro h1 span {
  color: #93c5fd;
}

.intro-desc {
  max-width: 420px;
  margin: 24px 0 0;
  color: #9fb3c8;
  font-size: 14px;
  line-height: 1.8;
}

.intro-points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 30px;
}

.intro-points span {
  padding: 6px 11px;
  color: #bfdbfe;
  font-size: 12px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(147, 197, 253, 0.16);
  border-radius: 999px;
}

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 8px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 18px;
  box-shadow: 0 28px 70px rgba(2, 12, 27, 0.4);
}

.login-head {
  margin-bottom: 26px;
}

.card-kicker {
  margin: 0 0 8px;
  color: var(--vst-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
}

.login-head h2 {
  margin: 0;
  color: var(--vst-text);
  font-size: 24px;
  font-weight: 650;
  letter-spacing: -0.4px;
}

.login-head > p:last-child {
  margin: 8px 0 0;
  color: var(--vst-text-secondary);
  font-size: 13px;
}

.submit {
  width: 100%;
  height: 42px;
  margin-top: 6px;
  font-size: 14px;
}

.tip {
  margin: 20px 0 0;
  color: #94a3b8;
  font-size: 11px;
  text-align: center;
}

.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.captcha-img {
  width: 120px;
  height: 38px;
  cursor: pointer;
  border: 1px solid var(--vst-border);
  border-radius: 8px;
}

@media (max-width: 960px) {
  .login-page {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 34px;
    padding: 8vh 24px;
  }

  .login-intro {
    width: min(100%, 460px);
  }

  .intro-mark {
    width: 42px;
    height: 42px;
    margin-bottom: 20px;
    font-size: 18px;
  }

  .login-intro h1 {
    font-size: clamp(30px, 7vw, 42px);
  }

  .intro-desc {
    margin-top: 16px;
  }

  .intro-points {
    margin-top: 20px;
  }

  .login-card {
    width: min(100%, 460px);
  }
}
</style>
