import { describe, expect, it } from 'vitest'
import { safeRedirect } from '../redirect'

describe('safeRedirect', () => {
  it('keeps same-site paths with query and hash', () => {
    expect(safeRedirect('/article/3')).toBe('/article/3')
    expect(safeRedirect('/search?keyword=vue#top')).toBe('/search?keyword=vue#top')
    expect(safeRedirect('/profile')).toBe('/profile')
  })

  it('falls back to home for empty or non-string input', () => {
    expect(safeRedirect(undefined)).toBe('/')
    expect(safeRedirect(null)).toBe('/')
    expect(safeRedirect('')).toBe('/')
    expect(safeRedirect(['/profile', '/admin'])).toBe('/')
  })

  it('rejects protocol-relative and absolute URLs', () => {
    expect(safeRedirect('//evil.com')).toBe('/')
    expect(safeRedirect('/\\evil.com')).toBe('/')
    expect(safeRedirect('https://evil.com')).toBe('/')
    expect(safeRedirect('javascript:alert(1)')).toBe('/')
  })

  it('rejects encoded bypasses', () => {
    expect(safeRedirect('%2F%2Fevil.com')).toBe('/')
    expect(safeRedirect('/%2F%2Fevil.com')).toBe('/')
    expect(safeRedirect('/%5Cevil.com')).toBe('/')
    expect(safeRedirect('/%E0%A4%A')).toBe('/')
  })

  it('does not send users back into the auth pages', () => {
    expect(safeRedirect('/login')).toBe('/')
    expect(safeRedirect('/login?redirect=/profile')).toBe('/')
    expect(safeRedirect('/register')).toBe('/')
    expect(safeRedirect('/reset-password')).toBe('/')
    expect(safeRedirect('/github/callback?code=1')).toBe('/')
    expect(safeRedirect('/loginx')).toBe('/loginx')
  })
})
