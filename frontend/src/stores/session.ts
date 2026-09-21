import { defineStore } from 'pinia'

export interface CurrentUser {
  id: string
  account: string
  nickname: string
  avatarUrl: string | null
}

function restoreUser(): CurrentUser | null {
  try {
    const stored = localStorage.getItem('team-toolbox-user')
    return stored ? JSON.parse(stored) as CurrentUser : null
  } catch {
    return null
  }
}

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('team-toolbox-token'),
    user: restoreUser(),
    activeGroupId: null as string | null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.user),
  },
  actions: {
    setAuth(token: string, user: CurrentUser) {
      this.token = token
      this.user = user
      localStorage.setItem('team-toolbox-token', token)
      localStorage.setItem('team-toolbox-user', JSON.stringify(user))
    },
    clearAuth() {
      this.token = null
      this.user = null
      this.activeGroupId = null
      localStorage.removeItem('team-toolbox-token')
      localStorage.removeItem('team-toolbox-user')
    },
    switchGroup(groupId: string | null) {
      this.activeGroupId = groupId
    },
  },
})
