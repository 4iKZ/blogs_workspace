import { beforeEach, describe, expect, it, vi } from 'vitest'

const post = vi.fn()
const get = vi.fn()
vi.mock('../axios', () => ({ default: { post, get } }))

/**
 * 可控的分片请求 mock：返回一个挂起的 Promise，记录 signal，可手动 resolve/reject。
 */
function makeControllablePostMock() {
  type Resolver = (value: unknown) => void
  type Rejecter = (reason?: unknown) => void
  const pending: Array<{
    url: string
    signal?: AbortSignal
    resolve: Resolver
    reject: Rejecter
  }> = []

  post.mockReset()
  post.mockImplementation((url: string, _data: unknown, config?: any) => {
    // 非分片接口立即返回
    const isChunk = String(url).includes('/upload-chunk')
    if (!isChunk) {
      if (String(url).includes('/init-upload')) {
        return Promise.resolve({
          uploadId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8',
          chunkSize: 5 * 1024 * 1024,
          maxFileSize: 20 * 1024 * 1024,
          expiresAt: Date.now() + 60_000
        })
      }
      return Promise.resolve({ success: true })
    }
    const signal = config?.signal as AbortSignal | undefined
    return new Promise((resolve, reject) => {
      const entry = { url, signal, resolve, reject }
      pending.push(entry)
      if (signal?.aborted) {
        const err = new Error('canceled') as any
        err.code = 'ERR_CANCELED'
        reject(err)
      } else {
        signal?.addEventListener('abort', () => {
          const err = new Error('canceled') as any
          err.code = 'ERR_CANCELED'
          reject(err)
        })
      }
    })
  })

  return {
    getAll: () => pending.slice(),
    find: (url: string) => pending.find(p => p.url.includes(url)),
    waitFor: async (url: string, count = 1) => {
      const deadline = Date.now() + 2000
      while (Date.now() < deadline) {
        const matches = pending.filter(p => p.url.includes(url))
        if (matches.length >= count) return matches
        await new Promise(r => setTimeout(r, 5))
      }
      throw new Error(`timeout waiting for ${url}`)
    }
  }
}

describe('chunked upload protocol', () => {
  beforeEach(() => {
    post.mockReset()
    get.mockReset()
    post.mockResolvedValueOnce({
      uploadId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8',
      chunkSize: 5 * 1024 * 1024,
      maxFileSize: 10 * 1024 * 1024,
      expiresAt: Date.now() + 60_000
    })
    post.mockResolvedValueOnce({ success: true })
    post.mockResolvedValueOnce({ url: 'https://example.com/cover.png' })
  })

  it('uses the server upload id and sends only the new protocol fields', async () => {
    const { uploadWithChunks } = await import('../chunkedUploader')
    const file = new File([new Uint8Array([1, 2, 3])], 'cover.png', { type: 'image/png' })

    await expect(uploadWithChunks(file, 'token')).resolves.toBe('https://example.com/cover.png')

    expect(post.mock.calls[0][0]).toBe('/article/init-upload')
    expect(post.mock.calls[0][1]).not.toHaveProperty('uploadId')
    const chunkForm = post.mock.calls[1][1] as FormData
    expect([...chunkForm.keys()].sort()).toEqual(['file', 'index', 'uploadId'])
    expect(post.mock.calls[2][1]).toEqual({ uploadId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8' })
  })

  it('hashes the complete file rather than only the first 2MiB', async () => {
    const { calculateFileHash } = await import('../chunkedUploader')
    const prefix = new Uint8Array(2 * 1024 * 1024)
    const first = new File([prefix, new Uint8Array([1])], 'a.png')
    const second = new File([prefix, new Uint8Array([2])], 'a.png')
    expect(await calculateFileHash(first)).not.toBe(await calculateFileHash(second))
  })

  it('resumes using uploadedIndices and sends only missing chunks', async () => {
    post.mockReset()
    get.mockResolvedValueOnce({ uploadedIndices: [0] })
    post.mockResolvedValueOnce({ success: true })
    post.mockResolvedValueOnce({ url: 'https://example.com/resumed.png' })
    const { resumeUpload } = await import('../chunkedUploader')
    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'cover.png')

    await expect(resumeUpload('6ba7b810-9dad-11d1-80b4-00c04fd430c8', file, 'token'))
      .resolves.toBe('https://example.com/resumed.png')
    const form = post.mock.calls[0][1] as FormData
    expect(form.get('index')).toBe('1')
  })

  it('checks for an owner resume session before initializing a new upload', async () => {
    post.mockReset()
    get.mockReset()
    get.mockResolvedValueOnce({ uploadId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8' })
    get.mockResolvedValueOnce({ uploadedIndices: [0] })
    post.mockResolvedValueOnce({ success: true })
    post.mockResolvedValueOnce({ url: 'https://example.com/resumed.png' })
    const { uploadWithChunks } = await import('../chunkedUploader')
    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'cover.png')

    await expect(uploadWithChunks(file, 'token')).resolves.toBe('https://example.com/resumed.png')

    expect(get.mock.calls[0][0]).toContain('/article/check-upload/')
    expect(get.mock.calls[1][0]).toBe(
      '/article/upload-status/6ba7b810-9dad-11d1-80b4-00c04fd430c8'
    )
    expect(post.mock.calls.every(call => call[0] !== '/article/init-upload')).toBe(true)
  })

  it('cancels an expired initialization', async () => {
    post.mockReset()
    post.mockResolvedValueOnce({
      uploadId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8',
      chunkSize: 5 * 1024 * 1024,
      maxFileSize: 10 * 1024 * 1024,
      expiresAt: Date.now() - 1
    })
    post.mockResolvedValueOnce({})
    const { uploadWithChunks } = await import('../chunkedUploader')
    await expect(uploadWithChunks(new File([new Uint8Array([1])], 'cover.png'), 'token')).rejects.toThrow()
    expect(post.mock.calls[1][0]).toBe('/article/cancel-upload')
  })

  it('sends cancellation even when the session is not active locally', async () => {
    post.mockReset()
    post.mockResolvedValueOnce({})
    const { cancelUpload } = await import('../chunkedUploader')
    cancelUpload('6ba7b810-9dad-11d1-80b4-00c04fd430c8', 'token')
    await Promise.resolve()
    expect(post.mock.calls[0][0]).toBe('/article/cancel-upload')
    expect(post.mock.calls[0][1]).toEqual({ uploadId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8' })
  })
})

describe('chunked upload cancellation semantics', () => {
  it('aborts other in-flight requests without retrying when one chunk exhausts its retries', async () => {
    get.mockReset()
    get.mockResolvedValue({ uploadId: null })
    const ctl = makeControllablePostMock()
    const { uploadWithChunks } = await import('../chunkedUploader')

    // 11MB 文件 → 3 个分片，并发 3，保证三个分片请求同时 in-flight
    const file = new File([new Uint8Array(11 * 1024 * 1024)], 'cover.png')
    const promise = uploadWithChunks(file, 'token', { maxRetries: 1, concurrent: 3 })

    const chunkRequests = await ctl.waitFor('/upload-chunk', 3)
    // 让第一个分片重试耗尽（retry=0，maxRetries=1 → 第一次失败即到达上限）
    const first = chunkRequests[0]
    first.reject(new Error('network down'))

    await expect(promise).rejects.toThrow('network down')

    // 其余 in-flight 分片应被终止（signal.aborted 为 true），且不计重试
    const others = chunkRequests.slice(1)
    for (const other of others) {
      expect(other.signal?.aborted).toBe(true)
    }
  })

  it('cancelUpload aborts in-flight requests', async () => {
    get.mockReset()
    get.mockResolvedValue({ uploadId: null })
    post.mockReset()
    post.mockImplementation((url: string, _data: unknown, config?: any) => {
      const signal = config?.signal as AbortSignal | undefined
      return new Promise((resolve, reject) => {
        if (String(url).includes('/init-upload')) {
          resolve({
            uploadId: 'cancel-me-1234',
            chunkSize: 5 * 1024 * 1024,
            maxFileSize: 10 * 1024 * 1024,
            expiresAt: Date.now() + 60_000
          })
          return
        }
        if (signal?.aborted) {
          const err = new Error('canceled') as any
          err.code = 'ERR_CANCELED'
          return reject(err)
        }
        signal?.addEventListener('abort', () => {
          const err = new Error('canceled') as any
          err.code = 'ERR_CANCELED'
          reject(err)
        })
        // 挂起：分片与 cancel-upload 均保持 in-flight 以便观察 abort
        void resolve
      })
    })

    const { uploadWithChunks, cancelUpload } = await import('../chunkedUploader')

    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'cover.png')
    const promise = uploadWithChunks(file, 'token', { concurrent: 3 })

    // 等待至少一个分片请求进入 in-flight
    await (async () => {
      const deadline = Date.now() + 2000
      while (Date.now() < deadline) {
        if (post.mock.calls.some(c => String(c[0]).includes('/upload-chunk'))) return
        await new Promise(r => setTimeout(r, 5))
      }
      throw new Error('timeout waiting for chunk upload')
    })()

    cancelUpload('cancel-me-1234', 'token')

    await expect(promise).rejects.toThrow('上传已取消')

    // 所有分片请求都应收到 abort
    const chunkCalls = post.mock.calls.filter(c => String(c[0]).includes('/upload-chunk'))
    expect(chunkCalls.length).toBeGreaterThan(0)
    for (const call of chunkCalls) {
      expect((call[2] as any)?.signal?.aborted).toBe(true)
    }
  })

  it('calculateFileHash runs exactly once across the full flow', async () => {
    get.mockReset()
    get.mockResolvedValue({ uploadId: null })
    post.mockReset()
    post.mockImplementation((url: string) => {
      if (String(url).includes('/init-upload')) {
        return Promise.resolve({
          uploadId: '6ba7b810-9dad-11d1-80b4-00c04fd430c8',
          chunkSize: 5 * 1024 * 1024,
          maxFileSize: 11 * 1024 * 1024,
          expiresAt: Date.now() + 60_000
        })
      }
      if (String(url).includes('/complete-upload')) {
        return Promise.resolve({ url: 'https://example.com/cover.png' })
      }
      // 分片上传
      return Promise.resolve({ success: true })
    })

    const hashSpy = vi.spyOn(crypto.subtle, 'digest')

    const { uploadWithChunks } = await import('../chunkedUploader')
    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'cover.png')

    await expect(uploadWithChunks(file, 'token')).resolves.toBe('https://example.com/cover.png')

    expect(hashSpy).toHaveBeenCalledTimes(1)
    hashSpy.mockRestore()
  })
})
