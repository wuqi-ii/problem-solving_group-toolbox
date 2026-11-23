import { defineStore } from 'pinia'
import {
  clearSessionAuth,
  getSessionActiveGroup,
  getSessionToken,
  getSessionUser,
  setSessionActiveGroup,
  setSessionAuth,
} from '@/utils/browserSession'

export interface CurrentUser {
  id: string
  account: string
  nickname: string
  avatarUrl: string | null
}

function restoreUser(): CurrentUser | null {
  try {
    const stored = getSessionUser()
    return stored ? JSON.parse(stored) as CurrentUser : null
  } catch {
    return null
  }
}

function restoreActiveGroupId(): string | null {
  return getSessionActiveGroup()
}

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: getSessionToken(),
    user: restoreUser(),
    activeGroupId: restoreActiveGroupId(),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token && state.user),
  },
  actions: {
    setAuth(token: string, user: CurrentUser) {
      this.token = token
      this.user = user
      setSessionAuth(token, JSON.stringify(user))
    },
    clearAuth() {
      this.token = null
      this.user = null
      this.activeGroupId = null
      clearSessionAuth()
    },
    switchGroup(groupId: string | null) {
      this.activeGroupId = groupId
      setSessionActiveGroup(groupId)
    },
  },
})
