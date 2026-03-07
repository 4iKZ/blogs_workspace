<template>
  <teleport to="body">
    <transition-group
      name="lumina-toast"
      tag="div"
      class="lumina-toast-container"
    >
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="[
          'lumina-toast',
          `lumina-toast--${toast.type}`
        ]"
        :style="{ transform: getStackTransform(toast) }"
      >
        <!-- 光效背景 -->
        <div class="lumina-toast__glow"></div>

        <!-- 内容 -->
        <div class="lumina-toast__content">
          <div v-if="toast.title" class="lumina-toast__title">{{ toast.title }}</div>
          <div class="lumina-toast__message">{{ toast.message }}</div>
        </div>
      </div>
    </transition-group>
  </teleport>
</template>

<script setup lang="ts">
import { toasts } from '@/composables/useLuminaToast'

function getStackTransform(toast: any) {
  const index = toasts.value.indexOf(toast)
  if (index <= 0) return undefined
  return `translateY(${index * 8}px) scale(${1 - index * 0.02})`
}
</script>

<style scoped>
/* 容器 */
.lumina-toast-container {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
  align-items: center;
}

/* 通知卡片 */
.lumina-toast {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  background: var(--bg-primary);
  border-radius: 10px;
  box-shadow:
    0 3px 10px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(0, 0, 0, 0.04),
    0 0 32px -6px var(--toast-color, rgba(64, 158, 255, 0.04));
  pointer-events: auto;
  overflow: hidden;
  isolation: isolate;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 光效背景 - 减弱效果 */
.lumina-toast__glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at top right,
    var(--toast-color, rgba(64, 158, 255, 0.02)),
    transparent 60%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
}

.lumina-toast:hover .lumina-toast__glow {
  opacity: 0.4;
}

/* 内容 */
.lumina-toast__content {
  text-align: center;
  width: 100%;
  font-family: "SimHei", "黑体", "Microsoft YaHei", "微软雅黑", sans-serif;
}

.lumina-toast__title {
  font-weight: 600;
  font-size: 12px;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.lumina-toast__message {
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-secondary);
}

/* 底部装饰线 */
.lumina-toast__accent {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(
    90deg,
    var(--toast-color, #409EFF),
    transparent
  );
  opacity: 0.8;
}

/* 类型颜色 - 统一使用 About 页面的主色蓝色 */
.lumina-toast--success,
.lumina-toast--error,
.lumina-toast--warning,
.lumina-toast--info,
.lumina-toast--like,
.lumina-toast--favorite {
  --toast-color: #409EFF;
}

/* 进入/离开动画 */
.lumina-toast-enter-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.lumina-toast-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 1, 1);
}

.lumina-toast-enter-from {
  opacity: 0;
  transform: translateY(-20px) scale(0.95);
}

.lumina-toast-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}

.lumina-toast-move {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* 响应式 */
@media (max-width: 480px) {
  .lumina-toast-container {
    top: 16px;
    left: 16px;
    right: 16px;
    transform: none;
  }

  .lumina-toast {
    min-width: auto;
    max-width: none;
  }
}
</style>
