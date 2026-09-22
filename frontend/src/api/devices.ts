import { http, type ApiResponse } from './http'

export interface DeviceItemView {
  id: string; type: 'TEXT' | 'LINK' | 'FILE'; content: string | null; fileName: string | null
  sizeBytes: number | null; sourceDevice: string | null; createdAt: string
}
export async function listDeviceItems() { return (await http.get<ApiResponse<DeviceItemView[]>>('/device-items')).data.data }
export async function createDeviceText(type: 'TEXT' | 'LINK', content: string, sourceDevice: string) {
  return (await http.post<ApiResponse<DeviceItemView>>('/device-items/text', { type, content, sourceDevice })).data.data
}
export async function createDeviceFile(file: File, sourceDevice: string) {
  const form = new FormData(); form.append('file', file)
  return (await http.post<ApiResponse<DeviceItemView>>('/device-items/files', form, { params: { sourceDevice } })).data.data
}
export async function deleteDeviceItem(id: string) { await http.delete(`/device-items/${id}`) }
export async function downloadDeviceFile(item: DeviceItemView) {
  const response = await http.get<Blob>(`/device-items/${item.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data); const anchor = document.createElement('a')
  anchor.href = url; anchor.download = item.fileName ?? '文件'; anchor.click(); URL.revokeObjectURL(url)
}
