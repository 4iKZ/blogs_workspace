import axios from '../utils/axios'
import type { ArticleStats, WebsiteStatistics, VisitStatistics } from '../types/statistics'

export const statisticsService = {
  // Article statistics
  getArticleStats: (articleId: number) => 
    axios.get<ArticleStats>(`/statistics/article/${articleId}`),
  
  incrementViewCount: (articleId: number) => 
    axios.post(`/statistics/article/view/${articleId}`),
  
  getHotArticlesStats: (limit: number = 10) => 
    axios.get<ArticleStats[]>('/statistics/article/hot', { params: { limit } }),
  
  getTopArticlesStats: (limit: number = 10) => 
    axios.get<ArticleStats[]>('/statistics/article/top', { params: { limit } }),
  
  getRecommendedArticlesStats: (limit: number = 10) => 
    axios.get<ArticleStats[]>('/statistics/article/recommended', { params: { limit } }),

  // Website statistics (admin)
  getWebsiteStats: () => 
    axios.get<WebsiteStatistics>('/admin/statistics'),
  
  getVisitStats: (type: 'day' | 'week' | 'month') => 
    axios.get<VisitStatistics>('/admin/visit-statistics', { params: { type } })
}
