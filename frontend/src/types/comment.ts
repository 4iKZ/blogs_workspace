// Comment related types
export interface Comment {
  id: number
  articleId: number
  parentId: number
  content: string
  userId: number
  nickname: string
  email?: string
  website?: string
  avatar?: string
  status: number // 历史字段，仅展示用途
  likeCount: number
  replyCount: number
  createTime: string
  updateTime: string
  children: Comment[]
  liked: boolean // 是否已点赞
  replyToCommentId?: number
  replyToUserId?: number
  replyToNickname?: string
}

export interface CommentListParams {
  articleId: number
  page?: number
  size?: number
  status?: number
  sortBy?: string
  sort?: string
  order?: string
}

export interface CommentCreateRequest {
  articleId: number
  parentId?: number
  content: string
  nickname?: string
  email?: string
  website?: string
  replyToCommentId?: number
}

export interface UserLike {
  id: number
  userId: number
  articleId: number
  createdAt: string
  article: {
    id: number
    title: string
    content: string
    summary: string
    status: number
    viewCount: number
    likeCount: number
    commentCount: number
    favoriteCount: number
    authorId: number
    categoryId: number
    createTime: string
    updateTime: string
    publishTime: string
    coverImage?: string
  }
}

export interface UserFavorite {
  favoriteId: number
  userId: number
  articleId: number
  createdAt: string
  article: {
    id: number
    title: string
    content: string
    summary: string
    status: number
    viewCount: number
    likeCount: number
    commentCount: number
    favoriteCount: number
    authorId: number
    categoryId: number
    createTime: string
    updateTime: string
    publishTime: string
    coverImage?: string
  }
}
