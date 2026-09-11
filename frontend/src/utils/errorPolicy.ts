import { toast } from '@/composables/useLuminaToast'

const TOAST_COOLDOWN_MS = 10000

let lastErrorToast = 0

export const showThrottledError = (message: string): void => {
  const now = Date.now()
  if (now - lastErrorToast > TOAST_COOLDOWN_MS) {
    toast.error(message)
    lastErrorToast = now
  }
}

export const isHtmlPayload = (payload: unknown): payload is string => {
  return typeof payload === 'string' && payload.startsWith('<!DOCTYPE html>')
}

interface BusinessErrorPayload {
  message?: string
  code?: unknown
}

type BusinessError = Error & {
  response: { data: unknown; status: unknown; statusText: string }
  config: unknown
  _handled: true
}

export const createBusinessError = (
  payload: BusinessErrorPayload,
  config: unknown
): BusinessError => {
  const error = new Error(payload.message || '请求失败') as BusinessError
  error.response = {
    data: payload,
    status: payload.code,
    statusText: payload.message || 'Error'
  }
  error.config = config
  error._handled = true
  return error
}

export const applyTransportErrorPolicy = (error: any): boolean => {
  const businessMessage = error?.response?.data?.message
  if (businessMessage) {
    error.message = businessMessage
    error._handled = true
    showThrottledError(businessMessage)
    return true
  }
  showThrottledError(error?.message || '网络连接失败')
  return false
}
