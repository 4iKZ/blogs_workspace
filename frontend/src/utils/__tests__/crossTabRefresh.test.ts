import { afterEach, describe, expect, it, vi } from 'vitest'
import { CrossTabRefreshCoordinator } from '../crossTabRefresh'

class SerialLockManager {
  private tail = Promise.resolve()

  request<T>(_name: string, callback: () => Promise<T>): Promise<T> {
    const result = this.tail.then(callback)
    this.tail = result.then(() => undefined, () => undefined)
    return result
  }
}

const deferred = <T>() => {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((resolvePromise) => {
    resolve = resolvePromise
  })
  return { promise, resolve }
}

class FakeBroadcastBus {
  readonly channels: FakeBroadcastChannel[] = []

  open = () => {
    const channel = new FakeBroadcastChannel(this)
    this.channels.push(channel)
    return channel
  }

  broadcast(sender: FakeBroadcastChannel, message: unknown) {
    this.channels
      .filter((channel) => channel !== sender && !channel.closed)
      .forEach((channel) => channel.deliver(message))
  }
}

class FakeBroadcastChannel {
  readonly listeners = new Set<(event: MessageEvent<any>) => void>()
  closed = false

  constructor(private readonly bus: FakeBroadcastBus) {}

  postMessage(message: any) {
    if (!this.closed) {
      this.bus.broadcast(this, message)
    }
  }

  addEventListener(_type: 'message', listener: (event: MessageEvent<any>) => void) {
    this.listeners.add(listener)
  }

  removeEventListener(_type: 'message', listener: (event: MessageEvent<any>) => void) {
    this.listeners.delete(listener)
  }

  close() {
    this.closed = true
    this.listeners.clear()
  }

  deliver(message: unknown) {
    this.listeners.forEach((listener) => listener({ data: message } as MessageEvent<any>))
  }
}

describe('CrossTabRefreshCoordinator', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

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

  it('lets a late joining tab wait for the active BroadcastChannel leader result', async () => {
    vi.useFakeTimers()
    const bus = new FakeBroadcastBus()
    const leaderResult = deferred<string>()
    const leaderRefresh = vi.fn(() => leaderResult.promise)
    const followerRefresh = vi.fn(async () => 'must-not-rotate')
    const leader = new CrossTabRefreshCoordinator(undefined, bus.open)
    const follower = new CrossTabRefreshCoordinator(undefined, bus.open)

    const leaderPromise = leader.run(leaderRefresh)
    await vi.advanceTimersByTimeAsync(35)
    expect(leaderRefresh).toHaveBeenCalledTimes(1)

    const followerPromise = follower.run(followerRefresh)
    await vi.advanceTimersByTimeAsync(35)
    expect(followerRefresh).not.toHaveBeenCalled()

    leaderResult.resolve('leader-token')
    await expect(Promise.all([leaderPromise, followerPromise]))
      .resolves.toEqual(['leader-token', 'leader-token'])
  })

  it('re-elects a follower after the BroadcastChannel leader lease expires', async () => {
    vi.useFakeTimers()
    const bus = new FakeBroadcastBus()
    const leaderRefresh = vi.fn(() => new Promise<string>(() => undefined))
    const followerRefresh = vi.fn(async () => 'recovered-token')
    const leader = new CrossTabRefreshCoordinator(undefined, bus.open)
    const follower = new CrossTabRefreshCoordinator(undefined, bus.open)

    void leader.run(leaderRefresh)
    await vi.advanceTimersByTimeAsync(35)
    const followerPromise = follower.run(followerRefresh)
    await vi.advanceTimersByTimeAsync(35)
    expect(followerRefresh).not.toHaveBeenCalled()

    bus.channels[0].close()
    await vi.advanceTimersByTimeAsync(5000)

    await expect(followerPromise).resolves.toBe('recovered-token')
    expect(followerRefresh).toHaveBeenCalledTimes(1)
  })
})
