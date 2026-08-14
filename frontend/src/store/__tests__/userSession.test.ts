import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import type { UserInfo } from '../../types/user'

const { getCurrentUser, refreshToken } = vi.hoisted(() => ({
  getCurrentUser: vi.fn<() => Promise<UserInfo>>(),
  refreshToken: vi.fn<() => Promise<{ token: string }>>()
}))

vi.mock('../../services/authService', () => ({
  authService: {
    getCurrentUser,
    refreshToken,
    logout: vi.fn()
  }
}))

import { useUserStore } from '../user'

const cachedUser = (role = 'user'): UserInfo => ({
  id: 7,
  username: 'tester',
  email: 'cached@example.com',
  phone: null,
  nickname: 'Tester',
  avatar: null,
  bio: null,
  website: null,
  position: null,
  company: null,
  status: 1,
  role,
  createTime: '2026-01-01T00:00:00',
  lastLoginTime: null,
  lastLoginIp: null,
  articleCount: 0,
  commentCount: 0
})

describe('user session initialization', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    getCurrentUser.mockReset()
    getCurrentUser.mockResolvedValue(cachedUser())
    refreshToken.mockReset()
    refreshToken.mockResolvedValue({ token: 'fresh-access-token' })
  })

  it('shares one request and refreshes a cached role from the server', async () => {
    localStorage.setItem('token', 'legacy-access-token')
    localStorage.setItem('refreshToken', 'legacy-refresh-token')
    localStorage.setItem('userInfo', JSON.stringify(cachedUser('user')))
    getCurrentUser.mockResolvedValue(cachedUser('admin'))
    const store = useUserStore()

    const first = store.initializeSession()
    const second = store.initializeSession()

    await Promise.all([first, second])
    expect(getCurrentUser).toHaveBeenCalledTimes(1)
    expect(store.getRole).toBe('admin')
    expect(store.token).toBe('fresh-access-token')
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('recovers from corrupt cached JSON through cookie refresh', async () => {
    localStorage.setItem('userInfo', '{broken')
    const store = useUserStore()

    await expect(store.initializeSession()).resolves.toBeUndefined()

    expect(store.userInfo?.id).toBe(7)
    expect(getCurrentUser).toHaveBeenCalled()
    expect(localStorage.getItem('userInfo')).not.toBeNull()
  })

  it('clears the login state when the profile endpoint returns 401', async () => {
    localStorage.setItem('token', 'expired-token')
    localStorage.setItem('refreshToken', 'expired-refresh')
    localStorage.setItem('userInfo', JSON.stringify(cachedUser()))
    refreshToken.mockRejectedValue({ response: { status: 401 } })
    const store = useUserStore()

    await store.initializeSession()

    expect(store.isLoggedIn).toBe(false)
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('falls back to anonymous while cookie refresh is unavailable', async () => {
    localStorage.setItem('userInfo', JSON.stringify(cachedUser('admin')))
    refreshToken.mockRejectedValue(new Error('offline'))
    const store = useUserStore()

    await store.initializeSession()

    expect(store.isLoggedIn).toBe(false)
    expect(store.userInfo).toBeNull()
  })

  it('does not permanently lock session restore after a transient failure', async () => {
    refreshToken
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce({ token: 'fresh-access-token' })
    const store = useUserStore()

    await store.initializeSession()
    expect(store.sessionInitialized).toBe(false)
    expect(store.isLoggedIn).toBe(false)

    await store.initializeSession()
    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('fresh-access-token')
  })

  it('persists only non-sensitive minimal fields to localStorage', async () => {
    const store = useUserStore()
    await store.initializeSession()

    const stored = JSON.parse(localStorage.getItem('userInfo') || '{}')
    expect(stored).not.toHaveProperty('email')
    expect(stored).not.toHaveProperty('phone')
    expect(stored).not.toHaveProperty('lastLoginIp')
    expect(stored.id).toBe(7)
    expect(stored.role).toBe('user')
  })
})
