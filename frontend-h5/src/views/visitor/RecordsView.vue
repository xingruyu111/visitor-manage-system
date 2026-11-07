<template>
  <div class="page">
    <van-nav-bar title="我的预约" left-arrow @click-left="$router.back()" />

    <van-empty v-if="loaded && records.length === 0" description="暂无预约" />

    <van-cell-group v-for="item in records" :key="item.id" inset class="card">
      <van-cell :title="item.targetName || '访客'" :label="`${item.expectedTime} ~ ${item.leaveTime}`">
        <template #value>
          <van-tag :type="TAG[item.status] ?? 'default'">{{ STATUS[item.status] ?? item.status }}</van-tag>
        </template>
      </van-cell>
      <van-cell :title="item.visitPurpose || '无备注'" :label="item.plateNumber || ''">
        <template #right-icon>
          <van-button
            v-if="item.status === 1 || item.status === 3 || item.status === 4"
            size="mini"
            type="primary"
            @click="showPass(item)"
          >
            通行码
          </van-button>
        </template>
      </van-cell>
    </van-cell-group>

    <van-dialog v-model:show="passVisible" title="通行凭证" confirm-button-text="关闭">
      <div class="pass" v-if="pass">
        <p class="token">{{ pass.qrToken }}</p>
        <p class="meta">有效期：{{ pass.validFrom }} ~ {{ pass.validTo }}</p>
        <p class="meta">通行次数：{{ pass.usedEntries }} / {{ pass.maxEntries }}</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
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

const records = ref<RecordItem[]>([])
const loaded = ref(false)
const pass = ref<Pass | null>(null)
const passVisible = ref(false)

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

async function showPass(item: RecordItem) {
  try {
    pass.value = await http.get<Pass>(`/web/visitor/records/${item.id}/pass`)
    passVisible.value = true
  } catch (e) {
    showToast(e instanceof Error ? e.message : '凭证尚未生成')
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 24px;
}
.card {
  margin-top: 12px;
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
  color: #969799;
  margin-top: 8px;
}
</style>
