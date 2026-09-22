import { http, type ApiResponse } from './http'

export interface TransferView {
  id: string
  fileName: string
  sizeBytes: number
  pickupCode: string | null
  status: string
  expiresAt: string
  maxDownloads: number
  downloadCount: number
  createdAt: string
}

export interface PickupView { fileName: string; sizeBytes: number; expiresAt: string; remainingDownloads: number }

export async function createTransfer(file: File, validHours: number, maxDownloads: number) {
  const form = new FormData(); form.append('file', file)
  const response = await http.post<ApiResponse<TransferView>>('/transfers', form, { params: { validHours, maxDownloads } })
  return response.data.data
}
export async function listTransfers() { return (await http.get<ApiResponse<TransferView[]>>('/transfers')).data.data }
export async function cancelTransfer(id: string) { await http.delete(`/transfers/${id}`) }
export async function inspectPickup(code: string) { return (await http.get<ApiResponse<PickupView>>(`/pickup/${code}`)).data.data }
export async function downloadPickup(code: string, fileName: string) {
  const response = await http.get<Blob>(`/pickup/${code}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data); const anchor = document.createElement('a')
  anchor.href = url; anchor.download = fileName; anchor.click(); URL.revokeObjectURL(url)
}
