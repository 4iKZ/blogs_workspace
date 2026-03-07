/**
 * 图片压缩工具类
 * 支持JPG、PNG、WebP格式压缩
 * 保持原始比例，质量不低于80%
 * 使用Canvas API实现渐进式压缩
 */

export interface CompressionOptions {
  maxWidth?: number
  maxHeight?: number
  quality?: number
  maxSize?: number
  preserveRatio?: boolean
}

export interface CompressionResult {
  file: File
  originalSize: number
  compressedSize: number
  compressionRatio: number
  width: number
  height: number
  success: boolean
  error?: string
  originalWidth?: number
  originalHeight?: number
}

export interface CompressionProgress {
  stage: 'reading' | 'compressing' | 'generating' | 'completed' | 'error'
  progress: number
  message: string
}

export type ProgressCallback = (progress: CompressionProgress) => void

const DEFAULT_OPTIONS: Required<CompressionOptions> = {
  maxWidth: 1920,
  maxHeight: 1080,
  quality: 0.8,
  maxSize: 3 * 1024 * 1024, // 3MB
  preserveRatio: true
}

/**
 * 检查文件是否为图片
 */
export function isImageFile(file: File): boolean {
  return file.type.startsWith('image/')
}

/**
 * 获取图片格式
 */
export function getImageFormat(file: File): string {
  const type = file.type.toLowerCase()
  if (type.includes('jpeg') || type.includes('jpg')) return 'jpeg'
  if (type.includes('png')) return 'png'
  if (type.includes('webp')) return 'webp'
  return 'jpeg' // 默认返回jpeg
}

/**
 * 读取文件为Image对象
 */
function readFileAsImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)

    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }

    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片加载失败'))
    }

    img.src = url
  })
}

/**
 * 计算压缩后的尺寸（保持比例）
 */
function calculateDimensions(
  originalWidth: number,
  originalHeight: number,
  maxWidth: number,
  maxHeight: number,
  preserveRatio: boolean
): { width: number; height: number } {
  if (!preserveRatio) {
    return { width: maxWidth, height: maxHeight }
  }

  let width = originalWidth
  let height = originalHeight

  // 如果图片尺寸已经在限制范围内，直接返回
  if (width <= maxWidth && height <= maxHeight) {
    return { width, height }
  }

  // 计算缩放比例
  const widthRatio = maxWidth / width
  const heightRatio = maxHeight / height
  const ratio = Math.min(widthRatio, heightRatio)

  width = Math.floor(width * ratio)
  height = Math.floor(height * ratio)

  return { width, height }
}

/**
 * 将Canvas转换为Blob
 */
function canvasToBlob(
  canvas: HTMLCanvasElement,
  format: string,
  quality: number
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('Canvas转换Blob失败'))
        }
      },
      `image/${format}`,
      quality
    )
  })
}

/**
 * 渐进式压缩图片
 * 如果压缩后仍超过大小限制，逐步降低质量重新压缩
 */
async function progressiveCompress(
  canvas: HTMLCanvasElement,
  format: string,
  initialQuality: number,
  maxSize: number,
  onProgress?: ProgressCallback
): Promise<{ blob: Blob; finalQuality: number }> {
  let quality = initialQuality
  let minQuality = 0.3 // 最低质量限制
  let blob: Blob | null = null
  let attempts = 0
  const maxAttempts = 5

  while (quality >= minQuality && attempts < maxAttempts) {
    attempts++

    onProgress?.({
      stage: 'compressing',
      progress: Math.round((initialQuality - quality) / (initialQuality - minQuality) * 50) + 25,
      message: `正在压缩图片... (尝试 ${attempts}/${maxAttempts}, 质量: ${Math.round(quality * 100)}%)`
    })

    blob = await canvasToBlob(canvas, format, quality)

    if (blob.size <= maxSize) {
      return { blob, finalQuality: quality }
    }

    // 如果仍然超过大小限制，降低质量继续压缩
    quality = Math.max(minQuality, quality - 0.15)
  }

  // 如果达到最低质量仍超过限制，返回最后一次的结果
  if (!blob) {
    throw new Error('图片压缩失败')
  }

  return { blob, finalQuality: quality }
}

/**
 * 压缩单个图片文件
 */
export async function compressImage(
  file: File,
  options: CompressionOptions = {},
  onProgress?: ProgressCallback
): Promise<CompressionResult> {
  const opts = { ...DEFAULT_OPTIONS, ...options }

  try {
    // 检查是否为图片
    if (!isImageFile(file)) {
      throw new Error('文件不是有效的图片格式')
    }

    // 如果文件大小已经在限制范围内，直接返回
    if (file.size <= opts.maxSize) {
      onProgress?.({
        stage: 'completed',
        progress: 100,
        message: '图片大小符合要求，无需压缩'
      })

      const img = await readFileAsImage(file)
      return {
        file,
        originalSize: file.size,
        compressedSize: file.size,
        compressionRatio: 0,
        width: img.width,
        height: img.height,
        success: true
      }
    }

    onProgress?.({
      stage: 'reading',
      progress: 10,
      message: '正在读取图片...'
    })

    // 读取图片
    const img = await readFileAsImage(file)
    const originalWidth = img.width
    const originalHeight = img.height

    onProgress?.({
      stage: 'compressing',
      progress: 25,
      message: '正在处理图片尺寸...'
    })

    // 计算压缩后的尺寸
    const { width, height } = calculateDimensions(
      originalWidth,
      originalHeight,
      opts.maxWidth,
      opts.maxHeight,
      opts.preserveRatio
    )

    // 创建Canvas
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height

    const ctx = canvas.getContext('2d')
    if (!ctx) {
      throw new Error('无法创建Canvas上下文')
    }

    // 使用高质量缩放
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'

    // 绘制图片
    ctx.drawImage(img, 0, 0, width, height)

    const format = getImageFormat(file)

    onProgress?.({
      stage: 'generating',
      progress: 50,
      message: '正在生成压缩后的图片...'
    })

    // 渐进式压缩
    const { blob } = await progressiveCompress(
      canvas,
      format,
      opts.quality,
      opts.maxSize,
      onProgress
    )

    onProgress?.({
      stage: 'completed',
      progress: 100,
      message: '图片压缩完成'
    })

    // 创建新的File对象
    const compressedFile = new File([blob], file.name, {
      type: `image/${format}`,
      lastModified: Date.now()
    })

    const compressionRatio = ((file.size - blob.size) / file.size) * 100

    return {
      file: compressedFile,
      originalSize: file.size,
      compressedSize: blob.size,
      compressionRatio: Math.round(compressionRatio * 100) / 100,
      width,
      height,
      success: true
    }

  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '图片压缩失败'
    onProgress?.({
      stage: 'error',
      progress: 0,
      message: errorMessage
    })

    return {
      file,
      originalSize: file.size,
      compressedSize: file.size,
      compressionRatio: 0,
      width: 0,
      height: 0,
      success: false,
      error: errorMessage
    }
  }
}

/**
 * 批量压缩图片
 */
export async function compressImages(
  files: File[],
  options: CompressionOptions = {},
  onProgress?: (index: number, total: number, progress: CompressionProgress) => void
): Promise<CompressionResult[]> {
  const results: CompressionResult[] = []

  for (let i = 0; i < files.length; i++) {
    const result = await compressImage(files[i], options, (progress) => {
      onProgress?.(i + 1, files.length, progress)
    })
    results.push(result)
  }

  return results
}

/**
 * 格式化文件大小显示
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 检查是否需要压缩
 */
export function needsCompression(file: File, maxSize: number = 3 * 1024 * 1024): boolean {
  return isImageFile(file) && file.size > maxSize
}

export default {
  compressImage,
  compressImages,
  isImageFile,
  getImageFormat,
  formatFileSize,
  needsCompression
}
