<template>
  <Layout>
    <div class="admin-comments">
      <h2 class="page-title">
        <SvgIcon
          name="comment"
          size="24px"
          style="margin-right: 8px; vertical-align: middle"
        />
        评论管理
      </h2>

      <div class="admin-content">
        <!-- 搜索和筛选区 -->
        <div class="search-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入评论内容"
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
            placeholder="评论状态"
            clearable
            style="width: 150px; margin-right: 16px"
            @change="handleSearch"
          >
            <el-option label="全部" :value="null"></el-option>
            <el-option label="待审核" :value="1"></el-option>
            <el-option label="已通过" :value="2"></el-option>
            <el-option label="已拒绝" :value="3"></el-option>
          </el-select>
        </div>

        <!-- 评论列表 -->
        <el-card class="comments-card" v-loading="loading">
          <div class="comments-table">
            <el-table :data="comments" stripe style="width: 100%">
              <el-table-column
                type="index"
                label="序号"
                width="80"
              ></el-table-column>
              <el-table-column
                prop="content"
                label="评论内容"
                min-width="300"
                show-overflow-tooltip
              ></el-table-column>
              <el-table-column
                prop="articleTitle"
                label="文章"
                width="200"
                show-overflow-tooltip
              ></el-table-column>
              <el-table-column
                prop="nickname"
                label="评论者"
                width="120"
              ></el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getStatusType(scope.row.status)">
                    {{ getStatusText(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="likeCount"
                label="点赞数"
                width="80"
              ></el-table-column>
              <el-table-column prop="createTime" label="评论时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                  <el-button
                    v-if="scope.row.status === 1"
                    type="success"
                    size="small"
                    @click="handleReview(scope.row.id, 2)"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-if="scope.row.status === 1"
                    type="warning"
                    size="small"
                    @click="handleReview(scope.row.id, 3)"
                  >
                    拒绝
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
import { Search } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import Layout from "../../components/Layout.vue";
import SvgIcon from "../../components/SvgIcon.vue";
import { adminService } from "../../services/adminService";

const loading = ref(false);
const searchKeyword = ref("");
const statusFilter = ref<number | null>(null);
const comments = ref<any[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const getComments = async () => {
  loading.value = true;
  try {
    const response = await adminService.getComments({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      status: statusFilter.value || undefined,
    });
    comments.value = response || [];
    total.value = 100;
  } catch (error: any) {
    console.error("获取评论列表失败:", error);
    ElMessage.error(
      error.response?.data?.message || error.message || "获取评论列表失败"
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
      return "warning";
    case 2:
      return "success";
    case 3:
      return "danger";
    default:
      return "info";
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 1:
      return "待审核";
    case 2:
      return "已通过";
    case 3:
      return "已拒绝";
    default:
      return "未知";
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  getComments();
};

const handleReview = async (commentId: number, status: number) => {
  const action = status === 2 ? "通过" : "拒绝";

  ElMessageBox.confirm(`确定要${action}这条评论吗？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await adminService.reviewComment(commentId, status);
        ElMessage.success(`${action}成功`);
        getComments();
      } catch (error: any) {
        console.error(`${action}失败:`, error);
        ElMessage.error(
          error.response?.data?.message || error.message || `${action}失败`
        );
      }
    })
    .catch(() => {});
};

const handleDelete = (commentId: number) => {
  ElMessageBox.confirm("确定要删除这条评论吗？删除后无法恢复！", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await adminService.deleteComment(commentId);
        ElMessage.success("删除成功");
        getComments();
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
  getComments();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  getComments();
};

onMounted(() => {
  getComments();
});

onUnmounted(() => {
  ElMessage.closeAll();
});
</script>

<style scoped>
.admin-comments {
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

.comments-card {
  margin-bottom: 20px;
}

.comments-table {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
