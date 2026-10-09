import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { defineComponent } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import ArticleCard from '../ArticleCard.vue'

// 测试用文章数据
const article = {
  id: 7,
  title: '测试文章标题',
  summary: '这是一段文章摘要',
  coverImage: 'https://example.com/cover.jpg',
  authorId: 1,
  authorNickname: '测试作者',
  categoryId: 2,
  categoryName: '技术',
  viewCount: 10,
  likeCount: 5,
  commentCount: 3,
  favoriteCount: 1,
  publishTime: '2026-10-09 10:00:00'
}

// 使用内存 history 的真实 router，保证 router-link 与 useRouter 可用
const createWrapper = () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/:pathMatch(.*)*', component: defineComponent({ template: '<div />' }) }
    ]
  })

  return mount(ArticleCard, {
    props: { article },
    global: { plugins: [router] }
  })
}

describe('ArticleCard', () => {
  it('不再渲染"阅读全文"按钮', () => {
    const wrapper = createWrapper()
    expect(wrapper.text()).not.toContain('阅读全文')
  })

  it('渲染移动端作者行 .mobile-meta', () => {
    const wrapper = createWrapper()
    expect(wrapper.find('.mobile-meta').exists()).toBe(true)
  })

  it('标题渲染正确并链接到文章详情', () => {
    const wrapper = createWrapper()
    const titleLink = wrapper.find('.article-title a')
    expect(titleLink.text()).toContain('测试文章标题')
    expect(titleLink.attributes('href')).toBe('/article/7')
  })
})