import axios from '../utils/axios'
import type { Comment, CommentListParams, CommentCreateRequest } from '../types/comment'

// FD-024: GET /comment/list 返回 Result<PageResult<CommentDTO>>，解包后为分页结构
export interface CommentListResult {
  items: Comment[]
  total: number
  page: number
  size: number
}

export const commentService = {
  // Comment CRUD
  create: (data: CommentCreateRequest) =>
    axios.post<number>('/comment', data),

  getList: async (params: CommentListParams): Promise<CommentListResult> => {
    const response = await axios.get<CommentListResult | Comment[]>('/comment/list', { params })
    // 兜底：旧版后端直接返回数组时转换为分页结构
    if (Array.isArray(response)) {
      return {
        items: response,
        total: response.length,
        page: params.page ?? 1,
        size: params.size ?? 10
      }
    }
    return response
  },
  
  getDetail: (id: number) => 
    axios.get<Comment>(`/comment/${id}`),
  
  delete: (id: number) => 
    axios.delete(`/comment/${id}`),

  // Comment like operations
  likeComment: (commentId: number) => 
    axios.post(`/comment/${commentId}/like`),
  
  unlikeComment: (commentId: number) => 
    axios.delete(`/comment/${commentId}/like`),
  
  checkLikeStatus: (commentId: number) =>
    axios.get<boolean>(`/comment/${commentId}/like-status`),

  batchCheckLikeStatus: (commentIds: number[]) =>
    axios.post<Record<number, boolean>>('/comment/like-status/batch', commentIds),
}
