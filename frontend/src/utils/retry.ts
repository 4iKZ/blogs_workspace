export async function withRetry<T>(
  task: () => Promise<T>,
  validate: (data: T) => boolean,
  retries = 3,
  delayMs = 1000,
  onAttempt?: (n: number) => void
): Promise<T> {
  let lastError: any
  for (let attempt = 1; attempt <= retries; attempt++) {
    if (onAttempt) onAttempt(attempt)
    try {
      const result = await task()
      if (validate(result)) return result
      lastError = new Error('Invalid data')
    } catch (err: any) {
      lastError = err
    }
    if (attempt < retries) await new Promise((r) => setTimeout(r, delayMs))
  }
  throw lastError
}
