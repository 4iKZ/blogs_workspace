import axios from '../utils/axios'

export interface LoginRequest {
  username: string
  password: string
  captcha: string
  captchaKey: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  avatar?: string
  position?: string
  company?: string
  bio?: string
  captcha: string
  captchaKey: string
  confirmPassword: string // 后端也需要验证该字段
}

export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

export interface SendResetCodeRequest {
  email: string
}

export interface CaptchaResponse {
  captchaKey: string
  captchaImage: string // base64 encoded image
}

export interface LoginResponse {
  id: number
  username: string
  email: string
  phone?: string
  nickname: string
  avatar?: string
  bio?: string
  website?: string
  status: number
  role: string
  createTime: string
  lastLoginTime?: string
  lastLoginIp?: string
  articleCount?: number
  commentCount?: number
  accessToken: string
  refreshToken: string
}

export const authService = {
  /**
   * Get captcha image for login/register
   */
  getCaptcha: async (): Promise<CaptchaResponse> => {
    // 响应拦截器已经解包，直接返回结果
    return axios.get<CaptchaResponse>('/captcha')
  },

  /**
   * User login with captcha
   */
  login: (data: LoginRequest) =>
    axios.post<LoginResponse>('/user/login', data),

  /**
   * User registration with captcha
   */
  register: (data: RegisterRequest) =>
    axios.post('/user/register', data),

  /**
   * Send password reset code to email
   */
  sendResetCode: (data: SendResetCodeRequest) =>
    axios.post('/user/password/reset/send', data),

  /**
   * Reset password using email code
   */
  resetPassword: (data: ResetPasswordRequest) =>
    axios.post('/user/password/reset', data),

  /**
   * Refresh access token
   */
  refreshToken: () =>
    axios.post<{ token: string }>('/user/token/refresh'),

  /**
   * User logout
   */
  logout: () =>
    axios.post('/user/logout'),

  /**
   * Check if token is valid
   */
  validateToken: () =>
    axios.get<boolean>('/user/token/validate')
}
