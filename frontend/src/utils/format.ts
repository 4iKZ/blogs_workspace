/**
 * 统一的日期格式化。固定使用 zh-CN，不依赖浏览器语言，
 * 避免同一篇文章在不同浏览器里显示成 "10/9/2026, 11:31:15 AM"。
 */

type DateInput = string | number | Date | null | undefined

const dateTimeFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'numeric',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

const dateFormatter = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'numeric',
  day: 'numeric',
})

const toValidDate = (value: DateInput): Date | null => {
  if (value === null || value === undefined || value === '') return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

/** 2026/10/9 11:31 */
export const formatDateTime = (value: DateInput): string => {
  const date = toValidDate(value)
  return date ? dateTimeFormatter.format(date) : '—'
}

/** 2026/10/9 */
export const formatDate = (value: DateInput): string => {
  const date = toValidDate(value)
  return date ? dateFormatter.format(date) : '—'
}
