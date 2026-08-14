import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const { getSensitiveWords } = vi.hoisted(() => ({
  getSensitiveWords: vi.fn()
}))

vi.mock('@/api/sensitiveWord', () => ({
  getSensitiveWords,
  createSensitiveWord: vi.fn(),
  updateSensitiveWord: vi.fn(),
  deleteSensitiveWord: vi.fn(),
  batchDeleteSensitiveWords: vi.fn(),
  batchImportSensitiveWords: vi.fn(),
  reloadSensitiveWordCache: vi.fn()
}))

import AdminSensitiveWordsView from '../admin/AdminSensitiveWordsView.vue'

describe('AdminSensitiveWordsView', () => {
  beforeEach(() => {
    getSensitiveWords.mockReset()
  })

  it('renders sensitive words returned by backend as items', async () => {
    getSensitiveWords.mockResolvedValue({
      items: [
        { id: 1, word: '敏感词甲', category: 'politics', level: 2, createTime: '', updateTime: '' }
      ],
      total: 1,
      page: 1,
      size: 10
    })

    const wrapper = mount(AdminSensitiveWordsView, {
      global: {
        stubs: {
          'el-card': { template: '<div><slot /></div>' },
          'el-form': { template: '<div><slot /></div>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': true,
          'el-select': true,
          'el-option': true,
          'el-button': { template: '<button><slot /></button>' },
          'el-table': { props: ['data'], template: '<div class="table-data">{{ data?.[0]?.word }}</div>' },
          'el-table-column': true,
          'el-pagination': true,
          'el-tag': { template: '<span><slot /></span>' }
        },
        directives: { loading: () => {} }
      }
    })
    await flushPromises()

    expect(getSensitiveWords).toHaveBeenCalled()
    expect(wrapper.text()).toContain('敏感词甲')
  })

  it('shows empty list when backend returns no items', async () => {
    getSensitiveWords.mockResolvedValue({ items: [], total: 0, page: 1, size: 10 })

    const wrapper = mount(AdminSensitiveWordsView, {
      global: {
        stubs: {
          'el-card': { template: '<div><slot /></div>' },
          'el-form': { template: '<div><slot /></div>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': true,
          'el-select': true,
          'el-option': true,
          'el-button': { template: '<button><slot /></button>' },
          'el-table': { props: ['data'], template: '<div class="table-data">{{ data?.length ?? 0 }}</div>' },
          'el-table-column': true,
          'el-pagination': true,
          'el-tag': { template: '<span><slot /></span>' }
        },
        directives: { loading: () => {} }
      }
    })
    await flushPromises()

    expect(getSensitiveWords).toHaveBeenCalled()
    expect(wrapper.find('.table-data').text()).toBe('0')
  })
})
