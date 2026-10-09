import { describe, expect, it } from 'vitest'
import router from '../index'

describe('site shell routes', () => {
  it('keeps login and other standalone pages outside the shell', () => {
    for (const path of ['/login', '/register', '/reset-password', '/github/callback', '/article/create', '/article/edit/1']) {
      const matched = router.resolve(path).matched
      expect(matched, path).toHaveLength(1)
    }
  })

  it('renders shell pages as children of the layout route', () => {
    for (const path of ['/', '/article/3', '/category', '/search', '/notifications']) {
      const matched = router.resolve(path).matched
      expect(matched[0].path, path).toBe('/')
      expect(matched.length, path).toBeGreaterThan(1)
    }
  })

  it('prefers the static create route over the article id route', () => {
    expect(router.resolve('/article/create').name).toBe('ArticleCreate')
    expect(router.resolve('/article/3').name).toBe('ArticleDetail')
  })

  it('hides the left sidebar on profile and 404 pages only', () => {
    expect(router.resolve('/profile').meta.leftSidebar).toBe(false)
    expect(router.resolve('/profile/followers').meta.leftSidebar).toBe(false)
    expect(router.resolve('/user/5').meta.leftSidebar).toBe(false)
    expect(router.resolve('/category/2').meta.leftSidebar).toBeUndefined()
    expect(router.resolve('/no/such/page').name).toBe('NotFound')
    expect(router.resolve('/no/such/page').meta.leftSidebar).toBe(false)
  })

  it('renders admin pages inside their own shell with admin-only meta', () => {
    for (const path of ['/admin', '/admin/users', '/admin/settings']) {
      const matched = router.resolve(path).matched
      expect(matched[0].path, path).toBe('/admin')
      expect(router.resolve(path).meta.requiresAdmin, path).toBe(true)
      expect(router.resolve(path).meta.requiresAuth, path).toBe(true)
    }
    expect(router.resolve('/admin/users').name).toBe('AdminUsers')
    expect(router.resolve('/admin').name).toBe('Admin')
  })

  it('lets the article page take over the right rail', () => {
    expect(router.resolve('/article/3').meta.rightRail).toBe('custom')
    expect(router.resolve('/').meta.rightRail).toBeUndefined()
    expect(router.resolve('/about').meta.rightRail).toBe('none')
  })

  it('scrolls to top on a path change, keeps position on query-only change, and restores saved positions', async () => {
    const scrollBehavior = router.options.scrollBehavior!
    const route = (path: string, query: Record<string, string> = {}, hash = '') =>
      ({ path, query, hash, fullPath: path }) as any

    expect(scrollBehavior(route('/article/4'), route('/article/3'), null)).toEqual({ top: 0 })
    expect(scrollBehavior(route('/search', { page: '2' }), route('/search', { page: '1' }), null)).toBe(false)
    expect(scrollBehavior(route('/about', {}, '#team'), route('/', {}), null)).toEqual({ el: '#team', behavior: 'smooth' })

    const saved = { left: 0, top: 480 }
    await expect(scrollBehavior(route('/'), route('/article/3'), saved as any)).resolves.toEqual(saved)
  })
})
