import axios from '../utils/axios'
import type { 
  ArticleStats, 
  WebsiteStatisticsDTO, 
  VisitTrendDTO, 
  TopPageItem, 
  TrafficSourceItem,
  PageData 
} from '../types/statistics'

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
    axios.get<WebsiteStatisticsDTO>('/admin/statistics'),
  
  getVisitStats: (type: 'day' | 'week' | 'month') => 
    axios.get<VisitTrendDTO>('/admin/visit-statistics', { params: { type } }),

  // 网站访问统计 API (对应 WebsiteStatisticsController)
  
  // 获取网站总体统计信息
  getWebsiteOverview: () => 
    axios.get<WebsiteStatisticsDTO>('/statistics/website/overview'),
  
  // 获取访问趋势数据
  getVisitTrend: (startDate: string, endDate: string) => 
    axios.get<VisitTrendDTO[]>('/statistics/website/trend', { 
      params: { startDate, endDate } 
    }),
  
  // 获取今日访问统计
  getTodayStatistics: () => 
    axios.get<WebsiteStatisticsDTO>('/statistics/website/today'),
  
  // 获取本周访问统计
  getWeekStatistics: () => 
    axios.get<WebsiteStatisticsDTO>('/statistics/website/week'),
  
  // 获取本月访问统计
  getMonthStatistics: () => 
    axios.get<WebsiteStatisticsDTO>('/statistics/website/month'),
  
  // 获取热门页面排行
  getTopPages: (page: number = 1, size: number = 10) => 
    axios.get<PageData<TopPageItem>>('/statistics/website/top-pages', { 
      params: { page, size } 
    }),
  
  // 获取访问来源统计
  getTrafficSources: () => 
    axios.get<TrafficSourceItem[]>('/statistics/website/traffic-sources'),
  
  // 记录页面访问
  recordPageView: (pageUrl: string) => 
    axios.post('/statistics/website/record', null, { params: { pageUrl } }),
  
  // 清理过期统计数据
  cleanExpiredStatistics: (daysToKeep: number = 90) => 
    axios.delete('/statistics/website/clean', { params: { daysToKeep } })
}