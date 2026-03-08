// Statistics related types

// 文章统计
export interface ArticleStats {
  articleId: number
  viewCount: number
  likeCount: number
  commentCount: number
  favoriteCount: number
}

// 网站总体统计 (管理后台首页)
export interface WebsiteStatistics {
  totalArticles: number
  totalUsers: number
  totalComments: number
  totalViews: number
  todayViews: number
  recentArticles: {
    id: number
    title: string
    viewCount: number
    createTime: string
  }[]
}

// 访问统计 DTO（对应后端 WebsiteStatisticsDTO）
export interface WebsiteStatisticsDTO {
  totalPageViews: number
  totalUniqueVisitors: number
  todayPageViews: number
  todayUniqueVisitors: number
  yesterdayPageViews: number
  yesterdayUniqueVisitors: number
  averageVisitDuration?: number
  bounceRate?: number
  statisticsDate: string
  createTime?: string
  updateTime?: string
}

// 访问趋势 DTO（对应后端 VisitTrendDTO）
export interface VisitTrendDTO {
  date: string
  pageViews: number
  uniqueVisitors: number
  newVisitors: number
  returningVisitors: number
  averageVisitDuration?: number
  bounceRate?: number
  createTime?: string
  updateTime?: string
}

// 访问统计（旧版）
export interface VisitStatistics {
  totalVisits: number
  dailyVisits: {
    date: string
    visits: number
  }[]
  trend: string
}

// 分类统计
export interface CategoryStatistics {
  id: number
  name: string
  articleCount: number
}

// 标签统计
export interface TagStatistics {
  id: number
  name: string
  color?: string
  articleCount: number
}

// 热门页面统计
export interface TopPageItem {
  page_url: string
  visit_count: number
  unique_visitor: number
}

// 访问来源统计
export interface TrafficSourceItem {
  source_type: string
  source_name: string
  visit_count: number
  unique_visitor: number
}

// 分页数据
export interface PageData<T> {
  page: number
  size: number
  total: number
  records: T[]
}