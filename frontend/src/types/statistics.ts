// Statistics related types
export interface ArticleStats {
  articleId: number
  viewCount: number
  likeCount: number
  commentCount: number
  favoriteCount: number
}

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

export interface VisitStatistics {
  totalVisits: number
  dailyVisits: {
    date: string
    visits: number
  }[]
  trend: string
}

export interface CategoryStatistics {
  id: number
  name: string
  articleCount: number
}

export interface TagStatistics {
  id: number
  name: string
  color?: string
  articleCount: number
}
