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
  // 传输层兜底：不透出 axios 原始英文文案；标记 _handled 由本处统一负责，
  // 业务层 !error._handled 时不再二次弹 toast
  error._handled = true
  showThrottledError('网络连接失败，请稍后重试')
  return false
}
