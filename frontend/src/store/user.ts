import { defineStore } from 'pinia'
import type { UserInfo } from '../types/user'
import { authService } from '../services/authService'
import { crossTabRefreshCoordinator } from '../utils/crossTabRefresh'

const removeLegacyTokens = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
}

// 仅持久化非敏感的最小字段，避免 email/phone/lastLoginIp 等 PII 落盘
const persistUserInfo = (userInfo: UserInfo) => {
  const minimal = {
    id: userInfo.id,
    username: userInfo.username,
    nickname: userInfo.nickname,
    avatar: userInfo.avatar,
    role: userInfo.role,
    status: userInfo.status
  }
  localStorage.setItem('userInfo', JSON.stringify(minimal))
}

export const useUserStore = defineStore('user', {
  state: () => {
    removeLegacyTokens()
    return {
    userInfo: null as UserInfo | null,
    token: '',
    isLoggedIn: false,
    sessionInitialized: false,
    sessionInitialization: null as Promise<void> | null
    }
  },

  getters: {
    getUserId: (state) => state.userInfo?.id,
    getUsername: (state) => state.userInfo?.username,
    getNickname: (state) => state.userInfo?.nickname,
    getRole: (state) => state.userInfo?.role,
    getAvatar: (state) => state.userInfo?.avatar
  },

  actions: {
    // 设置用户信息
    setUserInfo(userInfo: UserInfo) {
      this.userInfo = userInfo
      persistUserInfo(userInfo)
    },

    // 合并更新用户信息（内存保留全量，仅最小字段持久化）
    updateUserInfo(patch: Partial<UserInfo>) {
      if (!this.userInfo) return
      this.userInfo = { ...this.userInfo, ...patch }
      persistUserInfo(this.userInfo)
    },

    // 设置token
    setToken(token: string) {
      this.token = token
      this.isLoggedIn = true
    },

    // 清除用户信息
    clearUserInfo() {
      this.userInfo = null
      this.token = ''
      this.isLoggedIn = false
      localStorage.removeItem('userInfo')
      removeLegacyTokens()
    },

    initializeSession() {
      if (this.sessionInitialization) {
        return this.sessionInitialization
      }
      if (this.sessionInitialized) {
        return Promise.resolve()
      }

      this.sessionInitialization = this.performSessionInitialization()
      return this.sessionInitialization
    },

    async performSessionInitialization() {
      // 不沿用可能过期的缓存角色，isLoggedIn 仅在拿到服务端用户信息后置真
      this.userInfo = null

      try {
        const token = await crossTabRefreshCoordinator.run(async () => {
          const refreshed = await authService.refreshToken()
          return refreshed.token
        })
        const userInfo = await authService.getCurrentUser()
        this.token = token
        this.userInfo = userInfo
        this.isLoggedIn = true
        persistUserInfo(userInfo)
        this.sessionInitialized = true
      } catch (error: any) {
        this.clearUserInfo()
        // 明确的认证失败（如 refresh token 无效）视为会话结束；
        // 网络抖动等瞬时错误不锁定会话，下次 initializeSession 可重试
        this.sessionInitialized = error?.response?.status === 401 || error?.status === 401
      } finally {
        this.sessionInitialization = null
      }
    },

    // 退出登录
    async logout() {
      try {
        // 调用后端登出接口
        await authService.logout()
      } catch (error) {
        console.error('Logout API call failed:', error)
        // 即使API调用失败，也清除本地数据
      } finally {
        // 清除本地用户信息
        this.clearUserInfo()
      }
    }
  }
})
