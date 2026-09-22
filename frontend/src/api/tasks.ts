import { http, type ApiResponse } from './http'

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'SUBMITTED' | 'NEEDS_CHANGES' | 'COMPLETED' | 'CANCELLED'
export type TaskPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type ReviewDecision = 'APPROVED' | 'CHANGES_REQUESTED'

export interface TaskAssignee {
  userId: string
  nickname: string
}

export interface TaskSubmission {
  id: string
  versionNo: number
  content: string
  attachmentFileId: string | null
  attachmentName: string | null
  submittedBy: string
  submittedAt: string
  reviewDecision: ReviewDecision | null
  reviewComment: string | null
  reviewedAt: string | null
}

export interface TaskView {
  id: string
  groupId: string
  title: string
  description: string | null
  priority: TaskPriority
  status: TaskStatus
  createdBy: string
  dueAt: string | null
  assignees: TaskAssignee[]
  latestSubmission: TaskSubmission | null
  canStart: boolean
  canSubmit: boolean
  canReview: boolean
}

export interface CreateTaskPayload {
  title: string
  description: string
  priority: TaskPriority
  dueAt: string | null
  assigneeIds: string[]
}

export async function listTasks(groupId: string) {
  const response = await http.get<ApiResponse<TaskView[]>>(`/groups/${groupId}/tasks`)
  return response.data.data
}

export async function createTask(groupId: string, payload: CreateTaskPayload) {
  const response = await http.post<ApiResponse<TaskView>>(`/groups/${groupId}/tasks`, payload)
  return response.data.data
}

export async function startTask(taskId: string) {
  const response = await http.post<ApiResponse<TaskView>>(`/tasks/${taskId}/start`)
  return response.data.data
}

export async function submitTask(taskId: string, content: string, attachmentFileId: string) {
  const response = await http.post<ApiResponse<TaskView>>(`/tasks/${taskId}/submissions`, { content, attachmentFileId: attachmentFileId || null })
  return response.data.data
}

export async function reviewTask(taskId: string, decision: ReviewDecision, comment: string) {
  const response = await http.post<ApiResponse<TaskView>>(`/tasks/${taskId}/reviews`, { decision, comment: comment || null })
  return response.data.data
}
