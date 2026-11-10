<template>
  <div class="page">
    <van-nav-bar title="智慧校园访客">
      <template #right>
        <span class="role-chip">{{ roleLabel }}</span>
      </template>
    </van-nav-bar>

    <div class="content">
      <div class="hero">
        <h2>{{ heroTitle }}</h2>
        <p>{{ heroDesc }}</p>
      </div>

      <van-loading v-if="!loaded" class="loading" />

      <template v-else>
        <van-cell-group v-if="menu === 'VISITOR'" inset title="访客">
          <van-cell
            :title="isAlumni ? '校友预约' : '预约申请'"
            is-link
            :to="isAlumni ? '/v/apply?type=ALUMNI' : '/v/apply?type=NORMAL'"
          />
          <van-cell title="我的预约" is-link to="/v/records" />
        </van-cell-group>

        <van-cell-group v-else-if="menu === 'APPROVER'" inset title="审批">
          <van-cell title="我的待办" is-link to="/a/tasks" />
          <van-cell title="历史审批" is-link to="/a/history" />
        </van-cell-group>


        <van-empty v-else description="当前身份暂无 H5 功能，请使用 Web 管理端" />
      </template>

      <div class="actions">
        <van-button block plain type="primary" @click="logout">退出登录</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import { http } from '@/api/request'

interface RoleItem {
  code: number
  name: string
  schoolId?: number
}

interface Profile {
  id: number
  name: string
  phone: string
  currentRole: number
  personType?: number
  personTypeDesc?: string
  roles: RoleItem[]
}

type MenuKind = 'VISITOR' | 'APPROVER' | 'NONE'

const router = useRouter()
const profile = ref<Profile>()
const loaded = ref(false)

const isAlumni = computed(() => profile.value?.personType === 7 || profile.value?.currentRole === 9)
const roleLabel = computed(() => {
  if (!profile.value) return '加载中'
  if (profile.value.personTypeDesc) return profile.value.personTypeDesc
  if (profile.value.currentRole === 4) return '访客身份'
  return profile.value.roles?.find((role) => role.code === profile.value?.currentRole)?.name
    || '访客身份'
})

const menu = computed<MenuKind>(() => {
  const current = profile.value
  if (!current) return 'NONE'
  if (current.personType === 6) return 'NONE'
  if ([5, 6, 7, 8].includes(current.currentRole)) return 'APPROVER'
  if (current.currentRole === 9 || current.personType === 7) return 'VISITOR'
  if (current.currentRole === 4) return 'VISITOR'
  return 'NONE'
})

const heroTitle = computed(() => {
  if (menu.value === 'APPROVER') return '待我审批'
  if (menu.value === 'NONE') return '暂无 H5 功能'
  return isAlumni.value ? '校友预约' : '访客预约'
})

const heroDesc = computed(() => {
  if (menu.value === 'APPROVER') return '处理拜访预约的待办，填写审批意见并查看历史记录。'
  if (menu.value === 'NONE') return '当前身份没有配置 H5 入口，请使用 Web 管理端。'
  return '选择被访人、填写到访时间，提交后由被访人审批，通过后生成通行码。'
})

async function logout() {
  try {
    await http.post('/auth/logout')
  } catch {
    // 令牌已失效也继续清理本地状态
  }
  localStorage.removeItem('visitor_token')
  await router.replace('/login')
}

async function loadProfile() {
  try {
    profile.value = await http.get<Profile>('/auth/me')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '获取身份失败')
  } finally {
    loaded.value = true
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}
.content {
  padding: 16px 0 24px;
}
.hero {
  padding: 22px 20px;
  margin: 0 16px 4px;
  border-radius: 16px;
}
.hero h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #fff;
}
.hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  line-height: 1.6;
}
.role-chip {
  display: inline-block;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.1);
  color: #1677ff;
  font-size: 12px;
}
.loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}
.actions {
  margin: 24px 16px;
}
</style>
