<template>
  <div class="share-button-wrapper">
    <button
      :class="['share-button', { active: showCopied, loading }]"
      :disabled="loading"
      @click="handleShare"
      @mouseenter="handleHover"
      @mouseleave="handleLeave"
      ref="buttonRef"
    >
      <div class="button-inner">
        <!-- 链接图标容器 -->
        <div class="icon-container">
          <!-- SVG 链接图标 -->
          <svg class="link-icon" viewBox="0 0 24 24" fill="none">
            <path
              class="link-path"
              d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <path
              class="link-path"
              d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <!-- 复制成功的对勾 -->
          <svg class="check-icon" viewBox="0 0 24 24" fill="none">
            <path
              class="check-path"
              d="M20 6L9 17l-5-5"
              stroke="currentColor"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </div>

        <!-- 标签 -->
        <span class="label">{{ buttonLabel }}</span>
      </div>

      <!-- 链接飞出粒子 -->
      <div class="link-particles-container" ref="particlesRef">
        <div
          v-for="particle in particles"
          :key="particle.id"
          class="link-particle"
          :style="particle.style"
        ></div>
      </div>
    </button>

    <!-- 复制成功提示气泡 -->
    <transition name="bubble">
      <div v-if="showCopied" class="copied-bubble">
        <span class="bubble-icon">✓</span>
        <span class="bubble-text">链接已复制</span>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { toast } from '@/composables/useLuminaToast'

interface Particle {
  id: number
  style: Record<string, string>
}

const props = defineProps<{
  url?: string
}>()

const loading = ref(false)
const showCopied = ref(false)
const buttonRef = ref<HTMLElement | null>(null)
const particlesRef = ref<HTMLElement | null>(null)
const particles = ref<Particle[]>([])

let copiedTimer: number | null = null

const buttonLabel = computed(() => {
  return showCopied.value ? '已复制' : '分享'
})

// 创建链接飞出效果
function createLinkParticles() {
  if (!particlesRef.value || !buttonRef.value) return

  const newParticles: Particle[] = []
  const particleCount = 3

  for (let i = 0; i < particleCount; i++) {
    const duration = 800 + i * 200
    const delay = i * 100
    const distance = 50 + i * 20

    newParticles.push({
      id: Date.now() + i,
      style: {
        '--tx': `${distance}px`,
        '--ty': `${-20 - i * 10}px`,
        '--duration': `${duration}ms`,
        '--delay': `${delay}ms`,
        '--scale': 1 - i * 0.2,
      }
    })
  }

  particles.value = newParticles

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

// 处理分享
async function handleShare() {
  if (loading.value) return

  loading.value = true

  try {
    const urlToShare = props.url || window.location.href

    // 尝试复制到剪贴板
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(urlToShare)
    } else {
      // 降级方案：使用传统方法
      const textArea = document.createElement('textarea')
      textArea.value = urlToShare
      textArea.style.position = 'fixed'
      textArea.style.left = '-9999px'
      document.body.appendChild(textArea)
      textArea.select()
      document.execCommand('copy')
      document.body.removeChild(textArea)
    }

    // 显示成功状态
    showCopied.value = true
    await nextTick()
    createLinkParticles()
    toast.success('链接已复制到剪贴板')

    // 清除之前的定时器
    if (copiedTimer !== null) {
      clearTimeout(copiedTimer)
    }

    // 2秒后恢复原状
    copiedTimer = window.setTimeout(() => {
      showCopied.value = false
    }, 2000)
  } catch (error) {
    console.error('复制失败:', error)
    toast.error('复制失败，请手动复制链接')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.share-button-wrapper {
  display: inline-block;
  position: relative;
}

.share-button {
  position: relative;
  padding: 10px 20px;
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

.share-button:hover:not(:disabled) {
  border-color: var(--color-teal-500);
  background: rgba(20, 184, 166, 0.02);
}

.share-button:active:not(:disabled) {
  transform: scale(0.95);
}

.share-button.active {
  border-color: var(--color-teal-500);
  background: rgba(20, 184, 166, 0.08);
}

.share-button.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.button-inner {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-container {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.link-icon,
.check-icon {
  position: absolute;
  width: 24px;
  height: 24px;
  transition: all var(--duration-normal) var(--ease-spring);
}

.link-icon {
  color: var(--text-tertiary);
}

.check-icon {
  color: var(--color-teal-500);
  opacity: 0;
  transform: scale(0) rotate(-180deg);
}

.share-button:hover .link-icon {
  color: var(--color-teal-500);
  transform: scale(1.1);
}

.share-button.active .link-icon {
  opacity: 0;
  transform: scale(0) rotate(180deg);
}

.share-button.active .check-icon {
  opacity: 1;
  transform: scale(1) rotate(0deg);
  animation: check-pop 0.5s var(--ease-spring);
}

@keyframes check-pop {
  0% {
    transform: scale(0) rotate(-180deg);
  }
  50% {
    transform: scale(1.3) rotate(10deg);
  }
  100% {
    transform: scale(1) rotate(0deg);
  }
}

.label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-secondary);
  transition: color var(--duration-fast) var(--ease-default);
}

.share-button:hover .label {
  color: var(--color-teal-500);
}

.share-button.active .label {
  color: var(--color-teal-500);
}

/* 链接飞出粒子系统 */
.link-particles-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: visible;
}

.link-particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 16px;
  height: 16px;
  opacity: 0;
  animation: link-fly-out var(--duration, 800ms) cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
  animation-delay: var(--delay, 0ms);
}

.link-particle::before {
  content: '';
  position: absolute;
  inset: 0;
  border: 2px solid var(--color-teal-500);
  border-radius: 50%;
  transform: scale(var(--scale, 1));
}

@keyframes link-fly-out {
  0% {
    transform: translate(-50%, -50%) scale(0.5);
    opacity: 1;
  }
  100% {
    transform: translate(calc(-50% + var(--tx, 0px)), calc(-50% + var(--ty, 0px))) scale(0);
    opacity: 0;
  }
}

/* 复制成功气泡 */
.copied-bubble {
  position: absolute;
  bottom: calc(100% + 12px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--bg-card);
  border: 2px solid var(--color-teal-500);
  border-radius: var(--radius-full);
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.2);
  white-space: nowrap;
  z-index: 10;
}

.copied-bubble::after {
  content: '';
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: var(--color-teal-500);
}

.bubble-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  background: var(--color-teal-500);
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
}

.bubble-text {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--color-teal-500);
}

.bubble-enter-active,
.bubble-leave-active {
  transition: all var(--duration-normal) var(--ease-spring);
}

.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}

/* Dark mode */
.dark .share-button {
  background: var(--bg-card);
  border-color: var(--border-color);
}

.dark .share-button:hover:not(:disabled) {
  background: rgba(20, 184, 166, 0.05);
  border-color: var(--color-teal-500);
}

.dark .share-button.active {
  background: rgba(20, 184, 166, 0.1);
  border-color: var(--color-teal-500);
}

.dark .link-icon {
  color: var(--text-tertiary);
}

.dark .share-button:hover .link-icon {
  color: var(--color-teal-500);
}

.dark .check-icon {
  color: var(--color-teal-500);
}

.dark .label {
  color: var(--text-secondary);
}

.dark .link-particle::before {
  border-color: var(--color-teal-500);
}

.dark .copied-bubble {
  background: var(--bg-card);
  border-color: var(--color-teal-500);
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.3);
}

.dark .copied-bubble::after {
  border-top-color: var(--color-teal-500);
}

.dark .bubble-icon {
  background: var(--color-teal-500);
}

.dark .bubble-text {
  color: var(--color-teal-500);
}

/* 移动端 */
@media (max-width: 768px) {
  .share-button {
    padding: 12px 18px;
  }

  .icon-container {
    width: 22px;
    height: 22px;
  }

  .link-icon,
  .check-icon {
    width: 22px;
    height: 22px;
  }

  .label {
    font-size: var(--text-xs);
  }
}

/* 触摸设备优化 */
@media (hover: none) and (pointer: coarse) {
  .share-button:active:not(:disabled) {
    transform: scale(0.92);
    background: rgba(20, 184, 166, 0.12);
  }

  .share-button.active:active:not(:disabled) {
    background: rgba(20, 184, 166, 0.15);
  }
}
</style>
