import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface Author {
  id: number
  name: string
  avatar?: string
}

export const useArticleStore = defineStore('article', () => {
  // 文章列表状态
  const articles = ref<any[]>([])
  const loading = ref(false)
  const currentPage = ref(1)
  const total = ref(0)
  const pageSize = ref(10)

  // 新增作者筛选状态
  const filterAuthor = ref<Author | null>(null)
  const isFilteringByAuthor = computed(() => !!filterAuthor.value)

  // 设置作者筛选
  const setFilterAuthor = (author: Author | null) => {
    filterAuthor.value = author
  }

  // 清除作者筛选
  const clearFilterAuthor = () => {
    filterAuthor.value = null
  }

  // 重置文章列表
  const resetArticles = () => {
    articles.value = []
    currentPage.value = 1
    total.value = 0
  }

  return {
    // 文章列表相关
    articles,
    loading,
    currentPage,
    total,
    pageSize,
    resetArticles,

    // 作者筛选相关
    filterAuthor,
    isFilteringByAuthor,
    setFilterAuthor,
    clearFilterAuthor
  }
})
