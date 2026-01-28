<template>
  <Layout>
    <div class="admin-articles">
      <h2 class="page-title">
        <SvgIcon
          name="articles"
          size="24px"
          style="margin-right: 8px; vertical-align: middle"
        />
        文章管理
      </h2>

      <div class="admin-content">
        <!-- 搜索和筛选区 -->
        <div class="search-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入文章标题"
            clearable
            style="width: 300px; margin-right: 16px"
          >
            <template #append>
              <el-button @click="handleSearch"
                ><el-icon><Search /></el-icon
              ></el-button>
            </template>
          </el-input>

          <el-select
            v-model="statusFilter"
            placeholder="文章状态"
            clearable
            style="width: 150px; margin-right: 16px"
            @change="handleSearch"
          >
            <el-option label="全部" :value="null"></el-option>
            <el-option label="草稿" :value="1"></el-option>
            <el-option label="已发布" :value="2"></el-option>
            <el-option label="已下线" :value="3"></el-option>
          </el-select>
        </div>

        <!-- 文章列表 -->
        <el-card class="articles-card" v-loading="loading">
          <div class="articles-table">
            <el-table :data="articles" stripe style="width: 100%">
              <el-table-column
                type="index"
                label="序号"
                width="80"
              ></el-table-column>
              <el-table-column prop="title" label="标题" min-width="300">
                <template #default="scope">
                  <router-link
                    :to="`/article/${scope.row.id}`"
                    target="_blank"
                    >{{ scope.row.title }}</router-link
                  >
                </template>
              </el-table-column>
              <el-table-column
                prop="authorNickname"
                label="作者"
                width="120"
              ></el-table-column>
              <el-table-column
                prop="categoryName"
                label="分类"
                width="120"
              ></el-table-column>
              <el-table-column
                prop="viewCount"
                label="浏览量"
                width="80"
              ></el-table-column>
              <el-table-column
                prop="likeCount"
                label="点赞数"
                width="80"
              ></el-table-column>
              <el-table-column
                prop="commentCount"
                label="评论数"
                width="80"
              ></el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                  <el-button
                    type="primary"
                    size="small"
                    @click="handleEdit(scope.row.id)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    :type="scope.row.status === 2 ? 'warning' : 'success'"
                    size="small"
                    @click="handleToggleStatus(scope.row)"
                  >
                    {{ scope.row.status === 2 ? "下线" : "发布" }}
                  </el-button>
                  <el-button
                    type="danger"
                    size="small"
                    @click="handleDelete(scope.row.id)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
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
        </el-card>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { Search } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import Layout from "../../components/Layout.vue";
import SvgIcon from "../../components/SvgIcon.vue";
import { adminService } from "../../services/adminService";

const router = useRouter();
const loading = ref(false);
const searchKeyword = ref("");
const statusFilter = ref<number | null>(null);
const articles = ref<any[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const getArticles = async () => {
  loading.value = true;
  try {
    const response = await adminService.getArticles({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined,
    });
    articles.value = response || [];
    total.value = 100;
  } catch (error: any) {
    console.error("获取文章列表失败:", error);
    ElMessage.error(
      error.response?.data?.message || error.message || "获取文章列表失败"
    );
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleString();
};

const getStatusType = (status: number) => {
  switch (status) {
    case 1:
      return "info";
    case 2:
      return "success";
    case 3:
      return "warning";
    default:
      return "info";
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 1:
      return "草稿";
    case 2:
      return "已发布";
    case 3:
      return "已下线";
    default:
      return "未知";
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  getArticles();
};

const handleEdit = (articleId: number) => {
  router.push(`/article/edit/${articleId}`);
};

const handleToggleStatus = async (article: any) => {
  const newStatus = article.status === 2 ? 3 : 2;
  const action = newStatus === 2 ? "发布" : "下线";

  ElMessageBox.confirm(`确定要${action}这篇文章吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await adminService.updateArticleStatus(article.id, newStatus);
        ElMessage.success(`${action}成功`);
        article.status = newStatus;
      } catch (error: any) {
        console.error(`${action}失败:`, error);
        ElMessage.error(
          error.response?.data?.message || error.message || `${action}失败`
        );
      }
    })
    .catch(() => {});
};

const handleDelete = (articleId: number) => {
  ElMessageBox.confirm("确定要删除这篇文章吗？删除后无法恢复！", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await adminService.deleteArticle(articleId);
        ElMessage.success("删除成功");
        getArticles();
      } catch (error: any) {
        console.error("删除失败:", error);
        ElMessage.error(
          error.response?.data?.message || error.message || "删除失败"
        );
      }
    })
    .catch(() => {});
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  getArticles();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  getArticles();
};

onMounted(() => {
  getArticles();
});

onUnmounted(() => {
  ElMessage.closeAll();
});
</script>

<style scoped>
.admin-articles {
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

.search-actions {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.articles-card {
  margin-bottom: 20px;
}

.articles-table {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
