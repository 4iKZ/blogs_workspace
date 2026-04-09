import axios from '../utils/axios'

export interface LoginRequest {
  username: string
  password: string
  captcha: string
  captchaKey: string
}

export interface RegisterWithEmailCodeRequest {
  username: string
  password: string
  nickname?: string
  email: string
  avatar?: string
  position?: string
  company?: string
  bio?: string
  confirmPassword: string
  emailCode: string
}

export interface SendRegisterCodeRequest {
  email: string
  captcha: string
  captchaKey: string
}

export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

export interface SendResetCodeRequest {
  email: string
}

export interface RefreshTokenResponse {
  token: string
  refreshToken: string
}

export interface CaptchaResponse {
  captchaKey: string
  captchaImage: string
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

export interface GithubCallbackResponse {
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
    return axios.get<CaptchaResponse>('/captcha')
  },

  /**
   * User login with captcha
   */
  login: (data: LoginRequest) =>
    axios.post<LoginResponse>('/user/login', data),

  /**
   * User registration with email verification code
   */
  registerWithEmailCode: (data: RegisterWithEmailCodeRequest) =>
    axios.post('/user/register', data),

  /**
   * Send register email verification code (requires captcha)
   */
  sendRegisterVerifyCode: (data: SendRegisterCodeRequest) =>
    axios.post('/user/register/verify/send', data),

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
  refreshToken: (refreshToken: string) =>
    axios.post<RefreshTokenResponse>(
      '/user/token/refresh',
      { refreshToken }
    ),

  /**
   * User logout
   */
  logout: (refreshToken?: string) =>
    axios.post('/user/logout', null, {
      headers: refreshToken ? { 'X-Refresh-Token': refreshToken } : undefined
    }),

  /**
   * Check if token is valid
   */
  validateToken: () =>
    axios.get<boolean>('/user/token/validate'),

  /**
   * Get GitHub OAuth authorization URL
   */
  getGithubAuthUrl: () => {
    const clientId = import.meta.env.VITE_GITHUB_CLIENT_ID || 'Ov23lidcANzO4LFtikwT'
    const redirectUri = encodeURIComponent('https://luminablog.cn/github/callback')
    const scope = encodeURIComponent('read:user user:email')
    return `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${redirectUri}&scope=${scope}`
  },

  /**
   * Handle GitHub OAuth callback
   */
  githubCallback: (code: string) =>
    axios.get<GithubCallbackResponse>('/user/auth/github/callback', {
      params: { code }
    })
}