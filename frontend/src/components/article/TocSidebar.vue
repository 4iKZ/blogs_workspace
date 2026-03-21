<template>
  <div class="toc-sidebar" v-if="tocTree.length > 0">
    <div class="toc-card">
      <h3 class="card-title">
        <svg class="toc-icon" viewBox="0 0 24 24" fill="none">
          <path
            d="M4 6h16M4 12h16M4 18h16"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        目录
      </h3>
      <nav class="toc-nav">
        <TocItem
          v-for="item in tocTree"
          :key="item.id"
          :item="item"
          :active-id="activeId"
          @navigate="handleTocClick"
        />
      </nav>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import TocItem from './TocItem.vue'

interface TocItemData {
  id: string
  title: string
  level: number
  index: number
  children: TocItemData[]
  expanded: boolean
}

const props = defineProps<{
  content: string
}>()

const route = useRoute()
const tocList = ref<TocItemData[]>([])
const tocTree = ref<TocItemData[]>([])
const activeId = ref<string>('')
const headingCount = ref(0)

// 生成标题 ID
const generateId = (index: number) => {
  return `heading-${index}`
}

// 解析 Markdown 内容生成目录
const parseToc = (content: string): TocItemData[] => {
  const headings: TocItemData[] = []
  const lines = content.split('\n')

  for (let index = 0; index < lines.length; index++) {
    const line = lines[index]
    const match = line.match(/^(#{1,6})\s+(.+)$/)
    if (match) {
      const level = match[1].length
      const title = match[2].trim()
      
      // 只包含 H1-H3 级别
      if (level <= 3) {
        headings.push({
          id: generateId(headings.length),
          title,
          level,
          index: headings.length,
          children: [],
          expanded: true
        })
      }
    }
  }

  return headings
}

// 将扁平列表转换为树形结构
const buildTree = (items: TocItemData[]): TocItemData[] => {
  const result: TocItemData[] = []
  const stack: TocItemData[] = []

  for (const item of items) {
    // 弹出栈中所有级别大于等于当前项的元素
    while (stack.length > 0 && stack[stack.length - 1].level >= item.level) {
      stack.pop()
    }

    if (stack.length === 0) {
      // 没有父级，添加到结果中
      result.push(item)
    } else {
      // 有父级，添加为子元素
      stack[stack.length - 1].children.push(item)
    }

    stack.push(item)
  }

  return result
}

// 在 Markdown 渲染后为标题添加 ID
const addIdsToHeadings = () => {
  nextTick(() => {
    // 获取文章内容区域
    const contentElement = document.querySelector('.article-content')
    if (!contentElement) return

    // 查找所有标题
    const headingElements = contentElement.querySelectorAll('h1, h2, h3')
    
    headingElements.forEach((heading, index) => {
      const id = `heading-${index}`
      heading.setAttribute('id', id)
      if (heading instanceof HTMLElement) {
        heading.style.scrollMarginTop = '100px' // 为固定头部预留空间
      }
    })
  })
}

// 处理目录点击
const handleTocClick = (item: TocItemData) => {
  const element = document.getElementById(item.id)
  if (element) {
    element.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
    
    // 更新 URL hash
    history.pushState(null, '', '#' + item.id)
    activeId.value = item.id
  }
}

// 收集所有标题用于滚动检测
const collectAllItems = (items: TocItemData[]): TocItemData[] => {
  const result: TocItemData[] = []
  const traverse = (list: TocItemData[]) => {
    for (const item of list) {
      result.push(item)
      if (item.children.length > 0) {
        traverse(item.children)
      }
    }
  }
  traverse(items)
  return result
}

// 监听滚动，高亮当前章节
const handleScroll = () => {
  if (tocList.value.length === 0) return
  
  try {
    const allItems = collectAllItems(tocTree.value)
    const headings = allItems
      .map(item => document.getElementById(item.id))
      .filter((el): el is HTMLElement => el !== null)
    
    if (headings.length === 0) return
    
    let currentId = ''

    for (const heading of headings) {
      const rect = heading.getBoundingClientRect()
      if (rect.top <= 150) {
        currentId = heading.id
      }
    }

    if (currentId) {
      activeId.value = currentId
    }
  } catch (error) {
    console.error('[TocSidebar] 滚动处理失败:', error)
  }
}

// 监听内容变化
watch(
  () => props.content,
  (newContent, oldContent) => {
    if (newContent !== oldContent) {
      try {
        tocList.value = parseToc(newContent || '')
        tocTree.value = buildTree(tocList.value)
        // 等待 MdPreview 渲染完成后添加 ID
        setTimeout(() => {
          addIdsToHeadings()
        }, 100)
      } catch (error) {
        console.error('[TocSidebar] 解析目录失败:', error)
        tocList.value = []
        tocTree.value = []
      }
    }
  },
  { immediate: true }
)

// 监听路由变化（切换文章时重置）
watch(
  () => route.params.id,
  () => {
    activeId.value = ''
    headingCount.value = 0
    try {
      tocList.value = parseToc(props.content || '')
      tocTree.value = buildTree(tocList.value)
      setTimeout(() => {
        addIdsToHeadings()
      }, 100)
    } catch (error) {
      console.error('[TocSidebar] 解析目录失败:', error)
      tocList.value = []
      tocTree.value = []
    }
  }
)

onMounted(() => {
  try {
    tocList.value = parseToc(props.content || '')
    tocTree.value = buildTree(tocList.value)
    addIdsToHeadings()
  } catch (error) {
    console.error('[TocSidebar] 解析目录失败:', error)
    tocList.value = []
    tocTree.value = []
  }
  window.addEventListener('scroll', handleScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.toc-sidebar {
  margin-bottom: var(--space-6);
  width: 100%;
}

.toc-card {
  background-color: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  transition: all var(--duration-normal) var(--ease-default);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  width: 100%;
}

.card-title {
  font-family: var(--font-serif);
  font-size: var(--text-xl);
  font-weight: 600;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.toc-icon {
  width: 20px;
  height: 20px;
  opacity: 0.8;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .toc-card {
    padding: var(--space-4);
  }

  .card-title {
    font-size: var(--text-lg);
  }
}

/* 暗色模式支持 */
.dark .toc-card {
  background-color: var(--bg-primary);
  border-color: var(--border-color);
}

.dark .card-title {
  color: var(--text-primary);
  border-bottom-color: var(--border-color);
}

.dark .toc-icon {
  opacity: 0.9;
}
</style>