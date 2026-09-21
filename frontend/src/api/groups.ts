import { http, type ApiResponse } from './http'

export interface GroupView {
  id: string
  name: string
  description: string | null
  leaderId: string
  myRole: 'LEADER' | 'ADMIN' | 'MEMBER'
}

export interface MemberView {
  userId: string
  account: string
  nickname: string
  role: string
  joinedAt: string
  permissions: string[]
}

export type PermissionType = 'MEMBER_MANAGE' | 'TASK_CREATE' | 'TASK_REVIEW' | 'FILE_MANAGE' | 'GROUP_ANNOUNCE'

export async function listGroups() {
  const response = await http.get<ApiResponse<GroupView[]>>('/groups')
  return response.data.data
}

export async function createGroup(name: string, description: string) {
  const response = await http.post<ApiResponse<GroupView>>('/groups', { name, description })
  return response.data.data
}

export async function listMembers(groupId: string) {
  const response = await http.get<ApiResponse<MemberView[]>>(`/groups/${groupId}/members`)
  return response.data.data
}

export async function createInvitation(groupId: string) {
  const response = await http.post<ApiResponse<{ code: string; expiresAt: string }>>(
    `/groups/${groupId}/invitations`, { validHours: 72, maxUses: 20 })
  return response.data.data
}

export async function joinGroup(code: string) {
  const response = await http.post<ApiResponse<GroupView>>('/groups/join', { code })
  return response.data.data
}

export async function grantPermission(groupId: string, userId: string, permission: PermissionType) {
  await http.post(`/groups/${groupId}/members/${userId}/permissions`, { permission })
}

export async function revokePermission(groupId: string, userId: string, permission: PermissionType) {
  await http.delete(`/groups/${groupId}/members/${userId}/permissions/${permission}`)
}
