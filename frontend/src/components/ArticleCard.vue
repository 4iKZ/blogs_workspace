<template>
  <article
    class="article-card"
    @click="navigateToArticle"
  >
    <!-- Content Section -->
    <div class="content-section">
      <!-- 移动端作者行（移动端专用，桌面端隐藏） -->
      <div class="mobile-meta">
        <img
          v-if="article.authorAvatar"
          :src="article.authorAvatar"
          :alt="article.authorNickname"
          class="author-avatar"
        >
        <span class="author-name">{{ article.authorNickname }}</span>
        <span class="meta-divider">•</span>
        <span class="mobile-category">{{ article.categoryName || (article.category && article.category.name) || '未分类' }}</span>
      </div>

      <h3 class="article-title">
        <router-link :to="`/article/${article.id}`">
          {{ article.title }}
        </router-link>
      </h3>
      
      <p class="article-excerpt">
        {{ article.summary }}
      </p>
      
      <!-- 元信息行（桌面端显示，移动端由 mobile-meta 承担） -->
      <div class="article-footer">
        <div class="article-meta">
          <span class="meta-item category-badge">
            {{ article.categoryName || (article.category && article.category.name) || '未分类' }}
          </span>
          <span class="meta-item">
            <i class="fas fa-user" />
            {{ article.authorNickname }}
          </span>
          <span class="meta-item">
            <i class="fas fa-eye" />
            {{ article.viewCount }}
          </span>
          <span class="meta-item">
            <i class="fas fa-heart" />
            {{ article.likeCount }}
          </span>
          <span class="meta-item">
            <i class="fas fa-comment" />
            {{ article.commentCount }}
          </span>
        </div>
      </div>
    </div>

    <!-- Article Cover Image -->
    <div
      v-if="article.coverImage"
      class="cover-section"
    >
      <img
        :src="article.coverImage"
        :alt="article.title"
        class="article-cover"
        loading="lazy"
        @error="handleImageError"
      >
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
    viewCount: number
    likeCount: number
    commentCount: number
    favoriteCount: number
    publishTime: string
  }
}

const props = defineProps<Props>()

const router = useRouter()

// 处理图片加载错误
const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.style.display = 'none'
  target.parentElement?.classList.add('image-error')
}

// 导航到文章详情
const navigateToArticle = (event: MouseEvent) => {
  const target = event.target as HTMLElement

  // 防止点击链接元素或按钮时重复跳转
  if (target.closest('a') || target.closest('button')) {
    return
  }

  if (!props.article.id) {
    return
  }

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

/* Author styles */
.author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  font-family: var(--font-sans);
}

/* Content Section */
.content-section {
  flex: 1;
  min-width: 0;
}

/* 移动端专用，桌面端隐藏 */
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
  color: var(--color-blue-500);
  font-weight: 500;
}

/* Article Title */
.article-title {
  margin: 0 0 var(--space-2) 0;
  font-family: var(--font-sans);
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

/* Article Footer - 元信息行 */
.article-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

/* Article Meta */
.article-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-wrap: wrap;
  font-family: var(--font-sans);
  font-variant-numeric: tabular-nums;
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
  font-weight: 500;
}

.dark .category-badge {
  background-color: rgba(148, 163, 184, 0.1);
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
    align-items: stretch;
    padding: 12px;
    gap: 12px;
  }

  .mobile-meta {
    display: flex;
    align-items: center;
  }

  .content-section {
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  .article-title {
    font-size: 1rem;
    margin-bottom: 6px;
    line-height: 1.35;
  }

  .article-title a {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    white-space: normal;
  }

  .article-excerpt {
    margin-bottom: 8px;
    font-size: 0.75rem;
    line-height: 1.45;
    -webkit-line-clamp: 1;
  }

  .article-footer {
    display: none;
  }

  .cover-section {
    width: 112px;
    height: 84px;
    margin-top: 0;
    box-shadow: none;
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .article-cover {
    width: 112px;
    height: 84px;
    max-height: none;
    aspect-ratio: auto;
    border-radius: var(--radius-sm);
  }

  .cover-section.image-error {
    min-height: 84px;
  }

  .cover-section.image-error::after {
    width: 24px;
    height: 24px;
  }

  /* 移动端触摸反馈 */
  .article-card:active {
    background-color: var(--bg-secondary);
    transform: scale(0.99);
    transition: transform 0.1s ease, background-color 0.1s ease;
  }
}

/* 大屏手机优化 */
@media (max-width: 640px) {
  .article-card {
    padding: 8px;
    gap: 8px;
  }

  .article-title {
    font-size: 0.875rem;
  }

  .article-excerpt {
    font-size: 0.75rem;
  }

  .cover-section,
  .article-cover {
    width: 96px;
    height: 72px;
  }

  .cover-section.image-error {
    min-height: 72px;
  }
}

/* 小屏手机优化 */
@media (max-width: 480px) {
  .article-card {
    padding: 8px;
    gap: 8px;
  }

  .article-title {
    font-size: 0.875rem;
    line-height: 1.3;
  }

  .article-excerpt {
    display: -webkit-box;
    -webkit-line-clamp: 1;
  }

  .cover-section,
  .article-cover {
    width: 88px;
    height: 66px;
  }

  .cover-section.image-error {
    min-height: 66px;
  }

  .cover-section.image-error::after {
    width: 20px;
    height: 20px;
  }
}

/* 图片加载失败样式（占位图标颜色取 --text-tertiary #94a3b8） */
.cover-section.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--bg-secondary) 0%, var(--border-color) 100%);
  min-height: 120px;
}

.cover-section.image-error::after {
  content: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='3' y='3' width='18' height='18' rx='2'/%3E%3Ccircle cx='8.5' cy='8.5' r='1.5'/%3E%3Cpath d='m21 15-5-5L5 21'/%3E%3C/svg%3E");
  width: 32px;
  height: 32px;
  opacity: 0.5;
}

/* 图片加载占位 */
.article-cover {
  background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--border-color) 50%, var(--bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.article-cover[src] {
  background: transparent;
  animation: none;
}
</style>
