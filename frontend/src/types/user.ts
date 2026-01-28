export interface UserInfo {
  id: number
  username: string
  email: string
  phone: string | null
  nickname: string
  avatar: string | null
  bio: string | null
  website: string | null
  position: string | null
  company: string | null
  status: number
  role: string
  createTime: string
  lastLoginTime: string | null
  lastLoginIp: string | null
  articleCount: number
  commentCount: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  confirmPassword: string
  nickname?: string
  captcha: string
  captchaKey: string
  position?: string
  company?: string
  bio?: string
}

export interface UpdateUserInfoRequest {
  nickname?: string
  email?: string
  avatar?: string
  bio?: string
  website?: string
  position?: string
  company?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}