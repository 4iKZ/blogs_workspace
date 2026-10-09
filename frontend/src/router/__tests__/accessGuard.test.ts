import { describe, expect, it, vi } from 'vitest'
import { resolveRouteAccess } from '../index'

describe('route access guard', () => {
  it('waits for session initialization before allowing an admin refresh', async () => {
    const store = {
      isLoggedIn: true,
      getRole: 'user',
      initializeSession: vi.fn(async () => {
        store.getRole = 'admin'
      })
    }

    await expect(resolveRouteAccess(
      { name: 'Admin', meta: { requiresAuth: true, requiresAdmin: true } },
      store
    )).resolves.toBe(true)
    expect(store.initializeSession).toHaveBeenCalledTimes(1)
  })

  it('sends logged-out visitors to login with the original target', async () => {
    const store = {
      isLoggedIn: false,
      initializeSession: vi.fn(async () => undefined)
    }

    await expect(resolveRouteAccess(
      { name: 'Profile', fullPath: '/profile?tab=posts', meta: { requiresAuth: true } },
      store
    )).resolves.toEqual({ name: 'Login', query: { redirect: '/profile?tab=posts' } })
  })

  it('redirects an ordinary user away from admin routes', async () => {
    const store = {
      isLoggedIn: true,
      getRole: 'user',
      initializeSession: vi.fn(async () => undefined)
    }

    await expect(resolveRouteAccess(
      { name: 'Admin', meta: { requiresAuth: true, requiresAdmin: true } },
      store
    )).resolves.toEqual({ name: 'Home' })
  })

  it('redirects an anonymous user to login', async () => {
    const store = {
      isLoggedIn: false,
      getRole: undefined,
      initializeSession: vi.fn(async () => undefined)
    }

    await expect(resolveRouteAccess(
      { name: 'Admin', fullPath: '/admin', meta: { requiresAuth: true, requiresAdmin: true } },
      store
    )).resolves.toEqual({ name: 'Login', query: { redirect: '/admin' } })
  })
})
