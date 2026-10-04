<template>
  <Layout>
    <div class="search">
      <!-- 搜索结果标题 -->
      <h2 class="page-title">
        搜索结果 - "{{ searchKeyword }}"
      </h2>
      
      <!-- 搜索结果统计 -->
      <div
        v-if="total > 0"
        class="search-stats"
      >
        找到 {{ total }} 条相关文章
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-if="articles.length === 0 && !loading"
        icon="fas fa-search"
        :title="'未找到与 &quot;' + searchKeyword + '&quot; 相关的文章'"
        description="请尝试其他关键词"
      />

      <!-- 文章列表 -->
      <div
        v-if="articles.length > 0"
        class="articles"
      >
        <article-card
          v-for="article in articles"
          :key="article.id"
          :article="article"
        />
      </div>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import Layout from '../components/Layout.vue'
import ArticleCard from '../components/ArticleCard.vue'
import EmptyState from '../components/EmptyState.vue'
import axios from '../utils/axios'

const route = useRoute()

// 关键词归一化：query 为数组时取首个元素，非字符串返回空（防直链绕过 Header 的 trim，限长 100）
const normalizeKeyword = (raw: unknown): string => {
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' ? value.trim().slice(0, 100) : ''
}

// 搜索关键词
const searchKeyword = ref(normalizeKeyword(route.query.keyword))

// 文章列表数据
const articles = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 监听关键词变化
watch(() => route.query.keyword, (newKeyword) => {
  searchKeyword.value = normalizeKeyword(newKeyword)
  currentPage.value = 1
  searchArticles()
})

// 搜索文章
let searchRequestSeq = 0

const searchArticles = async () => {
  if (!searchKeyword.value) {
    articles.value = []
    total.value = 0
    return
  }

  const seq = ++searchRequestSeq
  loading.value = true
  try {
    const response = await axios.get('/article/search', {
      params: {
        keyword: searchKeyword.value,
        page: currentPage.value,
        size: pageSize.value
      }
    })

    // 过期响应丢弃（快速输入时避免旧结果覆盖新结果）
    if (seq !== searchRequestSeq) return

    // API now returns PageResult with items and total
    articles.value = response.items || []
    total.value = response.total || 0
  } catch (error) {
    if (seq !== searchRequestSeq) return
    console.error('搜索文章失败:', error)
    // 清空旧结果，避免新关键词标题与上一次搜索结果不匹配
    articles.value = []
    total.value = 0
  } finally {
    if (seq === searchRequestSeq) loading.value = false
  }
}

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size
  searchArticles()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  searchArticles()
}

// 初始化数据
onMounted(() => {
  searchArticles()
})
</script>

<style scoped>
.search {
  padding: 20px 0;
}

.page-title {
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 600;
}

.search-stats {
  margin-bottom: 24px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.articles {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>