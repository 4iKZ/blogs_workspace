<template>
  <div class="like-button-wrapper">
    <button
      :class="['like-button', { active: liked, loading }]"
      :disabled="loading"
      @click="handleLike"
      @mouseenter="handleHover"
      @mouseleave="handleLeave"
      ref="buttonRef"
    >
      <div class="button-inner">
        <!-- 心形图标容器 -->
        <div class="icon-container">
          <!-- SVG 心形 -->
          <svg class="heart-icon" viewBox="0 0 24 24" fill="none">
            <path
              class="heart-path"
              d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
              fill="currentColor"
            />
          </svg>
          <!-- 激活状态的光晕 -->
          <div class="glow-effect" v-if="liked"></div>
        </div>

        <!-- 计数标签 -->
        <span class="count-label">{{ displayCount }}</span>
      </div>

      <!-- 粒子容器 -->
      <div class="particles-container" ref="particlesRef">
        <div
          v-for="particle in particles"
          :key="particle.id"
          class="particle"
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
  initialLiked?: boolean
  initialCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  initialLiked: false,
  initialCount: 0,
})

const emit = defineEmits<{
  (e: 'update', liked: boolean, count: number): void
}>()

const userStore = useUserStore()
const liked = ref(props.initialLiked)
const likeCount = ref(props.initialCount)
const loading = ref(false)
const buttonRef = ref<HTMLElement | null>(null)
const particlesRef = ref<HTMLElement | null>(null)
const particles = ref<Particle[]>([])

// 格式化计数显示
const displayCount = computed(() => {
  if (likeCount.value >= 1000) {
    return `${(likeCount.value / 1000).toFixed(1)}k`
  }
  return likeCount.value.toString()
})

// 防抖定时器
let debounceTimer: number | null = null

watch(() => props.initialLiked, (val) => {
  liked.value = val
})

watch(() => props.initialCount, (val) => {
  likeCount.value = val
})

// 监听用户登录状态变化
watch(() => userStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn && Number(props.articleId) > 0) {
    checkLikeStatus()
  }
})

// 组件挂载时检查点赞状态
onMounted(async () => {
  if (Number(props.articleId) > 0 && userStore.isLoggedIn) {
    await checkLikeStatus()
  }
})

// 检查点赞状态
async function checkLikeStatus() {
  if (!userStore.isLoggedIn || Number(props.articleId) <= 0) return
  try {
    const isLiked = await articleService.checkLikeStatus(Number(props.articleId))
    if (isLiked !== liked.value) {
      liked.value = isLiked
      emit('update', isLiked, likeCount.value)
    }
  } catch (error) {
    console.error('检查点赞状态失败:', error)
  }
}

// 创建粒子效果
function createParticles() {
  if (!particlesRef.value || !buttonRef.value) return

  void buttonRef.value.getBoundingClientRect()

  const newParticles: Particle[] = []
  const particleCount = 12

  for (let i = 0; i < particleCount; i++) {
    const angle = (i / particleCount) * Math.PI * 2
    const distance = 40 + Math.random() * 30
    const duration = 600 + Math.random() * 400
    const delay = Math.random() * 100
    const size = 4 + Math.random() * 6

    newParticles.push({
      id: Date.now() + i,
      style: {
        '--tx': `${Math.cos(angle) * distance}px`,
        '--ty': `${Math.sin(angle) * distance - 20}px`,
        '--duration': `${duration}ms`,
        '--delay': `${delay}ms`,
        '--size': `${size}px`,
        '--opacity': `${String(0.6 + Math.random() * 0.4)}`,
      }
    })
  }

  particles.value = newParticles

  // 动画结束后清除粒子
  setTimeout(() => {
    particles.value = []
  }, 1500)
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

// 处理点赞操作（带 300ms 防抖）
function handleLike() {
  if (debounceTimer !== null) {
    clearTimeout(debounceTimer)
  }

  debounceTimer = window.setTimeout(async () => {
    await doLike()
    debounceTimer = null
  }, 300)
}

// 实际执行点赞逻辑
async function doLike() {
  if (loading.value) return

  const previousLiked = liked.value
  const previousCount = likeCount.value

  // 乐观更新 UI
  const willLike = !liked.value
  liked.value = willLike
  likeCount.value += willLike ? 1 : -1

  // 创建粒子效果
  if (willLike) {
    await nextTick()
    createParticles()
  }

  loading.value = true
  try {
    const articleId = Number(props.articleId)

    if (willLike) {
      await articleService.likeArticle(articleId)
      toast.like('点赞成功')
    } else {
      await articleService.unlikeArticle(articleId)
      toast.success('取消点赞')
    }

    emit('update', liked.value, likeCount.value)
  } catch (error: any) {
    // 回滚状态
    liked.value = previousLiked
    likeCount.value = previousCount

    if (error.response?.status === 401) {
      router.push('/login')
    } else {
      toast.error(error.response?.data?.message || '操作失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.like-button-wrapper {
  display: inline-flex;
  align-items: center;
}

.like-button {
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

.like-button:hover:not(:disabled) {
  border-color: var(--color-blue-400);
  background: rgba(59, 130, 246, 0.02);
}

.like-button:active:not(:disabled) {
  transform: scale(0.95);
}

.like-button.active {
  border-color: var(--color-blue-500);
  background: rgba(59, 130, 246, 0.08);
}

.like-button.loading {
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

.heart-icon {
  width: 17px;
  height: 17px;
  color: var(--text-tertiary);
  transition: all var(--duration-normal) var(--ease-spring);
}

.like-button:hover .heart-icon {
  color: var(--color-blue-400);
  transform: scale(1.1);
}

.like-button.active .heart-icon {
  color: var(--color-blue-500);
  animation: heart-pop 0.5s var(--ease-spring);
}

@keyframes heart-pop {
  0% { transform: scale(1); }
  25% { transform: scale(1.3); }
  50% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

.glow-effect {
  position: absolute;
  inset: -8px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%);
  border-radius: 50%;
  animation: glow-pulse 2s ease-in-out infinite;
  pointer-events: none;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.5; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
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

.like-button:hover .count-label {
  color: var(--color-blue-500);
}

.like-button.active .count-label {
  color: var(--color-blue-500);
}

/* 粒子系统 */
.particles-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: visible;
}

.particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: var(--size, 6px);
  height: var(--size, 6px);
  background: var(--color-blue-500);
  border-radius: 50%;
  opacity: var(--opacity, 0.8);
  animation: particle-explode var(--duration, 800ms) var(--ease-default) forwards;
  animation-delay: var(--delay, 0ms);
}

@keyframes particle-explode {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: var(--opacity, 0.8);
  }
  100% {
    transform: translate(calc(-50% + var(--tx, 0px)), calc(-50% + var(--ty, 0px))) scale(0);
    opacity: 0;
  }
}

/* Dark mode */
.dark .like-button {
  background: var(--bg-card);
  border-color: var(--border-color);
}

.dark .like-button:hover:not(:disabled) {
  background: rgba(96, 165, 250, 0.05);
  border-color: var(--color-blue-400);
}

.dark .like-button.active {
  background: rgba(96, 165, 250, 0.1);
  border-color: var(--color-blue-500);
}

.dark .heart-icon {
  color: var(--text-tertiary);
}

.dark .like-button:hover .heart-icon {
  color: var(--color-blue-400);
}

.dark .like-button.active .heart-icon {
  color: var(--color-blue-500);
}

.dark .count-label {
  color: var(--text-secondary);
}

.dark .particle {
  background: var(--color-blue-400);
}

/* 移动端 */
@media (max-width: 768px) {
  .like-button {
    padding: 6px 10px;
    min-width: auto;
  }

  .icon-container {
    width: 15px;
    height: 15px;
  }

  .heart-icon {
    width: 15px;
    height: 15px;
  }

  .count-label {
    display: none;
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .like-button:active:not(:disabled) {
    transform: scale(0.92);
    background: rgba(59, 130, 246, 0.12);
  }

  .like-button.active:active:not(:disabled) {
    background: rgba(59, 130, 246, 0.15);
  }
}
</style>
