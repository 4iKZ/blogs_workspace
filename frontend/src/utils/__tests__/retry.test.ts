import { describe, expect, it, vi } from 'vitest'
import { withRetry } from '../retry'

describe('withRetry', () => {
  it('throws a meaningful error when retries is 0', async () => {
    const task = vi.fn(async () => {
      throw new Error('boom')
    })
    await expect(withRetry(task, () => true, 0)).rejects.toThrow('Retry failed')
    expect(task).not.toHaveBeenCalled()
  })

  it('does not retry HTTP 4xx errors', async () => {
    const err = Object.assign(new Error('Forbidden'), { response: { status: 403 } })
    const task = vi.fn(async () => {
      throw err
    })
    await expect(withRetry(task, () => true, 3, 0)).rejects.toThrow('Forbidden')
    expect(task).toHaveBeenCalledTimes(1)
  })

  it('does not retry aborted requests', async () => {
    const err = Object.assign(new Error('canceled'), { code: 'ERR_CANCELED' })
    const task = vi.fn(async () => {
      throw err
    })
    await expect(withRetry(task, () => true, 3, 0)).rejects.toThrow('canceled')
    expect(task).toHaveBeenCalledTimes(1)
  })

  it('retries network errors and succeeds on a later attempt', async () => {
    const task = vi.fn()
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce('ok')
    await expect(withRetry(task, () => true, 3, 0)).resolves.toBe('ok')
    expect(task).toHaveBeenCalledTimes(2)
  })
})
