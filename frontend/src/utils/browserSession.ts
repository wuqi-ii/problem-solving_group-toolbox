const TOKEN_KEY = 'team-toolbox-token'
const USER_KEY = 'team-toolbox-user'
const ACTIVE_GROUP_KEY = 'team-toolbox-active-group'
const LEGACY_KEYS = [TOKEN_KEY, USER_KEY, ACTIVE_GROUP_KEY] as const

// 旧版本把登录态放在 localStorage，导致同一浏览器的所有标签页共用账号。
// 升级后的第一个刷新标签页继承旧会话，随后清除共享副本；其他标签页需独立登录。
function migrateLegacySession() {
  if (!sessionStorage.getItem(TOKEN_KEY)) {
    for (const key of LEGACY_KEYS) {
      const value = localStorage.getItem(key)
      if (value !== null) sessionStorage.setItem(key, value)
    }
  }
  for (const key of LEGACY_KEYS) localStorage.removeItem(key)
}

migrateLegacySession()

export function getSessionToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function getSessionUser() {
  return sessionStorage.getItem(USER_KEY)
}

export function getSessionActiveGroup() {
  return sessionStorage.getItem(ACTIVE_GROUP_KEY)
}

export function setSessionAuth(token: string, serializedUser: string) {
  sessionStorage.setItem(TOKEN_KEY, token)
  sessionStorage.setItem(USER_KEY, serializedUser)
}

export function setSessionActiveGroup(groupId: string | null) {
  if (groupId) sessionStorage.setItem(ACTIVE_GROUP_KEY, groupId)
  else sessionStorage.removeItem(ACTIVE_GROUP_KEY)
}

export function clearSessionAuth() {
  for (const key of LEGACY_KEYS) sessionStorage.removeItem(key)
}
