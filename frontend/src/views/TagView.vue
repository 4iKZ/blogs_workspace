<template>
  <Layout>
    <section class="tag-page">
      <header class="tag-header">
        <h1>{{ title }}</h1>
        <p v-if="!invalidTag">
          共 {{ total }} 篇文章
        </p>
      </header>

      <div
        v-if="loading"
        class="state"
      >
        正在加载文章…
      </div>
      <el-empty
        v-else-if="invalidTag"
        description="标签不存在"
      />
      <el-empty
        v-else-if="articles.length === 0"
        description="暂无文章"
      />
      <div
        v-else
        class="article-list"
      >
        <ArticleCard
          v-for="article in articles"
          :key="article.id"
          :article="article"
        />
      </div>

      <el-pagination
        v-if="!invalidTag && total > pageSize"
        class="pagination"
        background
        layout="prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        @current-change="changePage"
      />
    </section>
  </Layout>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Layout from '../components/Layout.vue'
import ArticleCard from '../components/ArticleCard.vue'
import { articleService } from '../services/articleService'
import type { Article } from '../types/article'

const route = useRoute()
const articles = ref<Article[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const tagName = ref('')
const invalidTag = ref(false)
let requestSequence = 0

const tagId = computed(() => Number(route.params.id))
const title = computed(() => {
  if (invalidTag.value) {
    return '标签不存在'
  }
  return tagName.value || `标签 #${tagId.value}`
})

const loadArticles = async () => {
  const currentTagId = tagId.value
  if (!Number.isInteger(currentTagId) || currentTagId <= 0) {
    invalidTag.value = true
    articles.value = []
    total.value = 0
    tagName.value = ''
    return
  }

  invalidTag.value = false
  loading.value = true
  const sequence = ++requestSequence
  try {
    const result = await articleService.getByTag(currentTagId, page.value, pageSize)
    if (sequence !== requestSequence) {
      return
    }
    articles.value = result.items
    total.value = result.total
    const matchingTag = result.items
      .flatMap((article) => article.tags ?? [])
      .find((tag) => tag.id === currentTagId)
    tagName.value = matchingTag?.name ?? ''
  } finally {
    if (sequence === requestSequence) {
      loading.value = false
    }
  }
}

const changePage = (nextPage: number) => {
  page.value = nextPage
  void loadArticles()
}

watch(
  () => route.params.id,
  () => {
    page.value = 1
    articles.value = []
    total.value = 0
    tagName.value = ''
    void loadArticles()
  },
  { immediate: true }
)
</script>

<style scoped>
.tag-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}

.tag-header {
  margin-bottom: 24px;
}

.tag-header h1 {
  margin: 0 0 8px;
}

.tag-header p {
  margin: 0;
  color: var(--text-secondary);
}

.article-list {
  display: grid;
  gap: 16px;
}

.state {
  padding: 48px 0;
  text-align: center;
  color: var(--text-secondary);
}

.pagination {
  justify-content: center;
  margin-top: 24px;
}
</style>
