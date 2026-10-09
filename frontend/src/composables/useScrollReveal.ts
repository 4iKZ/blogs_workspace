import { onBeforeUnmount, type Ref } from 'vue'

interface ScrollRevealOptions {
  threshold?: number
  rootMargin?: string
  staggerDelay?: number
  maxStaggerDelay?: number
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
    staggerDelay = 50,
    maxStaggerDelay = 300,
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
        // 同一批进入视口的元素按 DOM 顺序错开；延迟只取决于批内排名，
        // 因此无限滚动追加的新项不会因为列表靠后而等待。
        const batch = entries
          .filter((entry) => entry.isIntersecting)
          .map((entry) => entry.target as HTMLElement)
          .sort((a, b) => (a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1))

        batch.forEach((el, rank) => {
          el.style.transitionDelay = `${Math.min(rank * staggerDelay, maxStaggerDelay)}ms`
          el.classList.add('scroll-revealed')

          if (unobserveAfterReveal) {
            observer?.unobserve(el)
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
