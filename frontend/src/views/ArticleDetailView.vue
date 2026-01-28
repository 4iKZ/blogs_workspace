<template>
  <Layout>
    <div class="article-detail">
      <!-- 文章标题和元信息 -->
      <div class="article-header">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
          <span class="author">
            <el-avatar :size="24" :src="article.authorAvatar || ''">
              {{ article.authorNickname?.charAt(0) }}
            </el-avatar>
            {{ article.authorNickname }}
            <el-button 
              v-if="userStore.userInfo?.id !== article.authorId"
              type="primary" 
              link 
              size="small" 
              :loading="followLoading"
              @click="handleFollow"
            >
              {{ isFollowed ? '已关注' : '关注' }}
            </el-button>
          </span>
          <span class="time">{{ formatDate(article.publishTime) }}</span>
          <span class="category">
            <router-link :to="`/category/${article.categoryId}`">{{ article.categoryName }}</router-link>
          </span>
        </div>
      </div>
      
      <!-- 文章内容 -->
      <div class="article-content">
        <!-- 使用 md-editor-v3 预览组件渲染 Markdown -->
        <MdPreview 
          :model-value="article.content" 
          :theme="currentTheme"
          preview-theme="github"
          code-theme="github"
        />
      </div>
      

      
      <!-- 文章操作按钮 -->
      <div class="article-actions">
        <LikeButton
          :article-id="article.id"
          :initial-liked="article.liked"
          :initial-count="article.likeCount"
          @update="handleLikeUpdate"
        />
        <FavoriteButton
          :article-id="article.id"
          :initial-favorited="article.favorited"
          :initial-count="article.favoriteCount"
          @update="handleFavoriteUpdate"
        />
        <el-button type="info" :icon="Share" @click="handleShare" size="small">
          分享
        </el-button>
        
        <!-- 管理员或作者操作按钮 -->
        <div v-if="canManageArticle" class="admin-actions">
          <el-button type="warning" :icon="Edit" @click="handleEdit" size="small">
            编辑文章
          </el-button>
          <el-button type="danger" :icon="Delete" @click="handleDelete" size="small">
            删除文章
          </el-button>
        </div>
      </div>
      
      <!-- 分隔线 -->
      <div class="divider"></div>
      
      <!-- 评论区 -->
      <CommentSection v-if="Number(article.id) > 0" :article-id="Number(article.id)" />
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Share, Edit, Delete } from '@element-plus/icons-vue'
import { MdPreview } from 'md-editor-v3'
import type { Themes } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import Layout from '../components/Layout.vue'
import LikeButton from '../components/article/LikeButton.vue'
import FavoriteButton from '../components/article/FavoriteButton.vue'
import CommentSection from '../components/comment/CommentSection.vue'
import { articleService } from '../services/articleService'
import { authorService } from '../services/authorService'
import axios from '../utils/axios'
import { statisticsService } from '../services/statisticsService'
import { useUserStore } from '../store/user'
import type { Article } from '../types/article'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 文章ID
const articleId = ref(Number(route.params.id))

// 主题状态管理
const currentTheme = ref<Themes>('light')

// 初始化主题
const initTheme = () => {
  if (localStorage.theme === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    currentTheme.value = 'dark'
  } else {
    currentTheme.value = 'light'
  }
}

// 监听主题变化
const handleThemeChange = (e: MediaQueryListEvent) => {
  if (e.matches) {
    currentTheme.value = 'dark'
  } else {
    currentTheme.value = 'light'
  }
}

// 文章数据
const article = ref<Article>({
  id: 0,
  title: '',
  content: '',
  summary: '',
  coverImage: '',
  status: 0,
  allowComment: 1,
  viewCount: 0,
  likeCount: 0,
  commentCount: 0,
  favoriteCount: 0,
  authorId: 0,
  authorNickname: '',
  authorAvatar: '',
  categoryId: 0,
  categoryName: '',
  category: { id: 0, name: '', description: '', sortOrder: 0 },
  liked: false,
  favorited: false,
  createTime: '',
  updateTime: '',
  publishTime: ''
})

// 关注状态
const isFollowed = ref(false)
const followLoading = ref(false)

// 检查关注状态
const checkFollowStatus = async () => {
  if (userStore.isLoggedIn && article.value.authorId) {
    // 不关注自己
    if (userStore.userInfo?.id === article.value.authorId) return
    
    try {
      const status = await authorService.isFollowing(article.value.authorId)
      isFollowed.value = status
    } catch (error) {
      console.error('检查关注状态失败:', error)
    }
  }
}

// 处理关注/取消关注
const handleFollow = async () => {
  // 未登录跳转登录 - 统一交互行为：先提示再跳转
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (followLoading.value) return
  followLoading.value = true

  try {
    if (isFollowed.value) {
      await authorService.unfollow(article.value.authorId)
      isFollowed.value = false
      ElMessage.success('已取消关注')
    } else {
      await authorService.follow(article.value.authorId)
      isFollowed.value = true
      ElMessage.success('已关注')
    }

    // 刷新当前登录用户信息，确保侧边与个人中心面板计数及时更新
    try {
      const me = await axios.get('/user/info')
      userStore.setUserInfo(me as any)
    } catch (e) {
      console.warn('刷新用户信息失败:', e)
    }
  } catch (error: any) {
    console.error('操作失败:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

// 获取文章详情
const getArticleDetail = async () => {
  try {
    const response = await articleService.getDetail(articleId.value)
    article.value = response

    // 检查关注状态
    checkFollowStatus()
  } catch (error: any) {
    console.error('获取文章详情失败:', error)
    ElMessage.error(error.response?.data?.message || '加载文章失败')
    return
  }

  // Track view count - failures should not affect article display
  try {
    await statisticsService.incrementViewCount(articleId.value)
  } catch (error) {
    // Silently ignore statistics errors - they should not affect user experience
    console.debug('浏览统计记录失败（非关键）:', error)
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
}

// 处理点赞更新
const handleLikeUpdate = (liked: boolean, count: number) => {
  article.value.liked = liked
  article.value.likeCount = count
}

// 处理收藏更新
const handleFavoriteUpdate = (favorited: boolean) => {
  article.value.favorited = favorited
  if (favorited) {
    article.value.favoriteCount++
  } else {
    article.value.favoriteCount = Math.max(0, article.value.favoriteCount - 1)
  }
}

// 处理分享
const handleShare = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('分享失败')
  })
}

// 检查是否可以管理文章(管理员或作者)
const canManageArticle = computed(() => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (!userInfoStr) {
    return false
  }

  try {
    const userInfo = JSON.parse(userInfoStr)
    const isAdmin = userInfo.role === 'admin'
    const isAuthor = userInfo.id === article.value.authorId

    // 管理员或文章作者可以管理
    return isAdmin || isAuthor
  } catch (e) {
    console.error('[canManageArticle] 解析userInfo失败:', e)
    return false
  }
})

// 处理编辑
const handleEdit = () => {
  router.push(`/article/edit/${articleId.value}`)
}

// 处理删除
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这篇文章吗？此操作不可恢复。',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 调用删除API
    await articleService.delete(articleId.value)
    ElMessage.success('文章已删除')
    
    // 跳转到首页
    router.push('/')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除文章失败:', error)
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 监听路由参数变化
watch(
  () => route.params.id,
  (newId) => {
    articleId.value = Number(newId)
    getArticleDetail()
  }
)

// 初始化数据
onMounted(() => {
  initTheme()
  getArticleDetail()
  
  // 监听系统主题变化
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', handleThemeChange)
})

// 组件卸载前移除事件监听
onBeforeUnmount(() => {
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.removeEventListener('change', handleThemeChange)
})

// 监听localStorage主题变化
window.addEventListener('storage', (e) => {
  if (e.key === 'theme') {
    currentTheme.value = e.newValue as Themes
  }
})
</script>

<style scoped>
.article-detail {
  padding: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.article-header {
  margin-bottom: 40px;
}

.article-title {
  margin-bottom: 20px;
  color: var(--text-primary);
  font-size: 36px;
  font-weight: 700;
  line-height: 1.25;
  font-family: var(--font-serif);
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: var(--text-tertiary);
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.article-meta .author {
  display: flex;
  align-items: center;
  gap: 10px;
}

.article-meta .category a {
  color: var(--color-blue-500);
  text-decoration: none;
  transition: var(--transition);
}

.article-meta .category a:hover {
  color: var(--color-blue-600);
  text-decoration: underline;
}

.article-content {
  margin-bottom: 40px;
  line-height: 1.8;
  color: var(--text-primary);
  max-width: 100%;
  overflow-x: auto;
}

.markdown-body {
  font-family: var(--font-sans);
  color: var(--text-primary);
}

.markdown-body h1, .markdown-body h2, .markdown-body h3, .markdown-body h4, .markdown-body h5, .markdown-body h6 {
  margin: 32px 0 16px;
  color: var(--text-primary);
  font-family: var(--font-serif);
  font-weight: 600;
  line-height: 1.3;
}

.markdown-body h1 {
  font-size: 28px;
}

.markdown-body h2 {
  font-size: 24px;
}

.markdown-body h3 {
  font-size: 20px;
}

.markdown-body p {
  margin-bottom: 20px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.markdown-body ul, .markdown-body ol {
  margin-bottom: 20px;
  padding-left: 28px;
  color: var(--text-secondary);
}

.markdown-body li {
  margin-bottom: 8px;
}

.markdown-body a {
  color: var(--color-blue-500);
  text-decoration: none;
  transition: var(--transition);
}

.markdown-body a:hover {
  color: var(--color-blue-600);
  text-decoration: underline;
}

.markdown-body code {
  background-color: var(--bg-secondary);
  padding: 3px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: var(--font-mono);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.markdown-body pre {
  background-color: var(--bg-secondary);
  padding: 20px;
  border-radius: 10px;
  overflow-x: auto;
  margin-bottom: 24px;
  border: 1px solid var(--border-color);
}

.markdown-body pre code {
  background: transparent;
  padding: 0;
  border: none;
  font-size: 14px;
  line-height: 1.5;
}

.markdown-body blockquote {
  margin: 24px 0;
  padding: 16px 24px;
  border-left: 4px solid var(--color-blue-500);
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
  border-radius: 0 8px 8px 0;
}

.markdown-body img {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  margin: 20px 0;
}



.article-actions {
  display: flex;
  gap: 16px;
  margin-bottom: 40px;
  padding: 20px 0;
  border-bottom: 1px solid var(--border-color);
  align-items: center;
  flex-wrap: wrap;
}

.admin-actions {
  display: flex;
  gap: 12px;
  margin-left: auto;
}

/* 按钮样式覆盖 */
:deep(.el-button) {
  transition: var(--transition);
}

:deep(.el-button:hover) {
  transform: translateY(-1px);
}

/* md-editor-v3 暗色主题样式调整 */
.dark :deep(.md-editor-preview) {
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.dark :deep(.md-editor-preview h1),
.dark :deep(.md-editor-preview h2),
.dark :deep(.md-editor-preview h3),
.dark :deep(.md-editor-preview h4),
.dark :deep(.md-editor-preview h5),
.dark :deep(.md-editor-preview h6) {
  color: var(--text-primary);
}

.dark :deep(.md-editor-preview p),
.dark :deep(.md-editor-preview ul),
.dark :deep(.md-editor-preview ol) {
  color: var(--text-secondary);
}

.dark :deep(.md-editor-preview code) {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border-color: var(--border-color);
}

.dark :deep(.md-editor-preview pre) {
  background-color: var(--bg-secondary);
  border-color: var(--border-color);
}

.dark :deep(.md-editor-preview blockquote) {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
}

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 40px 0;
}

@media (max-width: 768px) {
  .article-title {
    font-size: 24px;
  }
  
  .article-meta {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .admin-actions {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
  }
  
  .article-actions {
    gap: 12px;
    flex-wrap: wrap;
  }

  .markdown-body h1 {
    font-size: 24px;
  }
  
  .markdown-body h2 {
    font-size: 20px;
  }
  
  .markdown-body h3 {
    font-size: 18px;
  }
}
</style>
