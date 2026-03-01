/**
 * 图片压缩 Web Worker
 * 在独立线程中执行压缩操作，避免阻塞主线程
 */

import type { CompressionOptions, CompressionResult, CompressionProgress } from '../utils/imageCompressor'

// Worker消息类型
interface WorkerMessage {
  type: 'compress'
  id: string
  file: File
  options: CompressionOptions
}

interface WorkerResponse {
  type: 'progress' | 'result' | 'error'
  id: string
  data?: CompressionResult | CompressionProgress
  error?: string
}

// 默认配置
const DEFAULT_OPTIONS: Required<CompressionOptions> = {
  maxWidth: 2048,
  maxHeight: 2048,
  quality: 0.8,
  maxSize: 5 * 1024 * 1024,
  preserveRatio: true
}

/**
 * 计算目标尺寸
 */
function calculateTargetDimensions(
  originalWidth: number,
  originalHeight: number,
  maxWidth: number,
  maxHeight: number,
  preserveRatio: boolean
): { width: number; height: number } {
  let width = originalWidth
  let height = originalHeight

  if (width <= maxWidth && height <= maxHeight) {
    return { width, height }
  }

  if (preserveRatio) {
    const widthRatio = maxWidth / width
    const heightRatio = maxHeight / height
    const ratio = Math.min(widthRatio, heightRatio)
    width = Math.floor(width * ratio)
    height = Math.floor(height * ratio)
  } else {
    width = Math.min(width, maxWidth)
    height = Math.min(height, maxHeight)
  }

  return { width, height }
}

/**
 * 获取图片格式
 */
function getImageFormat(file: File): string {
  const type = file.type.toLowerCase()
  if (type.includes('jpeg') || type.includes('jpg')) return 'jpeg'
  if (type.includes('png')) return 'png'
  if (type.includes('webp')) return 'webp'
  return 'jpeg'
}

/**
 * 使用ImageBitmap加载图片（Worker兼容方式）
 */
async function loadImageBitmap(file: File): Promise<{ bitmap: ImageBitmap; width: number; height: number }> {
  const blob = new Blob([file], { type: file.type })
  const bitmap = await createImageBitmap(blob)
  return { bitmap, width: bitmap.width, height: bitmap.height }
}

/**
 * Canvas转Blob
 */
function canvasToBlob(
  canvas: OffscreenCanvas,
  format: string,
  quality: number
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const options: ImageEncodeOptions = {}
    if (format === 'jpeg' || format === 'webp') {
      options.quality = quality
    }

    canvas.convertToBlob({ type: `image/${format}`, ...options })
      .then(resolve)
      .catch(reject)
  })
}

/**
 * 渐进式压缩
 */
async function progressiveCompress(
  canvas: OffscreenCanvas,
  format: string,
  initialQuality: number,
  maxSize: number,
  id: string,
  postMessage: (message: WorkerResponse) => void
): Promise<{ blob: Blob; finalQuality: number }> {
  let quality = initialQuality
  const minQuality = 0.3
  let blob: Blob | null = null
  let attempts = 0
  const maxAttempts = 5

  while (quality >= minQuality && attempts < maxAttempts) {
    attempts++

    // 发送进度更新
    postMessage({
      type: 'progress',
      id,
      data: {
        stage: 'compressing',
        progress: Math.round(50 + (attempts / maxAttempts) * 40),
        message: `正在压缩... (尝试 ${attempts}/${maxAttempts}, 质量: ${Math.round(quality * 100)}%)`
      }
    })

    blob = await canvasToBlob(canvas, format, quality)

    if (blob.size <= maxSize) {
      return { blob, finalQuality: quality }
    }

    quality = Math.max(minQuality, quality - 0.15)
  }

  if (!blob) {
    throw new Error('图片压缩失败')
  }

  return { blob, finalQuality: quality }
}

/**
 * 执行压缩
 */
async function performCompression(
  file: File,
  options: CompressionOptions,
  id: string,
  postMessage: (message: WorkerResponse) => void
): Promise<CompressionResult> {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  try {
    // 检查是否需要压缩
    if (file.size <= opts.maxSize) {
      postMessage({
        type: 'progress',
        id,
        data: {
          stage: 'completed',
          progress: 100,
          message: '图片大小符合要求，无需压缩'
        }
      })

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

    // 读取图片
    postMessage({
      type: 'progress',
      id,
      data: {
        stage: 'reading',
        progress: 10,
        message: '正在读取图片...'
      }
    })

    const { bitmap, width: originalWidth, height: originalHeight } = await loadImageBitmap(file)

    // 获取原始尺寸（用于回退）
    postMessage({
      type: 'progress',
      id,
      data: {
        stage: 'compressing',
        progress: 30,
        message: '正在处理图片...'
      }
    })

    // 计算目标尺寸
    const { width, height } = calculateTargetDimensions(
      originalWidth,
      originalHeight,
      opts.maxWidth,
      opts.maxHeight,
      opts.preserveRatio
    )

    // 创建OffscreenCanvas并绘制
    const canvas = new OffscreenCanvas(width, height)
    const ctx = canvas.getContext('2d')

    if (!ctx) {
      throw new Error('无法创建Canvas上下文')
    }

    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    ctx.drawImage(bitmap, 0, 0, width, height)

    // 关闭bitmap释放资源
    bitmap.close()

    postMessage({
      type: 'progress',
      id,
      data: {
        stage: 'compressing',
        progress: 50,
        message: '正在压缩图片...'
      }
    })

    // 渐进式压缩
    const format = getImageFormat(file)
    const result = await progressiveCompress(
      canvas,
      format,
      opts.quality,
      opts.maxSize,
      id,
      postMessage
    )

    postMessage({
      type: 'progress',
      id,
      data: {
        stage: 'generating',
        progress: 90,
        message: '正在生成压缩后的图片...'
      }
    })

    // 创建新的File对象
    const compressedFile = new File([result.blob], file.name, {
      type: `image/${format}`,
      lastModified: Date.now()
    })

    const compressionRatio = ((file.size - result.blob.size) / file.size) * 100

    postMessage({
      type: 'progress',
      id,
      data: {
        stage: 'completed',
        progress: 100,
        message: '压缩完成'
      }
    })

    return {
      file: compressedFile,
      originalSize: file.size,
      compressedSize: result.blob.size,
      compressionRatio: Math.round(compressionRatio * 100) / 100,
      width,
      height,
      originalWidth,
      originalHeight,
      success: true
    }

  } catch (error) {
    // 即使失败，也尝试保留基本信息
    postMessage({
      type: 'progress',
      id,
      data: {
        stage: 'error',
        progress: 0,
        message: error instanceof Error ? error.message : '压缩失败'
      }
    })

    return {
      file,
      originalSize: file.size,
      compressedSize: file.size,
      compressionRatio: 0,
      width: 0,
      height: 0,
      originalWidth: 0,
      originalHeight: 0,
      success: false,
      error: error instanceof Error ? error.message : '压缩失败'
    }
  }
}

// 监听主线程消息
self.onmessage = async (event: MessageEvent<WorkerMessage>) => {
  const { type, id, file, options } = event.data

  if (type === 'compress') {
    try {
      const result = await performCompression(file, options, id, self.postMessage.bind(self))
      self.postMessage({
        type: 'result',
        id,
        data: result
      })
    } catch (error) {
      self.postMessage({
        type: 'error',
        id,
        error: error instanceof Error ? error.message : '压缩过程中发生未知错误'
      })
    }
  }
}

// 导出类型供外部使用
export type { WorkerMessage, WorkerResponse }
