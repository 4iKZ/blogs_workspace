import { describe, expect, it, vi } from 'vitest'
import { validatePassword, passwordValidator } from '../validators'

describe('validatePassword', () => {
  it('accepts a password meeting all requirements', () => {
    expect(validatePassword('Abcd1234!').valid).toBe(true)
  })

  it('rejects a password missing an uppercase letter', () => {
    const result = validatePassword('abcd1234!')
    expect(result.valid).toBe(false)
    expect(result.message).toContain('大写字母')
  })

  it('rejects a password missing a lowercase letter', () => {
    const result = validatePassword('ABCD1234!')
    expect(result.valid).toBe(false)
    expect(result.message).toContain('小写字母')
  })

  it('rejects a password missing a number', () => {
    const result = validatePassword('Abcdefg!')
    expect(result.valid).toBe(false)
    expect(result.message).toContain('数字')
  })

  it('rejects a password missing a special character', () => {
    const result = validatePassword('Abcd1234')
    expect(result.valid).toBe(false)
    expect(result.message).toContain('特殊字符')
  })

  it('rejects a password below the minimum length', () => {
    const result = validatePassword('Ab1!')
    expect(result.valid).toBe(false)
    expect(result.message).toContain('8-20')
  })

  it('rejects a password above the maximum length', () => {
    const result = validatePassword('Abcdefghij1234567890!')
    expect(result.valid).toBe(false)
    expect(result.message).toContain('8-20')
  })

  it('returns the required message for an empty password', () => {
    const result = validatePassword('')
    expect(result.valid).toBe(false)
    expect(result.message).toBe('请输入密码')
  })
})

describe('passwordValidator', () => {
  it('calls callback() with no error for valid values', () => {
    const callback = vi.fn()
    passwordValidator(undefined, 'Abcd1234!', callback)
    expect(callback).toHaveBeenCalledWith()
  })

  it('calls callback() without error when value is empty (required handled elsewhere)', () => {
    const callback = vi.fn()
    passwordValidator(undefined, '', callback)
    expect(callback).toHaveBeenCalledWith()
  })

  it('calls callback with an Error for invalid values', () => {
    const callback = vi.fn()
    passwordValidator(undefined, 'abcd', callback)
    expect(callback).toHaveBeenCalledWith(expect.any(Error))
  })
})
