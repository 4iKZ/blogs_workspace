<template>
  <Layout>
    <div class="admin-categories">
      <h2 class="page-title">
        <SvgIcon
          name="categories"
          size="24px"
          style="margin-right: 8px; vertical-align: middle"
        />
        分类管理
      </h2>

      <div class="admin-content">
        <!-- 操作区 -->
        <div class="actions">
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            添加分类
          </el-button>
        </div>

        <!-- 分类列表 -->
        <el-card class="categories-card" v-loading="loading">
          <div class="categories-table">
            <el-table :data="categories" stripe style="width: 100%">
              <el-table-column
                type="index"
                label="序号"
                width="80"
              ></el-table-column>
              <el-table-column
                prop="name"
                label="分类名称"
                width="150"
              ></el-table-column>
              <el-table-column
                prop="description"
                label="描述"
                min-width="200"
                show-overflow-tooltip
              ></el-table-column>
              <el-table-column
                prop="sortOrder"
                label="排序"
                width="80"
              ></el-table-column>
              <el-table-column
                prop="articleCount"
                label="文章数"
                width="80"
              ></el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                    {{ scope.row.status === 1 ? "正常" : "禁用" }}
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
                    @click="handleEdit(scope.row)"
                  >
                    编辑
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
        </el-card>
      </div>

      <!-- 添加/编辑分类对话框 -->
      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="分类名称">
            <el-input
              v-model="form.name"
              placeholder="请输入分类名称"
            ></el-input>
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入分类描述"
            ></el-input>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number
              v-model="form.sortOrder"
              :min="0"
              :max="999"
            ></el-input-number>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">正常</el-radio>
              <el-radio :label="2">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting"
            >确定</el-button
          >
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessageBox } from "element-plus";
import { toast } from "@/composables/useLuminaToast";
import Layout from "../../components/Layout.vue";
import SvgIcon from "../../components/SvgIcon.vue";
import {
  categoryService,
  type CreateCategoryRequest,
  type UpdateCategoryRequest,
} from "../../services/categoryService";

const loading = ref(false);
const submitting = ref(false);
const categories = ref<any[]>([]);
const dialogVisible = ref(false);
const dialogTitle = computed(() => (form.value.id ? "编辑分类" : "添加分类"));
const form = ref<CreateCategoryRequest & { id?: number }>({
  name: "",
  description: "",
  sortOrder: 0,
  status: 1,
});

const getCategories = async () => {
  loading.value = true;
  try {
    const response = await categoryService.getList();
    categories.value = response || [];
  } catch (error: any) {
    console.error("获取分类列表失败:", error);
    toast.error(
      error.response?.data?.message || error.message || "获取分类列表失败"
    );
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleString();
};

const handleCreate = () => {
  form.value = {
    name: "",
    description: "",
    sortOrder: 0,
    status: 1,
  };
  dialogVisible.value = true;
};

const handleEdit = (category: any) => {
  form.value = {
    id: category.id,
    name: category.name,
    description: category.description,
    sortOrder: category.sortOrder,
    status: category.status,
  };
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!form.value.name) {
    toast.warning("请输入分类名称");
    return;
  }

  submitting.value = true;
  try {
    if (form.value.id) {
      await categoryService.update(
        form.value.id,
        form.value as UpdateCategoryRequest
      );
      toast.success("更新成功");
    } else {
      await categoryService.create(form.value);
      toast.success("创建成功");
    }
    dialogVisible.value = false;
    getCategories();
  } catch (error: any) {
    console.error("操作失败:", error);
    toast.error(
      error.response?.data?.message || error.message || "操作失败"
    );
  } finally {
    submitting.value = false;
  }
};

const handleDelete = (categoryId: number) => {
  ElMessageBox.confirm("确定要删除这个分类吗？删除后无法恢复！", "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await categoryService.delete(categoryId);
        toast.success("删除成功");
        getCategories();
      } catch (error: any) {
        console.error("删除失败:", error);
        toast.error(
          error.response?.data?.message || error.message || "删除失败"
        );
      }
    })
    .catch(() => {});
};

onMounted(() => {
  getCategories();
});

onUnmounted(() => {
  // Note: toast.closeAll() can be used here if needed
});
</script>

<style scoped>
.admin-categories {
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

.actions {
  margin-bottom: 20px;
}

.categories-card {
  margin-bottom: 20px;
}

.categories-table {
  margin-bottom: 20px;
}
</style>
