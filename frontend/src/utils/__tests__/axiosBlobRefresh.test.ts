import axios, {
  AxiosError,
  type AxiosAdapter,
  type AxiosResponse
} from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const { routerPush } = vi.hoisted(() => ({ routerPush: vi.fn() }))

vi.mock('@/router', () => ({
  default: { push: routerPush }
}))

vi.mock('@/composables/useLuminaToast', () => ({
  toast: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  }
}))

import service from '../axios'
import { useUserStore } from '../../store/user'

describe('blob responses with token refresh', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
  })

  it('retries a blob download after a 401 and returns the raw response', async () => {
    const store = useUserStore()
    store.setToken('old-token')
    let downloadAttempts = 0
    let refreshTimeout: number | undefined

    const refreshAdapter: AxiosAdapter = async (config) => {
      refreshTimeout = config.timeout
      return {
        data: {
          code: 200,
          data: { token: 'new-token' }
        },
        status: 200,
        statusText: 'OK',
        headers: {},
        config
      }
    }
    const downloadAdapter: AxiosAdapter = async (config) => {
      downloadAttempts += 1
      if (downloadAttempts === 1) {
        const response: AxiosResponse = {
          data: {},
          status: 401,
          statusText: 'Unauthorized',
          headers: {},
          config
        }
        throw new AxiosError(
          'Unauthorized',
          'ERR_BAD_REQUEST',
          config,
          undefined,
          response
        )
      }
      return {
        data: new Blob(['backup']),
        status: 200,
        statusText: 'OK',
        headers: {
          'content-disposition': "attachment; filename*=UTF-8''backup.sql"
        },
        config
      }
    }

    const originalRefreshAdapter = axios.defaults.adapter
    const originalDownloadAdapter = service.defaults.adapter
    axios.defaults.adapter = refreshAdapter
    service.defaults.adapter = downloadAdapter
    try {
      const response = await service.get<AxiosResponse<Blob>>('/download', {
        responseType: 'blob'
      })

      expect(response.status).toBe(200)
      expect(response.data).toBeInstanceOf(Blob)
      expect(downloadAttempts).toBe(2)
      expect(refreshTimeout).toBe(15000)
      expect(store.token).toBe('new-token')
      expect(localStorage.getItem('token')).toBeNull()
      expect(routerPush).not.toHaveBeenCalled()
    } finally {
      axios.defaults.adapter = originalRefreshAdapter
      service.defaults.adapter = originalDownloadAdapter
    }
  })
})
