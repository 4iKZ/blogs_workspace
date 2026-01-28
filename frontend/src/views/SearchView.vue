<template>
  <Layout>
    <div class="search">
      <!-- 搜索结果标题 -->
      <h2 class="page-title">搜索结果 - "{{ searchKeyword }}"</h2>
      
      <!-- 搜索结果统计 -->
      <div class="search-stats">
        找到 {{ total }} 条相关文章
      </div>
      
      <!-- 文章列表 -->
      <div class="articles">
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
import axios from '../utils/axios'

const route = useRoute()

// 搜索关键词
const searchKeyword = ref(route.query.keyword as string || '')

// 文章列表数据
const articles = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 监听关键词变化
watch(() => route.query.keyword, (newKeyword) => {
  searchKeyword.value = newKeyword as string || ''
  currentPage.value = 1
  searchArticles()
})

// 搜索文章
const searchArticles = async () => {
  if (!searchKeyword.value) {
    articles.value = []
    total.value = 0
    return
  }

  try {
    const response = await axios.get('/article/search', {
      params: {
        keyword: searchKeyword.value,
        page: currentPage.value,
        size: pageSize.value
      }
    })

    // API now returns PageResult with items and total
    articles.value = response.items || []
    total.value = response.total || 0
  } catch (error) {
    console.error('搜索文章失败:', error)
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
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.search-stats {
  margin-bottom: 24px;
  color: #909399;
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