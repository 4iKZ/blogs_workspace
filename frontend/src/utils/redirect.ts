/** 登录后不能回跳到的认证页，回跳到它们会形成循环或重复登录 */
const AUTH_PATHS = ['/login', '/register', '/reset-password', '/github/callback']

const hasControlChar = (value: string) => [...value].some((ch) => ch.charCodeAt(0) < 0x20)

/**
 * 校验登录后的回跳地址，只放行站内路径，防止开放重定向（如 ?redirect=//evil.com）。
 * 不合法、为空、是数组或指向认证页时回到首页。
 */
export const safeRedirect = (raw: unknown): string => {
  if (typeof raw !== 'string' || raw === '') return '/'
  // 必须是单个 "/" 开头的站内路径；拒绝协议相对地址、反斜杠与控制字符
  if (!raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\') || hasControlChar(raw)) return '/'

  // 解码后再检查一次，防止 %2F%2Fevil.com 之类的编码绕过
  let decoded: string
  try {
    decoded = decodeURIComponent(raw)
  } catch {
    return '/'
  }
  if (!decoded.startsWith('/') || decoded.startsWith('//') || decoded.includes('\\')) return '/'

  const pathname = decoded.split(/[?#]/)[0]
  if (AUTH_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`))) return '/'

  return raw
}
