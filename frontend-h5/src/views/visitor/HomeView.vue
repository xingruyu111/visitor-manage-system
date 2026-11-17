<template>
  <div class="page">
    <van-nav-bar title="智慧校园访客" :border="false">
      <template #right>
        <span class="role-chip">{{ roleLabel }}</span>
      </template>
    </van-nav-bar>

    <div class="content">
      <van-loading v-if="!loaded" class="loading" />

      <template v-else>
        <section class="hero">
          <div class="hero-orb hero-orb-a"></div>
          <div class="hero-orb hero-orb-b"></div>
          <div class="hero-copy">
            <span class="hero-tag">{{ roleLabel }}</span>
            <h1>{{ heroTitle }}</h1>
            <p>{{ heroSubtitle }}</p>
            <button class="hero-cta" type="button" @click="goPrimary">
              <span>{{ heroButtonText }}</span>
              <van-icon name="arrow" />
            </button>
          </div>
          <div class="hero-art" aria-hidden="true">
            <div class="art-ring"></div>
            <span class="float-card float-a"><van-icon name="qr" /></span>
            <span class="float-card float-b"><van-icon name="friends-o" /></span>
            <span class="float-card float-c"><van-icon name="passed" /></span>
            <span class="float-dot float-dot-a"></span>
            <span class="float-dot float-dot-b"></span>
          </div>
        </section>

        <section class="quick-card">
          <button
            v-for="(entry, index) in quickEntries"
            :key="entry.to"
            type="button"
            class="quick-item"
            @click="router.push(entry.to)"
          >
            <span class="quick-icon" :class="`tone-${index % 6}`">
              <van-icon :name="entry.icon" />
            </span>
            <span class="quick-label">{{ entry.title }}</span>
            <span v-if="entry.badge" class="quick-badge">{{ entry.badge > 99 ? '99+' : entry.badge }}</span>
          </button>
        </section>

        <section class="today-card">
          <div class="section-head">
            <h2>今日拜访</h2>
            <span>{{ todayCount }} 条</span>
          </div>

          <div v-if="whitelistRecord" class="pass-banner" @click="showWhitelistPass">
            <div class="pass-icon"><van-icon name="passed" /></div>
            <div class="pass-copy">
              <strong>您在白名单中</strong>
              <span>有效期至 {{ whitelistRecord.leaveTime }}，点击查看通行码</span>
            </div>
            <van-icon name="arrow" class="pass-arrow" />
          </div>

          <div v-else-if="alumniMonth?.eligible" class="pass-banner alumni-banner" @click="showAlumniPass">
            <div class="pass-icon alumni-icon"><van-icon name="friends-o" /></div>
            <div class="pass-copy">
              <strong>本月校友通行</strong>
              <span>有效期至 {{ alumniMonth.validTo }}，点击查看通行码</span>
            </div>
            <van-icon name="arrow" class="pass-arrow" />
          </div>

          <div v-else class="empty-state">
            <div class="empty-art" aria-hidden="true">
              <span class="empty-sheet empty-sheet-back"></span>
              <span class="empty-sheet empty-sheet-front"><van-icon name="records-o" /></span>
              <span class="empty-glow"></span>
            </div>
            <p>暂无相关信息</p>
            <span>有预约或通行凭证时会显示在这里</span>
          </div>
        </section>

        <p class="support">校园访客管理 · 安全通行</p>
      </template>

      <div class="actions">
        <button type="button" class="logout-link" @click="logout">退出登录</button>
      </div>
    </div>

    <van-dialog v-model:show="passVisible" title="通行凭证" confirm-button-text="关闭">
      <div v-if="pass" class="pass">
        <canvas ref="canvasRef" class="qr"></canvas>
        <p class="pass-meta">有效期：{{ pass.validFrom }} ~ {{ pass.validTo }}</p>
        <p class="pass-meta">通行次数：{{ pass.maxEntries >= 999999 ? '不限次' : (pass.usedEntries + ' / ' + pass.maxEntries) }}</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import QRCode from 'qrcode'
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

type MenuKind = 'VISITOR' | 'APPROVER' | 'GUARD' | 'SECURITY' | 'SECURITY_ADMIN' | 'DEPT' | 'NONE'

interface Entry {
  title: string
  to: string
  icon: string
  badge?: number
}

interface MenuGroup {
  key: Exclude<MenuKind, 'NONE'>
  title: string
  entries: Entry[]
}

const ROLE_KIND: Record<number, MenuKind> = {
  2: 'SECURITY_ADMIN',
  3: 'GUARD',
  4: 'VISITOR',
  5: 'APPROVER',
  6: 'APPROVER',
  7: 'APPROVER',
  8: 'APPROVER',
  9: 'VISITOR',
  11: 'DEPT',
  13: 'SECURITY',
  12: 'APPROVER',
}

interface RecordItem {
  id: number
  targetType?: string
  status: number
  expectedTime: string
  leaveTime: string
}

interface Pass {
  qrToken: string
  validFrom: string
  validTo: string
  usedEntries: number
  maxEntries: number
}

interface AlumniMonth {
  eligible: boolean
  recordId?: number
  month?: number
  validFrom?: string
  validTo?: string
}

const router = useRouter()
const profile = ref<Profile>()
const loaded = ref(false)
const pendingCount = ref(0)
const unreadCount = ref(0)
const whitelistRecord = ref<RecordItem>()
const alumniMonth = ref<AlumniMonth>()
const pass = ref<Pass>()
const passVisible = ref(false)
const canvasRef = ref<HTMLCanvasElement | null>(null)

const isAlumni = computed(() => {
  const current = profile.value
  if (!current) return false
  return current.personType === 7 || current.roles?.some((role) => role.code === 9) === true
})

const roleLabel = computed(() => {
  if (!profile.value) return '加载中'
  if (profile.value.personTypeDesc) return profile.value.personTypeDesc
  if (profile.value.currentRole === 4) return '访客身份'
  return profile.value.roles?.find((role) => role.code === profile.value?.currentRole)?.name
    || '访客身份'
})

const menuKinds = computed<Exclude<MenuKind, 'NONE'>[]>(() => {
  const current = profile.value
  if (!current) return []

  const kinds: Exclude<MenuKind, 'NONE'>[] = []
  const add = (kind: MenuKind) => {
    if (kind !== 'NONE' && !kinds.includes(kind)) {
      kinds.push(kind)
    }
  }

  // 主角色：附属学生不生成基础入口，只保留附加角色入口
  if (current.personType !== 6 && current.currentRole !== 1) {
    add(ROLE_KIND[current.currentRole] ?? 'NONE')
  }

  // 附加角色：跳过主角色本身，避免重复；附属学生的主角色 STUDENT 也不会生成审批入口
  for (const role of current.roles ?? []) {
    if (role.code === current.currentRole) continue
    add(ROLE_KIND[role.code] ?? 'NONE')
  }
  return kinds
})

const groups = computed<MenuGroup[]>(() => {
  const visitorEntries: Entry[] = [
    {
      title: isAlumni.value ? '校友预约' : '预约申请',
      to: isAlumni.value ? '/v/apply?type=ALUMNI' : '/v/apply?type=NORMAL',
      icon: 'edit',
    },
    { title: '我的预约', to: '/v/records', icon: 'records-o' },
  ]

  const definitions: Record<Exclude<MenuKind, 'NONE'>, MenuGroup> = {
    APPROVER: {
      key: 'APPROVER',
      title: '审批',
      entries: [
        { title: '我的待办', to: '/a/tasks', icon: 'todo-list-o', badge: pendingCount.value || undefined },
        { title: '历史审批', to: '/a/history', icon: 'clock-o' },
      ],
    },
    GUARD: {
      key: 'GUARD',
      title: '门卫',
      entries: [{ title: '扫码核验', to: '/g/verify?mode=verify', icon: 'scan' }],
    },
    SECURITY: {
      key: 'SECURITY',
      title: '保安',
      entries: [{ title: '车主查询', to: '/g/verify?mode=query', icon: 'search' }],
    },
    SECURITY_ADMIN: {
      key: 'SECURITY_ADMIN',
      title: '保卫处',
      entries: [
        { title: '我的待办', to: '/a/tasks', icon: 'todo-list-o', badge: pendingCount.value || undefined },
        { title: '历史审批', to: '/a/history', icon: 'clock-o' },
        { title: '批量预约', to: '/d/batch', icon: 'orders-o' },
        { title: '导入白名单', to: '/s/whitelist-import', icon: 'upgrade' },
      ],
    },
    DEPT: {
      key: 'DEPT',
      title: '部门经办人',
      entries: [
        { title: '批量预约', to: '/d/batch', icon: 'orders-o' },
      ],
    },
    VISITOR: {
      key: 'VISITOR',
      title: '访客',
      entries: visitorEntries,
    },
  }

  const seen = new Set<string>()
  return menuKinds.value
    .map((kind) => definitions[kind])
    .map((group) => ({
      ...group,
      entries: group.entries.filter((entry) => {
        if (seen.has(entry.to)) return false
        seen.add(entry.to)
        return true
      }),
    }))
    .filter((group) => group.entries.length > 0)
})

const isVisitorMenu = computed(() => menuKinds.value.includes('VISITOR'))

const quickEntries = computed<Entry[]>(() => {
  const entries = groups.value.flatMap((group) => group.entries)
  if (!entries.some((entry) => entry.to === '/v/notifications')) {
    entries.push({
      title: '站内通知',
      to: '/v/notifications',
      icon: 'bell',
      badge: unreadCount.value || undefined,
    })
  }
  return entries
})

const primaryEntry = computed<Entry | undefined>(() => {
  const entries = quickEntries.value
  if (isVisitorMenu.value) {
    return entries.find((entry) => entry.to.startsWith('/v/apply')) ?? entries[0]
  }
  if (menuKinds.value.includes('GUARD')) {
    return entries.find((entry) => entry.to.startsWith('/g/verify')) ?? entries[0]
  }
  return entries.find((entry) => entry.to === '/a/tasks') ?? entries[0]
})

const heroTitle = computed(() => {
  if (isVisitorMenu.value) return isAlumni.value ? '校友预约，轻松返校' : '一键预约，轻松拜访'
  if (menuKinds.value.includes('APPROVER')) return '审批事项，一处处理'
  if (menuKinds.value.includes('SECURITY_ADMIN')) return '访客安全，统一管理'
  if (menuKinds.value.includes('GUARD')) return '扫码核验，快速通行'
  if (menuKinds.value.includes('SECURITY')) return '车辆查询，快速核验'
  if (menuKinds.value.includes('DEPT')) return '批量预约，高效录入'
  return '访客服务，一站办理'
})

const heroSubtitle = computed(() => {
  if (isVisitorMenu.value) return '选择到访时段，填写信息即可预约。'
  if (menuKinds.value.includes('APPROVER') || menuKinds.value.includes('SECURITY_ADMIN')) {
    return '待办与历史审批集中查看，及时完成访客审核。'
  }
  if (menuKinds.value.includes('GUARD')) return '核验访客通行码，记录入校与离校。'
  if (menuKinds.value.includes('SECURITY')) return '输入车牌查询访客与通行信息。'
  if (menuKinds.value.includes('DEPT')) return 'Excel 批量导入访客预约，统一提交。'
  return '预约、审批与通行服务，集中处理。'
})

const heroButtonText = computed(() => {
  if (isVisitorMenu.value) return '立即预约'
  if (menuKinds.value.includes('APPROVER') || menuKinds.value.includes('SECURITY_ADMIN')) return '查看待办'
  if (menuKinds.value.includes('GUARD')) return '开始核验'
  if (menuKinds.value.includes('SECURITY')) return '车主查询'
  if (menuKinds.value.includes('DEPT')) return '批量预约'
  return '进入工作台'
})

const todayCount = computed(() => (whitelistRecord.value || alumniMonth.value?.eligible ? 1 : 0))

function goPrimary() {
  if (primaryEntry.value) {
    router.push(primaryEntry.value.to)
  }
}




async function loadWhitelist() {
  try {
    const data = await http.get<{ records: RecordItem[] }>('/web/visitor/records', { page: 1, size: 50 })
    const now = new Date()
    whitelistRecord.value = (data.records ?? []).find((item) => {
      if (item.targetType !== 'WHITELIST' || ![1, 3, 4].includes(item.status)) return false
      const from = new Date(item.expectedTime.replace(' ', 'T'))
      const to = new Date(item.leaveTime.replace(' ', 'T'))
      return from <= now && now <= to
    })
  } catch {
    whitelistRecord.value = undefined
  }
}

async function showWhitelistPass() {
  if (!whitelistRecord.value) return
  await openPass(whitelistRecord.value.id)
}

async function showAlumniPass() {
  if (!alumniMonth.value?.recordId) return
  await openPass(alumniMonth.value.recordId)
}

async function openPass(recordId: number) {
  try {
    pass.value = await http.get<Pass>(`/web/visitor/records/${recordId}/pass`)
    passVisible.value = true
    await nextTick()
    if (canvasRef.value) {
      await QRCode.toCanvas(canvasRef.value, pass.value.qrToken, { width: 220, margin: 1 })
    }
  } catch (e) {
    showToast(e instanceof Error ? e.message : '通行码暂不可用')
  }
}

async function loadAlumniMonth() {
  try {
    alumniMonth.value = await http.get<AlumniMonth>('/web/visitor/alumni-month')
  } catch {
    alumniMonth.value = undefined
  }
}

async function logout() {
  try {
    await http.post('/auth/logout')
  } catch {
    // 令牌已失效也继续清理本地状态
  }
  localStorage.removeItem('visitor_token')
  await router.replace('/login')
}

async function loadCounts() {
  try {
    const tasks = await http.get<{ total: number }>('/web/approval/tasks', { page: 1, size: 1 })
    pendingCount.value = tasks.total ?? 0
  } catch {
    pendingCount.value = 0
  }
  try {
    const unread = await http.get<{ count: number }>('/web/notifications/unread-count')
    unreadCount.value = unread.count ?? 0
  } catch {
    unreadCount.value = 0
  }
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

onMounted(() => {
  loadProfile()
  loadCounts()
  loadWhitelist()
  loadAlumniMonth()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f3f6fb;
}

.content {
  padding: 0 0 28px;
  gap: 0;
}

:deep(.van-nav-bar) {
  background: #ffffff;
}

:deep(.van-nav-bar__title) {
  color: #14213d;
  font-size: 16px;
  font-weight: 600;
}

:deep(.van-nav-bar::after) {
  display: none;
}

.role-chip {
  display: inline-block;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 3px 9px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 11px;
  font-weight: 500;
}

.loading {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

/* ---------- 头图 ---------- */
.hero {
  position: relative;
  overflow: hidden;
  margin: 0;
  padding: 24px 20px 46px;
  color: #fff;
  border-radius: 0;
  background:
    radial-gradient(220px 150px at 88% 8%, rgba(147, 197, 253, 0.38), transparent 70%),
    linear-gradient(135deg, #1e40af 0%, #2563eb 54%, #3b82f6 100%);
}

.hero::after {
  content: '';
  position: absolute;
  right: -30px;
  bottom: -70px;
  width: 240px;
  height: 160px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 50%;
  transform: rotate(-18deg);
}

.hero-copy {
  position: relative;
  z-index: 2;
  max-width: 72%;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  color: #dbeafe;
  font-size: 10px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
}

.hero h1 {
  margin: 10px 0 8px;
  color: #fff;
  font-size: 22px;
  font-weight: 650;
  line-height: 1.3;
  letter-spacing: -0.4px;
}

.hero p {
  max-width: 210px;
  margin: 0;
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
  line-height: 1.7;
}

.hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  padding: 9px 16px;
  color: #1d4ed8;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  background: #fff;
  border: 0;
  border-radius: 999px;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.18);
}

.hero-art {
  position: absolute;
  right: -8px;
  bottom: 18px;
  width: 150px;
  height: 142px;
  pointer-events: none;
}

.art-ring {
  position: absolute;
  top: 0;
  right: 6px;
  width: 122px;
  height: 122px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
}

.float-card {
  position: absolute;
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  color: #fff;
  font-size: 23px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.14);
  backdrop-filter: blur(7px);
}

.float-a {
  top: 18px;
  right: 46px;
  transform: rotate(-9deg);
}

.float-b {
  top: 62px;
  right: 8px;
  transform: rotate(8deg) scale(0.82);
}

.float-c {
  top: 82px;
  right: 70px;
  transform: rotate(5deg) scale(0.74);
}

.float-dot {
  position: absolute;
  width: 5px;
  height: 5px;
  background: #bfdbfe;
  border-radius: 50%;
}

.float-dot-a {
  top: 28px;
  right: 8px;
}

.float-dot-b {
  top: 100px;
  right: 30px;
  opacity: 0.55;
}

/* ---------- 快捷入口 ---------- */
.quick-card {
  position: relative;
  z-index: 3;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(58px, 1fr));
  gap: 14px 2px;
  margin: -26px 14px 0;
  padding: 18px 8px 14px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.08);
}

.quick-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 0;
  color: #3b4a60;
  font-family: inherit;
  background: none;
  border: 0;
}

.quick-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  color: #fff;
  font-size: 22px;
  border-radius: 14px;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.08);
}

.tone-0 { background: linear-gradient(145deg, #60a5fa, #2563eb); }
.tone-1 { background: linear-gradient(145deg, #fbbf24, #f59e0b); }
.tone-2 { background: linear-gradient(145deg, #fb7185, #e11d48); }
.tone-3 { background: linear-gradient(145deg, #34d399, #059669); }
.tone-4 { background: linear-gradient(145deg, #c084fc, #9333ea); }
.tone-5 { background: linear-gradient(145deg, #38bdf8, #0284c7); }

.quick-label {
  max-width: 100%;
  overflow: hidden;
  font-size: 11px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-badge {
  position: absolute;
  top: -3px;
  right: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  background: #ef4444;
  border: 2px solid #fff;
  border-radius: 999px;
}

/* ---------- 今日拜访 ---------- */
.today-card {
  margin: 14px;
  padding: 18px 16px 12px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(30, 64, 175, 0.05);
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-head h2 {
  position: relative;
  margin: 0;
  padding-left: 10px;
  color: #14213d;
  font-size: 16px;
  font-weight: 600;
}

.section-head h2::before {
  content: '';
  position: absolute;
  top: 3px;
  bottom: 3px;
  left: 0;
  width: 3px;
  background: #2563eb;
  border-radius: 2px;
}

.section-head > span {
  color: #94a3b8;
  font-size: 11px;
}

.pass-banner {
  display: flex;
  align-items: center;
  gap: 11px;
  margin-top: 14px;
  padding: 13px;
  background: linear-gradient(135deg, #eff6ff, #f8fbff);
  border: 1px solid #dbeafe;
  border-radius: 14px;
}

.pass-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  color: #fff;
  font-size: 20px;
  background: #2563eb;
  border-radius: 12px;
}

.alumni-icon {
  background: #059669;
}

.pass-copy {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.pass-copy strong {
  color: #14213d;
  font-size: 14px;
}

.pass-copy span {
  margin-top: 3px;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pass-arrow {
  color: #93c5fd;
}

.empty-state {
  padding: 6px 0 8px;
  text-align: center;
}

.empty-art {
  position: relative;
  width: 118px;
  height: 90px;
  margin: 10px auto 8px;
}

.empty-glow {
  position: absolute;
  right: 18px;
  bottom: 2px;
  left: 18px;
  height: 16px;
  background: rgba(37, 99, 235, 0.08);
  filter: blur(8px);
  border-radius: 50%;
}

.empty-sheet {
  position: absolute;
  display: grid;
  place-items: center;
  width: 58px;
  height: 68px;
  border-radius: 10px;
}

.empty-sheet-back {
  top: 8px;
  left: 24px;
  background: #e2e8f0;
  transform: rotate(-12deg);
}

.empty-sheet-front {
  top: 13px;
  left: 38px;
  color: #cbd5e1;
  font-size: 22px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  transform: rotate(5deg);
}

.empty-state p {
  margin: 0;
  color: #94a3b8;
  font-size: 13px;
}

.empty-state > span {
  display: block;
  margin-top: 4px;
  color: #cbd5e1;
  font-size: 10px;
}

.support {
  margin: 20px 0 6px;
  color: #b6c2d2;
  font-size: 10px;
  text-align: center;
  letter-spacing: 0.4px;
}

.actions {
  margin: 10px 16px 4px;
  text-align: center;
}

.logout-link {
  padding: 8px 14px;
  color: #94a3b8;
  font-family: inherit;
  font-size: 12px;
  background: none;
  border: 0;
}

.pass {
  padding: 18px 16px 10px;
  text-align: center;
}

.qr {
  width: 220px;
  height: 220px;
}

.pass-meta {
  margin: 8px 0 0;
  color: #61738c;
  font-size: 12px;
}

@media (max-width: 350px) {
  .hero-art {
    opacity: 0.55;
    right: -34px;
  }

  .hero-copy {
    max-width: 82%;
  }

  .hero h1 {
    font-size: 22px;
  }
}
</style>