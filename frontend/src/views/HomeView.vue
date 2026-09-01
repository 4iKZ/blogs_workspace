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
        
        <!-- 文章列表 -->
        <div
          ref="articlesContainer"
          class="articles"
        >
          <div
            v-for="article in articles"
            :key="article.id"
            class="scroll-reveal-item"
          >
            <article-card :article="article" />
          </div>
        </div>

        <!-- 加载中指示器 -->
        <div
          v-if="loading"
          class="loading-indicator"
        >
          <el-icon class="is-loading">
            <Loading />
          </el-icon>
          <span>加载中...</span>
        </div>

        <!-- 没有更多文章提示 -->
        <div
          v-if="!hasMore && articles.length > 0"
          class="no-more"
        >
          没有更多文章了
        </div>

        <!-- 空状态 -->
        <EmptyState
          v-if="articles.length === 0 && !loading"
          icon="fas fa-newspaper"
          title="暂无文章"
          description="还没有发布任何文章，请稍后再来"
        />
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import Layout from '../components/Layout.vue'
import ArticleCard from '../components/ArticleCard.vue'
import EmptyState from '../components/EmptyState.vue'
import { articleService } from '../services/articleService'
import { useScrollRevealList } from '../composables/useScrollReveal'
import type { Article } from '../types/article'

// 文章列表数据
const articles = ref<Article[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const hasMore = ref(true)

const articlesContainer = ref<HTMLElement | null>(null)
const { observe: observeScrollReveal } = useScrollRevealList(
  articlesContainer,
  '.scroll-reveal-item',
  { threshold: 0.1, staggerDelay: 80 }
)

// 文章排序选项（默认展示推荐文章）
const activeTab = ref<'popular' | 'latest'>('popular')

// 切换文章排序选项（添加之前缺失的代码）
const switchTab = (tab: 'popular' | 'latest') => {
  activeTab.value = tab
  currentPage.value = 1
  hasMore.value = true
  // 使进行中的请求失效并立即清空，避免旧排序数据混入
  articlesRequestSeq++
  articles.value = []
  reloadQueued = true
  getArticles()
}

// 路由信息
const route = useRoute()

// 节流定时器
let scrollTimer: number | null = null

// 请求序号，用于丢弃过期响应（切 tab/路由/筛选时）
let articlesRequestSeq = 0
let reloadQueued = false

// 获取文章列表
const getArticles = async (append = false) => {
  if (!append && currentPage.value > 1) return

  // 已有请求进行中：直接忽略，由发起重置的调用方负责排队重载
  if (loading.value) return

  const seq = ++articlesRequestSeq
  reloadQueued = false
  loading.value = true
  try {
    let response
    const baseParams = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: activeTab.value,
      status: 2,
    }

    if (route.path === '/following') {
      // 关注页面，获取关注的文章列表
      response = await articleService.getFollowingArticles(baseParams)
    } else {
      // 其他页面，获取普通文章列表
      response = await articleService.getList(baseParams)
    }

    // 过期响应直接丢弃（已切换 tab/路由/筛选）
    if (seq !== articlesRequestSeq) return

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
    if (seq === articlesRequestSeq) {
      nextTick(() => observeScrollReveal())
    }
    // 若期间有重置类加载被排队，立即补拉
    if (reloadQueued) {
      reloadQueued = false
      void getArticles()
    }
  }
}

// 监听路由变化，实时更新文章列表
watch(
  () => route.path,
  () => {
    // 重置分页和状态
    currentPage.value = 1
    hasMore.value = true
    articlesRequestSeq++
    articles.value = []
    reloadQueued = true
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
}
</style>