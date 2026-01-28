<template>
  <article class="article-card" @click="navigateToArticle">
    

    <!-- Content Section -->
    <div class="content-section">
      <!-- Mobile Author (Mobile Only) -->
      <div class="mobile-meta">
        <img v-if="article.authorAvatar" :src="article.authorAvatar" :alt="article.authorNickname" class="author-avatar" />
        <span class="author-name">{{ article.authorNickname }}</span>
        <span class="meta-divider">•</span>
        <span class="mobile-category">{{ article.categoryName || (article.category && article.category.name) || '未分类' }}</span>
      </div>

      <h3 class="article-title">
        <router-link :to="`/article/${article.id}`">{{ article.title }}</router-link>
      </h3>
      
      <p class="article-excerpt">{{ article.summary }}</p>
      
      <!-- Metadata Row with Read More -->
      <div class="article-footer">
        <div class="article-meta">
          <span class="meta-item category-badge">
            {{ article.categoryName || (article.category && article.category.name) || '未分类' }}
          </span>
          <span class="meta-item">
            <i class="fas fa-user"></i>
            {{ article.authorNickname }}
          </span>
          <span class="meta-item">
            <i class="fas fa-eye"></i>
            {{ article.viewCount }}
          </span>
          <span class="meta-item">
            <i class="fas fa-heart"></i>
            {{ article.likeCount }}
          </span>
          <span class="meta-item">
            <i class="fas fa-comment"></i>
            {{ article.commentCount }}
          </span>
        </div>
        
        <div class="read-more">
          <span class="read-more-text">阅读全文</span>
          <i class="fas fa-arrow-right read-more-icon"></i>
        </div>
      </div>

      <!-- Tags -->
      <div class="article-tags" v-if="article.tags && article.tags.length > 0">
        <router-link 
          v-for="tag in article.tags" 
          :key="tag.id" 
          :to="`/tag/${tag.id}`" 
          class="tag"
        >
          #{{ tag.name }}
        </router-link>
      </div>
    </div>

    <!-- Article Cover Image -->
    <div v-if="article.coverImage" class="cover-section">
      <img :src="article.coverImage" :alt="article.title" class="article-cover" loading="lazy" />
    </div>
  </article>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

// 组件属性
interface Props {
  article: {
    id: number | string
    title: string
    summary?: string
    coverImage?: string
    authorId: number
    authorNickname: string
    authorAvatar?: string
    categoryId: number
    categoryName: string
    category?: {
      id: number
      name: string
    }
    tags?: Array<{
      id: number
      name: string
      description?: string
      color?: string
    }>
    viewCount: number
    likeCount: number
    commentCount: number
    favoriteCount: number
    publishTime: string
  }
}

const props = defineProps<Props>()

const router = useRouter()

// 导航到文章详情
const navigateToArticle = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  
  // 添加调试日志
  console.log('=== 点击事件触发 ===')
  console.log('点击目标:', target)
  console.log('目标标签名:', target.tagName)
  console.log('目标类名:', target.className)
  console.log('查找最近的 <a>:', target.closest('a'))
  console.log('查找最近的 <button>:', target.closest('button'))
  
  // 防止点击链接元素或按钮时重复跳转
  if (target.closest('a') || target.closest('button')) {
    console.log('❌ 阻止跳转：点击了链接或按钮')
    return
  }
  
  console.log('✅ 执行跳转到:', `/article/${props.article.id}`)
  router.push(`/article/${props.article.id}`)
}
</script>

<style scoped>
.article-card {
  display: flex;
  gap: var(--space-6);
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--border-color);
  transition: all var(--duration-normal) var(--ease-default);
  cursor: pointer;
  background-color: transparent;
}

.article-card:last-child {
  border-bottom: none;
}

.article-card:hover {
  background-color: var(--bg-secondary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.article-card:hover .article-title a {
  color: var(--color-blue-500);
}

.article-card:hover .read-more {
  color: var(--color-blue-600);
}

.article-card:hover .read-more-icon {
  opacity: 1;
  transform: translateX(2px);
}

/* Author styles */
.author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-family: var(--font-mono);
}

/* Content Section */
.content-section {
  flex: 1;
  min-width: 0;
}

/* Mobile Meta - Hidden on Desktop */
.mobile-meta {
  display: none;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-bottom: var(--space-2);
  gap: var(--space-2);
}

 

.meta-divider {
  color: var(--text-tertiary);
}

.mobile-category {
  text-transform: uppercase;
  color: var(--color-blue-500);
  font-weight: 500;
}

/* Article Title */
.article-title {
  margin: 0 0 var(--space-2) 0;
  font-family: var(--font-serif);
  font-size: var(--text-2xl);
  font-weight: 700;
  line-height: var(--leading-snug);
}

.article-title a {
  color: var(--text-primary);
  text-decoration: none;
  transition: color var(--duration-normal) var(--ease-default);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Article Excerpt */
.article-excerpt {
  margin: 0 0 var(--space-3) 0;
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: var(--text-sm);
}

/* Article Footer - Meta and Read More in one row */
.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

/* Read More Link */
.read-more {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  background-color: transparent;
  transition: all var(--duration-fast) var(--ease-default);
}

.read-more:hover {
  color: var(--color-blue-600);
  background-color: rgba(59, 130, 246, 0.05);
  border-color: var(--color-blue-500);
}

.read-more-text {
  text-decoration: none;
}

.read-more-icon {
  font-size: 12px;
  color: var(--color-blue-500);
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--duration-fast) var(--ease-default);
}

/* Article Meta */
.article-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  transition: color var(--duration-fast) var(--ease-default);
}

.meta-item:hover {
  color: var(--color-blue-500);
}

.category-badge {
  background-color: var(--bg-secondary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 500;
}

.dark .category-badge {
  background-color: rgba(148, 163, 184, 0.1);
}

/* Article Tags */
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.tag {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  padding: var(--space-1) var(--space-3);
  background-color: transparent;
  color: var(--text-tertiary);
  text-decoration: none;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-color);
  transition: all var(--duration-fast) var(--ease-default);
}

.tag:hover {
  border-color: var(--color-blue-500);
  color: var(--color-blue-500);
  background-color: rgba(59, 130, 246, 0.05);
}

/* Cover Section */
.cover-section {
  width: 200px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--duration-normal) var(--ease-default);
}

.article-cover {
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
  transition: transform var(--duration-normal) var(--ease-default);
  display: block;
  border-radius: var(--radius-md);
}

.article-card:hover .article-cover {
  transform: scale(1.05);
}

.article-card:hover .cover-section {
  box-shadow: var(--shadow-md);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .cover-section {
    width: 180px;
  }
}

@media (max-width: 768px) {
  .article-card {
    flex-direction: column;
    padding: var(--space-4);
    gap: 0;
  }

  

  .mobile-meta {
    display: flex;
  }

  .article-title {
    font-size: var(--text-2xl);
    margin-bottom: var(--space-2);
  }

  .article-excerpt {
    margin-bottom: var(--space-3);
    font-size: var(--text-sm);
  }

  .read-more {
    margin-bottom: var(--space-3);
  }

  .article-meta {
    gap: var(--space-2);
    margin-bottom: var(--space-2);
  }

  .cover-section {
    width: 100%;
    margin-top: var(--space-3);
    box-shadow: none;
  }

  .article-cover {
    aspect-ratio: 16/9;
    height: auto;
  }
}
</style>
