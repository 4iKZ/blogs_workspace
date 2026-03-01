<template>
  <Layout>
    <div class="admin-users">
      <h2 class="page-title">用户管理</h2>

      <div class="admin-content">
        <!-- 搜索和操作区 -->
        <div class="search-actions">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入用户名或邮箱"
            clearable
            style="width: 300px; margin-right: 16px"
          >
            <template #append>
              <el-button @click="handleSearch"
                ><el-icon><Search /></el-icon
              ></el-button>
            </template>
          </el-input>
        </div>

        <!-- 用户列表 -->
        <el-card class="users-card">
          <div class="users-table">
            <el-table :data="users" stripe style="width: 100%">
              <el-table-column
                type="index"
                label="序号"
                width="80"
              ></el-table-column>
              <el-table-column
                prop="username"
                label="用户名"
                width="120"
              ></el-table-column>
              <el-table-column
                prop="email"
                label="邮箱"
                width="200"
              ></el-table-column>
              <el-table-column
                prop="nickname"
                label="昵称"
                width="120"
              ></el-table-column>
              <el-table-column prop="role" label="角色" width="100">
                <template #default="scope">
                  <el-tag
                    :type="scope.row.role === 'admin' ? 'success' : 'info'"
                  >
                    {{ scope.row.role === "admin" ? "管理员" : "普通用户" }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-radio-group
                    v-model="scope.row.status"
                    @change="(newStatus: number) => handleStatusChange(scope.row, newStatus)"
                  >
                    <el-radio :label="1">正常</el-radio>
                    <el-radio :label="0">禁用</el-radio>
                  </el-radio-group>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="注册时间" width="180">
                <template #default="scope">
                  {{ formatDate(scope.row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="lastLoginTime"
                label="最后登录"
                width="180"
              >
                <template #default="scope">
                  {{
                    scope.row.lastLoginTime
                      ? formatDate(scope.row.lastLoginTime)
                      : "从未登录"
                  }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                  <el-button
                    type="danger"
                    size="small"
                    @click="handleDeleteUser(scope.row.id)"
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
import { ElMessageBox } from "element-plus";
import { toast } from "@/composables/useLuminaToast";
import Layout from "../../components/Layout.vue";
import axios from "../../utils/axios";

// 搜索关键词
const searchKeyword = ref("");

// 用户列表数据
const users = ref<any[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

// 获取用户列表
const getUsers = async () => {
  try {
    const response = await axios.get("/user/admin/list", {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        keyword: searchKeyword.value,
      },
    });

    users.value = response;
    total.value = 100; // 模拟总条数，实际应该从响应中获取
  } catch (error) {
    console.error("获取用户列表失败:", error);
  }
};

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleString();
};

// 搜索用户
const handleSearch = () => {
  currentPage.value = 1;
  getUsers();
};

// 处理状态变化
const handleStatusChange = async (user: any, newStatus: number) => {
  try {
    await axios.put(`/user/admin/status/${user.id}`, null, {
      params: { status: newStatus },
    });
    toast.success("状态更新成功");
  } catch (error: any) {
    console.error("更新状态失败:", error);
    toast.error(error.message || "更新失败");
    user.status = user.status === 1 ? 0 : 1;
  }
};

// 删除用户
const handleDeleteUser = (userId: number) => {
  ElMessageBox.confirm("确定要删除这个用户吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await axios.delete(`/user/admin/${userId}`);
        toast.success("用户删除成功");
        getUsers();
      } catch (error: any) {
        console.error("删除用户失败:", error);
        toast.error(error.message || "删除失败");
      }
    })
    .catch(() => {
      // 取消删除操作
    });
};

// 分页处理
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  getUsers();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  getUsers();
};

// 初始化数据
onMounted(() => {
  getUsers();
});

onUnmounted(() => {
  // Note: toast.closeAll() can be used here if needed
});
</script>

<style scoped>
.admin-users {
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

.users-card {
  margin-bottom: 20px;
}

.users-table {
  margin-bottom: 20px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>