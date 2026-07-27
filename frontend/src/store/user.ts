import { defineStore } from 'pinia'
import type { UserInfo } from '../types/user'
import { authService } from '../services/authService'

const removeLegacyTokens = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
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
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
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

      try {
        const refreshed = await authService.refreshToken()
        this.setToken(refreshed.token)
        this.setUserInfo(await authService.getCurrentUser())
      } catch {
        this.clearUserInfo()
      } finally {
        this.sessionInitialized = true
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
