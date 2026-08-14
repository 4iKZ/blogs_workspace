import request from '../utils/axios'

export interface Author {
  id: number
  username: string
  nickname?: string
  avatar?: string
  followerCount: number
  followingCount?: number
  isFollowed?: boolean
}

/**
 * 关注用户
 */
export const follow = (userId: number) => {
  return request.post<void>(`/user/follow/${userId}`)
}

/**
 * 取消关注
 */
export const unfollow = (userId: number) => {
  return request.delete<void>(`/user/unfollow/${userId}`)
}

/**
 * 检查是否关注
 */
export const isFollowing = (userId: number) => {
  return request.get<boolean>(`/user/is-following/${userId}`)
}

/**
 * 获取作者排行榜
 */
export const getTopAuthors = (limit = 10, config: any = {}) => {
  return request.get<Author[]>('/user/top-authors', {
    params: { limit },
    ...config
  })
}

/**
 * 获取当前用户关注列表
 */
export const getFollowings = (page: number = 1, size: number = 10) => {
  return request.get<Author[]>('/user/followings', { params: { page, size } })
}

/**
 * 获取当前用户粉丝列表
 */
export const getFollowers = (page: number = 1, size: number = 10) => {
  return request.get<Author[]>('/user/followers', { params: { page, size } })
}

export interface AuthorsRankingParams {
  page?: number
  size?: number
  sortBy?: 'followers' | string
  order?: 'asc' | 'desc'
  signal?: AbortSignal
  noCache?: boolean
}

const authorsCache = new Map<string, { ts: number; data: Author[] }>()
const CACHE_TTL_MS = 60_000

// 惰性清理过期缓存条目，避免无界增长
const pruneAuthorsCache = () => {
  const now = Date.now()
  for (const [key, entry] of authorsCache) {
    if (now - entry.ts >= CACHE_TTL_MS) {
      authorsCache.delete(key)
    }
  }
}

export const getAuthorsRanking = async (params: AuthorsRankingParams = {}) => {
  const { size = 10, sortBy = 'followers', order = 'desc', signal, noCache } = params
  const key = JSON.stringify({ size, sortBy, order })
  pruneAuthorsCache()
  const cached = authorsCache.get(key)
  if (!noCache && cached && Date.now() - cached.ts < CACHE_TTL_MS) {
    return cached.data
  }
  // 后端 /user/top-authors 仅接受 limit，不支持 page/size/sortBy
  const list = await request.get<Author[]>('/user/top-authors', {
    params: { limit: size },
    signal
  })
  const sorted = [...list].sort((a, b) => {
    const af = a.followerCount || 0
    const bf = b.followerCount || 0
    return order === 'asc' ? af - bf : bf - af
  })
  if (!noCache) authorsCache.set(key, { ts: Date.now(), data: sorted })
  return sorted
}

export const authorService = {
  follow,
  unfollow,
  isFollowing,
  getTopAuthors,
  getAuthorsRanking,
  getFollowings,
  getFollowers
}
