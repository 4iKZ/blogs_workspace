/**
 * 分类图标映射：侧栏与分类页共用。图标名对应 public/images/icons/<name>.svg。
 * 数据库目前没有分类图标字段，图标只由分类名决定；未收录的分类使用 DEFAULT_CATEGORY_ICON。
 */
export const CATEGORY_ICON_MAP: Readonly<Record<string, string>> = {
  // 默认种子分类（data.sql），每个分类一个不同的图标
  '技术分享': 'code',
  'Java开发': 'coffee',
  '前端技术': 'layout',
  '数据库': 'database',
  '运维部署': 'server',
  '项目经验': 'briefcase',
  '生活随笔': 'book',
  '学习笔记': 'edit',
  '工具推荐': 'tool',
  // 历史名称保留，避免用户自建的同名分类图标变化
  '后端': 'code',
  '前端': 'layout',
  'Android': 'android',
  'iOS': 'apple',
  '人工智能': 'ai',
  '开发工具': 'tool',
  '代码人生': 'user',
  '阅读': 'book',
}

export const DEFAULT_CATEGORY_ICON = 'tag'

export const getCategoryIcon = (name: string): string => CATEGORY_ICON_MAP[name] ?? DEFAULT_CATEGORY_ICON
