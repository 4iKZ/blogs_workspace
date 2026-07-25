import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Article, PageResult } from '../../types/article'

const { getByTag } = vi.hoisted(() => ({
  getByTag: vi.fn<(tagId: number, page?: number, size?: number) => Promise<PageResult<Article>>>()
}))

vi.mock('../../services/articleService', () => ({
  articleService: { getByTag }
}))

import TagView from '../TagView.vue'

const article = (id: number, tagId: number, tagName: string): Article => ({
  id,
  title: `文章 ${id}`,
  content: '',
  status: 2,
  allowComment: 1,
  viewCount: 0,
  likeCount: 0,
  commentCount: 0,
  favoriteCount: 0,
  authorId: 1,
  authorNickname: '作者',
  categoryId: 1,
  categoryName: '分类',
  tags: [{ id: tagId, name: tagName }],
  liked: false,
  favorited: false,
  createTime: '',
  updateTime: '',
  publishTime: ''
})

const mountView = async (id = '3') => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/tag/:id', component: TagView }]
  })
  await router.push(`/tag/${id}`)
  await router.isReady()
  const wrapper = mount(TagView, {
    global: {
      plugins: [router],
    stubs: {
      Layout: { template: '<main><slot /></main>' },
      ArticleCard: { props: ['article'], template: '<article>{{ article.title }}</article>' },
      'el-empty': { props: ['description'], template: '<div>{{ description }}</div>' },
      'el-pagination': {
        template: '<button data-test="next-page" @click="$emit(\'current-change\', 2)">下一页</button>'
      }
      }
    }
  })
  return { wrapper, router }
}

describe('TagView', () => {
  beforeEach(() => {
    getByTag.mockReset()
  })

  it('loads results, displays the tag name, and changes pages', async () => {
    getByTag.mockResolvedValue({
      items: [article(1, 3, 'Vue')],
      total: 12,
      page: 1,
      size: 10
    })
    const { wrapper } = await mountView()
    await flushPromises()

    expect(getByTag).toHaveBeenCalledWith(3, 1, 10)
    expect(wrapper.text()).toContain('Vue')
    expect(wrapper.text()).toContain('文章 1')

    await wrapper.get('[data-test="next-page"]').trigger('click')
    await flushPromises()
    expect(getByTag).toHaveBeenLastCalledWith(3, 2, 10)
  })

  it('shows an empty state and reloads when the route id changes', async () => {
    getByTag.mockResolvedValue({ items: [], total: 0, page: 1, size: 10 })
    const { wrapper, router } = await mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('暂无文章')

    await router.push('/tag/4')
    await flushPromises()
    expect(getByTag).toHaveBeenLastCalledWith(4, 1, 10)
    expect(wrapper.text()).toContain('标签 #4')
  })

  it('does not request an invalid tag id', async () => {
    const { wrapper } = await mountView('0')
    await flushPromises()

    expect(getByTag).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('标签不存在')
  })
})
