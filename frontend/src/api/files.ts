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
  canManage: boolean
}

export async function listFiles(groupId: string) {
  const response = await http.get<ApiResponse<FileView[]>>(`/groups/${groupId}/files`)
  return response.data.data
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

export async function downloadFile(file: Pick<FileView, 'id' | 'name'>) {
  const response = await http.get<Blob>(`/files/${file.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = file.name
  anchor.click()
  URL.revokeObjectURL(url)
}
