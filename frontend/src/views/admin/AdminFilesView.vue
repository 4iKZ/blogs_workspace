<template>
  <Layout>
    <div class="admin-files">
      <div class="page-header">
        <h2>
          <SvgIcon name="articles" size="24px" style="margin-right: 8px; vertical-align: middle" />
          文件管理
        </h2>
        <div class="header-actions">
          <el-button @click="fetchFiles" :loading="loading">
            <i class="fas fa-sync-alt" style="margin-right: 4px;"></i> 刷新
          </el-button>
        </div>
      </div>

      <!-- 筛选和操作栏 -->
      <el-card class="filter-card" shadow="never">
        <div class="filter-actions">
          <el-select v-model="queryParams.fileType" placeholder="全部类型" clearable @change="handleFilter" style="width: 150px">
             <el-option label="图片" value="image" />
             <el-option label="视频" value="video" />
             <el-option label="文档" value="document" />
             <el-option label="其他" value="other" />
          </el-select>
        </div>
      </el-card>

      <!-- 文件列表 -->
      <el-card class="list-card" shadow="never">
        <el-table
          v-loading="loading"
          :data="fileList"
          style="width: 100%"
          border
          stripe
        >
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column label="预览" width="100" align="center">
            <template #default="{ row }">
              <el-image 
                v-if="row.fileType && row.fileType.startsWith('image/')"
                :src="row.fileUrl" 
                :preview-src-list="[row.fileUrl]"
                fit="cover"
                style="width: 40px; height: 40px; border-radius: 4px;"
                preview-teleported
              />
              <SvgIcon v-else name="articles" size="24px" />
            </template>
          </el-table-column>
          <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <a :href="row.fileUrl" target="_blank" class="file-link">{{ row.originalName || row.fileName }}</a>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column prop="fileType" label="类型" width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" label="上传时间" width="180" />
          
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-popconfirm
                title="确定要删除这个文件吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                confirm-button-type="danger"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <el-button type="danger" link size="small">
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import Layout from '../../components/Layout.vue'
import SvgIcon from '../../components/SvgIcon.vue'
import { fileService, type FileInfo } from '../../services/fileService'
import { toast } from '@/composables/useLuminaToast'

// 列表数据
const loading = ref(false)
const fileList = ref<FileInfo[]>([])
const total = ref(0)

// 查询参数
const queryParams = reactive({
  page: 1,
  size: 10,
  fileType: ''
})

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 获取文件列表
const fetchFiles = async () => {
  loading.value = true
  try {
    const res = await fileService.getFileList(
      queryParams.page,
      queryParams.size,
      queryParams.fileType || undefined
    )
    
    // axios 返回可能是包含整个response结构，这里简单判断
    const data = res as any
    // 后端返回的可能是 { total, list, pageNum, pageSize } 等
    if (data.list || data.records) {
       fileList.value = data.list || data.records || []
       total.value = data.total || 0
    } else if (Array.isArray(data)) {
       fileList.value = data
       total.value = data.length
    } else {
       fileList.value = []
       total.value = 0
    }
  } catch (error: any) {
    console.error('获取文件列表失败:', error)
    toast.error('获取文件列表失败')
  } finally {
    loading.value = false
  }
}

// 删除文件
const handleDelete = async (row: FileInfo) => {
  try {
    await fileService.deleteFile(row.id)
    toast.success('删除成功')
    
    // 如果当前页只有一条数据且不是第一页，删除后页码减1
    if (fileList.value.length === 1 && queryParams.page > 1) {
      queryParams.page--
    }
    fetchFiles()
  } catch (error: any) {
    console.error('删除文件失败:', error)
    toast.error('删除文件失败')
  }
}

// 搜索
const handleFilter = () => {
  queryParams.page = 1
  fetchFiles()
}

// 分页
const handleSizeChange = (val: number) => {
  queryParams.size = val
  fetchFiles()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchFiles()
}

onMounted(() => {
  fetchFiles()
})
</script>

<style scoped>
.admin-files {
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.page-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 600;
}

.filter-card {
  border-radius: 8px;
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.list-card {
  border-radius: 8px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.file-link {
  color: var(--color-blue-500);
  text-decoration: none;
}

.file-link:hover {
  text-decoration: underline;
}

:deep(.el-table) {
  --el-table-border-color: var(--border-color);
  --el-table-header-bg-color: var(--bg-secondary);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-text-color: var(--text-primary);
  --el-table-row-hover-bg-color: var(--bg-secondary);
  background-color: var(--bg-primary);
}

:deep(.el-table th.el-table__cell) {
  background-color: var(--bg-secondary);
}

:deep(.el-table tr) {
  background-color: var(--bg-primary);
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background-color: var(--bg-secondary);
  opacity: 0.5;
}
</style>
