import { describe, expect, it, vi } from 'vitest'
import { CrossTabRefreshCoordinator } from '../crossTabRefresh'

class SerialLockManager {
  private tail = Promise.resolve()

  request<T>(_name: string, callback: () => Promise<T>): Promise<T> {
    const result = this.tail.then(callback)
    this.tail = result.then(() => undefined, () => undefined)
    return result
  }
}

describe('CrossTabRefreshCoordinator', () => {
  it('serializes independent store and axios refreshes so both use the latest rotating cookie', async () => {
    const locks = new SerialLockManager()
    const storeCoordinator = new CrossTabRefreshCoordinator(locks)
    const axiosCoordinator = new CrossTabRefreshCoordinator(locks)
    let cookieGeneration = 0
    let activeRefreshes = 0
    let maxActiveRefreshes = 0
    const refreshEndpoint = vi.fn(async () => {
      activeRefreshes += 1
      maxActiveRefreshes = Math.max(maxActiveRefreshes, activeRefreshes)
      const generationReadByServer = cookieGeneration
      await Promise.resolve()
      expect(cookieGeneration).toBe(generationReadByServer)
      cookieGeneration += 1
      activeRefreshes -= 1
      return `access-${cookieGeneration}`
    })

    await expect(Promise.all([
      storeCoordinator.run(refreshEndpoint),
      axiosCoordinator.run(refreshEndpoint)
    ])).resolves.toEqual(['access-1', 'access-2'])

    expect(maxActiveRefreshes).toBe(1)
    expect(refreshEndpoint).toHaveBeenCalledTimes(2)
  })
})
