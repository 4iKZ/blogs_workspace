<template>
  <div class="left-sidebar-content">
    <!-- 关注标签 -->
    <div class="sidebar-item" :class="{ active: activeTab === 'follow' }" @click="handleTabClick('follow')">
      <SvgIcon name="user" size="16px" class="item-icon" />
      <span class="item-text">关注</span>
      <span v-if="hasNew" class="new-badge">●</span>
    </div>

    <!-- 综合标签 -->
    <div class="sidebar-item" :class="{ active: activeTab === 'all' }" @click="handleTabClick('all')">
      <SvgIcon name="articles" size="16px" class="item-icon" />
      <span class="item-text">综合</span>
    </div>

    <!-- 分类列表 -->
    <div class="category-section">
      <div class="section-title">分类</div>
      <div 
        v-for="category in categories" 
        :key="category.id"
        class="sidebar-item category-item"
        :class="{ active: activeCategoryId === category.id }"
        @click="handleCategoryClick(category.id)"
      >
        <SvgIcon :name="category.icon || getCategoryIcon(category.name)" size="16px" class="item-icon" />
        <span class="item-text">{{ category.name }}</span>
      </div>
    </div>


  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SvgIcon from './SvgIcon.vue'
import { categoryService } from '../services/categoryService'

const router = useRouter()
const route = useRoute()

// 状态
const activeTab = ref('all')
const activeCategoryId = ref<number | null>(null)
const hasNew = ref(false)
const loading = ref(false)

// 分类数据 - 从API获取
const categories = ref<any[]>([])

// 获取分类图标
const getCategoryIcon = (name: string) => {
  const iconMap: Record<string, string> = {
    '后端': 'code',
    '前端': 'layout',
    'Android': 'android',
    'iOS': 'apple',
    '人工智能': 'ai',
    '开发工具': 'tool',
    '代码人生': 'user',
    '阅读': 'book',
    '技术分享': 'code',
    '生活随笔': 'book',
    '学习笔记': 'calendar',
    '项目经验': 'code',
    '工具推荐': 'tool',
    'Java开发': 'code',
    '前端技术': 'layout',
    '数据库': 'articles',
    '运维部署': 'settings'
  }
  return iconMap[name] || 'articles'
}

// 标签点击
const handleTabClick = (tab: string) => {
  activeTab.value = tab
  activeCategoryId.value = null
  
  if (tab === 'all') {
    router.push('/')
  } else if (tab === 'follow') {
    // 跳转到关注的文章页面
    router.push('/following')
  }
}

// 分类点击
const handleCategoryClick = (categoryId: number) => {
  activeTab.value = ''
  activeCategoryId.value = categoryId
  router.push(`/category/${categoryId}`)
}

// 获取分类列表
const getCategories = async () => {
  loading.value = true
  try {
    const response = await categoryService.getList()
    // axios 拦截器已经解包了 data，response 直接就是数组
    categories.value = response || []
    console.log('获取分类列表成功:', categories.value)
  } catch (error) {
    console.error('获取分类列表失败:', error)
    categories.value = []
  } finally {
    loading.value = false
  }
}

// 初始化激活状态
const initActiveState = () => {
  const path = route.path
  if (path === '/') {
    activeTab.value = 'all'
  } else if (path === '/following') {
    activeTab.value = 'follow'
  } else if (path.startsWith('/category/')) {
    const categoryId = Number(route.params.id)
    activeCategoryId.value = categoryId
    activeTab.value = ''
  }
}

onMounted(() => {
  getCategories()
  initActiveState()
})

// 调试：监听 categories 变化
watch(() => categories.value, (newVal) => {
  console.log('[LeftSidebar] categories 变化:', {
    长度: newVal?.length,
    数据: newVal,
    类型: typeof newVal,
    是否为数组: Array.isArray(newVal)
  })
}, { deep: true, immediate: true })
</script>

<style scoped>
.left-sidebar-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;
  position: relative;
  color: var(--text-secondary);
}

.sidebar-item:hover {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.sidebar-item.active {
  background-color: var(--color-blue-50);
  color: var(--color-blue-500);
  font-weight: 500;
}

.dark .sidebar-item.active {
  background-color: rgba(59, 130, 246, 0.1);
}

.item-icon {
  flex-shrink: 0;
}

.item-text {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.new-badge {
  color: #ff4d4f;
  font-size: 12px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 分类区域 */
.category-section {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-color);
}

.section-title {
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.category-item {
  font-size: 13px;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-tertiary);
}
</style>
