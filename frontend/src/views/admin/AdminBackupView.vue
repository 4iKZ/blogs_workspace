<template>
  <Layout>
    <div class="admin-backup">
      <h2 class="page-title">
        <SvgIcon
          name="settings"
          size="24px"
          style="margin-right: 8px; vertical-align: middle"
        />
        数据备份与恢复
      </h2>

      <div class="admin-content">
        <!-- 数据库备份 -->
        <el-card class="backup-card">
          <template #header>
            <div class="card-header">
              <h3>数据库备份</h3>
              <el-button type="primary" @click="showCreateBackupDialog = true">
                <i class="fas fa-plus" style="margin-right: 4px"></i>
                创建备份
              </el-button>
            </div>
          </template>

          <el-table
            :data="backupList"
            v-loading="backupLoading"
            stripe
            style="width: 100%"
            empty-text="暂无备份记录"
          >
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column prop="backupType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="getBackupTypeTag(row.backupType)">
                  {{ getBackupTypeLabel(row.backupType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'"
                >
                  {{ row.status === 'success' ? '成功' : row.status === 'failed' ? '失败' : '进行中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  text
                  @click="handleRestore(row)"
                  :disabled="row.status !== 'success'"
                >
                  恢复
                </el-button>
                <el-button
                  type="info"
                  size="small"
                  text
                  @click="handleDownloadBackup(row)"
                  :disabled="row.status !== 'success'"
                >
                  下载
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="handleDeleteBackup(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 数据导出 -->
        <el-card class="backup-card">
          <template #header>
            <div class="card-header">
              <h3>数据导出</h3>
              <div class="export-actions">
                <el-button type="success" @click="handleExport('user')" :loading="exportLoading === 'user'">
                  导出用户
                </el-button>
                <el-button type="primary" @click="handleExport('article')" :loading="exportLoading === 'article'">
                  导出文章
                </el-button>
                <el-button type="warning" @click="handleExport('comment')" :loading="exportLoading === 'comment'">
                  导出评论
                </el-button>
              </div>
            </div>
          </template>

          <el-table
            :data="exportList"
            v-loading="exportListLoading"
            stripe
            style="width: 100%"
            empty-text="暂无导出记录"
          >
            <el-table-column prop="fileName" label="文件名" min-width="220" />
            <el-table-column prop="exportType" label="导出类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="getExportTypeTag(row.exportType)">
                  {{ getExportTypeLabel(row.exportType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="120">
              <template #default="{ row }">
                {{ formatFileSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="recordCount" label="记录数" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'"
                >
                  {{ row.status === 'success' ? '成功' : row.status === 'failed' ? '失败' : '进行中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="info"
                  size="small"
                  text
                  @click="handleDownloadExport(row)"
                  :disabled="row.status !== 'success'"
                >
                  下载
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="handleDeleteExport(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- 创建备份对话框 -->
      <el-dialog
        v-model="showCreateBackupDialog"
        title="创建数据库备份"
        width="480px"
        :close-on-click-modal="false"
      >
        <el-form :model="backupForm" label-width="80px">
          <el-form-item label="备份名称" required>
            <el-input
              v-model="backupForm.backupName"
              placeholder="请输入备份名称"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="备份描述">
            <el-input
              v-model="backupForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入备份描述（可选）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateBackupDialog = false">取消</el-button>
          <el-button
            type="primary"
            @click="handleCreateBackup"
            :loading="createBackupLoading"
          >
            创建
          </el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ElMessageBox } from "element-plus";
import { toast } from "@/composables/useLuminaToast";
import Layout from "../../components/Layout.vue";
import SvgIcon from "../../components/SvgIcon.vue";
import { backupService } from "../../services/backupService";
import type { BackupInfo, ExportInfo } from "../../types/backup";

// ===== 备份相关 =====
const backupList = ref<BackupInfo[]>([]);
const backupLoading = ref(false);
const showCreateBackupDialog = ref(false);
const createBackupLoading = ref(false);
const backupForm = ref({
  backupName: "",
  description: "",
});

// ===== 导出相关 =====
const exportList = ref<ExportInfo[]>([]);
const exportListLoading = ref(false);
const exportLoading = ref<string | null>(null);

// ===== 数据加载 =====
const loadBackupList = async () => {
  backupLoading.value = true;
  try {
    backupList.value = await backupService.getBackupList();
  } catch (error: any) {
    console.error("获取备份列表失败:", error);
  } finally {
    backupLoading.value = false;
  }
};

const loadExportList = async () => {
  exportListLoading.value = true;
  try {
    exportList.value = await backupService.getExportFileList();
  } catch (error: any) {
    console.error("获取导出列表失败:", error);
  } finally {
    exportListLoading.value = false;
  }
};

// ===== 备份操作 =====
const handleCreateBackup = async () => {
  if (!backupForm.value.backupName.trim()) {
    toast.warning("请输入备份名称");
    return;
  }

  createBackupLoading.value = true;
  try {
    await backupService.createDatabaseBackup(
      backupForm.value.backupName.trim(),
      backupForm.value.description.trim() || undefined
    );
    toast.success("备份创建成功");
    showCreateBackupDialog.value = false;
    backupForm.value = { backupName: "", description: "" };
    await loadBackupList();
  } catch (error: any) {
    console.error("创建备份失败:", error);
    toast.error(error.response?.data?.message || error.message || "创建备份失败");
  } finally {
    createBackupLoading.value = false;
  }
};

const handleRestore = async (row: BackupInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要使用备份「${row.fileName}」恢复数据库吗？此操作将覆盖当前数据，请确保已做好备份。`,
      "⚠️ 恢复确认",
      { confirmButtonText: "确认恢复", cancelButtonText: "取消", type: "warning" }
    );
    await backupService.restoreDatabase(row.backupId);
    toast.success("数据库恢复成功");
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("恢复数据库失败:", error);
      toast.error(error.response?.data?.message || error.message || "恢复失败");
    }
  }
};

const handleDownloadBackup = async (row: BackupInfo) => {
  try {
    await backupService.downloadBackup(row.backupId);
    toast.success("备份文件下载已触发");
  } catch (error: any) {
    console.error("下载备份失败:", error);
    toast.error("下载备份文件失败");
  }
};

const handleDeleteBackup = async (row: BackupInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除备份「${row.fileName}」吗？删除后将无法恢复。`,
      "删除确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning" }
    );
    await backupService.deleteBackup(row.backupId);
    toast.success("备份已删除");
    await loadBackupList();
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("删除备份失败:", error);
      toast.error(error.response?.data?.message || error.message || "删除失败");
    }
  }
};

// ===== 导出操作 =====
const handleExport = async (type: "user" | "article" | "comment") => {
  exportLoading.value = type;
  try {
    if (type === "user") {
      await backupService.exportUserData();
    } else if (type === "article") {
      await backupService.exportArticleData();
    } else {
      await backupService.exportCommentData();
    }
    const typeLabel = type === "user" ? "用户" : type === "article" ? "文章" : "评论";
    toast.success(`${typeLabel}数据导出成功`);
    await loadExportList();
  } catch (error: any) {
    console.error("导出失败:", error);
    toast.error(error.response?.data?.message || error.message || "导出失败");
  } finally {
    exportLoading.value = null;
  }
};

const handleDownloadExport = async (row: ExportInfo) => {
  try {
    await backupService.downloadExportFile(row.exportId);
    toast.success("导出文件下载已触发");
  } catch (error: any) {
    console.error("下载导出文件失败:", error);
    toast.error("下载导出文件失败");
  }
};

const handleDeleteExport = async (row: ExportInfo) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除导出文件「${row.fileName}」吗？`,
      "删除确认",
      { confirmButtonText: "确认删除", cancelButtonText: "取消", type: "warning" }
    );
    await backupService.deleteExportFile(row.exportId);
    toast.success("导出文件已删除");
    await loadExportList();
  } catch (error: any) {
    if (error !== "cancel") {
      console.error("删除导出文件失败:", error);
      toast.error(error.response?.data?.message || error.message || "删除失败");
    }
  }
};

// ===== 工具函数 =====
const formatFileSize = (bytes: number): string => {
  if (!bytes || bytes === 0) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + " " + units[i];
};

const formatTime = (time: string): string => {
  if (!time) return "-";
  try {
    return new Date(time).toLocaleString("zh-CN");
  } catch {
    return time;
  }
};

const getBackupTypeLabel = (type: string): string => {
  const map: Record<string, string> = { database: "数据库", user: "用户", article: "文章", comment: "评论" };
  return map[type] || type;
};

const getBackupTypeTag = (type: string): "" | "success" | "warning" | "danger" => {
  const map: Record<string, "" | "success" | "warning" | "danger"> = {
    database: "", user: "success", article: "warning", comment: "danger"
  };
  return map[type] || "";
};

const getExportTypeLabel = (type: string): string => {
  const map: Record<string, string> = { user: "用户数据", article: "文章数据", comment: "评论数据" };
  return map[type] || type;
};

const getExportTypeTag = (type: string): "" | "success" | "warning" => {
  const map: Record<string, "" | "success" | "warning"> = { user: "success", article: "", comment: "warning" };
  return map[type] || "";
};

// ===== 生命周期 =====
onMounted(() => {
  loadBackupList();
  loadExportList();
});
</script>

<style scoped>
.admin-backup {
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

.backup-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.export-actions {
  display: flex;
  gap: 8px;
}
</style>
