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
  return ['image/jpeg', 'image/png', 'image/gif'].includes(file.type)
}

/**
 * 获取图片格式
 */
export function getImageFormat(file: File): string {
  const type = file.type.toLowerCase()
  if (type.includes('jpeg') || type.includes('jpg')) return 'jpeg'
  if (type.includes('png')) return 'png'
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
  const minQuality = 0.3 // 最低质量限制
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

  if (blob.size > maxSize) {
    // 达到最低质量仍超限，不应谎报成功
    throw new Error('图片压缩后仍超过大小限制')
  }

  return { blob, finalQuality: quality }
}

/**
 * 解析 JPEG EXIF 方向（1-8），非 JPEG 或解析失败返回 1
 */
async function readExifOrientation(file: File): Promise<number> {
  if (file.type !== 'image/jpeg') return 1
  try {
    const buffer = await file.slice(0, 128 * 1024).arrayBuffer()
    const view = new DataView(buffer)
    if (view.byteLength < 4 || view.getUint16(0, false) !== 0xffd8) return 1

    let offset = 2
    while (offset + 4 <= view.byteLength) {
      if (view.getUint8(offset) !== 0xff) {
        offset++
        continue
      }
      const marker = view.getUint8(offset + 1)
      // 跳过独立的 SOI/EOI/RSTn 段
      if (marker === 0xd8 || (marker >= 0xd0 && marker <= 0xd7)) {
        offset += 2
        continue
      }
      const length = view.getUint16(offset + 2, false)
      if (length < 2) break

      // APP1 段：检查 Exif 头
      if (marker === 0xe1) {
        let text = ''
        try {
          text = new TextDecoder('latin1').decode(new Uint8Array(buffer, offset + 4, Math.min(6, view.byteLength - offset - 4)))
        } catch {
          return 1
        }
        if (text.startsWith('Exif\0\0')) {
          return readExifOrientationFromTIFF(view, offset + 10)
        }
      }

      offset += 2 + length
    }
    return 1
  } catch {
    return 1
  }
}

/**
 * 从 TIFF 头解析 Orientation 标签（0x0112）
 */
function readExifOrientationFromTIFF(view: DataView, tiffOffset: number): number {
  if (tiffOffset + 8 > view.byteLength) return 1
  const endianMarker = view.getUint16(tiffOffset, false)
  const littleEndian = endianMarker === 0x4949
  const magic = view.getUint16(tiffOffset + 2, littleEndian)
  if (magic !== 0x002a) return 1

  const ifdStart = tiffOffset + view.getUint32(tiffOffset + 4, littleEndian)
  if (ifdStart + 2 > view.byteLength) return 1
  const entryCount = view.getUint16(ifdStart, littleEndian)

  for (let i = 0; i < entryCount; i++) {
    const entryPos = ifdStart + 2 + i * 12
    if (entryPos + 12 > view.byteLength) break
    const tag = view.getUint16(entryPos, littleEndian)
    if (tag === 0x0112) {
      if (view.getUint16(entryPos + 2, littleEndian) !== 3) return 1 // 必须为 SHORT
      const value = view.getUint16(entryPos + 8, littleEndian)
      return value >= 1 && value <= 8 ? value : 1
    }
  }
  return 1
}

/**
 * 按 EXIF 方向对画布上下文应用变换（canvas 尺寸已按方向交换）
 */
function applyOrientation(
  ctx: CanvasRenderingContext2D,
  orientation: number,
  width: number,
  height: number
): void {
  switch (orientation) {
    case 2: ctx.transform(-1, 0, 0, 1, width, 0); break
    case 3: ctx.transform(-1, 0, 0, -1, width, height); break
    case 4: ctx.transform(1, 0, 0, -1, 0, height); break
    case 5: ctx.transform(0, 1, 1, 0, 0, 0); break
    case 6: ctx.transform(0, 1, -1, 0, height, 0); break
    case 7: ctx.transform(0, -1, -1, 0, height, width); break
    case 8: ctx.transform(0, -1, 1, 0, 0, width); break
    default: return
  }
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
      const orientation = await readExifOrientation(file)
      const rotated = orientation >= 5
      return {
        file,
        originalSize: file.size,
        compressedSize: file.size,
        compressionRatio: 0,
        width: rotated ? img.height : img.width,
        height: rotated ? img.width : img.height,
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
    const orientation = await readExifOrientation(file)
    // EXIF 方向 5-8 表示旋转 90/270 度，展示尺寸需交换
    const rotated = orientation >= 5
    const originalWidth = rotated ? img.height : img.width
    const originalHeight = rotated ? img.width : img.height

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

    // 按 EXIF 方向绘制（纠正相机照片旋转）
    ctx.save()
    applyOrientation(ctx, orientation, width, height)
    ctx.drawImage(img, 0, 0, width, height)
    ctx.restore()

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
