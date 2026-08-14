import { onBeforeUnmount, type Ref } from 'vue'

interface ScrollRevealOptions {
  threshold?: number
  rootMargin?: string
  staggerDelay?: number
  unobserveAfterReveal?: boolean
}

export function useScrollRevealList(
  containerRef: Ref<HTMLElement | null>,
  itemSelector: string,
  options: ScrollRevealOptions = {}
) {
  const {
    threshold = 0.1,
    rootMargin = '0px 0px -40px 0px',
    staggerDelay = 80,
    unobserveAfterReveal = true,
  } = options

  let observer: IntersectionObserver | null = null

  const observe = () => {
    const container = containerRef.value
    if (!container) return

    unobserve()

    const items = container.querySelectorAll(itemSelector)
    if (items.length === 0) return

    observer = new IntersectionObserver(
      (entries) => {
        // 实时查询容器内元素，避免快照在 v-for 重渲染后 indexOf 返回 -1
        const currentItems = container.querySelectorAll(itemSelector)
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const el = entry.target as HTMLElement
            const index = Array.from(currentItems).indexOf(el)
            el.style.transitionDelay = `${Math.max(0, index) * staggerDelay}ms`
            el.classList.add('scroll-revealed')

            if (unobserveAfterReveal) {
              observer?.unobserve(el)
            }
          }
        })
      },
      { threshold, rootMargin }
    )

    items.forEach((item) => observer!.observe(item))
  }

  const unobserve = () => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  }

  onBeforeUnmount(() => {
    unobserve()
  })

  return { observe, unobserve }
}
