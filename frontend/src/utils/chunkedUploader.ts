/**
 * 分片上传工具
 * 支持大文件分片上传、断点续传和进度显示
 */

import axios from './axios'

// 分片上传配置
export interface ChunkedUploadOptions {
  chunkSize: number // 分片大小（字节）
  concurrent: number // 并发上传数量
  maxRetries: number // 最大重试次数
  onProgress?: (progress: ChunkedUploadProgress) => void
  onChunkComplete?: (chunkIndex: number, response: any) => void
  onError?: (error: Error, chunkIndex: number) => void
}

// 分片上传进度
export interface ChunkedUploadProgress {
  totalChunks: number
  uploadedChunks: number
  progress: number // 0-100
  uploadedBytes: number
  totalBytes: number
  speed: number // bytes/s
  remainingTime: number // seconds
}

// 分片信息
interface ChunkInfo {
  index: number
  start: number
  end: number
  size: number
  retries: number
  status: 'pending' | 'uploading' | 'completed' | 'failed'
}

// 上传会话信息
interface UploadSession {
  uploadId: string
  file: File
  fileName: string
  fileSize: number
  chunks: ChunkInfo[]
  uploadedChunks: Set<number>
  startTime: number
  lastUpdateTime: number
  uploadedBytes: number
}

// 存储活动会话
const activeSessions = new Map<string, UploadSession>()

/**
 * 生成分片上传ID
 */
function generateUploadId(): string {
  return `upload_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

/**
 * 计算文件哈希（用于断点续传）
 */
export async function calculateFileHash(file: File): Promise<string> {
  const bufferSize = 2 * 1024 * 1024 // 2MB
  const hashBuffer = await crypto.subtle.digest('SHA-256', await file.slice(0, Math.min(file.size, bufferSize)).arrayBuffer())
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('') + `_${file.size}`
}

/**
 * 创建文件分片
 */
function createChunks(file: File, chunkSize: number): ChunkInfo[] {
  const chunks: ChunkInfo[] = []
  const totalChunks = Math.ceil(file.size / chunkSize)

  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize
    const end = Math.min(start + chunkSize, file.size)
    chunks.push({
      index: i,
      start,
      end,
      size: end - start,
      retries: 0,
      status: 'pending'
    })
  }

  return chunks
}

/**
 * 上传单个分片
 */
async function uploadChunk(
  file: File,
  chunk: ChunkInfo,
  uploadId: string,
  fileName: string,
  totalChunks: number,
  token: string
): Promise<any> {
  const chunkData = file.slice(chunk.start, chunk.end)
  const formData = new FormData()
  formData.append('file', chunkData)
  formData.append('chunkIndex', chunk.index.toString())
  formData.append('totalChunks', totalChunks.toString())
  formData.append('uploadId', uploadId)
  formData.append('fileName', fileName)
  formData.append('fileSize', file.size.toString())

  const response = await axios.post('/article/upload-chunk', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      'Authorization': `Bearer ${token}`
    },
    timeout: 60000 // 60秒超时
  })

  return response
}

/**
 * 完成分片上传
 */
async function completeChunkedUpload(
  uploadId: string,
  fileName: string,
  totalChunks: number,
  token: string
): Promise<string> {
  const response = await axios.post('/article/complete-upload', {
    uploadId,
    fileName,
    totalChunks
  }, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })

  return response.url
}

/**
 * 取消分片上传
 */
async function cancelChunkedUpload(
  uploadId: string,
  token: string
): Promise<void> {
  try {
    await axios.post('/article/cancel-upload', {
      uploadId
    }, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
  } catch (error) {
    console.warn('[ChunkedUpload] 取消上传失败:', error)
  }
}

/**
 * 更新上传进度
 */
function updateProgress(session: UploadSession, options?: ChunkedUploadOptions): void {
  const now = Date.now()
  const elapsed = (now - session.startTime) / 1000 // 秒
  const uploadedBytes = Array.from(session.uploadedChunks)
    .reduce((sum, index) => sum + session.chunks[index].size, 0)

  session.uploadedBytes = uploadedBytes
  session.lastUpdateTime = now

  // 计算速度
  const speed = elapsed > 0 ? uploadedBytes / elapsed : 0

  // 计算剩余时间
  const remainingBytes = session.fileSize - uploadedBytes
  const remainingTime = speed > 0 ? remainingBytes / speed : 0

  const progress: ChunkedUploadProgress = {
    totalChunks: session.chunks.length,
    uploadedChunks: session.uploadedChunks.size,
    progress: (uploadedBytes / session.fileSize) * 100,
    uploadedBytes,
    totalBytes: session.fileSize,
    speed,
    remainingTime
  }

  options?.onProgress?.(progress)
}

/**
 * 分片上传
 */
export async function uploadWithChunks(
  file: File,
  token: string,
  options: Partial<ChunkedUploadOptions> = {}
): Promise<string> {
  const opts: ChunkedUploadOptions = {
    chunkSize: 5 * 1024 * 1024, // 默认5MB
    concurrent: 3, // 默认3个并发
    maxRetries: 3,
    ...options
  }

  // 创建上传会话
  const uploadId = generateUploadId()
  const chunks = createChunks(file, opts.chunkSize)

  const session: UploadSession = {
    uploadId,
    file,
    fileName: file.name,
    fileSize: file.size,
    chunks,
    uploadedChunks: new Set(),
    startTime: Date.now(),
    lastUpdateTime: Date.now(),
    uploadedBytes: 0
  }

  activeSessions.set(uploadId, session)

  try {
    // 初始化上传会话
    await axios.post('/article/init-upload', {
      uploadId,
      fileName: file.name,
      fileSize: file.size,
      totalChunks: chunks.length,
      fileHash: await calculateFileHash(file)
    }, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    // 并发上传分片
    await uploadChunksConcurrently(session, token, opts)

    // 完成上传
    const fileUrl = await completeChunkedUpload(
      uploadId,
      file.name,
      chunks.length,
      token
    )

    return fileUrl

  } finally {
    activeSessions.delete(uploadId)
  }
}

/**
 * 并发上传分片
 */
async function uploadChunksConcurrently(
  session: UploadSession,
  token: string,
  options: ChunkedUploadOptions
): Promise<void> {
  const { chunks, uploadedChunks } = session
  const pendingChunks = new Set<number>()
  let activeCount = 0
  let hasError = false

  const uploadNextChunk = async (): Promise<void> => {
    if (hasError) return

    // 查找下一个待上传的分片
    let nextChunk: ChunkInfo | null = null
    for (const chunk of chunks) {
      if (chunk.status === 'pending' && !pendingChunks.has(chunk.index)) {
        nextChunk = chunk
        break
      }
    }

    if (!nextChunk) return

    nextChunk.status = 'uploading'
    pendingChunks.add(nextChunk.index)
    activeCount++

    try {
      const response = await uploadChunk(
        session.file,
        nextChunk,
        session.uploadId,
        session.fileName,
        chunks.length,
        token
      )

      nextChunk.status = 'completed'
      uploadedChunks.add(nextChunk.index)
      pendingChunks.delete(nextChunk.index)
      activeCount--

      options.onChunkComplete?.(nextChunk.index, response)
      updateProgress(session, options)

      // 继续上传下一个
      await uploadNextChunk()

    } catch (error) {
      nextChunk.retries++

      if (nextChunk.retries < options.maxRetries) {
        // 重试
        nextChunk.status = 'pending'
        pendingChunks.delete(nextChunk.index)
        activeCount--
        console.warn(`[ChunkedUpload] 分片 ${nextChunk.index} 上传失败，重试 ${nextChunk.retries}/${options.maxRetries}`)
        await uploadNextChunk()
      } else {
        // 达到最大重试次数
        nextChunk.status = 'failed'
        pendingChunks.delete(nextChunk.index)
        activeCount--
        hasError = true
        options.onError?.(error as Error, nextChunk.index)
        throw new Error(`分片 ${nextChunk.index} 上传失败: ${error}`)
      }
    }
  }

  // 启动初始并发上传
  const promises: Promise<void>[] = []
  for (let i = 0; i < options.concurrent; i++) {
    promises.push(uploadNextChunk())
  }

  await Promise.all(promises)
}

/**
 * 检查是否有未完成的上传
 */
export async function checkResumeUpload(file: File, token: string): Promise<string | null> {
  try {
    const fileHash = await calculateFileHash(file)
    const response = await axios.get(`/article/check-upload/${fileHash}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    return response.uploadId || null
  } catch {
    return null
  }
}

/**
 * 恢复上传
 */
export async function resumeUpload(
  uploadId: string,
  file: File,
  token: string,
  options?: Partial<ChunkedUploadOptions>
): Promise<string> {
  const opts: ChunkedUploadOptions = {
    chunkSize: 5 * 1024 * 1024,
    concurrent: 3,
    maxRetries: 3,
    ...options
  }

  // 获取已上传的分片信息
  const response = await axios.get(`/article/upload-status/${uploadId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })

  // 类型断言：uploadedChunks应该是number数组
  const uploadedChunksData = (response as any).uploadedChunks || []
  const uploadedChunks = new Set<number>(uploadedChunksData.map((n: unknown) => Number(n)))
  const chunks = createChunks(file, opts.chunkSize)

  // 标记已上传的分片
  chunks.forEach(chunk => {
    if (uploadedChunks.has(chunk.index)) {
      chunk.status = 'completed'
    }
  })

  // 创建会话
  const session: UploadSession = {
    uploadId,
    file,
    fileName: file.name,
    fileSize: file.size,
    chunks,
    uploadedChunks,
    startTime: Date.now(),
    lastUpdateTime: Date.now(),
    uploadedBytes: 0
  }

  activeSessions.set(uploadId, session)

  try {
    await uploadChunksConcurrently(session, token, opts)
    return await completeChunkedUpload(uploadId, file.name, chunks.length, token)
  } finally {
    activeSessions.delete(uploadId)
  }
}

/**
 * 取消上传
 */
export function cancelUpload(uploadId: string, token: string): void {
  const session = activeSessions.get(uploadId)
  if (session) {
    activeSessions.delete(uploadId)
    cancelChunkedUpload(uploadId, token).catch(console.error)
  }
}

/**
 * 格式化剩余时间
 */
export function formatRemainingTime(seconds: number): string {
  if (seconds < 60) {
    return `${Math.round(seconds)}秒`
  } else if (seconds < 3600) {
    return `${Math.round(seconds / 60)}分钟`
  } else {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.round((seconds % 3600) / 60)
    return `${hours}小时${minutes}分钟`
  }
}

/**
 * 格式化上传速度
 */
export function formatUploadSpeed(bytesPerSecond: number): string {
  if (bytesPerSecond < 1024) {
    return `${bytesPerSecond.toFixed(0)} B/s`
  } else if (bytesPerSecond < 1024 * 1024) {
    return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`
  } else {
    return `${(bytesPerSecond / (1024 * 1024)).toFixed(2)} MB/s`
  }
}
