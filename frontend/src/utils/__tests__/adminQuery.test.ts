import { describe, expect, it } from 'vitest'
import { parseArticleStatus } from '../adminQuery'

describe('parseArticleStatus', () => {
  it('accepts the three article statuses', () => {
    expect(parseArticleStatus('1')).toBe(1)
    expect(parseArticleStatus('2')).toBe(2)
    expect(parseArticleStatus('3')).toBe(3)
  })

  it('treats missing or unknown values as all', () => {
    expect(parseArticleStatus(undefined)).toBeNull()
    expect(parseArticleStatus(null)).toBeNull()
    expect(parseArticleStatus('')).toBeNull()
    expect(parseArticleStatus('0')).toBeNull()
    expect(parseArticleStatus('4')).toBeNull()
    expect(parseArticleStatus('2abc')).toBeNull()
    expect(parseArticleStatus(' 2')).toBeNull()
  })

  it('uses the first value when the query repeats', () => {
    expect(parseArticleStatus(['2', '1'])).toBe(2)
    expect(parseArticleStatus([])).toBeNull()
  })

  it('ignores non-string values', () => {
    expect(parseArticleStatus(2)).toBeNull()
    expect(parseArticleStatus({ status: '2' })).toBeNull()
  })
})
