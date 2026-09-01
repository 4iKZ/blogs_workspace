// 密码强度校验：8-20 位，必须包含大小写字母、数字、特殊字符
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 20

export interface PasswordValidationResult {
  valid: boolean
  message: string
}

const hasUppercase = (value: string) => /[A-Z]/.test(value)
const hasLowercase = (value: string) => /[a-z]/.test(value)
const hasNumber = (value: string) => /[0-9]/.test(value)
const hasSpecial = (value: string) => /[^A-Za-z0-9]/.test(value)

export const validatePassword = (value: string): PasswordValidationResult => {
  if (!value) {
    return { valid: false, message: '请输入密码' }
  }

  const lengthOk = value.length >= PASSWORD_MIN_LENGTH && value.length <= PASSWORD_MAX_LENGTH
  if (!lengthOk) {
    return { valid: false, message: `密码长度必须为 ${PASSWORD_MIN_LENGTH}-${PASSWORD_MAX_LENGTH} 位` }
  }
  if (!hasUppercase(value)) {
    return { valid: false, message: '密码必须包含至少一个大写字母' }
  }
  if (!hasLowercase(value)) {
    return { valid: false, message: '密码必须包含至少一个小写字母' }
  }
  if (!hasNumber(value)) {
    return { valid: false, message: '密码必须包含至少一个数字' }
  }
  if (!hasSpecial(value)) {
    return { valid: false, message: '密码必须包含至少一个特殊字符（!@#$%^&* 等）' }
  }
  return { valid: true, message: '' }
}

// Element Plus form validator：与 RegisterView 现有内联 validator 语义一致
export const passwordValidator = (_rule: any, value: string, callback: any) => {
  if (!value) {
    callback()
    return
  }
  const result = validatePassword(value)
  if (result.valid) {
    callback()
  } else {
    callback(new Error(result.message))
  }
}
