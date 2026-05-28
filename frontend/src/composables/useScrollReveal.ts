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
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const el = entry.target as HTMLElement
            const index = Array.from(items).indexOf(el)
            el.style.transitionDelay = `${index * staggerDelay}ms`
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
