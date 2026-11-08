<template>
  <div class="page">
    <h2 class="title">访客登录</h2>

    <van-notice-bar
      v-if="wechatCode"
      left-icon="info-o"
      text="已获取微信授权，请填写手机号完成绑定"
    />

    <van-form @submit="accountLogin">
      <van-cell-group inset>
        <van-field v-model="phone" label="手机号" placeholder="请输入手机号" :rules="[{ required: true, message: '请输入手机号' }]" />
        <van-field v-model="password" type="password" label="密码" placeholder="请输入密码" />
        <van-field v-if="captchaEnabled" v-model="captchaCode" label="验证码" placeholder="请输入验证码">
          <template #button>
            <img :src="captchaImage" class="captcha" alt="验证码" @click="loadCaptcha" />
          </template>
        </van-field>
      </van-cell-group>

      <div class="actions">
        <van-button round block type="primary" native-type="submit" :loading="loading">账号登录</van-button>
        <van-button
          v-if="wechatCode"
          round
          block
          type="success"
          class="wechat"
          :loading="wechatLoading"
          @click="wechatLogin"
        >
          微信授权登录
        </van-button>
      </div>
    </van-form>

    <p class="tip">首次使用微信登录会自动创建访客账号并绑定手机号。</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface LoginResult {
  token: string
}

interface CaptchaResult {
  enabled: boolean
  captchaId?: string
  image?: string
}

const route = useRoute()
const router = useRouter()
const phone = ref('')
const password = ref('')
const loading = ref(false)
const wechatLoading = ref(false)
const wechatCode = ref('')
const captchaEnabled = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const captchaCode = ref('')

function saveToken(token: string) {
  localStorage.setItem('visitor_token', token)
  router.replace('/v/home')
}

async function loadCaptcha() {
  try {
    const data = await http.get<CaptchaResult>('/auth/captcha')
    captchaEnabled.value = data.enabled
    captchaId.value = data.captchaId ?? ''
    captchaImage.value = data.image ?? ''
    captchaCode.value = ''
  } catch {
    captchaEnabled.value = false
  }
}

async function accountLogin() {
  if (!password.value) {
    showToast('账号登录请输入密码')
    return
  }
  if (captchaEnabled.value && !captchaCode.value) {
    showToast('请输入验证码')
    return
  }
  loading.value = true
  try {
    const data = await http.post<LoginResult>('/auth/login', {
      phone: phone.value,
      password: password.value,
      captchaId: captchaEnabled.value ? captchaId.value : undefined,
      captchaCode: captchaEnabled.value ? captchaCode.value : undefined,
    })
    saveToken(data.token)
  } catch (e) {
    showToast(e instanceof Error ? e.message : '登录失败')
    if (captchaEnabled.value) {
      await loadCaptcha()
    }
  } finally {
    loading.value = false
  }
}

async function wechatLogin() {
  if (!phone.value) {
    showToast('请填写手机号')
    return
  }
  wechatLoading.value = true
  try {
    const data = await http.post<LoginResult>('/auth/wechat', {
      code: wechatCode.value,
      phone: phone.value,
    })
    saveToken(data.token)
  } catch (e) {
    showToast(e instanceof Error ? e.message : '微信登录失败')
  } finally {
    wechatLoading.value = false
  }
}

onMounted(() => {
  const code = route.query.code
  if (typeof code === 'string' && code.length > 0) {
    wechatCode.value = code
  }
  loadCaptcha()
})
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
.wechat {
  margin-top: 12px;
}
.tip {
  margin: 16px;
  color: #969799;
  font-size: 12px;
  text-align: center;
}
.captcha {
  height: 32px;
  width: 96px;
  border: 1px solid #ebedf0;
  border-radius: 4px;
}
</style>
