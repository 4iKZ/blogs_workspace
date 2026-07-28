import axios from '../utils/axios'
import type { UserInfo } from '../types/user'

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
  captcha: string
  captchaKey: string
}

export interface RefreshTokenResponse {
  token: string
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
  refreshToken: () =>
    axios.post<RefreshTokenResponse>('/user/token/refresh'),

  /**
   * User logout
   */
  logout: () => axios.post('/user/logout'),

  /**
   * Check if token is valid
   */
  validateToken: () =>
    axios.get<boolean>('/user/token/validate'),

  /**
   * Refresh the current user's profile and authoritative role.
   */
  getCurrentUser: () =>
    axios.get<UserInfo>('/user/info'),

  /**
   * Get GitHub OAuth authorization URL
   * @param state OAuth state parameter for CSRF protection
   */
  getGithubAuthUrl: (state?: string) => {
    const clientId = import.meta.env.VITE_GITHUB_CLIENT_ID || 'Ov23lidcANzO4LFtikwT'
    const redirectUri = encodeURIComponent(
      import.meta.env.VITE_GITHUB_CALLBACK_URL || 'https://luminablog.cn/github/callback'
    )
    const scope = encodeURIComponent('read:user user:email')
    let url = `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${redirectUri}&scope=${scope}`
    if (state) {
      url += `&state=${state}`
    }
    return url
  },

  /**
   * Handle GitHub OAuth callback
   * @param code Authorization code from GitHub
   * @param state Optional state parameter for CSRF verification
   */
  githubCallback: (code: string, state?: string) =>
    axios.get<GithubCallbackResponse>('/user/auth/github/callback', {
      params: { code, ...(state && { state }) }
    }),

  /**
   * Generate GitHub OAuth state for CSRF protection
   */
  generateGithubState: () =>
    axios.post<string>('/user/auth/github/state')
}
