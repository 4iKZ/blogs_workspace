import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// 模块内部为单例状态，每个用例通过 resetModules + 动态导入获得全新实例
const importModule = () => import('@/composables/useLuminaToast')

describe('useLuminaToast 定时器治理', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('addToast 超过 MAX_TOASTS 挤出最早者后，其定时器被清理，到原 duration 不再触发移除副作用', async () => {
    const { toast, toasts } = await importModule()

    // 占满 MAX_TOASTS(4)：t1 的 duration 与其余不同，便于验证挤出后的定时器行为
    toast.info('t1', { duration: 1000 })
    toast.info('t2', { duration: 5000 })
    toast.info('t3', { duration: 5000 })
    toast.info('t4', { duration: 5000 })
    expect(toasts.value).toHaveLength(4)
    expect(vi.getTimerCount()).toBe(4)

    // 第 5 个加入，挤出 t1
    toast.info('t5', { duration: 5000 })
    expect(toasts.value).toHaveLength(4)
    expect(toasts.value[0].id).toBe(2)
    // t1 的定时器被清理：4 存活者各 1 个，而非 5 个
    expect(vi.getTimerCount()).toBe(4)

    // 推进到 t1 的原 duration，确认其残留定时器不再触发移除副作用
    vi.advanceTimersByTime(1000)
    expect(toasts.value).toHaveLength(4)

    // 推进到存活者的 duration，4 个正常自动关闭
    vi.advanceTimersByTime(4000)
    expect(toasts.value).toHaveLength(0)
    expect(vi.getTimerCount()).toBe(0)
  })

  it('手动 removeToast 后其定时器被清理，advanceTimers 无多余回调', async () => {
    const { toast, toasts, removeToast } = await importModule()

    const id = toast.info('手动移除', { duration: 2000 })
    expect(toasts.value).toHaveLength(1)
    expect(vi.getTimerCount()).toBe(1)

    removeToast(id)
    expect(toasts.value).toHaveLength(0)
    expect(vi.getTimerCount()).toBe(0)

    // 推进到原 duration，不应有任何残留回调触发
    vi.advanceTimersByTime(2000)
    expect(toasts.value).toHaveLength(0)
    expect(vi.getTimerCount()).toBe(0)
  })

  it('removeToast 对不存在的 id 容忍且不抛异常', async () => {
    const { toast, toasts, removeToast } = await importModule()

    const id = toast.info('存活', { duration: 3000 })
    expect(() => removeToast(9999)).not.toThrow()
    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0].id).toBe(id)
    expect(vi.getTimerCount()).toBe(1)
  })
})
