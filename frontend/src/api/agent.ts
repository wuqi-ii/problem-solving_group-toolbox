import { http, type ApiResponse } from './http'

export interface AgentToolDescriptor {
  name: string
  description: string
  riskLevel: string
  confirmationRequired: boolean
}

export interface AgentCapabilitiesView {
  enabled: boolean
  provider: string
  stage: string
  plannedTools: AgentToolDescriptor[]
}

export async function getAgentCapabilities() {
  return (await http.get<ApiResponse<AgentCapabilitiesView>>('/agent/capabilities')).data.data
}
