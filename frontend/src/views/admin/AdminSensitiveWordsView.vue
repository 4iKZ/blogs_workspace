<template>
  <div class="admin-sensitive-words">
    <div class="page-header">
      <h2>敏感词管理</h2>
      <div class="actions">
        <el-button type="warning" @click="handleReloadCache">
          <el-icon><Refresh /></el-icon>重载缓存
        </el-button>
        <el-button type="success" @click="handleBatchImport">
          <el-icon><Upload /></el-icon>批量导入
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增敏感词
        </el-button>
        <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>批量删除
        </el-button>
      </div>
    </div>

    <!-- 搜索筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryParams" @keyup.enter="handleSearch">
        <el-form-item label="关键字">
          <el-input
            v-model="queryParams.keyword"
            placeholder="请输入敏感词内容"
            clearable
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="queryParams.category"
            placeholder="请选择分类"
            clearable
            @change="handleSearch"
          >
            <el-option label="默认分类" value="default" />
            <el-option label="政治敏感" value="politics" />
            <el-option label="色情暴力" value="porn" />
            <el-option label="违禁品" value="contraband" />
            <el-option label="低俗词" value="vulgar" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表区 -->
    <el-card class="list-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="wordList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="word" label="敏感词" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="word-text">{{ row.word }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="getCategoryTagType(row.category)">
              {{ getCategoryName(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.level === 2 ? 'danger' : 'warning'">
              {{ row.level === 2 ? '禁止' : '警告' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </el-button-group>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="dialogType === 'add' ? '新增敏感词' : '编辑敏感词'"
      v-model="dialogVisible"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="formData.word" placeholder="请输入敏感词内容" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%" allow-create filterable>
            <el-option label="默认分类" value="default" />
            <el-option label="政治敏感" value="politics" />
            <el-option label="色情暴力" value="porn" />
            <el-option label="违禁品" value="contraband" />
            <el-option label="低俗词" value="vulgar" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-radio-group v-model="formData.level">
            <el-radio :label="1">警告 (1)</el-radio>
            <el-radio :label="2">禁止 (2)</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog
      title="批量导入敏感词"
      v-model="importDialogVisible"
      width="600px"
      @close="resetImportForm"
    >
      <el-form
        ref="importFormRef"
        :model="importFormData"
        :rules="importFormRules"
        label-width="80px"
      >
        <el-form-item label="分类" prop="category">
          <el-select v-model="importFormData.category" placeholder="请选择分类" style="width: 100%" allow-create filterable>
            <el-option label="默认分类" value="default" />
            <el-option label="政治敏感" value="politics" />
            <el-option label="色情暴力" value="porn" />
            <el-option label="违禁品" value="contraband" />
            <el-option label="低俗词" value="vulgar" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-radio-group v-model="importFormData.level">
            <el-radio :label="1">警告 (1)</el-radio>
            <el-radio :label="2">禁止 (2)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="敏感词" prop="wordsText">
          <el-input
            v-model="importFormData.wordsText"
            type="textarea"
            :rows="10"
            placeholder="请输入敏感词，每行一个。或者以英文逗号分隔"
          />
          <div class="form-tip">支持换行或以逗号、分号分隔</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitImport" :loading="importing">导入</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Search, Plus, Delete, Refresh, Upload } from '@element-plus/icons-vue'
import type { SensitiveWord, SensitiveWordCreateDTO } from '@/types/SensitiveWord'
import {
  getSensitiveWords,
  createSensitiveWord,
  updateSensitiveWord,
  deleteSensitiveWord,
  batchDeleteSensitiveWords,
  batchImportSensitiveWords,
  reloadSensitiveWordCache
} from '@/api/sensitiveWord'

// 列表查询相关
const loading = ref(false)
const wordList = ref<SensitiveWord[]>([])
const total = ref(0)
const queryParams = reactive({
  page: 1,
  size: 10,
  keyword: '',
  category: ''
})

const selectedIds = ref<number[]>([])

// 获取列表数据
const fetchList = async () => {
  loading.value = true
  try {
    const res = await getSensitiveWords(queryParams)
    if (res.code === 200) {
      wordList.value = res.data.list
      total.value = res.data.total
    } else {
      ElMessage.error(res.message || '获取列表失败')
    }
  } catch (error) {
    console.error('获取敏感词列表出错:', error)
    ElMessage.error('获取列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 查询与重置
const handleSearch = () => {
  queryParams.page = 1
  fetchList()
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.category = ''
  handleSearch()
}

// 分页处理
const handleSizeChange = (val: number) => {
  queryParams.size = val
  handleSearch()
}

const handleCurrentChange = (val: number) => {
  queryParams.page = val
  fetchList()
}

// 选项改变
const handleSelectionChange = (selection: SensitiveWord[]) => {
  selectedIds.value = selection.map(item => item.id)
}

// 字典翻译
const getCategoryName = (category: string) => {
  const map: Record<string, string> = {
    default: '默认分类',
    politics: '政治敏感',
    porn: '色情暴力',
    contraband: '违禁品',
    vulgar: '低俗词'
  }
  return map[category] || category
}

const getCategoryTagType = (category: string) => {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    default: 'info',
    politics: 'danger',
    porn: 'danger',
    contraband: 'warning',
    vulgar: 'warning'
  }
  return map[category] || 'info'
}

// 表单相关
const dialogVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentId = ref<number | null>(null)

const formData = reactive<SensitiveWordCreateDTO>({
  word: '',
  category: 'default',
  level: 1
})

const formRules: FormRules = {
  word: [
    { required: true, message: '请输入敏感词', trigger: 'blur' },
    { max: 50, message: '不能超过50个字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  level: [
    { required: true, message: '请选择级别', trigger: 'change' }
  ]
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  formData.word = ''
  formData.category = 'default'
  formData.level = 1
  currentId.value = null
}

const handleAdd = () => {
  dialogType.value = 'add'
  dialogVisible.value = true
}

const handleEdit = (row: SensitiveWord) => {
  dialogType.value = 'edit'
  currentId.value = row.id
  formData.word = row.word
  formData.category = row.category
  formData.level = row.level
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const reqItem = { ...formData }
        let res
        if (dialogType.value === 'add') {
          res = await createSensitiveWord(reqItem)
        } else if (currentId.value) {
          res = await updateSensitiveWord(currentId.value, reqItem)
        }
        
        if (res && res.code === 200) {
          ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
          dialogVisible.value = false
          fetchList()
        } else {
          ElMessage.error(res?.message || '操作失败')
        }
      } catch (error) {
        console.error('提交敏感词出错:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 删除
const handleDelete = (row: SensitiveWord) => {
  ElMessageBox.confirm(`确定要删除敏感词 "${row.word}" 吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteSensitiveWord(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        if (wordList.value.length === 1 && queryParams.page > 1) {
          queryParams.page--
        }
        fetchList()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除敏感词出错:', error)
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return
  
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个敏感词吗？`, '批量删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await batchDeleteSensitiveWords(selectedIds.value)
      if (res.code === 200) {
        ElMessage.success('批量删除成功')
        fetchList()
        selectedIds.value = []
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('批量删除敏感词出错:', error)
    }
  }).catch(() => {})
}

// 批量导入
const importDialogVisible = ref(false)
const importFormRef = ref<FormInstance>()
const importing = ref(false)

const importFormData = reactive({
  category: 'default',
  level: 1,
  wordsText: ''
})

const importFormRules: FormRules = {
  wordsText: [
    { required: true, message: '请输入要导入的敏感词', trigger: 'blur' }
  ]
}

const resetImportForm = () => {
  if (importFormRef.value) {
    importFormRef.value.resetFields()
  }
}

const handleBatchImport = () => {
  importDialogVisible.value = true
}

const submitImport = async () => {
  if (!importFormRef.value) return
  await importFormRef.value.validate(async (valid) => {
    if (valid) {
      importing.value = true
      try {
        // 解析文本为数组，支持换行、逗号、分号分隔
        const words = importFormData.wordsText
          .split(/[\n,;，；]/)
          .map(w => w.trim())
          .filter(w => w.length > 0)
        
        if (words.length === 0) {
          ElMessage.warning('没有解析到有效的敏感词')
          importing.value = false
          return
        }
        
        const res = await batchImportSensitiveWords({
          words,
          category: importFormData.category,
          level: importFormData.level
        })
        
        if (res.code === 200) {
          ElMessage.success(`成功导入 ${res.data} 个敏感词`)
          importDialogVisible.value = false
          // 跳转到第一页并刷新列表
          queryParams.page = 1
          fetchList()
        } else {
          ElMessage.error(res.message || '导入失败')
        }
      } catch (error) {
        console.error('批量导入敏感词出错:', error)
      } finally {
        importing.value = false
      }
    }
  })
}

// 重载缓存
const handleReloadCache = () => {
  ElMessageBox.confirm('重载缓存会将数据库中的敏感词重新加载到内存中，确认操作？', '重载缓存', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      const res = await reloadSensitiveWordCache()
      if (res.code === 200) {
        ElMessage.success('重载缓存成功')
      } else {
        ElMessage.error(res.message || '重载缓存失败')
      }
    } catch (error) {
      console.error('重载缓存出错:', error)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.admin-sensitive-words {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.filter-card {
  margin-bottom: 2px;
}

.word-text {
  font-weight: 500;
  color: var(--el-color-danger);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
</style>
