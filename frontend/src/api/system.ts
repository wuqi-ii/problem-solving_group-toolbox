import type { ApiResponse } from './http'
import { http } from './http'

export interface HealthView {
  status: string
  service: string
  time: string
}

export async function getSystemHealth(): Promise<HealthView> {
  const response = await http.get<ApiResponse<HealthView>>('/system/health')
  return response.data.data
}
