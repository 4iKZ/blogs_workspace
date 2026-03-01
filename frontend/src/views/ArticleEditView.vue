<template>
  <!-- 全屏编辑界面 -->
  <div class="article-editor-fullscreen">
    <!-- 固定顶部工具栏 -->
    <header class="editor-header">
      <div class="header-left">
        <el-button
          text
          @click="handleBack"
          class="back-btn"
          :icon="ArrowLeft"
        >
          返回
        </el-button>
      </div>

      <div class="header-right">
        <!-- 字数统计 -->
        <span class="word-count">{{ wordCount }} 字</span>

        <el-button type="primary" @click="showPublishDrawer">
          发布文章
        </el-button>
      </div>
    </header>

    <!-- 编辑区域 -->
    <div class="editor-container">
      <!-- 大字号无边框标题输入 -->
      <input
        v-model="articleForm.title"
        type="text"
        class="title-input"
        placeholder="输入文章标题..."
        maxlength="100"
      />

      <!-- Markdown编辑器 -->
        <MdEditor
          v-model="articleForm.content"
          :toolbars="toolbars"
          :preview="true"
          :toolbarsExclude="['github']"
          @on-upload-img="handleUploadImg"
          class="md-editor"
          placeholder="开始写作..."
          :theme="currentTheme"
        />
    </div>

    <!-- 发布设置弹框 -->
    <PublishDrawer
      v-model="publishDrawerVisible"
      :categories="categories"
      :initial-data="publishFormData"
      @publish="handlePublish"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { toast } from '@/composables/useLuminaToast'
import { ArrowLeft } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import type { Themes } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import PublishDrawer from '../components/article/PublishDrawer.vue'
import { useUserStore } from '../store/user'
import axios from '../utils/axios'
import {
  compressImageWithWorker,
  needsCompression
} from '../utils/enhancedImageCompressor'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 判断是否为编辑模式
const isEditing = computed(() => !!route.params.id)

// 状态管理
const publishDrawerVisible = ref(false)

// 主题状态管理
const currentTheme = ref<Themes>('light')

// 初始化主题
const initTheme = () => {
  if (localStorage.theme === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    currentTheme.value = 'dark'
  } else {
    currentTheme.value = 'light'
  }
}

// 监听主题变化
const handleThemeChange = (e: MediaQueryListEvent) => {
  if (e.matches) {
    currentTheme.value = 'dark'
  } else {
    currentTheme.value = 'light'
  }
}

// 分类数据
const categories = ref<any[]>([])

// 文章表单
const articleForm = ref({
  id: 0,
  title: '',
  content: '',
  summary: '',
  coverImage: '',
  categoryId: 11, // 默认分类：技术分享
  topicId: 0,
  status: 1,
  allowComment: 1
})

// 发布表单数据
const publishFormData = computed(() => ({
  categoryId: articleForm.value.categoryId,
  summary: articleForm.value.summary,
  coverImage: articleForm.value.coverImage,
  topicId: articleForm.value.topicId
}))

// 字数统计
const wordCount = computed(() => {
  const content = articleForm.value.content || ''
  // 移除Markdown语法后统计字数
  const plainText = content
    .replace(/```[\s\S]*?```/g, '') // 代码块
    .replace(/`[^`]*`/g, '') // 行内代码
    .replace(/!?\[[^\]]*\]\([^)]*\)/g, '') // 链接和图片
    .replace(/[#*_~`>-]/g, '') // Markdown符号
    .replace(/\s+/g, '') // 空白字符
  return plainText.length
})

// Markdown编辑器工具栏配置
const toolbars: typeof MdEditor['toolbars'] = [
  'bold',
  'underline',
  'italic',
  'strikeThrough',
  '-',
  'title',
  'sub',
  'sup',
  'quote',
  'unorderedList',
  'orderedList',
  'task',
  '-',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  '-',
  'revoke',
  'next',
  '=',
  'pageFullscreen',
  'fullscreen',
  'preview',
  'catalog'
]

// 获取分类列表
const getCategories = async () => {
  try {
    const response = await axios.get('/category/list')
    categories.value = response
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

// 获取文章详情（编辑模式下）
const getArticleDetail = async () => {
  try {
    const articleId = Number(route.params.id)
    const response = await axios.get(`/article/${articleId}`)
    const article = response

    articleForm.value = {
      id: article.id,
      title: article.title,
      content: article.content,
      summary: article.summary,
      coverImage: article.coverImage || '',
      categoryId: article.categoryId,
      topicId: article.topicId || 0,
      status: article.status,
      allowComment: article.allowComment
    }
  } catch (error) {
    console.error('获取文章详情失败:', error)
    toast.error('获取文章详情失败')
    router.push('/')
  }
}

// 图片上传处理（后台静默压缩上传）
const handleUploadImg = async (files: File[], callback: (urls: string[]) => void) => {
  const file = files[0]

  if (!file) {
    toast.error('未选择文件')
    return
  }

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

  try {
    let fileToUpload = file

    // 如果需要压缩，在后台静默压缩
    if (needsCompression(file)) {
      console.log('[图片上传] 开始后台压缩...')
      const result = await compressImageWithWorker(file, {
        maxWidth: 2048,
        maxHeight: 2048,
        quality: 0.8,
        maxSize: 5 * 1024 * 1024,
        preserveRatio: true
      })

      if (result.success && result.file) {
        fileToUpload = result.file
        console.log('[图片上传] 压缩完成:', {
          原始大小: `${(file.size / 1024).toFixed(1)}KB`,
          压缩后: `${(result.compressedSize / 1024).toFixed(1)}KB`,
          压缩率: `${result.compressionRatio.toFixed(1)}%`
        })
      }
    }

    // 上传图片
    const formData = new FormData()
    formData.append('file', fileToUpload)

    const response = await axios.post('/article/upload-cover', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    if (response) {
      callback([response])
      toast.success('图片上传成功')
    } else {
      toast.error('图片上传失败')
    }

  } catch (error: any) {
    console.error('[图片上传] 失败:', error)
    toast.error(error.message || '图片上传失败')
  }
}

// 显示发布抽屉
const showPublishDrawer = () => {
  if (!articleForm.value.title) {
    toast.warning('请输入文章标题')
    return
  }

  if (!articleForm.value.content) {
    toast.warning('请输入文章内容')
    return
  }

  publishDrawerVisible.value = true
}

// 发布文章
const handlePublish = async (publishData: any) => {
  try {
    // 合并表单数据和发布设置
    const submitData = {
      ...articleForm.value,
      ...publishData,
      status: 2 // 已发布状态
    }

    let response
    if (isEditing.value) {
      response = await axios.put(`/article/${articleForm.value.id}`, submitData)
      toast.success('文章发布成功')
    } else {
      response = await axios.post('/article/publish', submitData)
      toast.success('文章发布成功')
    }

    publishDrawerVisible.value = false

    // 跳转到文章详情页
    router.push(`/article/${isEditing.value ? articleForm.value.id : response}`)
  } catch (error: any) {
    toast.error(error.message || '发布失败')
  }
}

// 返回处理
const handleBack = () => {
  if (articleForm.value.title || articleForm.value.content) {
    ElMessageBox.confirm('确定要离开吗？未保存的内容可能会丢失', '提示', {
      confirmButtonText: '确定离开',
      cancelButtonText: '继续编辑',
      type: 'warning'
    }).then(() => {
      router.back()
    }).catch(() => {
      // 继续编辑
    })
  } else {
    router.back()
  }
}

// 初始化数据
onMounted(() => {
  initTheme()
  getCategories()

  if (isEditing.value) {
    getArticleDetail()
  }

  // 监听系统主题变化
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', handleThemeChange)
})

// 组件卸载前移除事件监听
onBeforeUnmount(() => {
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.removeEventListener('change', handleThemeChange)
})

// 监听localStorage主题变化
window.addEventListener('storage', (e) => {
  if (e.key === 'theme') {
    currentTheme.value = e.newValue as Themes
  }
})
</script>

<style scoped>
/* 全屏编辑器容器 */
.article-editor-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #fff;
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

/* 顶部工具栏 */
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  border-bottom: 1px solid #e4e6eb;
  background-color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  font-size: 15px;
  color: #1d2129;
}

.back-btn:hover {
  color: #1e80ff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.word-count {
  font-size: 13px;
  color: #86909c;
  padding: 0 12px;
}

/* 编辑区域 */
.editor-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

/* 大字号无边框标题输入 */
.title-input {
  width: 100%;
  font-size: 32px;
  font-weight: 600;
  line-height: 1.4;
  border: none;
  outline: none;
  padding: 12px 0;
  margin-bottom: 16px;
  color: #1d2129;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.title-input::placeholder {
  color: #c9cdd4;
  font-weight: 400;
}

/* Markdown编辑器样式 */
.md-editor {
  height: calc(100vh - 240px);
  border: none;
}

:deep(.md-editor-preview-wrapper),
:deep(.md-editor-input-wrapper) {
  padding: 16px;
}

:deep(.md-editor-input) {
  font-size: 16px;
  line-height: 1.8;
  color: #1d2129;
}

/* md-editor-v3 暗色主题样式调整 */
.dark .article-editor-fullscreen {
  background-color: var(--bg-primary);
}

.dark .editor-header {
  background-color: var(--bg-primary);
  border-bottom-color: var(--border-color);
}

.dark .title-input {
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.dark .title-input::placeholder {
  color: var(--text-tertiary);
}

.dark :deep(.md-editor-wrapper) {
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.dark :deep(.md-editor-toolbar) {
  background-color: var(--bg-secondary);
  border-bottom-color: var(--border-color);
}

.dark :deep(.md-editor-input-wrapper),
.dark :deep(.md-editor-preview-wrapper) {
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.dark :deep(.md-editor-input) {
  color: var(--text-primary);
  background-color: var(--bg-primary);
}

.dark :deep(.md-editor-preview h1),
.dark :deep(.md-editor-preview h2),
.dark :deep(.md-editor-preview h3),
.dark :deep(.md-editor-preview h4),
.dark :deep(.md-editor-preview h5),
.dark :deep(.md-editor-preview h6) {
  color: var(--text-primary);
}

.dark :deep(.md-editor-preview p),
.dark :deep(.md-editor-preview ul),
.dark :deep(.md-editor-preview ol) {
  color: var(--text-secondary);
}

.dark :deep(.md-editor-preview code) {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border-color: var(--border-color);
}

.dark :deep(.md-editor-preview pre) {
  background-color: var(--bg-secondary);
  border-color: var(--border-color);
}

.dark :deep(.md-editor-preview blockquote) {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
}

/* 响应式适配 */
@media (max-width: 768px) {
  .editor-container {
    padding: 16px;
  }

  .title-input {
    font-size: 24px;
  }

  .md-editor {
    height: calc(100vh - 200px);
  }

  .editor-header {
    padding: 12px 16px;
  }

  .word-count {
    display: none;
  }
}
</style>
