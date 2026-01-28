import axios from '../utils/axios'
import type { Notification, NotificationListParams, PageResult } from '../types/notification'

export const notificationService = {
  // 获取未读消息数量
  getUnreadCount: () =>
    axios.get<number>('/notification/unread-count'),

  // 获取消息列表（分页）
  getList: (params: NotificationListParams = {}) =>
    axios.get<PageResult<Notification>>('/notification/list', { params }),

  // 标记消息为已读
  markAsRead: (id: number) =>
    axios.put(`/notification/${id}/read`),

  // 标记所有消息为已读
  markAllAsRead: () =>
    axios.put('/notification/read-all'),

  // 删除消息
  delete: (id: number) =>
    axios.delete(`/notification/${id}`)
}
