// Common PageResult type
export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// Notification related types
export interface Notification {
  id: number
  userId: number
  senderId: number
  senderNickname: string
  senderAvatar?: string
  type: number // 1-文章点赞，2-文章评论，3-评论点赞，4-评论回复
  typeName: string
  targetId: number
  targetType: number // 1-文章，2-评论
  targetTitle: string
  content: string
  isRead: number // 0-未读，1-已读
  createTime: string
  updateTime: string
}

export interface NotificationListParams {
  page?: number
  size?: number
}
