import { describe, expect, it } from 'vitest'
import { formatDate, formatDateTime } from '../format'

describe('formatDateTime', () => {
  it('formats a valid timestamp in zh-CN regardless of browser locale', () => {
    // 本地时间构造，避免时区差异影响断言
    expect(formatDateTime(new Date(2026, 9, 9, 11, 31, 15))).toBe('2026/10/9 11:31')
  })

  it('accepts ISO strings and epoch numbers', () => {
    const local = new Date(2026, 0, 3, 8, 5)
    expect(formatDateTime(local.toISOString())).toBe('2026/1/3 08:05')
    expect(formatDateTime(local.getTime())).toBe('2026/1/3 08:05')
  })

  it('returns a dash for empty or invalid values', () => {
    expect(formatDateTime(null)).toBe('—')
    expect(formatDateTime(undefined)).toBe('—')
    expect(formatDateTime('')).toBe('—')
    expect(formatDateTime('not a date')).toBe('—')
  })
})

describe('formatDate', () => {
  it('formats the date part only', () => {
    expect(formatDate(new Date(2025, 11, 31, 23, 59))).toBe('2025/12/31')
  })

  it('returns a dash for invalid values', () => {
    expect(formatDate('')).toBe('—')
    expect(formatDate('garbage')).toBe('—')
  })
})
