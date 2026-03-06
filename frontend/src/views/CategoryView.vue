<template>
  <Layout>
    <div class="category-page">
      <!-- 分类列表视图 (无 ID 参数时显示) -->
      <div v-if="!categoryId" class="category-list-view">
        <h2 class="page-title">分类</h2>
        
        <!-- 加载中 -->
        <div v-if="loadingCategories" class="loading-indicator">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        
        <!-- 分类网格 -->
        <div v-else-if="categories.length > 0" class="category-grid">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-card"
            @click="goToCategory(cat.id)"
          >
            <div class="category-icon">
              <i :class="cat.icon || 'fas fa-folder'"></i>
            </div>
            <div class="category-info">
              <h3 class="category-name">{{ cat.name }}</h3>
              <p class="category-desc">{{ cat.description || '暂无描述' }}</p>
              <span class="category-count">{{ cat.articleCount || 0 }} 篇文章</span>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-else class="empty-state">
          <i class="fas fa-folder-open"></i>
          <p>暂无分类</p>
        </div>
      </div>
      
      <!-- 文章列表视图 (有 ID 参数时显示) -->
      <div v-else class="article-list-view">
        <!-- 返回按钮 -->
        <div class="back-btn" @click="goBackToCategories">
          <i class="fas fa-arrow-left"></i>
          <span>返回分类列表</span>
        </div>
        
        <!-- 分类标题 -->
        <h2 class="page-title">{{ categoryName }}</h2>

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
          <i class="fas fa-file-alt"></i>
          <p>该分类下暂无文章</p>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Loading } from "@element-plus/icons-vue";
import Layout from "../components/Layout.vue";
import ArticleCard from "../components/ArticleCard.vue";
import { categoryService } from "../services/categoryService";
import axios from "../utils/axios";
import type { PageResult } from "../types/article";

const route = useRoute();
const router = useRouter();

// 分类相关
const categoryId = ref<number | null>(null);
const categoryName = ref("");
const categories = ref<Category[]>([]);
const loadingCategories = ref(false);

// 文章列表数据
const articles = ref<any[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const hasMore = ref(true);

// 节流定时器
let scrollTimer: number | null = null;

// 分类类型
interface Category {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
  articleCount?: number;
}

// 文章类型
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

// 获取分类列表
const getCategories = async () => {
  loadingCategories.value = true;
  try {
    const response = await categoryService.getList();
    categories.value = response || [];
    console.log("获取分类列表成功:", categories.value.length);
  } catch (error) {
    console.error("获取分类列表失败:", error);
    categories.value = [];
  } finally {
    loadingCategories.value = false;
  }
};

// 跳转到分类文章列表
const goToCategory = (id: number) => {
  router.push(`/category/${id}`);
};

// 返回分类列表
const goBackToCategories = () => {
  router.push("/category");
};

// 获取分类下的文章列表
const getArticles = async (append = false) => {
  if (loading.value || (!append && currentPage.value > 1)) return;

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

    if (append) {
      articles.value = [...articles.value, ...response.items];
    } else {
      articles.value = response.items;
    }

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
    return;
  }

  try {
    const response = await categoryService.getById(categoryId.value);
    categoryName.value = response.name;
  } catch (error) {
    console.error("获取分类名称失败:", error);
    categoryName.value = "分类";
  }
};

// 滚动加载更多
const handleScroll = () => {
  if (scrollTimer || !categoryId.value) {
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

// 加载数据
const loadData = () => {
  const id = route.params.id;
  
  if (!id || id === '') {
    // 无 ID 参数，显示分类列表
    categoryId.value = null;
    categoryName.value = "";
    articles.value = [];
    getCategories();
  } else {
    // 有 ID 参数，显示文章列表
    const numId = Number(id);
    if (isNaN(numId)) {
      console.warn("路由参数 id 无效:", id);
      categoryId.value = null;
      categoryName.value = "无效分类";
      articles.value = [];
      return;
    }

    categoryId.value = numId;
    currentPage.value = 1;
    hasMore.value = true;
    articles.value = [];

    getCategoryName();
    getArticles();
  }
};

// 监听路由参数变化
watch(
  () => route.params.id,
  () => {
    loadData();
  }
);

// 初始化数据
onMounted(() => {
  loadData();
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
.category-page {
  padding: var(--space-6) 0;
  min-height: calc(100vh - 200px);
}

.page-title {
  margin-bottom: var(--space-6);
  color: var(--text-primary);
  font-size: var(--text-2xl);
  font-weight: 600;
}

/* 返回按钮 */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  margin-bottom: var(--space-4);
  color: var(--text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
}

.back-btn:hover {
  color: var(--color-blue-500);
  background-color: var(--bg-secondary);
}

/* 分类网格 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-6);
}

/* 分类卡片 */
.category-card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
}

.category-card:hover {
  border-color: var(--color-blue-500);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.category-card:active {
  transform: scale(0.98);
}

.category-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, var(--color-blue-500), var(--color-blue-600));
  border-radius: var(--radius-md);
  color: white;
  font-size: 20px;
  flex-shrink: 0;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  margin: 0 0 var(--space-1);
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.category-desc {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-count {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

/* 文章列表 */
.articles {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  margin-bottom: var(--space-8);
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-10) var(--space-5);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.loading-indicator .el-icon {
  font-size: 20px;
}

/* 没有更多 */
.no-more {
  text-align: center;
  padding: var(--space-10) var(--space-5);
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) var(--space-5);
  color: var(--text-tertiary);
}

.empty-state i {
  font-size: 48px;
  margin-bottom: var(--space-4);
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: var(--text-base);
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .category-page {
    padding: var(--space-4) 0;
  }

  .page-title {
    font-size: var(--text-xl);
    margin-bottom: var(--space-4);
  }

  .category-grid {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }

  .category-card {
    padding: var(--space-4);
  }

  .category-icon {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .category-name {
    font-size: var(--text-base);
  }

  .category-desc {
    display: none;
  }

  .category-count {
    display: none;
  }

  .articles {
    gap: var(--space-4);
  }
}

@media (max-width: 480px) {
  .category-card {
    padding: var(--space-3);
  }

  .category-icon {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }
}
</style>
