<template>
  <el-dialog
    v-model="visible"
    title="发布文章"
    width="480px"
    :before-close="handleClose"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="80px"
      label-position="top"
      class="publish-form"
    >
      <!-- 分类 -->
      <el-form-item label="分类" prop="categoryId">
        <el-select
          v-model="form.categoryId"
          placeholder="请选择文章分类"
          style="width: 100%;"
          size="large"
        >
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>

      <!-- 创作话题 -->
      <el-form-item label="创作话题">
        <el-select
          v-model="form.topicId"
          placeholder="请选择创作话题"
          style="width: 100%;"
          size="large"
        >
          <el-option
            v-for="category in categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
      </el-form-item>

      <!-- 摘要 -->
      <el-form-item label="编辑摘要">
        <el-input
          v-model="form.summary"
          type="textarea"
          placeholder="请输入文章摘要（不超过200字）"
          maxlength="200"
          show-word-limit
          :rows="4"
        />
      </el-form-item>

      <!-- 封面 -->
      <el-form-item label="选择封面">
        <el-upload
          class="cover-uploader"
          action="/api/article/upload-cover"
          name="file"
          :show-file-list="false"
          :on-success="handleCoverUpload"
          :on-error="handleUploadError"
          :before-upload="beforeCoverUpload"
          :on-progress="handleUploadProgress"
          :headers="uploadHeaders"
          accept="image/jpeg,image/png,image/gif,image/webp"
        >
          <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" />
          <div v-else class="cover-placeholder">
            <el-icon class="upload-icon"><Plus /></el-icon>
            <span class="upload-text">上传封面</span>
            <span v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress">
              {{ uploadProgress }}%
            </span>
          </div>
        </el-upload>
        <div class="cover-tips">建议尺寸：1200 x 600 像素，支持JPG、PNG、GIF、WEBP格式，大小不超过5MB</div>
        <div class="cover-actions" v-if="form.coverImage">
          <el-button text type="danger" @click="removeCover">删除封面</el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" size="large">取消</el-button>
        <el-button type="primary" @click="handlePublish" size="large" :loading="publishing">
          确定并发布
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'

interface Category {
  id: number
  name: string
}

interface PublishForm {
  categoryId: number | undefined
  summary: string
  coverImage: string
  topicId: number | undefined
}

interface Props {
  modelValue: boolean
  categories: Category[]
  initialData?: Partial<PublishForm>
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'publish', data: PublishForm): void
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  categories: () => [],
  initialData: () => ({})
})

const emit = defineEmits<Emits>()

const userStore = useUserStore()
const formRef = ref()
const publishing = ref(false)
const uploadProgress = ref(0)

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const form = ref<PublishForm>({
  categoryId: undefined as any,
  summary: '',
  coverImage: '',
  topicId: undefined as any
})

const rules = {
  categoryId: [
    { required: true, message: '请选择文章分类', trigger: 'change' },
    { type: 'number', min: 1, message: '请选择有效的分类', trigger: 'change' }
  ]
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

// 监听初始数据变化
watch(() => props.initialData, (newData) => {
  if (newData) {
    form.value = {
      ...form.value,
      ...newData
    }
  }
}, { immediate: true, deep: true })

const handleCoverUpload = (response: any) => {
  if (response.code === 200 && response.data) {
    form.value.coverImage = response.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '封面上传失败')
  }
  uploadProgress.value = 0
}

// 上传前验证
const beforeCoverUpload = (file: File) => {
  // 验证文件类型
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  
  // 验证文件大小（5MB）
  const maxSize = 5 * 1024 * 1024 // 5MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过5MB！')
    return false
  }
  
  return true
}

// 上传进度处理
const handleUploadProgress = (event: any) => {
  if (event.total > 0) {
    uploadProgress.value = Math.round((event.loaded / event.total) * 100)
  }
}

// 上传错误处理
const handleUploadError = (error: any) => {
  ElMessage.error('封面上传失败：' + error.message)
  uploadProgress.value = 0
}

// 删除封面
const removeCover = () => {
  form.value.coverImage = ''
  ElMessage.success('封面已删除')
}

const handleClose = () => {
  visible.value = false
  uploadProgress.value = 0
}

const handlePublish = async () => {
  try {
    await formRef.value.validate()
    publishing.value = true
    emit('publish', { ...form.value })
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    publishing.value = false
  }
}
</script>

<style scoped>
.publish-form {
  padding: 0 20px 20px;
}

.cover-uploader {
  width: 100%;
}

.cover-preview {
    width: 100%;
    aspect-ratio: 16/9;
    object-fit: cover;
    border-radius: 8px;
    cursor: pointer;
  }

  .cover-placeholder {
    width: 100%;
    aspect-ratio: 16/9;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background-color: var(--bg-secondary);
    border: 2px dashed var(--border-color);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
  }

.cover-placeholder:hover {
  border-color: var(--color-blue-500);
  background-color: var(--bg-hover);
}

.upload-icon {
  font-size: 48px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.upload-text {
  color: var(--text-secondary);
  font-size: 14px;
}

.cover-tips {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.upload-progress {
  font-size: 14px;
  color: var(--color-blue-500);
  margin-left: 8px;
}

.cover-actions {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
}
</style>
