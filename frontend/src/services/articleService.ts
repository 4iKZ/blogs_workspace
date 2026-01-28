import axios from '../utils/axios'
import type { Article, ArticleListParams, ArticleCreateRequest, ArticleUpdateRequest, PageResult } from '../types/article'

/**
 * 用户点赞DTO
 */
export interface UserLikeDTO {
  id: number
  userId: number
  articleId: number
  createdAt: string
  article?: Article
}

export const articleService = {
  // Article CRUD
  getList: (params: ArticleListParams) =>
    axios.get<PageResult<Article>>('/article/list', { params }),

  getDetail: (id: number | string) =>
    axios.get<Article>(`/article/${id}`),

  create: (data: ArticleCreateRequest) =>
    axios.post<number>('/article/publish', data),

  update: (id: number | string, data: ArticleUpdateRequest) =>
    axios.put(`/article/${id}`, data),

  delete: (id: number | string) =>
    axios.delete(`/article/${id}`),

  // Like operations (moved to UserLikeController for proper user_likes table persistence)
  likeArticle: (articleId: number) =>
    axios.post(`/user/like/${articleId}`),

  unlikeArticle: (articleId: number) =>
    axios.delete(`/user/like/${articleId}`),

  checkLikeStatus: (articleId: number) =>
    axios.get<boolean>(`/user/like/${articleId}/check`),

  getUserLikedArticles: (page: number = 1, size: number = 10) =>
    axios.get<PageResult<UserLikeDTO>>('/user/like/list', { params: { page, size } }),

  getLikeCount: () =>
    axios.get<number>('/user/like/count'),

  // Favorite operations
  favoriteArticle: (articleId: number) =>
    axios.post(`/user/favorite/${articleId}`),

  unfavoriteArticle: (articleId: number) =>
    axios.delete(`/user/favorite/${articleId}`),

  checkFavoriteStatus: (articleId: number) =>
    axios.get<boolean>(`/user/favorite/${articleId}/check`),

  getUserFavorites: (page: number = 1, size: number = 10) =>
    axios.get<PageResult<any>>('/user/favorite/list', { params: { page, size } }),

  getFavoriteCount: () =>
    axios.get<number>('/user/favorite/count'),

  // Hot & recommended
  getHotArticles: (limit: number = 10, type: 'day' | 'week' = 'week', config: any = {}) =>
    axios.get<Article[]>('/article/hot', { params: { limit, type }, ...config }),

  getRecommendedArticles: (limit: number = 10, config: any = {}) =>
    axios.get<Article[]>('/article/recommended', { params: { limit }, ...config }),

  // By category/tag
  getByCategory: (categoryId: number, page: number = 1, size: number = 10) =>
    axios.get<PageResult<Article>>(`/article/category/${categoryId}`, { params: { page, size } }),

  getByTag: (tagId: number, page: number = 1, size: number = 10) =>
    axios.get<PageResult<Article>>(`/article/tag/${tagId}`, { params: { page, size } }),

  // Search
  search: (keyword: string, page: number = 1, size: number = 10) =>
    axios.get<PageResult<Article>>('/article/search', { params: { keyword, page, size } }),

  // User articles
  getUserArticles: (userId: number, page: number = 1, size: number = 10, status?: string) =>
    axios.get<PageResult<Article>>(`/article/user/${userId}`, { params: { page, size, status } }),

  // Following articles
  getFollowingArticles: (params: ArticleListParams) =>
    axios.get<PageResult<Article>>('/article/following', { params }),

  // Upload cover
  uploadCover: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return axios.post<string>('/article/upload-cover', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
