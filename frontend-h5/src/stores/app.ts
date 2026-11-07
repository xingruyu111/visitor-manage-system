import { defineStore } from 'pinia'

export type RoleCode = 'VISITOR' | 'APPROVER' | 'GUARD'

export const useAppStore = defineStore('app', {
  state: () => ({
    role: 'VISITOR' as RoleCode,
    token: '',
  }),
  actions: {
    setRole(role: RoleCode) {
      this.role = role
    },
    setToken(token: string) {
      this.token = token
    },
  },
})
