type RetryRequest<T> = (token: string) => Promise<T>

interface PendingRequest<T = unknown> {
  retry: RetryRequest<T>
  resolve: (value: T | PromiseLike<T>) => void
  reject: (reason?: unknown) => void
}

export class TokenRefreshCoordinator {
  private refreshing = false
  private pending: PendingRequest[] = []

  constructor(
    private readonly refreshAccessToken: () => Promise<string>,
    private readonly onRefreshFailed?: (error: unknown) => void
  ) {}

  get pendingCount() {
    return this.pending.length
  }

  run<T>(retry: RetryRequest<T>): Promise<T> {
    const result = new Promise<T>((resolve, reject) => {
      this.pending.push({
        retry: retry as RetryRequest<unknown>,
        resolve: resolve as PendingRequest['resolve'],
        reject
      })
    })

    if (!this.refreshing) {
      this.refreshing = true
      void this.refreshAndFlush()
    }

    return result
  }

  private async refreshAndFlush() {
    try {
      const token = await this.refreshAccessToken()
      const queued = this.takePending()
      await Promise.all(
        queued.map(async ({ retry, resolve, reject }) => {
          try {
            resolve(await retry(token))
          } catch (error) {
            reject(error)
          }
        })
      )
    } catch (error) {
      const queued = this.takePending()
      queued.forEach(({ reject }) => reject(error))
      this.onRefreshFailed?.(error)
    } finally {
      this.refreshing = false
    }
  }

  private takePending() {
    const queued = this.pending
    this.pending = []
    return queued
  }
}
