import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const { toastError } = vi.hoisted(() => ({ toastError: vi.fn() }))

vi.mock('@/composables/useLuminaToast', () => ({
  toast: {
    error: toastError
  }
}))

type ErrorPolicyModule = typeof import('../errorPolicy')

const loadModule = async (): Promise<ErrorPolicyModule> => {
  vi.resetModules()
  return import('../errorPolicy')
}

const BASE_TIME = 1_700_000_000_000

describe('errorPolicy', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(BASE_TIME)
    toastError.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('showThrottledError', () => {
    it('toasts the first message, suppresses repeats within 10s and toasts again after the window', async () => {
      const { showThrottledError } = await loadModule()

      showThrottledError('boom')
      expect(toastError).toHaveBeenCalledTimes(1)
      expect(toastError).toHaveBeenCalledWith('boom')

      vi.advanceTimersByTime(9_999)
      showThrottledError('boom again')
      expect(toastError).toHaveBeenCalledTimes(1)

      vi.advanceTimersByTime(2)
      showThrottledError('boom later')
      expect(toastError).toHaveBeenCalledTimes(2)
      expect(toastError).toHaveBeenLastCalledWith('boom later')
    })

    it('shares one cooldown window across different messages', async () => {
      const { showThrottledError } = await loadModule()

      showThrottledError('first')
      showThrottledError('second')

      expect(toastError).toHaveBeenCalledTimes(1)
      expect(toastError).toHaveBeenCalledWith('first')
    })
  })

  describe('isHtmlPayload', () => {
    it('detects an HTML document payload', async () => {
      const { isHtmlPayload } = await loadModule()

      expect(isHtmlPayload('<!DOCTYPE html><html></html>')).toBe(true)
    })

    it('rejects non HTML payloads', async () => {
      const { isHtmlPayload } = await loadModule()

      expect(isHtmlPayload('<html></html>')).toBe(false)
      expect(isHtmlPayload('{"code":500}')).toBe(false)
      expect(isHtmlPayload(null)).toBe(false)
      expect(isHtmlPayload(undefined)).toBe(false)
      expect(isHtmlPayload({ html: true })).toBe(false)
    })
  })

  describe('createBusinessError', () => {
    it('builds an error carrying the response, config and handled marker', async () => {
      const { createBusinessError } = await loadModule()
      const config = { url: '/api/demo', method: 'post' }

      const error = createBusinessError({ message: '业务失败', code: 500 }, config)

      expect(error).toBeInstanceOf(Error)
      expect(error.message).toBe('业务失败')
      expect(error.response).toEqual({
        data: { message: '业务失败', code: 500 },
        status: 500,
        statusText: '业务失败'
      })
      expect(error.config).toBe(config)
      expect(error._handled).toBe(true)
    })

    it('falls back to default message and statusText', async () => {
      const { createBusinessError } = await loadModule()

      const error = createBusinessError({ code: 500 }, undefined)

      expect(error.message).toBe('请求失败')
      expect(error.response.statusText).toBe('Error')
      expect(error.response.status).toBe(500)
      expect(error._handled).toBe(true)
    })
  })

  describe('applyTransportErrorPolicy', () => {
    it('mutates and handles errors carrying a business message', async () => {
      const { applyTransportErrorPolicy } = await loadModule()
      const error: any = {
        message: 'Request failed with status code 400',
        response: { data: { message: '参数错误' } }
      }

      const handled = applyTransportErrorPolicy(error)

      expect(handled).toBe(true)
      expect(error.message).toBe('参数错误')
      expect(error._handled).toBe(true)
      expect(toastError).toHaveBeenCalledTimes(1)
      expect(toastError).toHaveBeenCalledWith('参数错误')
    })

    it('leaves unhandled errors intact and toasts their message', async () => {
      const { applyTransportErrorPolicy } = await loadModule()
      const error: any = {
        message: 'Network Error',
        response: { data: {} }
      }

      const handled = applyTransportErrorPolicy(error)

      expect(handled).toBe(false)
      expect(error.message).toBe('Network Error')
      expect(error._handled).toBeUndefined()
      expect(toastError).toHaveBeenCalledTimes(1)
      expect(toastError).toHaveBeenCalledWith('Network Error')
    })

    it('toasts the fallback message when the transport error has none', async () => {
      const { applyTransportErrorPolicy } = await loadModule()

      const handled = applyTransportErrorPolicy({})

      expect(handled).toBe(false)
      expect(toastError).toHaveBeenCalledTimes(1)
      expect(toastError).toHaveBeenCalledWith('网络连接失败')
    })
  })
})
