<template>
  <div class="page">
    <van-nav-bar title="智慧校园访客">
      <template #right>
        <span class="role-chip">{{ roleLabel }}</span>
      </template>
    </van-nav-bar>

    <div class="content">
      <van-loading v-if="!loaded" class="loading" />

      <template v-else>
        <van-cell-group v-for="group in groups" :key="group.key" inset :title="group.title" class="group">
          <van-cell
            v-for="entry in group.entries"
            :key="entry.to"
            :title="entry.title"
            is-link
            :to="entry.to"
          >
            <template #icon>
              <span class="entry-icon"><van-icon :name="entry.icon" /></span>
            </template>
          </van-cell>
        </van-cell-group>

        <van-empty v-if="groups.length === 0" description="当前身份暂无 H5 功能，请使用 Web 管理端" />
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

type MenuKind = 'VISITOR' | 'APPROVER' | 'GUARD' | 'DEPT' | 'NONE'

interface Entry {
  title: string
  to: string
  icon: string
}

interface MenuGroup {
  key: Exclude<MenuKind, 'NONE'>
  title: string
  entries: Entry[]
}

const ROLE_KIND: Record<number, MenuKind> = {
  3: 'GUARD',
  4: 'VISITOR',
  5: 'APPROVER',
  6: 'APPROVER',
  7: 'APPROVER',
  8: 'APPROVER',
  9: 'VISITOR',
  11: 'DEPT',
  12: 'APPROVER',
}

const router = useRouter()
const profile = ref<Profile>()
const loaded = ref(false)

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
  if (current.personType !== 6 && ![1, 2].includes(current.currentRole)) {
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
        { title: '我的待办', to: '/a/tasks', icon: 'todo-list-o' },
        { title: '历史审批', to: '/a/history', icon: 'clock-o' },
      ],
    },
    GUARD: {
      key: 'GUARD',
      title: '门卫',
      entries: [
        { title: '扫码核验', to: '/g/verify?mode=verify', icon: 'scan' },
        { title: '车主查询', to: '/g/verify?mode=query', icon: 'search' },
      ],
    },
    DEPT: {
      key: 'DEPT',
      title: '部门经办人',
      entries: [{ title: '批量预约', to: '/d/batch', icon: 'orders-o' }],
    },
    VISITOR: {
      key: 'VISITOR',
      title: '访客',
      entries: visitorEntries,
    },
  }

  return menuKinds.value.map((kind) => definitions[kind])
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
  background: transparent;
}
.content {
  padding: 8px 0 28px;
}
.role-chip {
  display: inline-block;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 3px 10px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #14508f;
  font-size: 12px;
  font-weight: 500;
}
.loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}
.group {
  margin-top: 14px;
}
.group :deep(.van-cell) {
  align-items: center;
}
.entry-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-right: 12px;
  border-radius: 10px;
  background: #edf4ff;
  color: #1677ff;
  font-size: 18px;
}
.actions {
  margin: 26px 16px 10px;
}
.actions :deep(.van-button) {
  border-color: #dbe8f8;
  background: rgba(255, 255, 255, 0.8);
  color: #4a6585;
}
</style>
