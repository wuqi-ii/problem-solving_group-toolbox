import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15_000,
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('team-toolbox-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('team-toolbox-token')
      localStorage.removeItem('team-toolbox-user')
      if (!window.location.pathname.startsWith('/login')) window.location.assign('/login')
    }
    return Promise.reject(error)
  },
)
