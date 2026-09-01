import { describe, expect, it, vi } from 'vitest'
import { TokenRefreshCoordinator } from '../tokenRefreshQueue'

const deferred = <T>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

describe('TokenRefreshCoordinator', () => {
  it('shares one refresh and retries all queued requests after success', async () => {
    const refresh = deferred<string>()
    const refreshAccessToken = vi.fn(() => refresh.promise)
    const coordinator = new TokenRefreshCoordinator(refreshAccessToken)
    const retry = vi.fn(async (token: string) => `retried:${token}`)

    const requests = [
      coordinator.run(retry),
      coordinator.run(retry),
      coordinator.run(retry)
    ]

    expect(refreshAccessToken).toHaveBeenCalledTimes(1)
    expect(coordinator.pendingCount).toBe(3)

    refresh.resolve('new-token')

    await expect(Promise.all(requests)).resolves.toEqual([
      'retried:new-token',
      'retried:new-token',
      'retried:new-token'
    ])
    expect(retry).toHaveBeenCalledTimes(3)
    expect(coordinator.pendingCount).toBe(0)
  })

  it('rejects every waiter, clears the queue, and allows a later refresh', async () => {
    const firstRefresh = deferred<string>()
    const refreshAccessToken = vi
      .fn<() => Promise<string>>()
      .mockReturnValueOnce(firstRefresh.promise)
      .mockResolvedValueOnce('recovered-token')
    const onRefreshFailed = vi.fn()
    const coordinator = new TokenRefreshCoordinator(refreshAccessToken, onRefreshFailed)
    const retry = vi.fn(async (token: string) => token)

    const requests = [
      coordinator.run(retry),
      coordinator.run(retry),
      coordinator.run(retry)
    ]

    const authError = Object.assign(new Error('Unauthorized'), {
      response: { status: 401 }
    })
    firstRefresh.reject(authError)

    const results = await Promise.allSettled(requests)
    expect(results).toHaveLength(3)
    expect(results.every((result) => result.status === 'rejected')).toBe(true)
    expect(results.map((result) => (result as PromiseRejectedResult).reason)).toEqual([
      authError,
      authError,
      authError
    ])
    expect(onRefreshFailed).toHaveBeenCalledTimes(1)
    expect(coordinator.pendingCount).toBe(0)

    await expect(coordinator.run(retry)).resolves.toBe('recovered-token')
    expect(refreshAccessToken).toHaveBeenCalledTimes(2)
  })

  it('drains requests that arrive while the first batch is retrying (no orphaned promises)', async () => {
    const refresh = deferred<string>()
    const refreshAccessToken = vi.fn(() => refresh.promise)
    const coordinator = new TokenRefreshCoordinator(refreshAccessToken)

    const firstRetry = deferred<string>()
    const retry = vi.fn(() => firstRetry.promise)
    const secondRetry = vi.fn(async (token: string) => `second:${token}`)

    const first = coordinator.run(retry)
    refresh.resolve('new-token')

    // Let refreshAndFlush progress past takePending() and start retrying the first batch
    await Promise.resolve()

    // Reentrancy case: queued while refreshing is still true
    const second = coordinator.run(secondRetry)
    expect(coordinator.pendingCount).toBe(1)

    firstRetry.resolve('first:new-token')

    await expect(first).resolves.toBe('first:new-token')
    await expect(second).resolves.toBe('second:new-token')
    expect(secondRetry).toHaveBeenCalledTimes(1)
    expect(secondRetry).toHaveBeenCalledWith('new-token')
    expect(refreshAccessToken).toHaveBeenCalledTimes(1)
    expect(coordinator.pendingCount).toBe(0)
  })
})
