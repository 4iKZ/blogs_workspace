<template>
  <Layout>
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
        <div class="stats-cards">
          <el-card
            v-loading="loading"
            class="stat-card"
          >
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">
                  {{ stats.totalArticles }}
                </p>
                <p class="stat-label">
                  总文章数
                </p>
              </div>
              <div class="stat-icon article-icon">
                <SvgIcon
                  name="articles"
                  size="32px"
                />
              </div>
            </div>
          </el-card>

          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">
                  {{ stats.totalUsers }}
                </p>
                <p class="stat-label">
                  总用户数
                </p>
              </div>
              <div class="stat-icon user-icon">
                <SvgIcon
                  name="users"
                  size="32px"
                />
              </div>
            </div>
          </el-card>

          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">
                  {{ stats.publishedArticles }}
                </p>
                <p class="stat-label">
                  已发布文章
                </p>
              </div>
              <div class="stat-icon category-icon">
                <SvgIcon
                  name="categories"
                  size="32px"
                />
              </div>
            </div>
          </el-card>

          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">
                  {{ stats.draftArticles }}
                </p>
                <p class="stat-label">
                  草稿文章
                </p>
              </div>
              <div class="stat-icon tag-icon">
                <SvgIcon
                  name="tag"
                  size="32px"
                />
              </div>
            </div>
          </el-card>
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

        <!-- 快速操作 -->
        <el-card class="quick-actions-card">
          <template #header>
            <h3>快速操作</h3>
          </template>
          <div class="quick-actions">
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="Document"
              @click="navigateTo('articles')"
            >
              管理文章
            </el-button>
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="DocumentChecked"
              @click="navigateTo('moderation')"
            >
              文章审核
            </el-button>
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="User"
              @click="navigateTo('users')"
            >
              管理用户
            </el-button>
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="ChatDotRound"
              @click="navigateTo('comments')"
            >
              管理评论
            </el-button>
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="FolderOpened"
              @click="navigateTo('categories')"
            >
              管理分类
            </el-button>
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="Folder"
              @click="navigateTo('files')"
            >
              管理文件
            </el-button>
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="Setting"
              @click="navigateTo('settings')"
            >
              系统设置
            </el-button>
            <el-button
              type="primary"
              class="quick-action-btn"
              :icon="Download"
              @click="navigateTo('backup')"
            >
              数据备份
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
  TrendCharts,
  Document,
  DocumentChecked,
  User,
  ChatDotRound,
  FolderOpened,
  Folder,
  Setting,
  Download
} from '@element-plus/icons-vue'
import Layout from "../../components/Layout.vue";
import SvgIcon from "../../components/SvgIcon.vue";
import WebsiteStatistics from "../../components/admin/WebsiteStatistics.vue";
import { adminService } from "../../services/adminService";

const router = useRouter();
const loading = ref(false);

const stats = ref({
  totalArticles: 0,
  totalUsers: 0,
  publishedArticles: 0,
  draftArticles: 0,
});

const getStats = async () => {
  loading.value = true;
  try {
    const response = await adminService.getStatistics();

    // 数据验证：确保返回的是有效对象
    if (response && typeof response === "object" && !Array.isArray(response)) {
      stats.value = {
        totalArticles: response.totalArticles || 0,
        totalUsers: response.totalUsers || 0,
        publishedArticles: response.publishedArticles || 0,
        draftArticles: response.draftArticles || 0,
      };
    } else {
      console.warn("获取统计数据返回异常数据:", typeof response);
      // 使用默认值
      stats.value = {
        totalArticles: 0,
        totalUsers: 0,
        publishedArticles: 0,
        draftArticles: 0,
      };
    }
  } catch (error: any) {
    console.error("获取统计数据失败:", error);
  } finally {
    loading.value = false;
  }
};

const navigateTo = (path: string) => {
  router.push(`/admin/${path}`);
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

.quick-actions-card .el-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-actions-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
  gap: 12px;
}

.quick-action-btn {
  width: 100%;
  height: 44px;
  margin: 0;
  font-size: 14px;
}

.quick-action-btn + .quick-action-btn {
  margin-left: 0;
}

</style>