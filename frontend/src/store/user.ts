import { defineStore } from 'pinia'
import type { UserInfo } from '../types/user'
import { authService } from '../services/authService'
import { crossTabRefreshCoordinator } from '../utils/crossTabRefresh'

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
    },

    // 合并更新用户信息
    updateUserInfo(patch: Partial<UserInfo>) {
      if (!this.userInfo) return
      this.userInfo = { ...this.userInfo, ...patch }
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
      this.sessionInitialized = false
      this.sessionInitialization = null
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
      // 不沿用可能过期的缓存角色，isLoggedIn 仅在拿到服务端用户信息后置真。
      // 仅当当前为未登录态时才清空 userInfo，避免覆盖 init 过程中已登录的状态。
      if (!this.isLoggedIn) {
        this.userInfo = null
      }

      try {
        // 先落地新 token，后续 /user/info 才能携带 Authorization，避免刷新触发二次令牌轮换
        this.token = await crossTabRefreshCoordinator.run(async () => {
          const refreshed = await authService.refreshToken()
          return refreshed.token
        })
        const userInfo = await authService.getCurrentUser()
        this.userInfo = userInfo
        this.isLoggedIn = true
        this.sessionInitialized = true
      } catch (error: any) {
        const isAuthFailure = error?.response?.status === 401 || error?.status === 401
        // clearUserInfo 会重置 isLoggedIn，先捕获进入 catch 前的登录态
        const wasLoggedIn = this.isLoggedIn
        // 真 401 即会话已失效，无论此前是否标记为已登录都清除；
        // 未登录态的瞬时错误也清理，避免残留脏数据
        if (isAuthFailure || !wasLoggedIn) {
          this.clearUserInfo()
        }
        // 仅“已登录会话真正失效”才永久锁定，避免反复重试；
        // 匿名无会话或网络抖动不锁定，多标签登录后本标签下次导航可经 refresh 恢复
        this.sessionInitialized = isAuthFailure && wasLoggedIn
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
