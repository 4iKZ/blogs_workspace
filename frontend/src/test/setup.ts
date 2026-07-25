import { afterEach, vi } from 'vitest'

if (!URL.createObjectURL) {
  URL.createObjectURL = () => 'blob:mock'
}
if (!URL.revokeObjectURL) {
  URL.revokeObjectURL = () => undefined
}

afterEach(() => {
  vi.restoreAllMocks()
  localStorage.clear()
})
