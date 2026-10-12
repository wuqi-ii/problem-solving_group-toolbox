import { http, type ApiResponse } from './http'

export interface FileView {
  id: string
  groupId: string
  name: string
  contentType: string | null
  sizeBytes: number
  uploadedBy: string
  uploaderName: string
  createdAt: string
  category: FileCategory
  versionNo: number
  status: 'ACTIVE' | 'DELETED'
  deletedAt: string | null
  previewable: boolean
  canManage: boolean
}
export type FileCategory = 'GENERAL' | 'DOCUMENT' | 'IMAGE' | 'DATA' | 'ARCHIVE' | 'TASK_RESULT'
export interface FileQuotaView { usedBytes: number; limitBytes: number; remainingBytes: number }

export async function listFiles(groupId: string) {
  const response = await http.get<ApiResponse<FileView[]>>(`/groups/${groupId}/files`)
  return response.data.data
}
export async function listTrash(groupId: string) {
  return (await http.get<ApiResponse<FileView[]>>(`/groups/${groupId}/files/trash`)).data.data
}
export async function getFileQuota(groupId: string) {
  return (await http.get<ApiResponse<FileQuotaView>>(`/groups/${groupId}/files/quota`)).data.data
}

export async function downloadTaskResults(groupId: string) {
  const response = await http.get<Blob>(`/groups/${groupId}/task-results/archive`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = 'task-results.zip'
  anchor.click()
  URL.revokeObjectURL(url)
}

export async function uploadFile(groupId: string, file: File, onProgress?: (percent: number) => void) {
  const form = new FormData()
  form.append('file', file)
  const response = await http.post<ApiResponse<FileView>>(`/groups/${groupId}/files`, form, {
    onUploadProgress: (event) => {
      if (event.total) onProgress?.(Math.round(event.loaded * 100 / event.total))
    },
  })
  return response.data.data
}

export async function renameFile(fileId: string, name: string) {
  const response = await http.patch<ApiResponse<FileView>>(`/files/${fileId}`, { name })
  return response.data.data
}

export async function deleteFile(fileId: string) {
  await http.delete(`/files/${fileId}`)
}
export async function updateFileCategory(fileId: string, category: FileCategory) {
  return (await http.patch<ApiResponse<FileView>>(`/files/${fileId}/category`, { category })).data.data
}
export async function restoreFile(fileId: string) { return (await http.post<ApiResponse<FileView>>(`/files/${fileId}/restore`)).data.data }
export async function purgeFile(fileId: string) { await http.delete(`/files/${fileId}/purge`) }
export async function previewFile(file: Pick<FileView, 'id' | 'name'>) {
  const response = await http.get<Blob>(`/files/${file.id}/preview`, { responseType: 'blob' })
  return response.data
}

export async function downloadFile(file: Pick<FileView, 'id' | 'name'>) {
  const response = await http.get<Blob>(`/files/${file.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = file.name
  anchor.click()
  URL.revokeObjectURL(url)
}
