<template>
  <Layout :show-left-sidebar="false">
    <div class="profile-container">
      <!-- 1. User Info Header -->
      <ProfileHeaderCard
        :user="userInfo"
        :avatar-size="90"
      >
        <template #action>
          <el-button
            v-if="!isMe"
            class="follow-btn"
            :type="userInfo.isFollowed ? 'default' : 'primary'"
            :plain="userInfo.isFollowed"
            :loading="followLoading"
            @click="handleFollow"
          >
            {{ userInfo.isFollowed ? '已关注' : '关注' }}
          </el-button>
        </template>
      </ProfileHeaderCard>

      <!-- 2. Main Navigation Tabs -->
      <div class="main-content shadow-sm">
        <el-tabs
          v-model="activeMainTab"
          class="profile-tabs"
        >
          <!-- Dynamic Tab (My Articles) -->
          <el-tab-pane
            label="动态"
            name="dynamic"
          >
            <div
              v-if="loadingArticles"
              class="loading"
            >
              <el-skeleton
                :rows="3"
                animated
              />
            </div>
            <div
              v-else-if="userArticles.length > 0"
              class="articles-list"
            >
              <div
                v-for="article in userArticles"
                :key="article.id"
                class="article-item"
              >
                <div
                  v-if="article.coverImage"
                  class="article-cover"
                >
                  <img
                    :src="article.coverImage"
                    :alt="article.title"
                    loading="lazy"
                  >
                </div>
                <div class="article-item-content">
                  <h4 class="article-item-title">
                    <router-link :to="`/article/${article.id}`">
                      {{ article.title }}
                    </router-link>
                  </h4>
                  <div
                    v-if="article.summary"
                    class="article-item-summary"
                  >
                    {{ article.summary }}
                  </div>
                  <div class="article-item-meta">
                    <span>发布于 {{ formatDate(article.publishTime || article.createTime) }}</span>
                    <span>浏览 {{ article.viewCount }} · 点赞 {{ article.likeCount }} · 评论 {{ article.commentCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div
              v-else
              class="empty"
            >
              <el-empty description="暂无动态" />
            </div>
          </el-tab-pane>

          <!-- Favorites Tab -->
          <el-tab-pane
            v-if="isMe"
            label="收藏"
            name="favorites"
          >
            <div
              v-if="loadingFavorites"
              class="loading"
            >
              <el-skeleton
                :rows="3"
                animated
              />
            </div>
            <div
              v-else-if="favoriteArticles.length > 0"
              class="articles-list"
            >
              <div
                v-for="article in favoriteArticles"
                :key="article.id"
                class="article-item"
              >
                <div
                  v-if="article.coverImage"
                  class="article-cover"
                >
                  <img
                    :src="article.coverImage"
                    :alt="article.title"
                    loading="lazy"
                  >
                </div>
                <div class="article-item-content">
                  <h4 class="article-item-title">
                    <router-link :to="`/article/${article.id}`">
                      {{ article.title }}
                    </router-link>
                  </h4>
                  <div
                    v-if="article.summary"
                    class="article-item-summary"
                  >
                    {{ article.summary }}
                  </div>
                  <div class="article-item-meta">
                    <span>发布于 {{ formatDate(article.publishTime || article.createTime) }}</span>
                    <span>浏览 {{ article.viewCount }} · 点赞 {{ article.likeCount }} · 评论 {{ article.commentCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div
              v-else
              class="empty"
            >
              <el-empty description="暂无收藏" />
            </div>
          </el-tab-pane>

          <!-- Liked Articles Tab -->
          <el-tab-pane
            v-if="isMe"
            label="赞过的文章"
            name="liked"
          >
            <div
              v-if="loadingLiked"
              class="loading"
            >
              <el-skeleton
                :rows="3"
                animated
              />
            </div>
            <div
              v-else-if="likedArticles.length > 0"
              class="articles-list"
            >
              <div
                v-for="article in likedArticles"
                :key="article.id"
                class="article-item"
              >
                <div
                  v-if="article.coverImage"
                  class="article-cover"
                >
                  <img
                    :src="article.coverImage"
                    :alt="article.title"
                    loading="lazy"
                  >
                </div>
                <div class="article-item-content">
                  <h4 class="article-item-title">
                    <router-link :to="`/article/${article.id}`">
                      {{ article.title }}
                    </router-link>
                  </h4>
                  <div
                    v-if="article.summary"
                    class="article-item-summary"
                  >
                    {{ article.summary }}
                  </div>
                  <div class="article-item-meta">
                    <span>发布于 {{ formatDate(article.publishTime || article.createTime) }}</span>
                    <span>浏览 {{ article.viewCount }} · 点赞 {{ article.likeCount }} · 评论 {{ article.commentCount }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div
              v-else
              class="empty"
            >
              <el-empty description="暂无赞过的文章" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from '@/composables/useLuminaToast'
import Layout from '../components/Layout.vue'
import ProfileHeaderCard from '../components/profile/ProfileHeaderCard.vue'
import { authorService } from '../services/authorService'
import axios from '../utils/axios'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 用户信息
const userInfo = ref({
  id: 0,
  username: '',
  nickname: '',
  avatar: '',
  bio: '',
  website: '',
  position: '',
  company: '',
  role: '',
  createTime: '',
  articleCount: 0,
  commentCount: 0,
  followerCount: 0,
  followingCount: 0,
  isFollowed: false
})

const currentUserId = ref<number>(0)
const followLoading = ref(false)

// 标签页控制
const activeMainTab = ref('dynamic')

// 我的文章 (动态)
const userArticles = ref<any[]>([])
const loadingArticles = ref(false)

// 收藏/点赞文章
const favoriteArticles = ref<any[]>([])
const likedArticles = ref<any[]>([])
const loadingFavorites = ref(false)
const loadingLiked = ref(false)

// 判断是否是自己
const isMe = computed(() => {
  return userInfo.value.id === currentUserId.value
})

// 获取当前登录用户ID（以 Pinia store 为准，避免依赖可篡改/过期的 localStorage）
const getCurrentUser = () => {
  currentUserId.value = userStore.userInfo?.id ?? 0
}

// 获取目标用户信息
const getUserInfo = async (userId: string) => {
  try {
    const response = await axios.get(`/user/public/${userId}`)
    userInfo.value = response
  } catch (error: any) {
    console.error('获取用户信息失败:', error)
    // 不显示 toast，由 axios 拦截器统一处理
  }
}

// 获取用户文章 (动态)
const getUserArticles = async (userId: string) => {
  loadingArticles.value = true
  try {
    const response = await axios.get('/article/list', {
      params: { authorId: Number(userId), page: 1, size: 10 }
    })
    userArticles.value = response.items || response
  } catch (error: any) {
    console.error('获取用户文章失败:', error)
    if (!error._handled) {
      toast.error(error.response?.data?.message || '加载文章失败')
    }
  } finally {
    loadingArticles.value = false
  }
}

// 获取用户收藏文章
const getUserFavorites = async (userId: string) => {
  loadingFavorites.value = true
  try {
    const response = await axios.get(`/article/user/${userId}/favorite`)
    favoriteArticles.value = response.items || response
  } catch (error: any) {
    console.error('获取用户收藏失败:', error)
    if (!error._handled) {
      toast.error(error.response?.data?.message || '加载收藏失败')
    }
  } finally {
    loadingFavorites.value = false
  }
}

// 获取用户点赞文章
const getUserLiked = async (userId: string) => {
  loadingLiked.value = true
  try {
    const response = await axios.get(`/article/user/${userId}/liked`)
    likedArticles.value = response.items || response
  } catch (error: any) {
    console.error('获取用户点赞失败:', error)
    if (!error._handled) {
      toast.error(error.response?.data?.message || '加载点赞文章失败')
    }
  } finally {
    loadingLiked.value = false
  }
}

// 关注/取消关注
const handleFollow = async () => {
  if (!currentUserId.value) {
    router.push('/login')
    return
  }

  followLoading.value = true
  try {
    if (userInfo.value.isFollowed) {
      await authorService.unfollow(userInfo.value.id)
      toast.success('已取消关注')
    } else {
      await authorService.follow(userInfo.value.id)
      toast.success('关注成功')
    }
    // 重新获取用户信息以更新粉丝数和关注数
    const userId = route.params.id as string
    await getUserInfo(userId)
    // 如果查看的是自己的主页，同步更新 store 中的用户信息（store 内部仅持久化最小字段）
    if (userId === String(currentUserId.value)) {
      try {
        const me = await axios.get('/user/info')
        userStore.setUserInfo(me)
      } catch (e) {
        console.warn('同步本地用户信息失败:', e)
      }
    }
  } catch (error: any) {
    console.error('操作失败:', error)
    if (!error._handled) {
      toast.error(error.response?.data?.message || '操作失败')
    }
  } finally {
    followLoading.value = false
  }
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString()
}

// 监听路由变化
watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      const userId = Array.isArray(newId) ? newId[0] : newId
      activeMainTab.value = 'dynamic'
      // 重置目标用户信息，避免沿用上一用户的 id 导致 isMe 瞬时错误
      userInfo.value = {
        id: 0,
        username: '',
        nickname: '',
        avatar: '',
        bio: '',
        website: '',
        position: '',
        company: '',
        role: '',
        createTime: '',
        articleCount: 0,
        commentCount: 0,
        followerCount: 0,
        followingCount: 0,
        isFollowed: false
      }
      userArticles.value = []
      favoriteArticles.value = []
      likedArticles.value = []
      loadedTabKey = ''
      getUserInfo(userId)
      // 显式加载动态，避免同 tab 路由切换时 watch 不触发
      loadTabData(userId, 'dynamic')
    }
  }
)

// 按需加载当前标签页数据（去重：同一用户同一标签只加载一次）
let loadedTabKey = ''

const loadTabData = (userId: string, tabName: string) => {
  const key = `${userId}:${tabName}`
  if (loadedTabKey === key) return
  loadedTabKey = key

  if (tabName === 'dynamic') {
    if (userArticles.value.length === 0) {
      getUserArticles(userId)
    }
  } else if (tabName === 'favorites') {
    if (isMe.value && favoriteArticles.value.length === 0) {
      getUserFavorites(userId)
    }
  } else if (tabName === 'liked') {
    if (isMe.value && likedArticles.value.length === 0) {
      getUserLiked(userId)
    }
  }
}

// 监听标签页变化，实时加载对应数据
watch(
  () => activeMainTab.value,
  (tabName) => {
    const userId = route.params.id as string
    if (!userId) return
    loadTabData(userId, tabName)
  },
  { immediate: true }
)

// 初始化数据
onMounted(async () => {
  getCurrentUser()
  const userId = route.params.id as string
  if (userId) {
    await getUserInfo(userId)
  }
})
</script>

<style scoped>
.profile-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px 0;
}

.user-header {
  background: var(--bg-card);
  border-radius: 4px;
  padding: 24px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.shadow-sm {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.user-info-block {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.avatar-section {
  margin-right: 24px;
}

.info-content {
  flex: 1;
}

.username {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.position-info {
  display: flex;
  align-items: center;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 12px;
}

.divider {
  margin: 0 10px;
  color: var(--border-color);
}

.intro {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 22px;
  white-space: pre-wrap;
}

.action-section {
  margin-left: 24px;
}

.main-content {
  background: var(--bg-card);
  border-radius: 4px;
  padding: 0 24px;
  min-height: 400px;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border-color);
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 20px;
  height: 50px;
  line-height: 50px;
}

/* List Styles */
.articles-list {
  padding: 10px 0;
}

.article-item {
  display: flex;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.article-item:last-child {
  border-bottom: none;
}

.article-cover {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  margin-right: 16px;
  flex-shrink: 0;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-item-content {
  flex: 1;
  min-width: 0;
}

.article-item-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
  line-height: 24px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.article-item-title a {
  color: var(--text-primary);
  text-decoration: none;
}

.article-item-title a:hover {
  color: var(--color-blue-500);
}

.article-item-summary {
  color: var(--text-tertiary);
  font-size: 13px;
  line-height: 22px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.article-item-meta {
  font-size: 13px;
  color: var(--text-tertiary);
}

.empty, .loading {
  padding: 40px 0;
  text-align: center;
}

.user-list {
  padding: 10px 0;
}

.user-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.user-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: inherit;
}

.user-info-text {
  margin-left: 12px;
}

.user-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-stats {
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>
