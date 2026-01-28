<template>
  <Layout>
    <div class="home">
      <div class="articles-section">
        <!-- 推荐/最新切换按钮 -->
        <div class="sort-tabs">
          <div 
            class="sort-tab" 
            :class="{ active: activeTab === 'popular' }" 
            @click="switchTab('popular')"
          >
            推荐
          </div>
          <div 
            class="sort-tab" 
            :class="{ active: activeTab === 'latest' }" 
            @click="switchTab('latest')"
          >
            最新
          </div>
        </div>
        
        <!-- 筛选状态显示 -->
        <div v-if="articleStore.isFilteringByAuthor && articleStore.filterAuthor" class="filter-status">
          <div class="filter-info">
            <el-avatar 
              :size="32" 
              :src="articleStore.filterAuthor.avatar || ''"
              class="author-avatar"
            >
              {{ (articleStore.filterAuthor.name || '').charAt(0) || '?' }}
            </el-avatar>
            <span class="filter-text">
              正在查看作者 <strong>{{ articleStore.filterAuthor.name || '未知作者' }}</strong> 的文章
            </span>
          </div>
          <button 
            @click="articleStore.clearFilterAuthor" 
            class="clear-filter-btn"
          >
            清除筛选
          </button>
        </div>
        
        <!-- 文章列表 -->
        <div class="articles">
          <article-card 
            v-for="article in articles" 
            :key="article.id" 
            :article="article"
          />
        </div>
        
        <!-- 加载中指示器 -->
        <div v-if="loading" class="loading-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        
        <!-- 没有更多文章提示 -->
        <div v-if="!hasMore && articles.length > 0" class="no-more">
          没有更多文章了
        </div>
        
        <!-- 空状态 -->
        <div v-if="articles.length === 0 && !loading" class="empty-state">
          暂无文章
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import Layout from '../components/Layout.vue'
import ArticleCard from '../components/ArticleCard.vue'
import { articleService } from '../services/articleService'
import { useArticleStore } from '../store/article'
import type { Article } from '../types/article'

// 文章列表数据
const articles = ref<Article[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const hasMore = ref(true)

// 文章排序选项（默认展示推荐文章）
const activeTab = ref<'popular' | 'latest'>('popular')

// 切换文章排序选项（添加之前缺失的代码）
const switchTab = (tab: 'popular' | 'latest') => {
  activeTab.value = tab
  currentPage.value = 1
  hasMore.value = true
  getArticles()
}

// 路由信息
const route = useRoute()

// 文章状态管理
const articleStore = useArticleStore()

// 节流定时器
let scrollTimer: number | null = null

// 获取文章列表
const getArticles = async (append = false) => {
  if (loading.value || (!append && currentPage.value > 1)) return
  
  loading.value = true
  try {
    let response
    const baseParams = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: activeTab.value
    }
    
    // 如果设置了筛选作者，无论当前路由是什么，都使用作者筛选
    if (articleStore.filterAuthor) {
      // 使用作者筛选，调用普通文章列表 API
      response = await articleService.getList({
        ...baseParams,
        authorId: articleStore.filterAuthor.id, // 添加作者筛选参数
      })
    } else if (route.path === '/following') {
      // 关注页面，获取关注的文章列表
      response = await articleService.getFollowingArticles(baseParams)
    } else {
      // 其他页面，获取普通文章列表
      response = await articleService.getList(baseParams)
    }
    
    if (append) {
      articles.value = [...articles.value, ...response.items]
    } else {
      articles.value = response.items
    }

    // 如果返回的数据少于 pageSize，说明没有更多了
    if (response.items.length < pageSize.value) {
      hasMore.value = false
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 监听作者筛选状态变化，实时更新文章列表
watch(
  () => articleStore.filterAuthor,
  (newVal, oldVal) => {
    console.log('筛选作者状态变化:', oldVal, '->', newVal)
    // 重置分页和状态
    currentPage.value = 1
    hasMore.value = true
    // 重新获取文章列表
    getArticles()
  },
  { deep: true, immediate: true }
)

// 监听路由变化，实时更新文章列表
watch(
  () => route.path,
  () => {
    // 重置分页和状态
    currentPage.value = 1
    hasMore.value = true
    // 重新获取文章列表
    getArticles()
  }
)

// 滚动加载更多
const handleScroll = () => {
  if (scrollTimer) {
    return
  }
  
  scrollTimer = window.setTimeout(() => {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop
    const clientHeight = document.documentElement.clientHeight
    const scrollHeight = document.documentElement.scrollHeight
    
    // 当滚动到距离底部 300px 时加载
    if (scrollTop + clientHeight >= scrollHeight - 300 && !loading.value && hasMore.value) {
      currentPage.value++
      getArticles(true)
    }
    
    scrollTimer = null
  }, 200) // 200ms 节流
}

// 初始化数据
onMounted(() => {
  getArticles()
  window.addEventListener('scroll', handleScroll)
})

// 清理事件监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (scrollTimer) {
    clearTimeout(scrollTimer)
  }
})
</script>

<style scoped>
.home {
  padding: var(--space-6);
}

.articles-section {
  min-width: 0;
  max-width: 768px;
  margin: 0 auto;
}

/* 推荐/最新切换按钮样式 */
.sort-tabs {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--border-color);
}

.sort-tab {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
  border-bottom: 2px solid transparent;
}

.sort-tab:hover {
  color: var(--text-primary);
  background-color: var(--bg-secondary);
}

.sort-tab.active {
  color: var(--color-blue-500);
  font-weight: 600;
  border-bottom-color: var(--color-blue-500);
  background-color: transparent;
}

.sort-tab.active:hover {
  color: var(--color-blue-600);
  border-bottom-color: var(--color-blue-600);
}

/* 筛选状态样式 */
.filter-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  margin-bottom: var(--space-6);
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-fast) var(--ease-default);
}

.filter-status:hover {
  box-shadow: var(--shadow-md);
}

.filter-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.author-avatar {
  flex-shrink: 0;
}

.filter-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.filter-text strong {
  color: var(--text-primary);
  font-weight: 600;
}

.clear-filter-btn {
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-xs);
  font-weight: 500;
  background-color: transparent;
  color: var(--color-blue-500);
  border: 1px solid var(--color-blue-500);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  white-space: nowrap;
}

.clear-filter-btn:hover {
  background-color: var(--color-blue-500);
  color: white;
  transform: translateY(-1px);
}

.clear-filter-btn:active {
  transform: translateY(0);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-status {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-3);
  }
  
  .filter-info {
    justify-content: center;
  }
  
  .clear-filter-btn {
    width: 100%;
  }
}

.articles {
  display: flex;
  flex-direction: column;
  margin-bottom: var(--space-8);
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-12) var(--space-6);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  font-family: var(--font-mono);
}

.loading-indicator .el-icon {
  font-size: 20px;
}

.no-more {
  text-align: center;
  padding: var(--space-12) var(--space-6);
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  font-family: var(--font-mono);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.empty-state {
  text-align: center;
  padding: var(--space-24) var(--space-6);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.empty-state i {
  font-size: 48px;
  margin-bottom: var(--space-4);
  display: block;
  color: var(--text-disabled);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home {
    padding: var(--space-4);
  }
  
  .page-title {
    font-size: var(--text-3xl);
    margin-bottom: var(--space-6);
  }
  
  .articles-section {
    max-width: 100%;
  }
  
  .loading-indicator,
  .no-more {
    padding: var(--space-8) var(--space-4);
  }
  
  .empty-state {
    padding: var(--space-16) var(--space-4);
  }
}
</style>