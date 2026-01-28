import axios from '../utils/axios'
import type { Comment, CommentListParams, CommentCreateRequest } from '../types/comment'

export const commentService = {
  // Comment CRUD
  create: (data: CommentCreateRequest) => 
    axios.post<number>('/comment', data),
  
  getList: (params: CommentListParams) => 
    axios.get<Comment[]>('/comment/list', { params }),
  
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

  // Admin review
  reviewComment: (commentId: number, status: number) => 
    axios.put(`/comment/${commentId}/review`, null, { params: { status } })
}
