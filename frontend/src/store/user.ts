import { defineStore } from 'pinia'
import type { UserInfo } from '../types/user'
import { authService } from '../services/authService'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null as UserInfo | null,
    token: localStorage.getItem('token') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    isLoggedIn: !!localStorage.getItem('token')
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

    // 初始化用户信息
    initUserInfo() {
      const userInfoStr = localStorage.getItem('userInfo')
      if (userInfoStr) {
        this.userInfo = JSON.parse(userInfoStr)
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