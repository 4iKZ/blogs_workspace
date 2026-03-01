/**
 * 图片压缩缓存管理器
 * 使用IndexedDB存储压缩结果，避免重复压缩相同图片
 */

import type { CompressionResult } from './imageCompressor'

// 缓存条目
interface CacheEntry {
  hash: string
  fileHash: string // 基于文件内容的哈希
  fileName: string
  originalSize: number
  compressedSize: number
  compressedBlob: Blob
  compressedDataUrl: string
  width: number
  height: number
  compressionRatio: number
  createdAt: number
  expiresAt: number
}

// 缓存配置
interface CacheConfig {
  dbName: string
  storeName: string
  maxEntries: number // 最大缓存条目数
  maxAge: number // 缓存有效期（毫秒）
  maxSize: number // 最大缓存大小（字节）
}

const DEFAULT_CONFIG: CacheConfig = {
  dbName: 'ImageCompressionCache',
  storeName: 'compressed_images',
  maxEntries: 100,
  maxAge: 7 * 24 * 60 * 60 * 1000, // 7天
  maxSize: 100 * 1024 * 1024 // 100MB
}

/**
 * 生成文件哈希（基于文件内容）
 */
async function generateFileHash(file: File): Promise<string> {
  const buffer = await file.arrayBuffer()
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

/**
 * 压缩缓存管理器
 */
export class CompressionCacheManager {
  private config: CacheConfig
  private db: IDBDatabase | null = null
  private initPromise: Promise<void> | null = null

  constructor(config: Partial<CacheConfig> = {}) {
    this.config = { ...DEFAULT_CONFIG, ...config }
    this.initPromise = this.initDB()
  }

  /**
   * 初始化IndexedDB
   */
  private async initDB(): Promise<void> {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.config.dbName, 1)

      request.onerror = () => {
        reject(new Error('无法打开IndexedDB'))
      }

      request.onsuccess = () => {
        this.db = request.result
        resolve()
      }

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result
        if (!db.objectStoreNames.contains(this.config.storeName)) {
          const store = db.createObjectStore(this.config.storeName, { keyPath: 'hash' })
          store.createIndex('fileHash', 'fileHash', { unique: false })
          store.createIndex('createdAt', 'createdAt', { unique: false })
          store.createIndex('expiresAt', 'expiresAt', { unique: false })
        }
      }
    })
  }

  /**
   * 确保数据库已初始化
   */
  private async ensureDB(): Promise<IDBDatabase> {
    if (!this.db) {
      await this.initPromise!
    }
    if (!this.db) {
      throw new Error('数据库未初始化')
    }
    return this.db
  }

  /**
   * 检查缓存中是否存在压缩结果
   */
  async has(file: File): Promise<boolean> {
    const hash = await generateFileHash(file)
    return this.hasByHash(hash)
  }

  /**
   * 根据哈希检查缓存
   */
  async hasByHash(hash: string): Promise<boolean> {
    try {
      const entry = await this.get(hash)
      return entry !== null
    } catch {
      return false
    }
  }

  /**
   * 从缓存获取压缩结果
   */
  async get(file: File): Promise<CompressionResult | null>
  async get(hash: string): Promise<CompressionResult | null>
  async get(fileOrHash: File | string): Promise<CompressionResult | null> {
    await this.ensureDB()

    const hash = typeof fileOrHash === 'string'
      ? fileOrHash
      : await generateFileHash(fileOrHash)

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readonly')
      const store = transaction.objectStore(this.config.storeName)
      const request = store.get(hash)

      request.onsuccess = () => {
        const entry: CacheEntry | undefined = request.result

        if (!entry) {
          resolve(null)
          return
        }

        // 检查是否过期
        if (Date.now() > entry.expiresAt) {
          this.delete(hash).catch(() => {})
          resolve(null)
          return
        }

        // 将Blob转换为File
        const compressedFile = new File([entry.compressedBlob], entry.fileName, {
          type: `image/jpeg`,
          lastModified: entry.createdAt
        })

        resolve({
          file: compressedFile,
          originalSize: entry.originalSize,
          compressedSize: entry.compressedSize,
          compressionRatio: entry.compressionRatio,
          width: entry.width,
          height: entry.height,
          success: true
        })
      }

      request.onerror = () => {
        reject(new Error('获取缓存失败'))
      }
    })
  }

  /**
   * 将压缩结果存入缓存
   */
  async set(
    file: File,
    result: CompressionResult
  ): Promise<void> {
    await this.ensureDB()

    const fileHash = await generateFileHash(file)
    const cacheKey = `${fileHash}_${result.width}_${result.height}_${Math.round(result.compressionRatio)}`

    const entry: CacheEntry = {
      hash: cacheKey,
      fileHash,
      fileName: result.file.name,
      originalSize: result.originalSize,
      compressedSize: result.compressedSize,
      compressedBlob: result.file,
      compressedDataUrl: '',
      width: result.width,
      height: result.height,
      compressionRatio: result.compressionRatio,
      createdAt: Date.now(),
      expiresAt: Date.now() + this.config.maxAge
    }

    // 检查缓存大小限制
    await this.enforceSizeLimit()

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readwrite')
      const store = transaction.objectStore(this.config.storeName)
      const request = store.put(entry)

      request.onsuccess = () => resolve()
      request.onerror = () => reject(new Error('保存缓存失败'))
    })
  }

  /**
   * 删除缓存条目
   */
  async delete(hash: string): Promise<void> {
    await this.ensureDB()

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readwrite')
      const store = transaction.objectStore(this.config.storeName)
      const request = store.delete(hash)

      request.onsuccess = () => resolve()
      request.onerror = () => reject(new Error('删除缓存失败'))
    })
  }

  /**
   * 清空所有缓存
   */
  async clear(): Promise<void> {
    await this.ensureDB()

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readwrite')
      const store = transaction.objectStore(this.config.storeName)
      const request = store.clear()

      request.onsuccess = () => resolve()
      request.onerror = () => reject(new Error('清空缓存失败'))
    })
  }

  /**
   * 清理过期缓存
   */
  async cleanExpired(): Promise<number> {
    await this.ensureDB()

    const now = Date.now()
    let count = 0

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readwrite')
      const store = transaction.objectStore(this.config.storeName)
      const index = store.index('expiresAt')
      const request = index.openCursor(IDBKeyRange.upperBound(now))

      request.onsuccess = (event) => {
        const cursor = (event.target as IDBRequest).result
        if (cursor) {
          cursor.delete()
          count++
          cursor.continue()
        } else {
          resolve(count)
        }
      }

      request.onerror = () => reject(new Error('清理过期缓存失败'))
    })
  }

  /**
   * 获取缓存统计信息
   */
  async getStats(): Promise<{
    count: number
    totalSize: number
    oldestEntry: number | null
    newestEntry: number | null
  }> {
    await this.ensureDB()

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readonly')
      const store = transaction.objectStore(this.config.storeName)
      const countRequest = store.count()
      let totalSize = 0
      let oldestEntry: number | null = null
      let newestEntry: number | null = null

      countRequest.onsuccess = () => {
        const count = countRequest.result

        if (count === 0) {
          resolve({ count: 0, totalSize: 0, oldestEntry: null, newestEntry: null })
          return
        }

        // 遍历所有条目计算统计信息
        const cursorRequest = store.openCursor()

        cursorRequest.onsuccess = (event) => {
          const cursor = (event.target as IDBRequest).result
          if (cursor) {
            const entry: CacheEntry = cursor.value
            totalSize += entry.compressedSize

            if (oldestEntry === null || entry.createdAt < oldestEntry) {
              oldestEntry = entry.createdAt
            }
            if (newestEntry === null || entry.createdAt > newestEntry) {
              newestEntry = entry.createdAt
            }

            cursor.continue()
          } else {
            resolve({ count, totalSize, oldestEntry, newestEntry })
          }
        }

        cursorRequest.onerror = () => reject(new Error('获取缓存统计失败'))
      }

      countRequest.onerror = () => reject(new Error('获取缓存数量失败'))
    })
  }

  /**
   * 强制执行缓存大小限制
   */
  private async enforceSizeLimit(): Promise<void> {
    const stats = await this.getStats()

    // 检查条目数量限制
    if (stats.count >= this.config.maxEntries) {
      await this.removeOldestEntries(stats.count - this.config.maxEntries + 1)
    }

    // 检查总大小限制
    if (stats.totalSize >= this.config.maxSize) {
      // 删除最旧的条目直到满足大小限制
      await this.removeOldestEntriesBySize(stats.totalSize - this.config.maxSize + 10 * 1024 * 1024) // 额外删除10MB
    }
  }

  /**
   * 删除最旧的N个条目
   */
  private async removeOldestEntries(count: number): Promise<void> {
    await this.ensureDB()

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readwrite')
      const store = transaction.objectStore(this.config.storeName)
      const index = store.index('createdAt')
      const request = index.openCursor()
      let removed = 0

      request.onsuccess = (event) => {
        const cursor = (event.target as IDBRequest).result
        if (cursor && removed < count) {
          cursor.delete()
          removed++
          cursor.continue()
        } else {
          resolve()
        }
      }

      request.onerror = () => reject(new Error('删除旧缓存失败'))
    })
  }

  /**
   * 删除旧条目直到释放足够空间
   */
  private async removeOldestEntriesBySize(targetSize: number): Promise<void> {
    await this.ensureDB()

    let freedSize = 0

    return new Promise((resolve, reject) => {
      const transaction = this.db!.transaction([this.config.storeName], 'readwrite')
      const store = transaction.objectStore(this.config.storeName)
      const index = store.index('createdAt')
      const request = index.openCursor()

      request.onsuccess = (event) => {
        const cursor = (event.target as IDBRequest).result
        if (cursor && freedSize < targetSize) {
          const entry: CacheEntry = cursor.value
          freedSize += entry.compressedSize
          cursor.delete()
          cursor.continue()
        } else {
          resolve()
        }
      }

      request.onerror = () => reject(new Error('删除缓存失败'))
    })
  }
}

// 导出单例
export const compressionCache = new CompressionCacheManager()

/**
 * 使用缓存的压缩函数包装器
 */
export async function compressWithCache(
  file: File,
  compressFn: (file: File) => Promise<CompressionResult>
): Promise<CompressionResult> {
  // 检查缓存
  const cached = await compressionCache.get(file)
  if (cached) {
    console.log('[CompressionCache] 使用缓存结果')
    return cached
  }

  // 执行压缩
  const result = await compressFn(file)

  // 存入缓存
  if (result.success) {
    compressionCache.set(file, result).catch(err => {
      console.warn('[CompressionCache] 保存缓存失败:', err)
    })
  }

  return result
}
