<template>
  <div class="comment-section">
    <!-- 评论数量 -->
    <div class="comment-header">
      <h3 class="comment-count">{{ totalComments }} 条评论</h3>
      <div class="sort-tabs">
        <div 
          class="sort-tab" 
          :class="{ active: activeSort === 'time' }" 
          @click="sortComments('time')"
        >
          按时间
        </div>
        <div 
          class="sort-tab" 
          :class="{ active: activeSort === 'hot' }" 
          @click="sortComments('hot')"
        >
          按热度
        </div>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list" v-if="!loading">
      <div 
        class="comment-item" 
        v-for="comment in comments" 
        :key="comment.id"
      >
        <div class="comment-avatar">
          <el-avatar :src="comment.avatar" size="40">{{ comment.nickname.charAt(0) }}</el-avatar>
        </div>
        <div class="comment-content">
          <div class="comment-header-info">
            <span class="comment-author">{{ comment.nickname }}</span>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
          </div>
          <div class="comment-text">{{ comment.content }}</div>
          <div class="comment-actions">
            <div class="action-item" @click="likeComment(comment.id)">
              <i :class="['el-icon', comment.liked ? 'el-icon-star-on' : 'el-icon-star-off']"></i>
              <span>{{ comment.likeCount }}</span>
            </div>
            <div class="action-item" @click="replyComment(comment)">
              <i class="el-icon-chat-dot-round"></i>
              <span>回复</span>
            </div>
            <div class="action-item" @click="deleteComment(comment.id)">
              <i class="el-icon-delete"></i>
              <span>删除</span>
            </div>
          </div>

          <!-- 回复列表 -->
          <div class="reply-list" v-if="comment.children && comment.children.length > 0">
            <div 
              class="reply-item" 
              v-for="reply in comment.children" 
              :key="reply.id"
            >
              <div class="reply-avatar">
                <el-avatar :src="reply.avatar" size="32">{{ reply.nickname.charAt(0) }}</el-avatar>
              </div>
              <div class="reply-content">
                <div class="reply-header-info">
                  <span class="reply-author">{{ reply.nickname }}</span>
                  <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                </div>
                <div class="reply-text">
                  <span v-if="reply.parentId > 0">@{{ getParentAuthor(reply.parentId) }}：</span>
                  {{ reply.content }}
                </div>
                <div class="reply-actions">
                  <div class="action-item" @click="likeComment(reply.id)">
                    <i :class="['el-icon', reply.liked ? 'el-icon-star-on' : 'el-icon-star-off']"></i>
                    <span>{{ reply.likeCount }}</span>
                  </div>
                  <div class="action-item" @click="replyComment(reply)">
                    <i class="el-icon-chat-dot-round"></i>
                    <span>回复</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 回复输入框 -->
          <div class="reply-input-container" v-if="replyToComment && replyToComment.id === comment.id">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="3"
              placeholder="写下你的回复..."
            ></el-input>
            <div class="reply-input-actions">
              <el-button type="primary" @click="submitReply">提交回复</el-button>
              <el-button @click="cancelReply">取消</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 加载更多按钮 -->
    <div class="load-more-container" v-if="!loading && hasMore && comments.length > 0">
      <el-button type="primary" @click="loadMoreComments" :loading="loading">
        加载更多评论
      </el-button>
    </div>
    
    <!-- 没有更多数据提示 -->
    <div class="no-more" v-if="!loading && !hasMore && comments.length > 0">
      没有更多评论了
    </div>

    <!-- 加载中 -->
    <div class="loading-container" v-if="loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 发表评论 -->
    <div class="comment-form">
      <h4 class="form-title">发表评论</h4>
      <el-form :model="commentForm" :rules="commentRules" ref="commentFormRef">
        <el-form-item prop="content">
          <el-input
            v-model="commentForm.content"
            type="textarea"
            :rows="4"
            placeholder="写下你的评论..."
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitComment" :loading="submitting">发表评论</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from '../utils/axios'

// Props
const props = defineProps<{
  articleId: number
}>()

// 评论数据
const comments = ref<any[]>([])
const totalComments = ref(0)
const loading = ref(false)
const submitting = ref(false)
const activeSort = ref('time')
const currentPage = ref(1)
const hasMore = ref(true)

// 发表评论表单
const commentForm = ref({
  content: ''
})

const commentRules = {
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { min: 1, max: 1000, message: '评论内容长度在 1 到 1000 个字符', trigger: 'blur' }
  ]
}

const commentFormRef = ref()

// 回复功能
const replyToComment = ref<any>(null)
const replyContent = ref('')

// 获取评论列表
const fetchComments = async (append = false) => {
  if (loading.value || (!append && !hasMore.value)) return
  
  loading.value = true
  try {
    const response = await axios.get('/api/comment/list', {
      params: {
        articleId: props.articleId,
        page: append ? currentPage.value + 1 : 1,
        size: 20,
        sortBy: activeSort.value
      }
    })
    
    const commentList = response.data
    
    if (append) {
      comments.value = [...comments.value, ...commentList]
      currentPage.value++
    } else {
      comments.value = commentList
      currentPage.value = 1
    }
    
    // 检查是否还有更多数据
    hasMore.value = commentList.length >= 20
    
    fetchCommentCount()
  } catch (error) {
    ElMessage.error('获取评论失败')
    console.error('获取评论失败:', error)
  } finally {
    loading.value = false
  }
}

// 删除评论
const deleteComment = async (commentId: number) => {
  try {
    await axios.delete(`/api/comment/${commentId}`)
    ElMessage.success('评论删除成功')
    fetchComments() // 重新获取评论列表
  } catch (error) {
    ElMessage.error('评论删除失败')
    console.error('删除评论失败:', error)
  }
}

// 获取评论数量
const fetchCommentCount = async () => {
  try {
    const response = await axios.get(`/api/comment/article/${props.articleId}/count`)
    totalComments.value = response.data
  } catch (error) {
    console.error('获取评论数量失败:', error)
  }
}

// 发表评论
const submitComment = async () => {
  if (!commentFormRef.value) return
  
  await (commentFormRef.value as any).validate(async (valid: boolean) => {
    if (valid) {
      submitting.value = true
      try {
        // 检查敏感词
        const sensitiveCheckResponse = await axios.post('/api/comment/check-sensitive', {
          content: commentForm.value.content
        })
        
        if (sensitiveCheckResponse.data) {
          ElMessage.warning('评论内容包含敏感词，请修改后重试')
          return
        }
        
        // 发表评论
        const response = await axios.post('/api/comment', {
          articleId: props.articleId,
          content: commentForm.value.content,
          parentId: 0
        })
        
        if (response.data) {
          ElMessage.success('评论发表成功')
          commentForm.value.content = ''
          fetchComments() // 重新获取评论列表
        }
      } catch (error) {
        ElMessage.error('评论发表失败')
        console.error('发表评论失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 排序评论
const sortComments = (sortBy: string) => {
  activeSort.value = sortBy
  hasMore.value = true // 重置是否还有更多数据
  fetchComments() // 重新获取第一页数据
}

// 加载更多评论
const loadMoreComments = () => {
  fetchComments(true)
}

// 点赞评论
const likeComment = async (commentId: number) => {
  try {
    // 检查是否已点赞
    const likeStatusResponse = await axios.get(`/api/comment/${commentId}/like-status`)
    
    if (likeStatusResponse.data) {
      // 取消点赞
      await axios.delete(`/api/comment/${commentId}/like`)
      ElMessage.success('取消点赞成功')
    } else {
      // 点赞
      await axios.post(`/api/comment/${commentId}/like`)
      ElMessage.success('点赞成功')
    }
    
    fetchComments() // 重新获取评论列表
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('点赞操作失败:', error)
  }
}

// 回复评论
const replyComment = (comment: any) => {
  replyToComment.value = comment
  replyContent.value = ''
}

// 取消回复
const cancelReply = () => {
  replyToComment.value = null
  replyContent.value = ''
}

// 提交回复
const submitReply = async () => {
  if (!replyToComment.value || !replyContent.value.trim()) {
    return
  }
  
  try {
    // 检查敏感词
    const sensitiveCheckResponse = await axios.post('/api/comment/check-sensitive', {
      content: replyContent.value
    })
    
    if (sensitiveCheckResponse.data) {
      ElMessage.warning('回复内容包含敏感词，请修改后重试')
      return
    }
    
    // 提交回复
    const response = await axios.post('/api/comment', {
      articleId: props.articleId,
      content: replyContent.value,
      parentId: replyToComment.value.id
    })
    
    if (response.data) {
      ElMessage.success('回复成功')
      cancelReply()
      fetchComments() // 重新获取评论列表
    }
  } catch (error) {
    ElMessage.error('回复失败')
    console.error('提交回复失败:', error)
  }
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString()
}

// 获取父评论作者
const getParentAuthor = (parentId: number) => {
  const parentComment = comments.value.find(comment => comment.id === parentId)
  return parentComment ? parentComment.nickname : ''
}

// 初始化
onMounted(() => {
  fetchComments()
})

// 监听文章ID变化
watch(() => props.articleId, () => {
  fetchComments()
})
</script>

<style scoped>
.comment-section {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.comment-count {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.sort-tabs {
  display: flex;
  gap: 10px;
}

.sort-tab {
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s;
}

.sort-tab:hover {
  background-color: #f0f0f0;
}

.sort-tab.active {
  color: #409eff;
  background-color: #ecf5ff;
  border-bottom: 2px solid #409eff;
}

.comment-list {
  margin-bottom: 30px;
}

.comment-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-content {
  flex: 1;
}

.comment-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-text {
  margin-bottom: 12px;
  line-height: 1.6;
}

.comment-actions {
  display: flex;
  gap: 20px;
  font-size: 12px;
  color: #909399;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: color 0.3s;
}

.action-item:hover {
  color: #409eff;
}

.reply-list {
  margin-top: 16px;
  margin-left: 52px;
}

.reply-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #fafafa;
  border-radius: 8px;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-content {
  flex: 1;
}

.reply-header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.reply-author {
  font-weight: 600;
  font-size: 13px;
}

.reply-time {
  font-size: 11px;
  color: #909399;
}

.reply-text {
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;
}

.reply-actions {
  display: flex;
  gap: 16px;
  font-size: 11px;
  color: #909399;
}

.reply-input-container {
  margin-top: 16px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 8px;
}

.reply-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  gap: 10px;
  color: #909399;
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.no-more {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 14px;
}

.comment-form {
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.form-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-form .el-button {
  margin-top: 10px;
}
</style>
