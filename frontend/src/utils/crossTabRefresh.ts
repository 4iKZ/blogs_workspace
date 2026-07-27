interface LockManagerLike {
  request<T>(name: string, callback: () => Promise<T>): Promise<T>
}

interface RefreshMessage {
  type: 'candidate' | 'success' | 'failure'
  id: string
  token?: string
  message?: string
}

interface BroadcastChannelLike {
  postMessage(message: RefreshMessage): void
  addEventListener(type: 'message', listener: (event: MessageEvent<RefreshMessage>) => void): void
  removeEventListener(type: 'message', listener: (event: MessageEvent<RefreshMessage>) => void): void
  close(): void
}

const LOCK_NAME = 'blog-auth-refresh'
const ELECTION_WINDOW_MS = 30

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
    return this.runWithBroadcastElection(refresh)
  }

  private async runWithBroadcastElection(refresh: () => Promise<string>) {
    const channel = this.channelFactory()
    if (!channel) {
      return refresh()
    }

    const id = crypto.randomUUID()
    const candidates = new Set<string>([id])
    let settleRemote!: (message: RefreshMessage) => void
    const remoteResult = new Promise<RefreshMessage>((resolve) => {
      settleRemote = resolve
    })
    const listener = (event: MessageEvent<RefreshMessage>) => {
      const message = event.data
      if (message.type === 'candidate') {
        candidates.add(message.id)
      } else if (message.id !== id) {
        settleRemote(message)
      }
    }
    channel.addEventListener('message', listener)
    channel.postMessage({ type: 'candidate', id })
    await new Promise((resolve) => setTimeout(resolve, ELECTION_WINDOW_MS))

    try {
      if ([...candidates].sort()[0] === id) {
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
        }
      }

      const result = await remoteResult
      if (result.type === 'success' && result.token) {
        return result.token
      }
      throw new Error(result.message || 'Refresh token failed')
    } finally {
      channel.removeEventListener('message', listener)
      channel.close()
    }
  }
}

export const crossTabRefreshCoordinator = new CrossTabRefreshCoordinator()
