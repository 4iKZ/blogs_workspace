import { beforeEach, describe, expect, it, vi } from 'vitest'

const post = vi.fn()
const get = vi.fn()
vi.mock('../axios', () => ({ default: { post, get } }))

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
