<template>
  <div class="favorite-button-wrapper">
    <button
      :class="['favorite-button', { active: favorited, loading }]"
      :disabled="loading"
      @click="handleFavorite"
      @mouseenter="handleHover"
      @mouseleave="handleLeave"
      ref="buttonRef"
    >
      <div class="button-inner">
        <!-- 星星图标容器 -->
        <div class="icon-container">
          <!-- SVG 星星 -->
          <svg class="star-icon" viewBox="0 0 24 24" fill="none">
            <path
              class="star-path"
              d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
              fill="currentColor"
            />
          </svg>
          <!-- 激活状态的光芒 -->
          <div class="sparkle-container" v-if="favorited">
            <div class="sparkle sparkle-1"></div>
            <div class="sparkle sparkle-2"></div>
            <div class="sparkle sparkle-3"></div>
            <div class="sparkle sparkle-4"></div>
          </div>
        </div>

        <!-- 计数标签 -->
        <span class="count-label">{{ displayCount }}</span>
      </div>

      <!-- 星星粒子容器 -->
      <div class="stars-particles-container" ref="particlesRef">
        <div
          v-for="particle in particles"
          :key="particle.id"
          class="star-particle"
          :style="particle.style"
        ></div>
      </div>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/composables/useLuminaToast'
import { articleService } from '../../services/articleService'
import { useUserStore } from '../../store/user'

const router = useRouter()

interface Particle {
  id: number
  style: Record<string, string>
}

interface Props {
  articleId: number | string
  initialFavorited?: boolean
  initialCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  initialFavorited: false,
  initialCount: 0,
})

const emit = defineEmits<{
  (e: 'update', favorited: boolean, count: number): void
}>()

const userStore = useUserStore()
const favorited = ref(props.initialFavorited)
const favoriteCount = ref(props.initialCount)
const loading = ref(false)
const buttonRef = ref<HTMLElement | null>(null)
const particlesRef = ref<HTMLElement | null>(null)
const particles = ref<Particle[]>([])

// 格式化计数显示
const displayCount = computed(() => {
  if (favoriteCount.value >= 1000) {
    return `${(favoriteCount.value / 1000).toFixed(1)}k`
  }
  return favoriteCount.value.toString()
})

// 防抖定时器
let debounceTimer: number | null = null

watch(() => props.initialFavorited, (val) => {
  favorited.value = val
})

watch(() => props.initialCount, (val) => {
  favoriteCount.value = val
})

// 监听用户登录状态变化
watch(() => userStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn && Number(props.articleId) > 0) {
    checkFavoriteStatus()
  }
})

// 组件挂载时检查收藏状态
onMounted(async () => {
  if (Number(props.articleId) > 0 && userStore.isLoggedIn) {
    await checkFavoriteStatus()
  }
})

// 检查收藏状态
async function checkFavoriteStatus() {
  if (!userStore.isLoggedIn || Number(props.articleId) <= 0) return
  try {
    const isFavorited = await articleService.checkFavoriteStatus(Number(props.articleId))
    if (isFavorited !== favorited.value) {
      favorited.value = isFavorited
      emit('update', isFavorited, favoriteCount.value)
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

// 创建星星粒子效果
function createStarParticles() {
  if (!particlesRef.value || !buttonRef.value) return

  void buttonRef.value.getBoundingClientRect()
  const newParticles: Particle[] = []
  const particleCount = 8

  for (let i = 0; i < particleCount; i++) {
    const angle = (i / particleCount) * Math.PI * 2
    const distance = 35 + Math.random() * 25
    const duration = 700 + Math.random() * 400
    const delay = Math.random() * 150
    const rotation = Math.random() * 360

    newParticles.push({
      id: Date.now() + i,
      style: {
        '--tx': `${Math.cos(angle) * distance}px`,
        '--ty': `${Math.sin(angle) * distance - 15}px`,
        '--duration': `${duration}ms`,
        '--delay': `${delay}ms`,
        '--rotation': `${rotation}deg`,
        '--scale': `${String(0.5 + Math.random() * 0.5)}`,
      }
    })
  }

  particles.value = newParticles

  setTimeout(() => {
    particles.value = []
  }, 1800)
}

// 悬停效果
function handleHover() {
  if (buttonRef.value && !loading.value) {
    buttonRef.value.style.setProperty('--hover-scale', '1.05')
  }
}

function handleLeave() {
  if (buttonRef.value) {
    buttonRef.value.style.setProperty('--hover-scale', '1')
  }
}

// 处理收藏操作（带 300ms 防抖）
function handleFavorite() {
  if (debounceTimer !== null) {
    clearTimeout(debounceTimer)
  }

  debounceTimer = window.setTimeout(async () => {
    await doFavorite()
    debounceTimer = null
  }, 300)
}

// 实际执行收藏逻辑
async function doFavorite() {
  if (loading.value) return

  const previousFavorited = favorited.value
  const previousCount = favoriteCount.value
  const willFavorite = !favorited.value

  loading.value = true
  try {
    const articleId = Number(props.articleId)

    // 先执行收藏/取消收藏操作
    if (willFavorite) {
      await articleService.favoriteArticle(articleId)
      favorited.value = true
      favoriteCount.value = previousCount + 1
      toast.favorite('收藏成功')
      // 创建粒子效果
      await nextTick()
      createStarParticles()
    } else {
      await articleService.unfavoriteArticle(articleId)
      favorited.value = false
      favoriteCount.value = Math.max(0, previousCount - 1)
      toast.success('取消收藏')
    }

    emit('update', favorited.value, favoriteCount.value)
  } catch (error: any) {
    // 回滚状态
    favorited.value = previousFavorited
    favoriteCount.value = previousCount

    if (error.response?.status === 401) {
      router.push('/login')
    } else if (!error._handled) {
      toast.error(error.response?.data?.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.favorite-button-wrapper {
  display: inline-flex;
  align-items: center;
}

.favorite-button {
  position: relative;
  padding: 7px 14px;
  background: var(--bg-card);
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  overflow: visible;
  outline: none;
  --hover-scale: 1;
  transform: scale(var(--hover-scale));
}

.favorite-button:hover:not(:disabled) {
  border-color: var(--color-amber-400);
  background: rgba(251, 191, 36, 0.02);
}

.favorite-button:active:not(:disabled) {
  transform: scale(0.95);
}

.favorite-button.active {
  border-color: var(--color-amber-400);
  background: rgba(251, 191, 36, 0.08);
}

.favorite-button.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.button-inner {
  display: flex;
  align-items: center;
  gap: 7px;
}

.icon-container {
  position: relative;
  width: 17px;
  height: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.star-icon {
  width: 17px;
  height: 17px;
  color: var(--text-tertiary);
  transition: all var(--duration-normal) var(--ease-spring);
}

.favorite-button:hover .star-icon {
  color: var(--color-amber-400);
  transform: scale(1.1) rotate(5deg);
}

.favorite-button.active .star-icon {
  color: var(--color-amber-400);
  animation: star-pop 0.6s var(--ease-spring);
}

@keyframes star-pop {
  0% {
    transform: scale(1) rotate(0deg);
  }
  30% {
    transform: scale(1.4) rotate(15deg);
  }
  60% {
    transform: scale(0.85) rotate(-5deg);
  }
  100% {
    transform: scale(1) rotate(0deg);
  }
}

/* 星星光芒效果 */
.sparkle-container {
  position: absolute;
  inset: -12px;
  pointer-events: none;
}

.sparkle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: var(--color-amber-400);
  border-radius: 50%;
  animation: sparkle-twinkle 2s ease-in-out infinite;
}

.sparkle-1 {
  top: 0;
  left: 50%;
  animation-delay: 0s;
}

.sparkle-2 {
  top: 50%;
  right: 0;
  animation-delay: 0.5s;
}

.sparkle-3 {
  bottom: 0;
  left: 50%;
  animation-delay: 1s;
}

.sparkle-4 {
  top: 50%;
  left: 0;
  animation-delay: 1.5s;
}

@keyframes sparkle-twinkle {
  0%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

.count-label {
  font-size: var(--text-sm);
  font-weight: 600;
  font-family: var(--font-mono);
  color: var(--text-secondary);
  transition: color var(--duration-fast) var(--ease-default);
  min-width: 32px;
  text-align: center;
}

.favorite-button:hover .count-label {
  color: var(--color-amber-500);
}

.favorite-button.active .count-label {
  color: var(--color-amber-500);
}

/* 星星粒子系统 */
.stars-particles-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: visible;
}

.star-particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 12px;
  height: 12px;
  opacity: 0.8;
  animation: star-particle-fly var(--duration, 900ms) cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
  animation-delay: var(--delay, 0ms);
}

.star-particle::before {
  content: '★';
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--color-amber-400);
  transform: scale(var(--scale, 1)) rotate(var(--rotation, 0deg));
}

@keyframes star-particle-fly {
  0% {
    transform: translate(-50%, -50%) scale(0) rotate(0deg);
    opacity: 1;
  }
  50% {
    opacity: 1;
  }
  100% {
    transform: translate(calc(-50% + var(--tx, 0px)), calc(-50% + var(--ty, 0px))) scale(0) rotate(180deg);
    opacity: 0;
  }
}

/* Dark mode */
.dark .favorite-button {
  background: var(--bg-card);
  border-color: var(--border-color);
}

.dark .favorite-button:hover:not(:disabled) {
  background: rgba(251, 191, 36, 0.05);
  border-color: var(--color-amber-400);
}

.dark .favorite-button.active {
  background: rgba(251, 191, 36, 0.1);
  border-color: var(--color-amber-400);
}

.dark .star-icon {
  color: var(--text-tertiary);
}

.dark .favorite-button:hover .star-icon {
  color: var(--color-amber-400);
}

.dark .favorite-button.active .star-icon {
  color: var(--color-amber-400);
}

.dark .count-label {
  color: var(--text-secondary);
}

.dark .sparkle {
  background: var(--color-amber-400);
}

.dark .star-particle::before {
  color: var(--color-amber-400);
}

/* 移动端 */
@media (max-width: 768px) {
  .favorite-button {
    padding: 6px 10px;
    min-width: auto;
  }

  .icon-container {
    width: 15px;
    height: 15px;
  }

  .star-icon {
    width: 15px;
    height: 15px;
  }

  .count-label {
    display: none;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .favorite-button:active:not(:disabled) {
    transform: scale(0.92);
    background: rgba(251, 191, 36, 0.12);
  }

  .favorite-button.active:active:not(:disabled) {
    background: rgba(251, 191, 36, 0.15);
  }
}
</style>
