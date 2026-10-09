<template>
  <div class="admin-home">
    <h2 class="page-title">
      <SvgIcon
        name="dashboard"
        size="24px"
        style="margin-right: 8px; vertical-align: middle"
      />
      管理后台
    </h2>

    <div class="admin-content">
      <!-- 内容统计卡片 -->
      <!-- 内容统计卡片：点击进入对应列表，失败时显示 — 并提供重试 -->
      <div class="stats-cards">
        <router-link
          v-for="card in statCards"
          :key="card.key"
          :to="card.to"
          class="stat-link"
        >
          <el-card
            v-loading="loading"
            class="stat-card"
          >
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">
                  {{ card.value }}
                </p>
                <p class="stat-label">
                  {{ card.label }}
                </p>
              </div>
              <div :class="['stat-icon', card.iconClass]">
                <SvgIcon
                  :name="card.icon"
                  size="32px"
                />
              </div>
            </div>
          </el-card>
        </router-link>
      </div>
      <div
        v-if="loadError && !loading"
        class="stats-error"
      >
        <span>统计数据加载失败</span>
        <el-button
          link
          type="primary"
          @click="getStats"
        >
          重试
        </el-button>
      </div>

      <!-- 网站访问统计可视化 -->
      <el-card class="statistics-card">
        <template #header>
          <div class="card-header">
            <h3>
              <el-icon style="margin-right: 8px">
                <TrendCharts />
              </el-icon>
              网站访问统计
            </h3>
          </div>
        </template>
        <WebsiteStatistics />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import {
  TrendCharts
} from '@element-plus/icons-vue'
import SvgIcon from "../../components/SvgIcon.vue";
import WebsiteStatistics from "../../components/admin/WebsiteStatistics.vue";
import { adminService } from "../../services/adminService";

const loading = ref(false);
const loadError = ref(false);

// 加载失败时各项为 null，界面显示 —，而不是伪造的 0
const stats = ref<{
  totalArticles: number | null;
  totalUsers: number | null;
  publishedArticles: number | null;
  draftArticles: number | null;
}>({
  totalArticles: null,
  totalUsers: null,
  publishedArticles: null,
  draftArticles: null,
});

const displayValue = (value: number | null) => (value === null ? "—" : value);

const statCards = computed(() => [
  {
    key: "total",
    label: "总文章数",
    value: displayValue(stats.value.totalArticles),
    to: "/admin/articles",
    icon: "articles",
    iconClass: "article-icon",
  },
  {
    key: "users",
    label: "总用户数",
    value: displayValue(stats.value.totalUsers),
    to: "/admin/users",
    icon: "users",
    iconClass: "user-icon",
  },
  {
    key: "published",
    label: "已发布文章",
    value: displayValue(stats.value.publishedArticles),
    to: "/admin/articles?status=2",
    icon: "categories",
    iconClass: "category-icon",
  },
  {
    key: "draft",
    label: "草稿文章",
    value: displayValue(stats.value.draftArticles),
    to: "/admin/articles?status=1",
    icon: "tag",
    iconClass: "tag-icon",
  },
]);

const getStats = async () => {
  loading.value = true;
  loadError.value = false;
  try {
    const response = await adminService.getStatistics();
    if (!response || typeof response !== "object" || Array.isArray(response)) {
      throw new Error("统计数据格式异常");
    }
    stats.value = {
      totalArticles: response.totalArticles || 0,
      totalUsers: response.totalUsers || 0,
      publishedArticles: response.publishedArticles || 0,
      draftArticles: response.draftArticles || 0,
    };
  } catch (error: any) {
    console.error("获取统计数据失败:", error);
    stats.value = {
      totalArticles: null,
      totalUsers: null,
      publishedArticles: null,
      draftArticles: null,
    };
    loadError.value = true;
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  getStats();
});
</script>

<style scoped>
.admin-home {
  padding: 20px 0;
}

.page-title {
  margin-bottom: 24px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 600;
}

.admin-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-cards {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 200px;
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.stat-label {
  font-size: 14px;
  color: var(--text-tertiary);
  margin: 4px 0 0 0;
}

.stat-icon {
  font-size: 48px;
  opacity: 0.2;
}

.article-icon {
  color: var(--color-blue-500);
}

.user-icon {
  color: #67c23a;
}

.category-icon {
  color: #e6a23c;
}

.tag-icon {
  color: #f56c6c;
}

.statistics-card {
  border-radius: 8px;
}

.statistics-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color-lighter, #EBEEF5);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary, #303133);
  display: flex;
  align-items: center;
}

.quick-action-btn + .quick-action-btn {
  margin-left: 0;
}


/* 统计卡片作为链接：悬停上浮，键盘聚焦可见 */
.stat-link {
  display: block;
  flex: 1;
  min-width: 200px;
  color: inherit;
  text-decoration: none;
  border-radius: var(--radius-lg);
}

.stat-link:hover .stat-card {
  transform: translateY(-2px);
}

.stat-link:focus-visible {
  outline: 2px solid var(--border-focus);
  outline-offset: 2px;
}

.stat-card {
  transition: transform var(--duration-fast) var(--ease-default);
}

.stats-error {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin: var(--space-3) 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
</style>