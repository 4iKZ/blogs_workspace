import { defineStore } from 'pinia'
import { notificationService } from '../services/notificationService'
import type { Notification, PageResult } from '../types/notification'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
    notifications: [] as Notification[],
    loading: false,
    total: 0,
    currentPage: 1,
    pageSize: 20
  }),

  getters: {
    hasUnread: (state) => state.unreadCount > 0,
    totalPages: (state) => Math.ceil(state.total / state.pageSize) || 0,
    hasNextPage: (state) => state.currentPage * state.pageSize < state.total,
    hasPreviousPage: (state) => state.currentPage > 1
  },

  actions: {
    // 获取未读消息数量
    async fetchUnreadCount() {
      try {
        const count = await notificationService.getUnreadCount()
        this.unreadCount = count
        return count
      } catch (error) {
        console.error('获取未读消息数量失败:', error)
        return 0
      }
    },

    // 获取通知列表（分页）
    async fetchNotifications(params: { page?: number; size?: number } = {}) {
      try {
        this.loading = true
        const response: PageResult<Notification> = await notificationService.getList({
          page: params.page || this.currentPage,
          size: params.size || this.pageSize
        })

        this.notifications = response.items
        this.total = response.total
        this.currentPage = response.page
        this.pageSize = response.size

        return response
      } catch (error) {
        console.error('加载通知列表失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    // 标记消息为已读
    async markAsRead(id: number) {
      try {
        await notificationService.markAsRead(id)
        // 更新本地状态
        const notification = this.notifications.find(n => n.id === id)
        if (notification && notification.isRead === 0) {
          notification.isRead = 1
          this.unreadCount = Math.max(0, this.unreadCount - 1)
        }
      } catch (error) {
        console.error('标记消息已读失败:', error)
        throw error
      }
    },

    // 标记所有消息为已读
    async markAllAsRead() {
      try {
        await notificationService.markAllAsRead()
        this.notifications.forEach(n => n.isRead = 1)
        this.unreadCount = 0
      } catch (error) {
        console.error('标记所有消息已读失败:', error)
        throw error
      }
    },

    // 删除消息
    async deleteNotification(id: number) {
      try {
        await notificationService.delete(id)
        const index = this.notifications.findIndex(n => n.id === id)
        if (index > -1) {
          const notification = this.notifications[index]
          if (notification.isRead === 0) {
            this.unreadCount = Math.max(0, this.unreadCount - 1)
          }
          this.notifications.splice(index, 1)
          this.total = Math.max(0, this.total - 1)
        }
      } catch (error) {
        console.error('删除消息失败:', error)
        throw error
      }
    },

    // 增加未读计数（用于实时更新）
    incrementUnread(count: number = 1) {
      this.unreadCount += count
    },

    // 减少未读计数
    decrementUnread(count: number = 1) {
      this.unreadCount = Math.max(0, this.unreadCount - count)
    },

    // 清空通知（退出登录时调用）
    clearNotifications() {
      this.unreadCount = 0
      this.notifications = []
      this.loading = false
      this.total = 0
      this.currentPage = 1
    }
  }
})
