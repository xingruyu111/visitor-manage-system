<template>
  <div class="page">
    <van-nav-bar title="我的预约" left-arrow @click-left="$router.back()" />

    <van-loading v-if="!loaded" class="loading" />

    <van-empty v-else-if="records.length === 0" description="暂无预约记录" />

    <div v-else class="list">
      <div v-for="item in records" :key="item.id" class="record-card">
        <div class="record-head">
          <div class="record-title">
            <span class="name">{{ item.targetName || '访客' }}</span>
            <span class="time">{{ item.expectedTime }} ~ {{ item.leaveTime }}</span>
          </div>
          <van-tag :type="TAG[item.status] ?? 'default'" round>{{ STATUS[item.status] ?? item.status }}</van-tag>
        </div>
        <div class="record-meta">
          <span>{{ item.visitPurpose || '无备注' }}</span>
          <span v-if="item.plateNumber" class="plate">{{ item.plateNumber }}</span>
        </div>
        <div class="record-actions">
          <van-button size="small" round plain @click="openDetail(item)">详情</van-button>
          <van-button
            v-if="item.status === 0"
            size="small"
            round
            plain
            type="danger"
            @click="cancelRecord(item)"
          >
            取消申请
          </van-button>
          <van-button
            v-if="item.status === 1 || item.status === 3 || item.status === 4"
            size="small"
            round
            type="primary"
            @click="showPass(item)"
          >
            通行码
          </van-button>
          <van-button size="small" round plain type="primary" @click="showApproval(item)">审批意见</van-button>
        </div>
      </div>
    </div>

    <VisitDetailSheet
      v-model:show="detailVisible"
      title="预约详情"
      :record="detailRecord"
      :approval="detailApproval"
    />

    <van-dialog v-model:show="approvalVisible" title="审批意见" confirm-button-text="关闭">
      <div class="pass" v-if="approval">
        <p class="meta">{{ approval.nodeName }} · {{ approval.actedAt }}</p>
        <p class="token">{{ approval.comment || '无意见' }}</p>
      </div>
    </van-dialog>

    <van-dialog v-model:show="passVisible" title="通行凭证" confirm-button-text="关闭">
      <div class="pass" v-if="pass">
        <canvas ref="canvasRef" class="qr"></canvas>
        <p class="meta">有效期：{{ pass.validFrom }} ~ {{ pass.validTo }}</p>
        <p class="meta">通行次数：{{ pass.usedEntries }} / {{ pass.maxEntries }}</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import QRCode from 'qrcode'
import VisitDetailSheet, { type VisitRecord as DetailRecord, type ApprovalInfo } from '@/components/VisitDetailSheet.vue'
import { http } from '@/api/request'

interface RecordItem {
  id: number
  targetName?: string
  expectedTime: string
  leaveTime: string
  visitPurpose?: string
  plateNumber?: string
  status: number
}

interface Pass {
  qrToken: string
  validFrom: string
  validTo: string
  usedEntries: number
  maxEntries: number
}

const STATUS: Record<number, string> = {
  0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已入校', 4: '已离校', 5: '已取消', 6: '已过期',
}
const TAG: Record<number, 'primary' | 'success' | 'danger' | 'warning' | 'default'> = {
  0: 'warning', 1: 'success', 2: 'danger', 3: 'primary', 4: 'default', 5: 'default', 6: 'default',
}

const route = useRoute()
const records = ref<RecordItem[]>([])
const loaded = ref(false)
const pass = ref<Pass | null>(null)
const passVisible = ref(false)
const approvalVisible = ref(false)
const approval = ref<{ nodeName?: string; comment?: string; actedAt?: string } | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const detailVisible = ref(false)
const detailRecord = ref<DetailRecord | null>(null)
const detailApproval = ref<ApprovalInfo | null>(null)

async function load() {
  try {
    const data = await http.get<{ records: RecordItem[] }>('/web/visitor/records', { page: 1, size: 50 })
    records.value = data.records
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载失败')
  } finally {
    loaded.value = true
  }
}

async function openDetailById(id: number) {
  try {
    detailRecord.value = await http.get<DetailRecord>(`/web/visitor/records/${id}`)
    detailApproval.value = await http
      .get<ApprovalInfo>(`/web/visitor/records/${id}/approval`)
      .catch(() => null)
    detailVisible.value = true
  } catch (e) {
    showToast(e instanceof Error ? e.message : '加载详情失败')
  }
}

async function cancelRecord(item: RecordItem) {
  try {
    await showConfirmDialog({
      title: '取消申请',
      message: '确定取消这条待审批的预约吗？',
      confirmButtonText: '取消申请',
      cancelButtonText: '再想想',
    })
    await http.post(`/web/visitor/records/${item.id}/cancel`)
    showToast('已取消')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      showToast(e instanceof Error ? e.message : '取消失败')
    }
  }
}

async function showApproval(item: RecordItem) {
  try {
    approval.value = await http.get<{ nodeName?: string; comment?: string; actedAt?: string }>(
      `/web/visitor/records/${item.id}/approval`,
    )
    approvalVisible.value = true
  } catch (e) {
    showToast(e instanceof Error ? e.message : '暂无审批意见')
  }
}

async function showPass(item: RecordItem) {
  try {
    pass.value = await http.get<Pass>(`/web/visitor/records/${item.id}/pass`)
    passVisible.value = true
    await nextTick()
    if (canvasRef.value && pass.value) {
      await QRCode.toCanvas(canvasRef.value, pass.value.qrToken, { width: 180, margin: 1 })
    }
  } catch (e) {
    showToast(e instanceof Error ? e.message : '凭证尚未生成')
  }
}

async function openDetail(item: RecordItem) {
  await openDetailById(item.id)
}

onMounted(async () => {
  await load()
  const recordId = Number(route.query.recordId)
  if (recordId) {
    await openDetailById(recordId)
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 28px;
}
.loading {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
}
.record-card {
  background: #ffffff;
  border: 1px solid rgba(226, 236, 249, 0.9);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 6px 20px rgba(16, 42, 80, 0.06);
}
.record-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.record-title {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.record-title .name {
  font-size: 17px;
  font-weight: 600;
  color: #10233f;
}
.record-title .time {
  font-size: 12px;
  color: #61738c;
}
.record-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  color: #4a5f7a;
  font-size: 13px;
}
.record-meta .plate {
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f6fd;
  color: #31577f;
  font-size: 12px;
}
.record-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eff4fb;
}
.qr {
  display: block;
  margin: 0 auto 12px;
}
.pass {
  padding: 16px;
  text-align: center;
}
.token {
  font-size: 12px;
  word-break: break-all;
  color: #323233;
}
.meta {
  font-size: 12px;
  color: #7b8ba1;
  margin-top: 8px;
}
</style>
