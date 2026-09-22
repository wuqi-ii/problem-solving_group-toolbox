import { http, type ApiResponse } from './http'

export interface NotificationView {
  id: string; type: string; title: string; content: string; targetPath: string | null; read: boolean; createdAt: string
}
export async function listNotifications() { return (await http.get<ApiResponse<NotificationView[]>>('/notifications')).data.data }
export async function unreadCount() { return (await http.get<ApiResponse<number>>('/notifications/unread-count')).data.data }
export async function markNotificationRead(id: string) { await http.patch(`/notifications/${id}/read`) }
export async function markAllNotificationsRead() { await http.patch('/notifications/read-all') }
