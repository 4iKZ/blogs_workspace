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
        <!-- 统计卡片 -->
        <div class="stats-cards">
          <el-card class="stat-card" v-loading="loading">
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">{{ stats.totalArticles }}</p>
                <p class="stat-label">总文章数</p>
              </div>
              <div class="stat-icon article-icon">
                <SvgIcon name="articles" size="32px" />
              </div>
            </div>
          </el-card>

          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">{{ stats.totalUsers }}</p>
                <p class="stat-label">总用户数</p>
              </div>
              <div class="stat-icon user-icon">
                <SvgIcon name="users" size="32px" />
              </div>
            </div>
          </el-card>

          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">{{ stats.publishedArticles }}</p>
                <p class="stat-label">已发布文章</p>
              </div>
              <div class="stat-icon category-icon">
                <SvgIcon name="categories" size="32px" />
              </div>
            </div>
          </el-card>

          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-number">{{ stats.draftArticles }}</p>
                <p class="stat-label">草稿文章</p>
              </div>
              <div class="stat-icon tag-icon">
                <SvgIcon name="tag" size="32px" />
              </div>
            </div>
          </el-card>
        </div>

        <!-- 快速操作 -->
        <el-card class="quick-actions-card">
          <template #header>
            <h3>快速操作</h3>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="navigateTo('articles')">
              <SvgIcon
                name="articles"
                size="16px"
                style="margin-right: 4px; vertical-align: middle"
              />
              管理文章
            </el-button>
            <el-button type="success" @click="navigateTo('users')">
              <SvgIcon
                name="users"
                size="16px"
                style="margin-right: 4px; vertical-align: middle"
              />
              管理用户
            </el-button>
            <el-button type="warning" @click="navigateTo('comments')">
              <SvgIcon
                name="comment"
                size="16px"
                style="margin-right: 4px; vertical-align: middle"
              />
              管理评论
            </el-button>
            <el-button type="info" @click="navigateTo('categories')">
              <SvgIcon
                name="categories"
                size="16px"
                style="margin-right: 4px; vertical-align: middle"
              />
              管理分类
            </el-button>
            <el-button type="danger" @click="navigateTo('settings')">
              <SvgIcon
                name="settings"
                size="16px"
                style="margin-right: 4px; vertical-align: middle"
              />
              系统设置
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
import Layout from "../../components/Layout.vue";
import SvgIcon from "../../components/SvgIcon.vue";
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
  color: #2c3e50;
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
  color: #2c3e50;
  margin: 0;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin: 4px 0 0 0;
}

.stat-icon {
  font-size: 48px;
  opacity: 0.2;
}

.article-icon {
  color: #409eff;
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
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
