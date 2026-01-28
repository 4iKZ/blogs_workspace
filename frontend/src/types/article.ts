// Article related types
export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface Article {
  id: number | string
  title: string
  content: string
  summary?: string
  coverImage?: string
  status: number // 0-草稿，1-已发布
  allowComment: number // 0-不允许，1-允许
  viewCount: number
  likeCount: number
  commentCount: number
  favoriteCount: number
  authorId: number
  authorNickname: string
  authorAvatar?: string
  categoryId: number
  categoryName: string
  category?: Category // 改为可选，因为后端可能不返回
  tags?: Tag[]
  liked: boolean
  favorited: boolean
  hotScore?: number // 热度分数（排行榜用）
  createTime: string
  updateTime: string
  publishTime: string
}

export interface Category {
  id: number
  name: string
  description?: string
  sortOrder: number
  icon?: string
  articleCount?: number
}

export interface ArticleListParams {
  page?: number
  size?: number
  keyword?: string
  categoryId?: number
  status?: number
  authorId?: number
  sortBy?: string
}

export interface ArticleCreateRequest {
  title: string
  content: string
  summary?: string
  coverImage?: string
  categoryId: number
  status: number
  allowComment: number
}

export interface ArticleUpdateRequest extends ArticleCreateRequest { }

export interface ArticleStats {
  articleId: number
  viewCount: number
  likeCount: number
  commentCount: number
  favoriteCount: number
}

export interface Tag {
  id: number
  name: string
  description?: string
  color?: string
}
