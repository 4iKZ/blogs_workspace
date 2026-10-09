import { describe, expect, it } from 'vitest'
import { CATEGORY_ICON_MAP, DEFAULT_CATEGORY_ICON, getCategoryIcon } from '../categoryIcon'

// public/images/icons/ 中可用的图标名（与目录内容保持一致；新增图标时请同步此列表）
const ICON_FILES = [
  'ai', 'android', 'apple', 'articles', 'book', 'briefcase', 'calendar', 'categories',
  'code', 'coffee', 'comment', 'dashboard', 'database', 'delete', 'edit', 'favorite',
  'layout', 'like', 'like-filled', 'search', 'server', 'settings', 'share', 'tag',
  'tool', 'user', 'users',
]

const SEED_CATEGORIES = [
  '技术分享', 'Java开发', '前端技术', '数据库', '运维部署',
  '项目经验', '生活随笔', '学习笔记', '工具推荐',
]

describe('category icons', () => {
  it('only references icons that exist in the icon directory', () => {
    const referenced = new Set([...Object.values(CATEGORY_ICON_MAP), DEFAULT_CATEGORY_ICON])
    for (const name of referenced) {
      expect(ICON_FILES, name).toContain(name)
    }
  })

  it('gives each seeded category its own icon', () => {
    const icons = SEED_CATEGORIES.map(getCategoryIcon)
    expect(new Set(icons).size).toBe(SEED_CATEGORIES.length)
  })

  it('falls back to the default icon for unknown names', () => {
    expect(getCategoryIcon('不存在的分类')).toBe(DEFAULT_CATEGORY_ICON)
    expect(getCategoryIcon('')).toBe(DEFAULT_CATEGORY_ICON)
  })
})
