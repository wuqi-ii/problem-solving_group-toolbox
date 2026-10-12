import { http, type ApiResponse } from './http'

export interface AuditLogView {
  id: string
  method: string
  path: string
  responseStatus: number
  clientIp: string
  occurredAt: string
}

export async function listAuditLogs() {
  const response = await http.get<ApiResponse<AuditLogView[]>>('/audit-logs')
  return response.data.data
}
