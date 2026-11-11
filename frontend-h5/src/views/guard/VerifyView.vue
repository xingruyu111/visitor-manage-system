<template>
  <div class="page">
    <van-nav-bar :title="title" left-arrow @click-left="$router.back()">
      <template v-if="mode && canScan && canQuery" #right>
        <span class="nav-link" @click="switchMode">{{ mode === 'verify' ? '车主查询' : '扫码核验' }}</span>
      </template>
    </van-nav-bar>

    <van-cell-group v-if="showVerify" inset title="扫码核验">
      <van-field v-model="token" label="通行码" placeholder="粘贴或扫描通行码" />
      <div class="actions">
        <van-button block type="primary" :loading="verifying" @click="verify">核验</van-button>
      </div>
    </van-cell-group>

    <van-cell-group v-if="showVerify && result" inset title="核验结果">
      <van-cell title="方向" :value="result.direction === 'IN' ? '入校' : '离校'" />
      <van-cell title="访客" :value="result.visitorName" />
    </van-cell-group>

    <van-cell-group v-if="showQuery" inset title="车主查询">
      <van-field v-model="plate" label="车牌号" placeholder="输入车牌查询预约" />
      <div class="actions">
        <van-button block plain type="primary" :loading="querying" @click="query">查询</van-button>
      </div>
    </van-cell-group>

    <van-cell-group v-if="showQuery && records.length" inset title="查询结果">
      <van-cell
        v-for="item in records"
        :key="item.id"
        :title="`${item.visitorName} ${item.plateNumber}`"
        :label="`${item.expectedTime} ~ ${item.leaveTime}`"
      />
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { http } from '@/api/request'

interface VerifyResult {
  direction: string
  visitorName: string
}

interface RecordItem {
  id: number
  visitorName: string
  plateNumber?: string
  expectedTime: string
  leaveTime: string
}

const route = useRoute()
const router = useRouter()
const mode = computed(() => (typeof route.query.mode === 'string' ? route.query.mode : ''))
const showVerify = computed(() => mode.value !== 'query')
const showQuery = computed(() => mode.value !== 'verify')
const title = computed(() => {
  if (mode.value === 'query') return '车主查询'
  if (mode.value === 'verify') return '扫码核验'
  return '门卫核验'
})

const token = ref('')
const plate = ref('')
const result = ref<VerifyResult | null>(null)
const records = ref<RecordItem[]>([])
const verifying = ref(false)
const querying = ref(false)
const canScan = ref(false)
const canQuery = ref(false)

async function loadAccess() {
  try {
    const me = await http.get<{ roles: Array<{ code: number }> }>('/auth/me')
    const roles = new Set((me.roles ?? []).map((role) => role.code))
    canScan.value = [1, 2, 3].some((code) => roles.has(code))
    canQuery.value = [1, 2, 13].some((code) => roles.has(code))
    if (!canScan.value && !canQuery.value) {
      showToast('当前身份无核验权限')
      router.replace('/v/home')
      return
    }
    if (!mode.value) {
      router.replace({ path: '/g/verify', query: { mode: canScan.value ? 'verify' : 'query' } })
    } else if (mode.value === 'verify' && !canScan.value) {
      router.replace({ path: '/g/verify', query: { mode: 'query' } })
    } else if (mode.value === 'query' && !canQuery.value) {
      router.replace({ path: '/g/verify', query: { mode: 'verify' } })
    }
  } catch {
    showToast('获取权限失败，请重新登录')
  }
}

onMounted(loadAccess)

function switchMode() {
  router.replace({ path: '/g/verify', query: { mode: mode.value === 'verify' ? 'query' : 'verify' } })
}

async function verify() {
  if (!token.value) {
    showToast('请输入通行码')
    return
  }
  verifying.value = true
  try {
    result.value = await http.post<VerifyResult>(`/web/guard/verify?token=${encodeURIComponent(token.value)}`)
    showToast(result.value.direction === 'IN' ? '已入校' : '已离校')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '核验失败')
  } finally {
    verifying.value = false
  }
}

async function query() {
  if (!plate.value) {
    showToast('请输入车牌号')
    return
  }
  querying.value = true
  try {
    records.value = await http.get<RecordItem[]>('/web/guard/query', { plate: plate.value })
    if (records.value.length === 0) {
      showToast('未找到预约')
    }
  } catch (e) {
    showToast(e instanceof Error ? e.message : '查询失败')
  } finally {
    querying.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 32px;
}
.actions {
  padding: 12px 16px 4px;
}
.van-cell-group--inset {
  margin-top: 12px;
}
.nav-link {
  color: #14508f;
  font-size: 14px;
  font-weight: 500;
}
</style>
