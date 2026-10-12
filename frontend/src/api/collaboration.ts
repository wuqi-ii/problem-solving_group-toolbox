import { http, type ApiResponse } from './http'

export type CollaborationPostType = 'DISCUSSION' | 'TASK_COMMENT' | 'ANNOUNCEMENT'

export interface CollaborationPostView {
  id: string
  groupId: string
  taskId: string | null
  type: CollaborationPostType
  content: string
  createdBy: string
  authorName: string
  pinned: boolean
  canDelete: boolean
  canPin: boolean
  createdAt: string
}

export async function listDiscussions(groupId: string) {
  return (await http.get<ApiResponse<CollaborationPostView[]>>(`/groups/${groupId}/discussions`)).data.data
}

export async function createDiscussion(groupId: string, content: string) {
  return (await http.post<ApiResponse<CollaborationPostView>>(`/groups/${groupId}/discussions`, { content })).data.data
}

export async function listAnnouncements(groupId: string) {
  return (await http.get<ApiResponse<CollaborationPostView[]>>(`/groups/${groupId}/announcements`)).data.data
}

export async function createAnnouncement(groupId: string, content: string) {
  return (await http.post<ApiResponse<CollaborationPostView>>(`/groups/${groupId}/announcements`, { content })).data.data
}

export async function listTaskComments(taskId: string) {
  return (await http.get<ApiResponse<CollaborationPostView[]>>(`/tasks/${taskId}/comments`)).data.data
}

export async function createTaskComment(taskId: string, content: string) {
  return (await http.post<ApiResponse<CollaborationPostView>>(`/tasks/${taskId}/comments`, { content })).data.data
}

export async function setPostPinned(postId: string, pinned: boolean) {
  return (await http.patch<ApiResponse<CollaborationPostView>>(`/collaboration-posts/${postId}/pin`, { pinned })).data.data
}

export async function deletePost(postId: string) {
  await http.delete(`/collaboration-posts/${postId}`)
}
