import axios, { type AxiosInstance } from 'axios'
import { toast } from '@/composables/useLuminaToast'
import { useUserStore } from '@/store/user'
import router from '@/router'

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
        const userStore = useUserStore()
        userStore.clearUserInfo()
        router.push({ name: 'Login' })
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
      const userStore = useUserStore()
      userStore.clearUserInfo()
      router.push({ name: 'Login' })
      return Promise.reject(error)
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
