<template>
  <div class="home">
    <div class="articles-section">
      <!-- 推荐/最新切换：可键盘操作的 tab，选中指示是一块会滑动的滑块 -->
      <div
        v-if="route.path !== '/following'"
        ref="sortTabsRef"
        class="sort-tabs"
        role="tablist"
        aria-label="文章排序"
      >
        <button
          v-for="tab in sortTabs"
          :key="tab.value"
          type="button"
          role="tab"
          class="sort-tab"
          :class="{ active: activeTab === tab.value }"
          :aria-selected="activeTab === tab.value"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
        <span
          class="sort-indicator"
          :class="{ 'is-static': indicatorStatic }"
          :style="indicatorStyle"
          aria-hidden="true"
        />
      </div>

      <!-- 文章列表 -->
      <div
        ref="articlesContainer"
        :class="['articles', `articles--from-${enterDirection}`]"
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

      <!-- 加载失败 -->
      <EmptyState
        v-if="loadError && !loading"
        icon="fas fa-exclamation-triangle"
        title="加载失败"
        description="文章列表加载失败，请稍后重试"
        show-action
      >
        <template #action>
          <el-button
            type="primary"
            size="small"
            @click="getArticles()"
          >
            重试
          </el-button>
        </template>
      </EmptyState>

      <!-- 没有更多文章提示 -->
      <div
        v-if="!hasMore && articles.length > 0"
        class="no-more"
      >
        没有更多文章了
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-if="articles.length === 0 && !loading && !loadError"
        icon="fas fa-newspaper"
        title="暂无文章"
        description="还没有发布任何文章，请稍后再来"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
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
// 首屏加载失败标记（成功或重试后清除），用于区分“加载失败”与“暂无文章”
const loadError = ref(false)

const articlesContainer = ref<HTMLElement | null>(null)
const { observe: observeScrollReveal } = useScrollRevealList(
  articlesContainer,
  '.scroll-reveal-item',
  { threshold: 0.1 }
)

// 文章排序选项（默认展示推荐文章）
const activeTab = ref<'popular' | 'latest'>('popular')

const sortTabs = [
  { value: 'popular', label: '推荐' },
  { value: 'latest', label: '最新' },
] as const

const sortTabsRef = ref<HTMLElement | null>(null)
const indicatorStyle = ref({ transform: 'translateX(0px)', width: '0px' })
// 首次显示、窗口缩放、字体加载完成时直接到位，不做过渡
const indicatorStatic = ref(true)
// 新列表的入场方向：切到“最新”从右侧进入，切回“推荐”从左侧进入；首次加载与加载更多保持从下方进入
const enterDirection = ref<'bottom' | 'left' | 'right'>('bottom')

const placeIndicator = (animate: boolean) => {
  const container = sortTabsRef.value
  if (!container) return
  const index = sortTabs.findIndex((tab) => tab.value === activeTab.value)
  const el = container.querySelectorAll<HTMLElement>('.sort-tab')[index]
  if (!el) return
  indicatorStatic.value = !animate
  indicatorStyle.value = {
    transform: `translateX(${el.offsetLeft}px)`,
    width: `${el.offsetWidth}px`,
  }
}

const handleViewportResize = () => placeIndicator(false)

// 切换文章排序选项
const switchTab = (tab: 'popular' | 'latest') => {
  if (tab === activeTab.value) return
  enterDirection.value = tab === 'latest' ? 'right' : 'left'
  activeTab.value = tab
  placeIndicator(true)
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
  // 首屏（第 1 页非追加）请求发起时清除错误态，重试后重置
  if (!append && currentPage.value === 1) {
    loadError.value = false
  }
  try {
    let response
    const baseParams = {
      page: currentPage.value,
      size: pageSize.value,
      sortBy: activeTab.value,
      status: 2,
    }

    if (route.path === '/following') {
      // 关注页面，获取关注的文章列表（后端仅绑定 page/size，固定按发布时间倒序）
      response = await articleService.getFollowingArticles({
        page: currentPage.value,
        size: pageSize.value,
      })
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
    // 仅首屏（第 1 页非追加）失败的当前请求进入错误态；追加失败维持现状
    if (seq === articlesRequestSeq && !append && currentPage.value === 1) {
      loadError.value = true
    }
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
  window.addEventListener('resize', handleViewportResize)
  placeIndicator(false)
  // 字体加载会改变标签宽度，加载完成后重新定位
  document.fonts?.ready.then(() => placeIndicator(false))
})

// 清理事件监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleViewportResize)
  if (scrollTimer) {
    clearTimeout(scrollTimer)
  }
})
</script>

<style scoped>
.home {
  /* 水平内边距由 .main-content 统一提供，这里只留上下间距，避免两层叠加压窄文本列 */
  padding: var(--space-6) 0;
}

.articles-section {
  min-width: 0;
  max-width: 768px;
  margin: 0 auto;
}

/* 推荐/最新切换按钮样式 */
.sort-tabs {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--border-color);
}

.sort-tab {
  /* 按钮重置为文字样式 */
  appearance: none;
  background: none;
  border: none;
  font: inherit;
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast) var(--ease-default),
    background-color var(--duration-fast) var(--ease-default);
}

.sort-tab:hover {
  color: var(--text-primary);
  background-color: var(--bg-secondary);
}

.sort-tab.active {
  color: var(--color-blue-500);
  font-weight: 600;
}

/* 选中指示：一块在标签之间滑动的底板 */
.sort-indicator {
  position: absolute;
  left: 0;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: var(--color-blue-500);
  transition: transform var(--duration-normal) var(--ease-default),
    width var(--duration-normal) var(--ease-default);
}

.sort-indicator.is-static {
  transition: none;
}

.articles {
  display: flex;
  flex-direction: column;
  margin-bottom: var(--space-8);
}

/* 切换排序时新列表从左右进入（仅作用于尚未显示的项） */
.articles--from-right .scroll-reveal-item:not(.scroll-revealed) {
  transform: translateX(12px);
}

.articles--from-left .scroll-reveal-item:not(.scroll-revealed) {
  transform: translateX(-12px);
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-12) var(--space-6);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  font-family: var(--font-sans);
}

.loading-indicator .el-icon {
  font-size: 20px;
}

.no-more {
  text-align: center;
  padding: var(--space-12) var(--space-6);
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  font-family: var(--font-sans);
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
