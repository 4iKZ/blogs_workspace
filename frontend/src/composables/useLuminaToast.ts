import { ref } from 'vue'

// 简单的状态管理
const toasts = ref<Array<{
  id: number
  type: 'success' | 'error' | 'warning' | 'info' | 'like' | 'favorite'
  title?: string
  message: string
  duration: number
  paused: boolean
  startTime: number
  remaining: number
}>>([])

let toastIdCounter = 0
const MAX_TOASTS = 4

// 持久化到 window，确保跨组件共享
if (typeof window !== 'undefined') {
  (window as any).__luminaToasts = toasts
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
    duration,
    paused: false,
    startTime: Date.now(),
    remaining: duration
  }

  // 限制最大数量
  if (toasts.value.length >= MAX_TOASTS) {
    toasts.value.shift()
  }

  toasts.value.push(toast)

  // 自动关闭
  if (duration > 0) {
    setTimeout(() => {
      removeToast(id)
    }, duration)
  }

  return id
}

function removeToast(id: number) {
  const index = toasts.value.findIndex(t => t.id === id)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

// 同步的 toast 方法 - 无需 await
export const toast = {
  success(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ ...options, message, type: 'success' })
  },
  error(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ ...options, message, type: 'error', duration: 2000 })
  },
  warning(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ ...options, message, type: 'warning' })
  },
  info(message: string, options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ ...options, message, type: 'info' })
  },
  like(message: string = '点赞成功', options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ ...options, message, type: 'like', duration: 1000 })
  },
  favorite(message: string = '收藏成功', options?: Omit<ToastOptions, 'message' | 'type'>) {
    return addToast({ ...options, message, type: 'favorite', duration: 1000 })
  }
}

// 导出 composable（兼容性）
export const useLuminaToast = () => toast

// 导出状态供组件使用
export { toasts, removeToast }
export default useLuminaToast
