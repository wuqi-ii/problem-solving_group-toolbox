import { http, type ApiResponse } from './http'

export interface UserView {
  id: string
  account: string
  nickname: string
  avatarUrl: string | null
}

export interface AuthView {
  accessToken: string
  tokenType: string
  user: UserView
}

export async function login(account: string, password: string) {
  const response = await http.post<ApiResponse<AuthView>>('/auth/login', { account, password })
  return response.data.data
}

export async function register(account: string, password: string, nickname: string) {
  const response = await http.post<ApiResponse<AuthView>>('/auth/register', { account, password, nickname })
  return response.data.data
}

export async function logout() {
  await http.post('/auth/logout')
}
