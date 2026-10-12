import axios from 'axios'

import type { ApiResponse } from './http'

export function apiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError<ApiResponse<unknown>>(error)) return error.response?.data?.message ?? fallback
  return fallback
}

export function isDialogDismissal(error: unknown) {
  return error === 'cancel' || error === 'close'
}
