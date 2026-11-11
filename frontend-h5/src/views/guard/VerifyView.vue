<template>
  <div class="page">
    <van-nav-bar :title="title" left-arrow @click-left="$router.back()" />

    <van-cell-group v-if="showVerify" inset title="扫码核验">
      <template v-if="!result">
        <div class="scanner">
          <video ref="videoRef" class="scanner-video" muted playsinline />
          <div class="scan-frame" />
          <div v-if="scanning" class="scanner-status">正在扫描…</div>
          <div v-else-if="scanError" class="scanner-status error">{{ scanError }}</div>
        </div>
        <p class="scanner-hint">将访客通行码放入框内，识别后自动核验</p>
        <div v-if="!scanning" class="actions">
          <van-button block type="primary" :loading="verifying" @click="startScan">
            {{ scanError ? '重新扫码' : '继续扫码' }}
          </van-button>
        </div>
      </template>
    </van-cell-group>

    <van-cell-group v-if="showVerify && result" inset title="核验结果">
      <van-cell title="方向" :value="result.direction === 'IN' ? '入校' : '离校'" />
      <van-cell title="访客" :value="result.visitorName" />
      <van-cell title="手机号" :value="result.visitorPhone || '—'" />
      <van-cell v-if="result.plateNumber" title="车牌号" :value="result.plateNumber" />
      <van-cell title="被访人" :value="result.targetName || '—'" />
      <van-cell title="到访时间" :value="result.expectedTime || '—'" />
      <van-cell title="离校时间" :value="result.leaveTime || '—'" />
      <div class="actions">
        <van-button block plain type="primary" @click="openResultDetail">查看完整详情</van-button>
        <van-button block type="primary" class="again" @click="scanAgain">继续扫码</van-button>
      </div>
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
        is-link
        @click="openQueryDetail(item)"
      />
    </van-cell-group>

    <VisitDetailSheet
      v-model:show="detailVisible"
      title="预约详情"
      :record="detailRecord"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { BrowserQRCodeReader } from '@zxing/browser'
import { http } from '@/api/request'
import VisitDetailSheet, { type VisitRecord } from '@/components/VisitDetailSheet.vue'

interface VerifyResult {
  direction: string
  visitorName: string
  visitorPhone?: string
  plateNumber?: string
  targetName?: string
  expectedTime?: string
  leaveTime?: string
  recordStatus?: number
  usedEntries?: number
  maxEntries?: number
}

interface RecordItem {
  id: number
  visitorName: string
  visitorPhone?: string
  plateNumber?: string
  targetName?: string
  visitPurpose?: string
  expectedTime: string
  leaveTime: string
  passMode?: string
  status?: number
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

const plate = ref('')
const result = ref<VerifyResult | null>(null)
const records = ref<RecordItem[]>([])
const verifying = ref(false)
const querying = ref(false)
const canScan = ref(false)
const canQuery = ref(false)
const videoRef = ref<HTMLVideoElement>()
const scanning = ref(false)
const scanError = ref('')
const detailVisible = ref(false)
const detailRecord = ref<VisitRecord | null>(null)
const codeReader = new BrowserQRCodeReader()
let controls: { stop: () => void } | null = null

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

function cameraErrorMessage(error: unknown) {
  if (error instanceof DOMException) {
    if (error.name === 'NotAllowedError') return '请允许使用摄像头后重试'
    if (error.name === 'NotFoundError') return '未检测到摄像头'
    if (error.name === 'NotReadableError') return '摄像头被其他应用占用'
  }
  return error instanceof Error ? error.message : '摄像头启动失败'
}

function stopScan() {
  controls?.stop()
  controls = null
  scanning.value = false
}

async function startScan() {
  if (scanning.value) return
  result.value = null
  scanError.value = ''
  await nextTick()
  if (!videoRef.value) return
  scanning.value = true
  try {
    controls = await codeReader.decodeFromConstraints(
      { video: { facingMode: { ideal: 'environment' } } },
      videoRef.value,
      async (scanResult) => {
        if (!scanResult || verifying.value || !scanning.value) return
        stopScan()
        await verify(scanResult.getText())
      },
    )
  } catch (error) {
    stopScan()
    scanError.value = cameraErrorMessage(error)
  }
}

async function verify(code: string) {
  verifying.value = true
  try {
    result.value = await http.post<VerifyResult>(`/web/guard/verify?token=${encodeURIComponent(code)}`)
    showToast(result.value.direction === 'IN' ? '已入校' : '已离校')
  } catch (e) {
    showToast(e instanceof Error ? e.message : '核验失败')
    await startScan()
  } finally {
    verifying.value = false
  }
}

function scanAgain() {
  result.value = null
  startScan()
}

watch(
  () => route.query.mode,
  async () => {
    stopScan()
    if (showVerify.value) {
      await nextTick()
      startScan()
    }
  },
)

onMounted(async () => {
  await loadAccess()
  if (showVerify.value && canScan.value) {
    await nextTick()
    startScan()
  }
})

onBeforeUnmount(stopScan)

function openQueryDetail(item: RecordItem) {
  detailRecord.value = item
  detailVisible.value = true
}

function openResultDetail() {
  if (!result.value) return
  detailRecord.value = {
    visitorName: result.value.visitorName,
    visitorPhone: result.value.visitorPhone,
    plateNumber: result.value.plateNumber,
    targetName: result.value.targetName,
    expectedTime: result.value.expectedTime,
    leaveTime: result.value.leaveTime,
    status: result.value.recordStatus,
  }
  detailVisible.value = true
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
.scanner {
  position: relative;
  margin: 16px;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  border-radius: 18px;
  background: #0b1626;
}
.scanner-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.scan-frame {
  position: absolute;
  inset: 18%;
  border: 2px solid rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  box-shadow: 0 0 0 999px rgba(0, 0, 0, 0.28);
}
.scan-frame::before,
.scan-frame::after {
  content: '';
  position: absolute;
  width: 26px;
  height: 26px;
  border-color: #4ea1ff;
  border-style: solid;
}
.scan-frame::before {
  left: -2px;
  top: -2px;
  border-width: 3px 0 0 3px;
  border-radius: 10px 0 0;
}
.scan-frame::after {
  right: -2px;
  bottom: -2px;
  border-width: 0 3px 3px 0;
  border-radius: 0 0 10px;
}
.scanner-status {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #ffffff;
  font-size: 12px;
  white-space: nowrap;
}
.scanner-status.error {
  background: rgba(240, 68, 56, 0.9);
}
.scanner-hint {
  margin: 0 16px;
  color: #61738c;
  font-size: 13px;
  text-align: center;
}
.actions {
  padding: 12px 16px 4px;
}
.van-cell-group--inset {
  margin-top: 12px;
}
</style>
