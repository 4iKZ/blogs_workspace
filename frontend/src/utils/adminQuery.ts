/**
 * 解析后台文章列表的 status 查询参数（如 /admin/articles?status=2）。
 * 只接受 1 草稿 / 2 已发布 / 3 已下线，其它值（含数组、非数字）一律视为“全部”（null）。
 */
export const parseArticleStatus = (raw: unknown): number | null => {
  const value = Array.isArray(raw) ? raw[0] : raw
  if (typeof value !== 'string') return null
  return /^[123]$/.test(value) ? Number(value) : null
}
