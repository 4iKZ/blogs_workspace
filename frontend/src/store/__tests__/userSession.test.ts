import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import type { UserInfo } from '../../types/user'

const { getCurrentUser } = vi.hoisted(() => ({
  getCurrentUser: vi.fn<() => Promise<UserInfo>>()
}))

vi.mock('../../services/authService', () => ({
  authService: {
    getCurrentUser,
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
    setActivePinia(createPinia())
    getCurrentUser.mockReset()
  })

  it('shares one request and refreshes a cached role from the server', async () => {
    localStorage.setItem('token', 'access-token')
    localStorage.setItem('userInfo', JSON.stringify(cachedUser('user')))
    getCurrentUser.mockResolvedValue(cachedUser('admin'))
    const store = useUserStore()

    const first = store.initializeSession()
    const second = store.initializeSession()

    await Promise.all([first, second])
    expect(getCurrentUser).toHaveBeenCalledTimes(1)
    expect(store.getRole).toBe('admin')
  })

  it('does not block startup when cached JSON is corrupt or no token exists', async () => {
    localStorage.setItem('userInfo', '{broken')
    const store = useUserStore()

    await expect(store.initializeSession()).resolves.toBeUndefined()

    expect(store.userInfo).toBeNull()
    expect(getCurrentUser).not.toHaveBeenCalled()
    expect(localStorage.getItem('userInfo')).toBeNull()
  })

  it('clears the login state when the profile endpoint returns 401', async () => {
    localStorage.setItem('token', 'expired-token')
    localStorage.setItem('refreshToken', 'expired-refresh')
    localStorage.setItem('userInfo', JSON.stringify(cachedUser()))
    getCurrentUser.mockRejectedValue({ response: { status: 401 } })
    const store = useUserStore()

    await store.initializeSession()

    expect(store.isLoggedIn).toBe(false)
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('keeps a valid cached profile on an ordinary network error', async () => {
    localStorage.setItem('token', 'access-token')
    localStorage.setItem('userInfo', JSON.stringify(cachedUser('admin')))
    getCurrentUser.mockRejectedValue(new Error('offline'))
    const store = useUserStore()

    await store.initializeSession()

    expect(store.isLoggedIn).toBe(true)
    expect(store.getRole).toBe('admin')
  })
})
