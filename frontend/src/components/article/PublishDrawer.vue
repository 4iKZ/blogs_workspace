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
        <!-- 上传区域 -->
        <div
          class="cover-uploader"
          :class="{ 'is-dragover': isDragover, 'is-uploading': isUploading }"
          @click="!isUploading && handleSelectFile()"
          @dragover.prevent="isDragover = true"
          @dragleave.prevent="isDragover = false"
          @drop.prevent="handleFileDrop"
        >
          <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" />
          <div v-else class="cover-placeholder">
            <el-icon class="upload-icon"><Plus /></el-icon>
            <span class="upload-text">{{ isUploading ? '正在处理...' : '点击或拖拽上传封面' }}</span>
          </div>
        </div>

        <!-- 隐藏的文件输入 -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          style="display: none"
          @change="handleFileChange"
        />

        <div class="cover-tips">
          建议尺寸：1200 x 600 像素，支持JPG、PNG、GIF、WEBP格式
          <br>
          大文件将自动在后台压缩后上传
        </div>
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
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import { toast } from '@/composables/useLuminaToast'
import {
  compressImageWithWorker,
  needsCompression,
  uploadImage
} from '../../utils/enhancedImageCompressor'

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
const fileInputRef = ref<HTMLInputElement>()
const publishing = ref(false)
const isDragover = ref(false)
const isUploading = ref(false)

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

// 监听初始数据变化
watch(() => props.initialData, (newData) => {
  if (newData) {
    form.value = {
      ...form.value,
      ...newData
    }
  }
}, { immediate: true, deep: true })

// 选择文件
const handleSelectFile = () => {
  if (isUploading.value) {
    return
  }
  fileInputRef.value?.click()
}

// 文件选择变化
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (file) {
    processImageFile(file)
  }
  // 清空input，允许重复选择同一文件
  target.value = ''
}

// 拖拽文件
const handleFileDrop = (event: DragEvent) => {
  isDragover.value = false
  const file = event.dataTransfer?.files[0]
  if (file) {
    if (!file.type.startsWith('image/')) {
      toast.error('只能上传图片文件')
      return
    }
    processImageFile(file)
  }
}

// 处理图片文件（后台静默压缩上传）
const processImageFile = async (file: File) => {
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    toast.error('只能上传图片文件')
    return
  }

  // 验证文件大小（限制为50MB）
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    toast.error('文件大小不能超过50MB')
    return
  }

  isUploading.value = true

  try {
    let fileToUpload = file

    // 如果需要压缩，在后台静默压缩
    if (needsCompression(file)) {
      console.log('[封面上传] 开始后台压缩...')
      const result = await compressImageWithWorker(file, {
        maxWidth: 2048,
        maxHeight: 2048,
        quality: 0.8,
        maxSize: 5 * 1024 * 1024,
        preserveRatio: true
      })

      if (result.success && result.file) {
        fileToUpload = result.file
        console.log('[封面上传] 压缩完成:', {
          原始大小: `${(file.size / 1024).toFixed(1)}KB`,
          压缩后: `${(result.compressedSize / 1024).toFixed(1)}KB`,
          压缩率: `${result.compressionRatio.toFixed(1)}%`
        })
      }
    }

    // 上传文件（自动选择普通/分片上传）
    const imageUrl = await uploadImage(fileToUpload, userStore.token, {
      endpoint: '/article/upload-cover',
      chunkThreshold: 10 * 1024 * 1024 // 10MB以上使用分片
    })

    form.value.coverImage = imageUrl
    toast.success('封面上传成功')

  } catch (error: any) {
    console.error('[封面上传] 失败:', error)
    toast.error(error.message || '封面上传失败')
  } finally {
    isUploading.value = false
  }
}

// 删除封面
const removeCover = () => {
  form.value.coverImage = ''
  toast.success('封面已删除')
}

const handleClose = () => {
  visible.value = false
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
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  position: relative;
}

.cover-uploader.is-dragover {
  box-shadow: 0 0 0 2px var(--el-color-primary);
}

.cover-uploader.is-uploading {
  cursor: wait;
  opacity: 0.7;
}

.cover-uploader.is-uploading .cover-placeholder::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.8);
}

.cover-preview {
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
  border-radius: 8px;
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
  line-height: 1.5;
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
