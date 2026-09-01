import { describe, expect, it } from 'vitest'
import { nextPollingDelay } from '../notification'

describe('nextPollingDelay 通知轮询退避序列', () => {
  it('0 次失败 → 30000ms（基础间隔）', () => {
    expect(nextPollingDelay(0)).toBe(30000)
  })

  it('1 次失败 → 60000ms', () => {
    expect(nextPollingDelay(1)).toBe(60000)
  })

  it('2 次失败 → 120000ms', () => {
    expect(nextPollingDelay(2)).toBe(120000)
  })

  it('3 次失败 → 300000ms（封顶）', () => {
    expect(nextPollingDelay(3)).toBe(300000)
  })

  it('5 次失败 → 300000ms（封顶）', () => {
    expect(nextPollingDelay(5)).toBe(300000)
  })
})
