import axios from '../utils/axios'
import type { Category } from '../types/article'

export interface CreateCategoryRequest {
  name: string
  description?: string
  sortOrder?: number
  icon?: string
  status?: number
}

export interface UpdateCategoryRequest {
  name?: string
  description?: string
  sortOrder?: number
  icon?: string
  status?: number
}

export const categoryService = {
  /**
   * 获取分类列表
   */
  getList: () =>
    axios.get<Category[]>('/category/list'),

  /**
   * 根据ID获取分类
   */
  getById: (categoryId: number) =>
    axios.get<Category>(`/category/${categoryId}`),

  /**
   * 创建分类（管理员）
   */
  create: (data: CreateCategoryRequest) =>
    axios.post<Category>('/category', data),

  /**
   * 更新分类（管理员）
   */
  update: (categoryId: number, data: UpdateCategoryRequest) =>
    axios.put<Category>(`/category/${categoryId}`, data),

  /**
   * 删除分类（管理员）
   */
  delete: (categoryId: number) =>
    axios.delete(`/category/${categoryId}`),

  /**
   * 获取分类下的文章数量
   */
  getArticleCount: (categoryId: number) =>
    axios.get<number>(`/category/${categoryId}/count`)
}
