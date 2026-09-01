import { ref } from 'vue'

// 简单的状态管理
const toasts = ref<Array<{
  id: number
  type: 'success' | 'error' | 'warning' | 'info' | 'like' | 'favorite'
  title?: string
  message: string
  duration: number
}>>([])

let toastIdCounter = 0
const MAX_TOASTS = 4

// 统一登记每个 toast 的自动关闭定时器，移除/挤出时同步清理，避免悬空回调
const timers = new Map<number, ReturnType<typeof setTimeout>>()

function clearTimer(id: number) {
  const timer = timers.get(id)
  if (timer !== undefined) {
    clearTimeout(timer)
    timers.delete(id)
  }
}

export interface ToastOptions {
  type?: 'success' | 'error' | 'warning' | 'info' | 'like' | 'favorite'
  title?: string
  message: string
  duration?: number
}

function addToast(options: ToastOptions): number {
  const id = ++toastIdCounter
  const duration = options.duration ?? 1500

  const toast = {
    id,
    type: options.type ?? 'info',
    title: options.title ?? '',
    message: options.message,
    duration
  }

  // 限制最大数量，挤出最早者时同步清理其定时器
  if (toasts.value.length >= MAX_TOASTS) {
    const oldest = toasts.value.shift()
    if (oldest) {
      clearTimer(oldest.id)
    }
  }

  toasts.value.push(toast)

  // 自动关闭
  if (duration > 0) {
    timers.set(id, setTimeout(() => {
      clearTimer(id)
      removeToast(id)
    }, duration))
  }

  return id
}

function removeToast(id: number) {
  clearTimer(id)
  const index = toasts.value.findIndex(t => t.id === id)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

// 同步的 toast 方法 - 无需 await
export const toast = {
  success(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ message, type: 'success', ...options })
  },
  error(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ message, type: 'error', duration: 2000, ...options })
  },
  warning(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ message, type: 'warning', ...options })
  },
  info(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ message, type: 'info', ...options })
  },
  like(message: string = '点赞成功', options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ message, type: 'like', duration: 1000, ...options })
  },
  favorite(message: string = '收藏成功', options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ message, type: 'favorite', duration: 1000, ...options })
  }
}

// 导出 composable（兼容性）
export const useLuminaToast = () => toast

// 导出状态供组件使用
export { toasts, removeToast }
export default useLuminaToast
