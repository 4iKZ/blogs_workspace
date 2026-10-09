import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { useScrollRevealList } from '../useScrollReveal'

type RevealCallback = (entries: Array<Partial<IntersectionObserverEntry>>) => void

// 记录每次创建的观察器，测试中手动触发回调模拟“进入视口”
class FakeIntersectionObserver {
  static instances: FakeIntersectionObserver[] = []
  constructor(public callback: RevealCallback) {
    FakeIntersectionObserver.instances.push(this)
  }
  observe() {}
  unobserve() {}
  disconnect() {}
}

const latestObserver = () => FakeIntersectionObserver.instances[FakeIntersectionObserver.instances.length - 1]

const mountList = (count: number) => {
  let api!: ReturnType<typeof useScrollRevealList>
  const Host = defineComponent({
    setup() {
      const container = ref<HTMLElement | null>(null)
      api = useScrollRevealList(container, '.item', { threshold: 0.1 })
      return () => h(
        'div',
        { ref: container, class: 'list' },
        Array.from({ length: count }, (_, i) => h('div', { class: 'item', 'data-i': i }))
      )
    },
  })
  const wrapper = mount(Host, { attachTo: document.body })
  const items = wrapper.findAll('.item').map((w) => w.element as HTMLElement)
  return { api, wrapper, items }
}

const enter = (targets: HTMLElement[]) => {
  latestObserver().callback(targets.map((target) => ({ target, isIntersecting: true })))
}

describe('useScrollRevealList stagger', () => {
  beforeEach(() => {
    FakeIntersectionObserver.instances = []
    vi.stubGlobal('IntersectionObserver', FakeIntersectionObserver)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('staggers items entering together by their DOM order, not by arrival order', () => {
    const { api, wrapper, items } = mountList(3)
    api.observe()

    // 三个同时进入，回调里的顺序被打乱，延迟仍应按 DOM 顺序 0 / 50 / 100ms
    enter([items[2], items[0], items[1]])

    expect(items.map((el) => el.style.transitionDelay)).toEqual(['0ms', '50ms', '100ms'])
    wrapper.unmount()
  })

  it('restarts from zero for a later batch, regardless of the item position in the list', () => {
    const { api, wrapper, items } = mountList(4)
    api.observe()

    enter([items[0], items[1]])
    // 第二批只有最后一项进入：它是本批的第一项，不应等待前面已经显示过的项
    enter([items[3]])

    expect(items[3].style.transitionDelay).toBe('0ms')
    expect(items[3].classList.contains('scroll-revealed')).toBe(true)
    wrapper.unmount()
  })

  it('caps the delay so large batches do not wait too long', () => {
    const { api, wrapper, items } = mountList(10)
    api.observe()

    enter(items)

    expect(items[9].style.transitionDelay).toBe('300ms')
    wrapper.unmount()
  })

  it('ignores entries that are not intersecting', () => {
    const { api, wrapper, items } = mountList(2)
    api.observe()

    latestObserver().callback([{ target: items[0], isIntersecting: false }])

    expect(items[0].classList.contains('scroll-revealed')).toBe(false)
    wrapper.unmount()
  })
})
