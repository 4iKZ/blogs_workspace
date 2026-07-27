interface LockManagerLike {
  request<T>(name: string, callback: () => Promise<T>): Promise<T>
}

type RefreshMessage =
  | { type: 'request'; id: string }
  | { type: 'candidate'; id: string }
  | { type: 'lease'; id: string; expiresAt: number }
  | { type: 'success'; id: string; token: string }
  | { type: 'failure'; id: string; message: string }

interface BroadcastChannelLike {
  postMessage(message: RefreshMessage): void
  addEventListener(type: 'message', listener: (event: MessageEvent<RefreshMessage>) => void): void
  removeEventListener(type: 'message', listener: (event: MessageEvent<RefreshMessage>) => void): void
  close(): void
}

const LOCK_NAME = 'blog-auth-refresh'
const DISCOVERY_WINDOW_MS = 30
const LEASE_MS = 1000
const HEARTBEAT_MS = 250

const browserLockManager = () => {
  if (typeof navigator === 'undefined' || !('locks' in navigator)) {
    return undefined
  }
  return navigator.locks as unknown as LockManagerLike
}

const createBrowserChannel = () => {
  if (typeof BroadcastChannel === 'undefined') {
    return undefined
  }
  return new BroadcastChannel(LOCK_NAME) as unknown as BroadcastChannelLike
}

const wait = (milliseconds: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, milliseconds))

export class CrossTabRefreshCoordinator {
  private inFlight: Promise<string> | null = null

  constructor(
    private readonly lockManager = browserLockManager(),
    private readonly channelFactory: () => BroadcastChannelLike | undefined = createBrowserChannel
  ) {}

  run(refresh: () => Promise<string>): Promise<string> {
    if (this.inFlight) {
      return this.inFlight
    }

    this.inFlight = this.runCoordinated(refresh).finally(() => {
      this.inFlight = null
    })
    return this.inFlight
  }

  private runCoordinated(refresh: () => Promise<string>) {
    if (this.lockManager) {
      return this.lockManager.request(LOCK_NAME, refresh)
    }
    return this.runWithBroadcastLease(refresh)
  }

  private async runWithBroadcastLease(refresh: () => Promise<string>) {
    const channel = this.channelFactory()
    if (!channel) {
      return refresh()
    }

    const id = crypto.randomUUID()
    const candidates = new Set<string>()
    let discovering = false
    let leading = false
    let lease: { id: string; expiresAt: number } | null = null
    let result: RefreshMessage | null = null
    let signalWaiter: (() => void) | null = null

    const signal = () => {
      signalWaiter?.()
      signalWaiter = null
    }
    const publishLease = () => {
      channel.postMessage({ type: 'lease', id, expiresAt: Date.now() + LEASE_MS })
    }
    const listener = (event: MessageEvent<RefreshMessage>) => {
      const message = event.data
      if (message.type === 'request') {
        candidates.add(message.id)
        if (leading) {
          publishLease()
        } else if (discovering) {
          channel.postMessage({ type: 'candidate', id })
        }
      } else if (message.type === 'candidate') {
        candidates.add(message.id)
      } else if (message.type === 'lease' && message.id !== id) {
        lease = { id: message.id, expiresAt: message.expiresAt }
        signal()
      } else if (
        (message.type === 'success' || message.type === 'failure')
        && message.id !== id
      ) {
        result = message
        signal()
      }
    }
    const waitForSignal = (timeoutMs: number) => new Promise<void>((resolve) => {
      const timeout = setTimeout(() => {
        signalWaiter = null
        resolve()
      }, Math.max(0, timeoutMs))
      signalWaiter = () => {
        clearTimeout(timeout)
        resolve()
      }
    })
    const unwrapResult = () => {
      if (result?.type === 'success') {
        return result.token
      }
      if (result?.type === 'failure') {
        throw new Error(result.message)
      }
      return null
    }
    const currentLease = () => lease as { id: string; expiresAt: number } | null

    channel.addEventListener('message', listener)
    try {
      while (true) {
        const completed = unwrapResult()
        if (completed) {
          return completed
        }

        const observedLease = currentLease()
        if (observedLease && observedLease.expiresAt > Date.now()) {
          await waitForSignal(observedLease.expiresAt - Date.now())
          continue
        }

        lease = null
        candidates.clear()
        candidates.add(id)
        discovering = true
        channel.postMessage({ type: 'request', id })
        await wait(DISCOVERY_WINDOW_MS)
        discovering = false

        const discoveredResult = unwrapResult()
        if (discoveredResult) {
          return discoveredResult
        }
        const discoveredLease = currentLease()
        if (discoveredLease && discoveredLease.expiresAt > Date.now()) {
          continue
        }
        if ([...candidates].sort()[0] !== id) {
          await waitForSignal(DISCOVERY_WINDOW_MS)
          continue
        }

        leading = true
        publishLease()
        const heartbeat = setInterval(publishLease, HEARTBEAT_MS)
        try {
          const token = await refresh()
          channel.postMessage({ type: 'success', id, token })
          return token
        } catch (error) {
          channel.postMessage({
            type: 'failure',
            id,
            message: error instanceof Error ? error.message : 'Refresh token failed'
          })
          throw error
        } finally {
          clearInterval(heartbeat)
          leading = false
        }
      }
    } finally {
      channel.removeEventListener('message', listener)
      channel.close()
    }
  }
}

export const crossTabRefreshCoordinator = new CrossTabRefreshCoordinator()
