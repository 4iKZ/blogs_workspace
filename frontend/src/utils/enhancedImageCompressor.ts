/**
 * 增强的图片压缩工具
 * 整合WebWorker、缓存和分片上传功能
 */

import type { CompressionOptions, CompressionResult, CompressionProgress } from './imageCompressor'
import { compressionCache, compressWithCache } from './compressionCache'
import {
  uploadWithChunks,
  checkResumeUpload,
  resumeUpload,
  cancelUpload,
  type ChunkedUploadOptions,
  type ChunkedUploadProgress,
  formatRemainingTime,
  formatUploadSpeed
} from './chunkedUploader'

// Worker实例管理
let workerInstance: Worker | null = null
let taskIdCounter = 0
const pendingTasks = new Map<string, {
  resolve: (result: CompressionResult) => void
  reject: (error: Error) => void
  onProgress?: (progress: CompressionProgress) => void
}>()

/**
 * 获取或创建Worker实例
 */
function getWorker(): Worker {
  if (!workerInstance) {
    workerInstance = new Worker(
      new URL('../workers/compression.worker.ts', import.meta.url),
      { type: 'module' }
    )

    workerInstance.onmessage = (event: MessageEvent) => {
      const { type, id, data, error } = event.data

      const task = pendingTasks.get(id)
      if (!task) return

      if (type === 'progress') {
        task.onProgress?.(data as CompressionProgress)
      } else if (type === 'result') {
        task.resolve(data as CompressionResult)
        pendingTasks.delete(id)
      } else if (type === 'error') {
        task.reject(new Error(error || '压缩失败'))
        pendingTasks.delete(id)
      }
    }

    workerInstance.onerror = (error) => {
      console.error('[CompressionWorker] 错误:', error)
    }
  }

  return workerInstance
}

/**
 * 生成任务ID
 */
function generateTaskId(): string {
  return `task_${Date.now()}_${taskIdCounter++}`
}

/**
 * 使用Worker压缩图片（异步，不阻塞主线程）
 */
export async function compressImageWithWorker(
  file: File,
  options: CompressionOptions = {},
  onProgress?: (progress: CompressionProgress) => void
): Promise<CompressionResult> {
  // 检查缓存
  const cached = await compressionCache.get(file)
  if (cached) {
    console.log('[EnhancedCompressor] 使用缓存结果')
    onProgress?.({
      stage: 'completed',
      progress: 100,
      message: '使用缓存结果'
    })
    return cached
  }

  // 使用Worker压缩
  const worker = getWorker()
  const taskId = generateTaskId()

  return new Promise<CompressionResult>((resolve, reject) => {
    pendingTasks.set(taskId, { resolve, reject, onProgress })

    worker.postMessage({
      type: 'compress',
      id: taskId,
      file,
      options
    })
  }).then(async (result) => {
    // 存入缓存
    if (result.success) {
      await compressionCache.set(file, result)
    }
    return result
  })
}

/**
 * 智能压缩函数（自动选择最佳方案）
 */
export async function smartCompressImage(
  file: File,
  options: CompressionOptions = {},
  onProgress?: (progress: CompressionProgress) => void
): Promise<CompressionResult> {
  // 小文件直接返回
  if (file.size < 1024 * 1024) { // 1MB以下
    return {
      file,
      originalSize: file.size,
      compressedSize: file.size,
      compressionRatio: 0,
      width: 0,
      height: 0,
      success: true
    }
  }

  // 检查是否支持Worker
  const supportsWorker = typeof Worker !== 'undefined'

  if (supportsWorker) {
    try {
      return await compressImageWithWorker(file, options, onProgress)
    } catch (error) {
      console.warn('[EnhancedCompressor] Worker压缩失败，降级到同步压缩:', error)
    }
  }

  // 降级到同步压缩（使用原有的imageCompressor）
  const { compressImage } = await import('./imageCompressor')
  return compressWithCache(file, (f) => compressImage(f, options, onProgress))
}

/**
 * 上传图片（自动选择普通上传或分片上传）
 */
export async function uploadImage(
  file: File,
  token: string,
  options: {
    chunkThreshold?: number // 超过此大小使用分片上传（字节）
    chunkOptions?: Partial<ChunkedUploadOptions>
    endpoint?: string // 上传端点，默认 '/article/upload-cover'
  } = {}
): Promise<string> {
  const {
    chunkThreshold = 10 * 1024 * 1024, // 默认10MB
    chunkOptions,
    endpoint = '/article/upload-cover'
  } = options

  // 小文件直接上传
  if (file.size <= chunkThreshold) {
    return uploadImageDirectly(file, token, endpoint)
  }

  // 检查是否有可恢复的上传
  const uploadId = await checkResumeUpload(file, token)
  if (uploadId) {
    console.log('[EnhancedCompressor] 恢复之前未完成的上传')
    return resumeUpload(uploadId, file, token, chunkOptions)
  }

  // 大文件分片上传
  return uploadWithChunks(file, token, chunkOptions)
}

/**
 * 直接上传图片
 */
async function uploadImageDirectly(
  file: File,
  token: string,
  endpoint: string
): Promise<string> {
  const { default: axios } = await import('./axios')

  const formData = new FormData()
  formData.append('file', file)

  const response = await axios.post(endpoint, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${token}`
    }
  })

  return response
}

/**
 * 批量压缩图片
 */
export async function compressImagesBatch(
  files: File[],
  options: CompressionOptions = {},
  onProgress?: (index: number, total: number, progress: CompressionProgress) => void
): Promise<CompressionResult[]> {
  const results: CompressionResult[] = []

  for (let i = 0; i < files.length; i++) {
    const result = await smartCompressImage(files[i], options, (progress) => {
      onProgress?.(i + 1, files.length, progress)
    })
    results.push(result)
  }

  return results
}

/**
 * 批量上传图片
 */
export async function uploadImagesBatch(
  files: File[],
  token: string,
  options: {
    chunkThreshold?: number
    chunkOptions?: Partial<ChunkedUploadOptions>
    endpoint?: string
    onProgress?: (index: number, total: number, progress: number) => void
  } = {}
): Promise<string[]> {
  const urls: string[] = []
  const { onProgress, ...uploadOptions } = options

  for (let i = 0; i < files.length; i++) {
    const url = await uploadImage(files[i], token, {
      ...uploadOptions,
      chunkOptions: {
        ...uploadOptions.chunkOptions,
        onProgress: uploadOptions.chunkOptions?.onProgress ?
          (progress) => {
            uploadOptions.chunkOptions!.onProgress!(progress)
            onProgress?.(i + 1, files.length, progress.progress)
          } :
          undefined
      }
    })
    urls.push(url)
    onProgress?.(i + 1, files.length, 100)
  }

  return urls
}

/**
 * 清理压缩缓存
 */
export async function clearCompressionCache(): Promise<void> {
  await compressionCache.clear()
}

/**
 * 获取缓存统计
 */
export async function getCacheStats(): Promise<{
  count: number
  totalSize: number
  totalSizeFormatted: string
  oldestEntry: Date | null
  newestEntry: Date | null
}> {
  const stats = await compressionCache.getStats()

  return {
    ...stats,
    totalSizeFormatted: formatFileSize(stats.totalSize),
    oldestEntry: stats.oldestEntry ? new Date(stats.oldestEntry) : null,
    newestEntry: stats.newestEntry ? new Date(stats.newestEntry) : null
  }
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`
  } else if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`
  } else if (bytes < 1024 * 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  } else {
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
  }
}

/**
 * 终止所有活动的Worker任务
 */
export function terminateWorker(): void {
  if (workerInstance) {
    workerInstance.terminate()
    workerInstance = null
  }
  pendingTasks.clear()
}

// 导出所有功能
export {
  compressionCache,
  compressWithCache,
  uploadWithChunks,
  checkResumeUpload,
  resumeUpload,
  cancelUpload,
  formatRemainingTime,
  formatUploadSpeed
}

// 重新导出imageCompressor中的常用函数
export { needsCompression, formatFileSize } from './imageCompressor'

export type {
  CompressionOptions,
  CompressionResult,
  CompressionProgress,
  ChunkedUploadOptions,
  ChunkedUploadProgress
}
