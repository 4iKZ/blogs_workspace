import axios, {
  AxiosError,
  type AxiosAdapter,
  type AxiosResponse
} from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const { routerPush } = vi.hoisted(() => ({ routerPush: vi.fn() }))

vi.mock('@/router', () => ({
  default: { push: routerPush }
}))

const { toastError } = vi.hoisted(() => ({ toastError: vi.fn() }))

vi.mock('@/composables/useLuminaToast', () => ({
  toast: {
    error: toastError,
    warning: vi.fn(),
    success: vi.fn()
  }
}))

import service from '../axios'
import { useUserStore } from '../../store/user'

const unauthorized = (config: any): AxiosError => {
  const response: AxiosResponse = {
    data: {},
    status: 401,
    statusText: 'Unauthorized',
    headers: {},
    config
  }
  return new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, response)
}

const forbidden = (config: any): AxiosError => {
  const response: AxiosResponse = {
    data: { code: 403, message: '没有权限删除此评论' },
    status: 403,
    statusText: 'Forbidden',
    headers: {},
    config
  }
  return new AxiosError(
    'Request failed with status code 403',
    'ERR_BAD_REQUEST',
    config,
    undefined,
    response
  )
}

const businessFailure = (config: any): AxiosResponse => ({
  data: { code: 500, message: '系统异常' },
  status: 200,
  statusText: 'OK',
  headers: {},
  config
})

const createRefreshAdapter = (onRefresh: () => void): AxiosAdapter => {
  return async (config) => {
    onRefresh()
    return {
      data: { code: 200, data: { token: 'new-token' } },
      status: 200,
      statusText: 'OK',
      headers: {},
      config
    }
  }
}

describe('write requests with token refresh', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    toastError.mockReset()
  })

  it('refreshes once and replays a POST after a 401', async () => {
    const store = useUserStore()
    store.setToken('old-token')
    let attempts = 0
    let refreshCalls = 0

    const refreshAdapter = createRefreshAdapter(() => {
      refreshCalls += 1
    })
    const postAdapter: AxiosAdapter = async (config) => {
      attempts += 1
      if (attempts === 1) {
        throw unauthorized(config)
      }
      return {
        data: { code: 200, data: { ok: true } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      }
    }

    const originalRefreshAdapter = axios.defaults.adapter
    const originalPostAdapter = service.defaults.adapter
    axios.defaults.adapter = refreshAdapter
    service.defaults.adapter = postAdapter
    try {
      const result = await service.post('/articles/1/like')

      expect(result).toEqual({ ok: true })
      expect(attempts).toBe(2)
      expect(refreshCalls).toBe(1)
      expect(store.token).toBe('new-token')
      expect(routerPush).not.toHaveBeenCalled()
    } finally {
      axios.defaults.adapter = originalRefreshAdapter
      service.defaults.adapter = originalPostAdapter
    }
  })

  it('logs out when the replayed POST still returns 401', async () => {
    const store = useUserStore()
    store.setToken('old-token')
    const clearSpy = vi.spyOn(store, 'clearUserInfo')
    let attempts = 0
    let refreshCalls = 0

    const refreshAdapter = createRefreshAdapter(() => {
      refreshCalls += 1
    })
    const postAdapter: AxiosAdapter = async (config) => {
      attempts += 1
      throw unauthorized(config)
    }

    const originalRefreshAdapter = axios.defaults.adapter
    const originalPostAdapter = service.defaults.adapter
    axios.defaults.adapter = refreshAdapter
    service.defaults.adapter = postAdapter
    try {
      await expect(service.post('/comments/1')).rejects.toBeTruthy()

      expect(attempts).toBe(2)
      expect(refreshCalls).toBe(1)
      expect(clearSpy).toHaveBeenCalledTimes(1)
      expect(store.token).toBe('')
      expect(routerPush).toHaveBeenCalledWith({ name: 'Login' })
    } finally {
      axios.defaults.adapter = originalRefreshAdapter
      service.defaults.adapter = originalPostAdapter
    }
  })

  it('does not log out on 403 and surfaces the server message', async () => {
    const store = useUserStore()
    store.setToken('old-token')
    const clearSpy = vi.spyOn(store, 'clearUserInfo')

    const deleteAdapter: AxiosAdapter = async (config) => {
      throw forbidden(config)
    }

    const originalDeleteAdapter = service.defaults.adapter
    service.defaults.adapter = deleteAdapter
    try {
      await expect(service.delete('/comments/1')).rejects.toBeTruthy()

      expect(clearSpy).not.toHaveBeenCalled()
      expect(store.token).toBe('old-token')
      expect(routerPush).not.toHaveBeenCalled()
      expect(toastError).toHaveBeenCalledWith('没有权限删除此评论')
    } finally {
      service.defaults.adapter = originalDeleteAdapter
    }
  })

  it('keeps the GET retry behaviour after a 401', async () => {
    const store = useUserStore()
    store.setToken('old-token')
    let attempts = 0
    let refreshCalls = 0

    const refreshAdapter = createRefreshAdapter(() => {
      refreshCalls += 1
    })
    const getAdapter: AxiosAdapter = async (config) => {
      attempts += 1
      if (attempts === 1) {
        throw unauthorized(config)
      }
      return {
        data: { code: 200, data: { id: 1 } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      }
    }

    const originalRefreshAdapter = axios.defaults.adapter
    const originalGetAdapter = service.defaults.adapter
    axios.defaults.adapter = refreshAdapter
    service.defaults.adapter = getAdapter
    try {
      const result = await service.get('/comments/1')

      expect(result).toEqual({ id: 1 })
      expect(attempts).toBe(2)
      expect(refreshCalls).toBe(1)
      expect(store.token).toBe('new-token')
      expect(routerPush).not.toHaveBeenCalled()
    } finally {
      axios.defaults.adapter = originalRefreshAdapter
      service.defaults.adapter = originalGetAdapter
    }
  })
})

// showThrottledError 的 10 秒 toast 节流依赖 Date.now()：
// 用假时钟并在每个用例推进时间，避免用例之间互相节流
let toastClock = 1_800_000_000_000

describe('silent background requests (_silent)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    toastError.mockReset()
    vi.useFakeTimers({ toFake: ['Date'] })
    toastClock += 60_000
    vi.setSystemTime(toastClock)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('stays quiet and marks handled on silent transport errors (403)', async () => {
    const deleteAdapter: AxiosAdapter = async (config) => {
      throw forbidden(config)
    }
    const originalAdapter = service.defaults.adapter
    service.defaults.adapter = deleteAdapter
    try {
      const error: any = await service
        .post('/statistics/article/view/1', undefined, { _silent: true })
        .catch((e) => e)

      expect(error._handled).toBe(true)
      expect(error.message).toBe('没有权限删除此评论')
      expect(toastError).not.toHaveBeenCalled()
    } finally {
      service.defaults.adapter = originalAdapter
    }
  })

  it('stays quiet and marks handled on silent network failures', async () => {
    const networkAdapter: AxiosAdapter = async (config) => {
      throw new AxiosError('Network Error', 'ERR_NETWORK', config)
    }
    const originalAdapter = service.defaults.adapter
    service.defaults.adapter = networkAdapter
    try {
      const error: any = await service
        .post('/statistics/article/view/1', undefined, { _silent: true })
        .catch((e) => e)

      expect(error._handled).toBe(true)
      expect(toastError).not.toHaveBeenCalled()
    } finally {
      service.defaults.adapter = originalAdapter
    }
  })

  it('stays quiet on silent business errors (code !== 200) but still rejects handled', async () => {
    const businessAdapter: AxiosAdapter = async (config) =>
      businessFailure(config)
    const originalAdapter = service.defaults.adapter
    service.defaults.adapter = businessAdapter
    try {
      const error: any = await service
        .post('/statistics/article/view/1', undefined, { _silent: true })
        .catch((e) => e)

      expect(error._handled).toBe(true)
      expect(error.message).toBe('系统异常')
      expect(toastError).not.toHaveBeenCalled()
    } finally {
      service.defaults.adapter = originalAdapter
    }
  })

  it('still toasts transport errors for non-silent requests', async () => {
    const networkAdapter: AxiosAdapter = async (config) => {
      throw new AxiosError('Network Error', 'ERR_NETWORK', config)
    }
    const originalAdapter = service.defaults.adapter
    service.defaults.adapter = networkAdapter
    try {
      await expect(service.post('/comments/1')).rejects.toBeTruthy()

      expect(toastError).toHaveBeenCalledWith('网络连接失败，请稍后重试')
    } finally {
      service.defaults.adapter = originalAdapter
    }
  })

  it('still toasts business errors for non-silent requests', async () => {
    const businessAdapter: AxiosAdapter = async (config) =>
      businessFailure(config)
    const originalAdapter = service.defaults.adapter
    service.defaults.adapter = businessAdapter
    try {
      const error: any = await service.post('/comments/1').catch((e) => e)

      expect(error._handled).toBe(true)
      expect(toastError).toHaveBeenCalledWith('系统异常')
    } finally {
      service.defaults.adapter = originalAdapter
    }
  })
})