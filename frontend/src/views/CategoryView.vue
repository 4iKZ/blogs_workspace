<template>
  <Layout>
    <div class="category">
      <!-- 分类标题 -->
      <h2 class="page-title">{{ categoryName }} - 文章列表</h2>

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
        该分类下暂无文章
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute } from "vue-router";
import { Loading } from "@element-plus/icons-vue";
import Layout from "../components/Layout.vue";
import ArticleCard from "../components/ArticleCard.vue";
import axios from "../utils/axios";
import type { PageResult } from "../types/article";

const route = useRoute();

// 分类ID
const categoryId = ref<number | null>(null);
const categoryName = ref("");

// 文章列表数据
const articles = ref<any[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const hasMore = ref(true);

// 节流定时器
let scrollTimer: number | null = null;

// 定义文章类型
interface Article {
  id: number;
  title: string;
  summary: string;
  content: string;
  status: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  favoriteCount: number;
  authorId: number;
  authorNickname: string;
  categoryId: number;
  createTime: string;
  updateTime: string;
  publishTime: string;
  category: {
    id: number;
    name: string;
    description: string;
    sortOrder: number;
  };
}

// 获取分类下的文章列表
const getArticles = async (append = false) => {
  if (loading.value || (!append && currentPage.value > 1)) return;

  // 验证 categoryId 有效
  if (!categoryId.value) {
    console.warn("categoryId 无效，跳过请求");
    return;
  }

  loading.value = true;
  try {
    const response = await axios.get<PageResult<Article>>(
      "/article/category/" + categoryId.value,
      {
        params: {
          page: currentPage.value,
          size: pageSize.value,
        },
      }
    );

    // 使用 response.items 获取文章数组
    if (append) {
      articles.value = [...articles.value, ...response.items];
    } else {
      articles.value = response.items;
    }

    // 如果返回的数据少于 pageSize，说明没有更多了
    if (response.items.length < pageSize.value) {
      hasMore.value = false;
    }

    console.log("获取分类文章成功:", {
      categoryId: categoryId.value,
      文章数量: response.items.length,
      总数: response.total,
    });
  } catch (error) {
    console.error("获取分类文章列表失败:", error);
    if (!append) {
      articles.value = [];
    }
  } finally {
    loading.value = false;
  }
};

// 获取分类名称
const getCategoryName = async () => {
  if (!categoryId.value) {
    console.warn("categoryId 无效，跳过请求");
    return;
  }

  try {
    const response = await axios.get("/category/" + categoryId.value);
    categoryName.value = response.name;
  } catch (error) {
    console.error("获取分类名称失败:", error);
    categoryName.value = "分类";
  }
};

// 滚动加载更多
const handleScroll = () => {
  if (scrollTimer) {
    return;
  }

  scrollTimer = window.setTimeout(() => {
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    const clientHeight = document.documentElement.clientHeight;
    const scrollHeight = document.documentElement.scrollHeight;

    if (
      scrollTop + clientHeight >= scrollHeight - 300 &&
      !loading.value &&
      hasMore.value
    ) {
      currentPage.value++;
      getArticles(true);
    }

    scrollTimer = null;
  }, 200);
};

// 加载分类数据
const loadCategoryData = () => {
  const id = Number(route.params.id);
  if (isNaN(id)) {
    console.warn("路由参数 id 无效:", route.params.id);
    categoryId.value = null;
    categoryName.value = "无效分类";
    articles.value = [];
    return;
  }

  categoryId.value = id;
  currentPage.value = 1;
  hasMore.value = true;
  articles.value = [];

  getCategoryName();
  getArticles();
};

// 监听路由参数变化
watch(
  () => route.params.id,
  () => {
    loadCategoryData();
  }
);

// 初始化数据
onMounted(() => {
  loadCategoryData();
  window.addEventListener("scroll", handleScroll);
});

// 清理事件监听
onUnmounted(() => {
  window.removeEventListener("scroll", handleScroll);
  if (scrollTimer) {
    clearTimeout(scrollTimer);
  }
});
</script>

<style scoped>
.category {
  padding: 20px 0;
}

.page-title {
  margin-bottom: 24px;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 600;
}

.articles {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}

.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 20px;
  color: var(--text-secondary);
  font-size: 14px;
}

.loading-indicator .el-icon {
  font-size: 20px;
}

.no-more {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-tertiary);
  font-size: 14px;
}
</style>