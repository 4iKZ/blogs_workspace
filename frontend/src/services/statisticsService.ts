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

  // 网站访问统计 API (对应 WebsiteStatisticsController)

  // 获取网站总体统计信息
  getWebsiteOverview: () =>
    axios.get<WebsiteStatisticsDTO>('/statistics/website/overview'),

  // 获取访问趋势数据
  getVisitTrend: (startDate: string, endDate: string) =>
    axios.get<VisitTrendDTO[]>('/statistics/website/trend', {
      params: { startDate, endDate }
    }),

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
    axios.post('/statistics/website/record', null, { params: { pageUrl } })
}
