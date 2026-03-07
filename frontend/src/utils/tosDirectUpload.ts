/**
 * TOS 客户端直传工具
 * 通过后端签发预签名URL，前端直接将文件上传到火山云TOS，绕开服务器带宽瓶颈
 */

import axios from './axios'
import { needsCompression, type CompressionOptions } from './imageCompressor'
import { compressImageWithWorker } from './enhancedImageCompressor'

export interface PresignedUploadResponse {
  signedUrl: string
  publicUrl: string
  objectKey: string
  expiresIn: number
}

export interface DirectUploadProgress {
  loaded: number
  total: number
  percent: number
}

export interface DirectUploadOptions {
  compress?: boolean
  compressionOptions?: CompressionOptions
  onProgress?: (progress: DirectUploadProgress) => void
}

/**
 * 向后端请求预签名上传URL
 */
async function requestPresignedUrl(
  fileName: string,
  contentType: string,
  fileSize: number
): Promise<PresignedUploadResponse> {
  return await axios.post('/article/upload-presign', {
    fileName,
    contentType,
    fileSize
  })
}

/**
 * 使用预签名URL将文件直接PUT到TOS
 * 使用原生XMLHttpRequest，不走axios拦截器（避免自动加/api前缀和Bearer Token）
 */
function uploadToTOS(
  file: File,
  signedUrl: string,
  contentType: string,
  onProgress?: (progress: DirectUploadProgress) => void
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()

    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable && onProgress) {
        onProgress({
          loaded: e.loaded,
          total: e.total,
          percent: Math.round((e.loaded / e.total) * 100)
        })
      }
    })

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`TOS上传失败: HTTP ${xhr.status}`))
      }
    })

    xhr.addEventListener('error', () => {
      reject(new Error('网络错误，TOS上传失败'))
    })

    xhr.addEventListener('timeout', () => {
      reject(new Error('上传超时'))
    })

    xhr.open('PUT', signedUrl, true)
    xhr.setRequestHeader('Content-Type', contentType)
    xhr.timeout = 300000 // 5分钟超时
    xhr.send(file)
  })
}

/**
 * 完整的客户端直传流程：可选压缩 → 请求签名 → 直传TOS → 返回publicUrl
 */
export async function directUploadImage(
  file: File,
  options: DirectUploadOptions = {}
): Promise<string> {
  const {
    compress = true,
    compressionOptions = {
      maxWidth: 2048,
      maxHeight: 2048,
      quality: 0.8,
      maxSize: 5 * 1024 * 1024,
      preserveRatio: true
    },
    onProgress
  } = options

  let fileToUpload = file

  if (compress && needsCompression(file)) {
    console.log('[TOS直传] 开始压缩...')
    const result = await compressImageWithWorker(file, compressionOptions)
    if (result.success && result.file) {
      fileToUpload = result.file
      console.log('[TOS直传] 压缩完成:', {
        原始大小: `${(file.size / 1024).toFixed(1)}KB`,
        压缩后: `${(result.compressedSize / 1024).toFixed(1)}KB`,
        压缩率: `${result.compressionRatio.toFixed(1)}%`
      })
    }
  }

  console.log('[TOS直传] 请求预签名URL...')
  const presigned = await requestPresignedUrl(
    fileToUpload.name || file.name,
    fileToUpload.type || file.type || 'image/jpeg',
    fileToUpload.size
  )

  console.log('[TOS直传] 开始上传到TOS...')
  await uploadToTOS(
    fileToUpload,
    presigned.signedUrl,
    fileToUpload.type || file.type || 'image/jpeg',
    onProgress
  )

  console.log('[TOS直传] 上传成功:', presigned.publicUrl)
  return presigned.publicUrl
}
