<template>
  <div class="comment-section">
    <div class="section-header">
      <h3 class="section-title">
        评论
        <span class="section-count">{{ totalComments }}</span>
      </h3>
    </div>

    <div class="comment-form-shell">
      <CommentForm
        :article-id="articleId"
        @submit="loadComments"
      />
    </div>

    <div v-if="loading" class="loading">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else class="comment-list-wrapper">
      <div v-if="comments.length > 0" class="comment-list-header">
        <div class="sort-tabs">
          <button
            type="button"
            :class="['sort-tab', { active: sortMode === 'hot' }]"
            @click="changeSort('hot')"
          >
            最热
          </button>
          <span class="sort-divider"></span>
          <button
            type="button"
            :class="['sort-tab', { active: sortMode === 'time' }]"
            @click="changeSort('time')"
          >
            最新
          </button>
        </div>
      </div>

      <div v-if="comments.length > 0" class="comments-list">
        <CommentItem
          v-for="comment in comments"
          :key="comment.id"
          :comment="comment"
          :root-id="comment.id"
          :initial-liked="likeStatusMap[comment.id] || false"
          @delete="handleCommentDelete"
          @refresh="loadComments"
          @update:liked="handleLikeStatusChange"
          @update:likeCount="handleLikeCountChange"
        />

        <div v-if="total > pageSize" class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @size-change="loadComments"
            @current-change="loadComments"
          />
        </div>
      </div>

      <el-empty v-else description="暂无评论" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { toast } from '@/composables/useLuminaToast'
import { commentService } from '../../services/commentService'
import { useUserStore } from '../../store/user'
import type { Comment } from '../../types/comment'
import CommentForm from './CommentForm.vue'
import CommentItem from './CommentItem.vue'

interface Props {
  articleId: number | string
}

const props = defineProps<Props>()

const userStore = useUserStore()
const comments = ref<Comment[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalComments = ref(0)
const sortMode = ref<'time' | 'hot'>('time')
// Map to store like status for all comments: commentId -> isLiked
const likeStatusMap = ref<Record<number, boolean>>({})

// Extract all comment IDs (including nested children)
const extractAllCommentIds = (commentList: Comment[]): number[] => {
  const ids: number[] = []
  const extract = (list: Comment[]) => {
    for (const comment of list) {
      ids.push(comment.id)
      if (comment.children && comment.children.length > 0) {
        extract(comment.children)
      }
    }
  }
  extract(commentList)
  return ids
}

// Batch load like statuses for all comments
const loadLikeStatuses = async () => {
  if (!userStore.isLoggedIn) {
    likeStatusMap.value = {}
    return
  }

  try {
    const commentIds = extractAllCommentIds(comments.value)
    if (commentIds.length === 0) {
      likeStatusMap.value = {}
      return
    }

    const statuses = await commentService.batchCheckLikeStatus(commentIds)
    likeStatusMap.value = statuses
  } catch (error: any) {
    // Don't show error message for like status check failures - it's not critical
    console.error('Failed to load like statuses:', error)

    // Show a non-blocking notification for network errors
    const status = error.response?.status
    if (status !== 401 && !error.message?.includes('cancel')) {
      // Only show notification for actual network/server errors (not auth cancels)
      console.warn('Like status loading failed, showing all comments as not liked')
      // Don't show ElMessage as it's too intrusive - just log it
      // Users can still see and interact with comments normally
    }

    // Don't block comment loading if like status check fails
    likeStatusMap.value = {}
  }
}

// Calculate total number of comments including children
const countAllComments = (list: Comment[]): number => {
  let count = 0
  for (const comment of list) {
    count++
    if (comment.children && comment.children.length > 0) {
      count += countAllComments(comment.children)
    }
  }
  return count
}

const loadComments = async () => {
  loading.value = true
  try {
    const response = await commentService.getList({
      articleId: Number(props.articleId),
      page: currentPage.value,
      size: pageSize.value,
      status: 2, // Only show approved comments
      sortBy: sortMode.value
    })
    comments.value = response
    totalComments.value = countAllComments(response)

    // Load like statuses after comments are loaded (non-blocking)
    loadLikeStatuses().catch(err => {
      console.error('Background like status loading failed:', err)
    })
  } catch (error: any) {
    const status = error.response?.status
    const errorCode = error.response?.data?.code

    if (status === 401 || errorCode === 401) {
      // 未登录用户也能浏览评论，401 时静默处理，不显示通知
      console.log('未登录状态下加载评论失败，跳过')
    } else if (status === 403 || errorCode === 403) {
      toast.warning('没有权限查看评论')
    } else if (status === 404 || errorCode === 404) {
      toast.error('文章不存在或已被删除')
    } else if (status >= 500) {
      toast.error('服务器错误，请稍后重试')
    } else if (error.response?.data?.message) {
      toast.error(error.response.data.message)
    } else {
      toast.error('加载评论失败，请检查网络连接')
    }
  } finally {
    loading.value = false
  }
}

const handleCommentDelete = (commentId: number) => {
  // Remove comment from list
  const removeComment = (list: Comment[]): Comment[] => {
    return list.filter(comment => {
      if (comment.id === commentId) {
        return false
      }
      if (comment.children && comment.children.length > 0) {
        comment.children = removeComment(comment.children)
      }
      return true
    })
  }

  comments.value = removeComment(comments.value)
  totalComments.value = countAllComments(comments.value)
}

// Handle like status change from child component
const handleLikeStatusChange = (commentId: number, isLiked: boolean) => {
  likeStatusMap.value[commentId] = isLiked
}

// Handle like count change from child component
const handleLikeCountChange = (commentId: number, newCount: number) => {
  // Update the comment's like count in the local list
  const updateComment = (list: Comment[]): boolean => {
    for (const comment of list) {
      if (comment.id === commentId) {
        comment.likeCount = newCount
        return true
      }
      if (comment.children && comment.children.length > 0) {
        if (updateComment(comment.children)) {
          return true
        }
      }
    }
    return false
  }
  updateComment(comments.value)
}

const changeSort = (mode: 'time' | 'hot') => {
  if (sortMode.value === mode || loading.value) {
    return
  }
  sortMode.value = mode
  currentPage.value = 1
  comments.value = []
  likeStatusMap.value = {}
  totalComments.value = 0
  loadComments()
}

// 监听 articleId 变化，当切换文章时重新加载评论
watch(() => props.articleId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    // 重置分页到第一页
    currentPage.value = 1
    // 清空现有评论和点赞状态
    comments.value = []
    likeStatusMap.value = {}
    total.value = 0
    totalComments.value = 0
    // 重新加载评论
    loadComments()
  }
}, { immediate: false }) // 不使用 immediate，因为 onMounted 会处理初始加载

onMounted(() => {
  loadComments()
})
</script>

<style scoped>
.comment-section {
  margin-top: 40px;
  padding: 24px;
  background: var(--bg-primary);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.section-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--color-blue-600);
  font-size: 13px;
  font-weight: 600;
}

.comment-form-shell {
  margin-bottom: 24px;
  padding: 18px;
  border-radius: 14px;
  background: var(--bg-card);
}

.comment-list-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-list-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.sort-tabs {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px;
  border-radius: 999px;
  background: var(--bg-card);
}

.sort-tab {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  padding: 0;
  cursor: pointer;
}

.sort-tab.active {
  color: var(--color-blue-600);
}

.sort-divider {
  width: 1px;
  height: 12px;
  background: var(--border-color);
}

.loading {
  padding: 20px 0;
}

.comments-list {
  border-radius: 16px;
  overflow: hidden;
  background: var(--bg-card);
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .comment-section {
    margin-top: 24px;
    padding: 16px;
    border-radius: 14px;
  }

  .section-header {
    margin-bottom: 14px;
  }

  .section-title {
    font-size: 18px;
    gap: 8px;
  }

  .section-count {
    min-width: 24px;
    height: 24px;
    padding: 0 8px;
    font-size: 12px;
  }

  .comment-form-shell {
    margin-bottom: 16px;
    padding: 12px;
    border-radius: 12px;
  }

  .comment-list-wrapper {
    gap: 12px;
  }

  .comment-list-header {
    justify-content: flex-start;
  }

  .sort-tabs {
    width: 100%;
    justify-content: center;
  }

  .pagination {
    margin-top: 16px;
    padding-top: 16px;
  }
}

@media (max-width: 480px) {
  .comment-section {
    padding: 12px;
  }

  .section-title {
    font-size: 16px;
  }

  .comment-form-shell {
    padding: 10px;
  }

  .sort-tabs {
    gap: 10px;
  }
}
</style>
