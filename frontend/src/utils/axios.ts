import axios, { type AxiosInstance } from 'axios'
import { toast } from '@/composables/useLuminaToast'
import { useUserStore } from '@/store/user'
import router from '@/router'

interface RetryableRequestConfig {
  _retry?: boolean
}

// 定义自定义 Axios 实例类型，返回解包后的数据
interface CustomAxiosInstance extends AxiosInstance {
  get<T = any>(url: string, config?: any): Promise<T>
  delete<T = any>(url: string, config?: any): Promise<T>
  head<T = any>(url: string, config?: any): Promise<T>
  options<T = any>(url: string, config?: any): Promise<T>
  post<T = any>(url: string, data?: any, config?: any): Promise<T>
  put<T = any>(url: string, data?: any, config?: any): Promise<T>
  patch<T = any>(url: string, data?: any, config?: any): Promise<T>
}

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 后端API基础URL
  timeout: 10000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
}) as CustomAxiosInstance

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 在发送请求之前，从 Pinia store 中获取 token
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    // 请求错误处理
    console.error('请求错误:', error)
    toast.error(error.message || '请求发送失败')
    return Promise.reject(error)
  }
)

// 响应拦截器
let lastErrorToast = 0
const TOAST_COOLDOWN_MS = 10000
let isRefreshing = false
let refreshSubscribers: Array<(token: string) => void> = []

const isAuthEndpoint = (url?: string) => {
  if (!url) {
    return false
  }
  return (
    url.includes('/user/login') ||
    url.includes('/user/register') ||
    url.includes('/user/token/refresh') ||
    url.includes('/user/token/validate') ||
    url.includes('/user/logout')
  )
}

const subscribeTokenRefresh = (cb: (token: string) => void) => {
  refreshSubscribers.push(cb)
}

const notifyTokenRefreshed = (newToken: string) => {
  refreshSubscribers.forEach((cb) => cb(newToken))
  refreshSubscribers = []
}

const handleAuthExpired = () => {
  const userStore = useUserStore()
  userStore.clearUserInfo()
  router.push({ name: 'Login' })
}

const requestNewAccessToken = async (): Promise<string> => {
  const userStore = useUserStore()
  const refreshToken = userStore.refreshToken
  if (!refreshToken) {
    throw new Error('Missing refresh token')
  }

  // Use raw axios to avoid interceptor recursion.
  const response = await axios.post('/api/user/token/refresh', { refreshToken })
  const payload = response.data
  if (!payload || payload.code !== 200 || !payload.data?.token || !payload.data?.refreshToken) {
    throw new Error(payload?.message || 'Refresh token failed')
  }

  const newAccessToken = payload.data.token as string
  const newRefreshToken = payload.data.refreshToken as string
  userStore.setTokens(newAccessToken, newRefreshToken)
  return newAccessToken
}

const tryRefreshAndRetry = async (originalRequest: any) => {
  // 创建一个带有 401 标记的错误，便于调用方识别
  const createAuthError = (message: string) => {
    const error = new Error(message) as any
    error.response = { status: 401 }
    return error
  }

  if (!originalRequest) {
    handleAuthExpired()
    throw createAuthError('Unauthorized')
  }

  const requestConfig = originalRequest as RetryableRequestConfig

  if (requestConfig._retry || isAuthEndpoint(originalRequest?.url)) {
    handleAuthExpired()
    throw createAuthError('Unauthorized')
  }

  requestConfig._retry = true

  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      subscribeTokenRefresh((newToken: string) => {
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
        }
        service(originalRequest).then(resolve).catch(reject)
      })
    })
  }

  isRefreshing = true
  try {
    const newToken = await requestNewAccessToken()
    notifyTokenRefreshed(newToken)
    if (originalRequest.headers) {
      originalRequest.headers.Authorization = `Bearer ${newToken}`
    }
    return service(originalRequest)
  } catch (refreshError) {
    handleAuthExpired()
    throw createAuthError('Unauthorized')
  } finally {
    isRefreshing = false
  }
}

service.interceptors.response.use(
  (response) => {
    const res = response.data

    // 检测是否是 HTML 响应（后端返回错误页面时可能出现）
    if (typeof res === 'string' && res.startsWith('<!DOCTYPE html>')) {
      console.error('API 返回了 HTML 错误页面:', res.substring(0, 200))
      toast.error('服务暂时不可用，请稍后重试')
      const error = new Error('后端返回了 HTML 页面') as any
      error.response = response
      return Promise.reject(error)
    }

    // 业务错误 (code !== 200)
    if (res.code !== 200) {
      console.warn('响应错误:', res.message, {
        url: response.config?.url,
        method: response.config?.method
      })

      // 401: 未认证/Token过期 - 不显示通知，直接跳转
      if (res.code === 401) {
        return tryRefreshAndRetry(response.config)
      } else {
        const now = Date.now()
        if (now - lastErrorToast > TOAST_COOLDOWN_MS) {
          toast.error(res.message || '系统异常')
          lastErrorToast = now
        }
      }

      // 创建带有 response 属性的错误对象，保持与网络错误一致的格式
      const error = new Error(res.message || '请求失败') as any
      error.response = {
        data: res,
        status: res.code,
        statusText: res.message || 'Error'
      }
      error.config = response.config
      return Promise.reject(error)
    }

    // 成功，直接返回解包后的 data
    return res.data
  },
  (error) => {
    // 网络错误或其它 Axios 错误
    if (error?.code === 'ERR_CANCELED') {
      return Promise.reject(error)
    }

    const status = error.response?.status

    // 如果是 401 错误，同样清除用户信息并跳转到登录页 - 不显示通知
    if (status === 401) {
      return tryRefreshAndRetry(error.config)
    }

    // 如果是 403 错误，也跳转到登录页（可能是未认证被拒绝）
    if (status === 403) {
      router.push({ name: 'Login' })
      return Promise.reject(error)
    }

    console.warn('网络错误:', error.message, {
      status,
      url: error.config?.url,
      method: error.config?.method
    })

    const now = Date.now()
    if (now - lastErrorToast > TOAST_COOLDOWN_MS) {
      toast.error(error.message || '网络连接失败')
      lastErrorToast = now
    }
    return Promise.reject(error)
  }
)

export default service
