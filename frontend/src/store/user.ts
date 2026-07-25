import { defineStore } from 'pinia'
import type { UserInfo } from '../types/user'
import { authService } from '../services/authService'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null as UserInfo | null,
    token: localStorage.getItem('token') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    isLoggedIn: !!localStorage.getItem('token'),
    sessionInitialized: false,
    sessionInitialization: null as Promise<void> | null
  }),

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
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },

    // 设置token
    setToken(token: string) {
      this.token = token
      this.isLoggedIn = true
      localStorage.setItem('token', token)
    },

    setRefreshToken(refreshToken: string) {
      this.refreshToken = refreshToken
      localStorage.setItem('refreshToken', refreshToken)
    },

    // 同时设置双token
    setTokens(accessToken: string, refreshToken: string) {
      this.setToken(accessToken)
      this.setRefreshToken(refreshToken)
    },

    // 清除用户信息
    clearUserInfo() {
      this.userInfo = null
      this.token = ''
      this.refreshToken = ''
      this.isLoggedIn = false
      localStorage.removeItem('userInfo')
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    },

    restoreCachedUserInfo() {
      const userInfoStr = localStorage.getItem('userInfo')
      if (!userInfoStr) {
        return
      }
      try {
        this.userInfo = JSON.parse(userInfoStr) as UserInfo
      } catch {
        this.userInfo = null
        localStorage.removeItem('userInfo')
      }
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
      this.restoreCachedUserInfo()

      if (!this.token) {
        this.sessionInitialized = true
        this.sessionInitialization = null
        return
      }

      try {
        this.setUserInfo(await authService.getCurrentUser())
      } catch (error: unknown) {
        const status = (error as { response?: { status?: number } }).response?.status
        if (status === 401) {
          this.clearUserInfo()
        }
      } finally {
        this.sessionInitialized = true
        this.sessionInitialization = null
      }
    },

    // 退出登录
    async logout() {
      try {
        // 调用后端登出接口
        await authService.logout(this.refreshToken || undefined)
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
