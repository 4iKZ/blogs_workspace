import axios from '../utils/axios'
import type { Article } from '../types/article'
import type { Comment } from '../types/comment'

export interface AdminArticleQuery {
  page?: number
  size?: number
  keyword?: string
  status?: number
  authorId?: number
}

export interface AdminCommentQuery {
  page?: number
  size?: number
  status?: number
  articleId?: number
  userId?: number
  keyword?: string
}

export interface SystemConfig {
  siteName?: string
  siteDescription?: string
  siteKeywords?: string
  allowRegister?: boolean
  commentAudit?: boolean
  maxUploadSize?: number
}

export interface AdminStatistics {
  totalArticles: number
  totalUsers: number
  publishedArticles: number
  draftArticles: number
  activeUsers: number
}

export const adminService = {
  // ===== 文章管理 =====
  /**
   * 获取所有文章（管理员）
   */
  getArticles: (params: AdminArticleQuery) =>
    axios.get<Article[]>('/admin/articles', { params }),

  /**
   * 更新文章状态
   */
  updateArticleStatus: (articleId: number, status: number) =>
    axios.put(`/admin/articles/${articleId}/status`, { status }),

  /**
   * 删除文章（管理员）
   */
  deleteArticle: (articleId: number) =>
    axios.delete(`/admin/articles/${articleId}`),

  // ===== 评论管理 =====
  /**
   * 获取所有评论（管理员）
   */
  getComments: (params: AdminCommentQuery) =>
    axios.get<Comment[]>('/admin/comments', { params }),

  /**
   * 审核评论
   */
  reviewComment: (commentId: number, status: number) =>
    axios.put(`/comment/${commentId}/review`, { status }),

  /**
   * 删除评论（管理员）
   */
  deleteComment: (commentId: number) =>
    axios.delete(`/comment/${commentId}`),

  // ===== 系统配置 =====
  /**
   * 获取系统配置
   */
  getSystemConfig: () =>
    axios.get<SystemConfig>('/system/config/all'),

  /**
   * 更新系统配置
   */
  updateSystemConfig: (config: SystemConfig) =>
    axios.put('/system/config', config),

  // ===== 统计信息 =====
  /**
   * 获取管理员统计数据
   */
  getStatistics: () =>
    axios.get<AdminStatistics>('/admin/statistics'),

  /**
   * 获取访问统计
   */
  getVisitStatistics: (type: 'day' | 'week' | 'month') =>
    axios.get('/admin/visit-statistics', { params: { type } }),

  // ===== 缓存管理 =====
  /**
   * 清除缓存
   */
  clearCache: () =>
    axios.post<{ clearedCount: number }>('/admin/cache/clear')
}
