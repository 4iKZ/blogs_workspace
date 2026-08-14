const isRetryableError = (err: any): boolean => {
  if (err?.code === 'ERR_CANCELED') return false
  const status = err?.response?.status
  if (status !== undefined) {
    // 4xx 不重试（参数/鉴权错误重试无意义），5xx 与网络错误可重试
    return status >= 500
  }
  return true
}

export async function withRetry<T>(
  task: () => Promise<T>,
  validate: (data: T) => boolean,
  retries = 3,
  delayMs = 1000,
  onAttempt?: (n: number) => void
): Promise<T> {
  let lastError: any = new Error('Retry failed')
  for (let attempt = 1; attempt <= retries; attempt++) {
    if (onAttempt) onAttempt(attempt)
    try {
      const result = await task()
      if (validate(result)) return result
      lastError = new Error('Invalid data')
    } catch (err: any) {
      lastError = err
      // 非可重试错误直接抛出
      if (!isRetryableError(err)) throw err
    }
    if (attempt < retries) {
      // 指数退避：delay * 2^(attempt-1)
      const backoff = delayMs * Math.pow(2, attempt - 1)
      await new Promise((r) => setTimeout(r, backoff))
    }
  }
  throw lastError
}
