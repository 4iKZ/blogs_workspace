import { useRoute, useRouter } from 'vue-router'
import { toast } from '@/composables/useLuminaToast'

/**
 * 需要登录的操作（点赞、收藏等）在未登录时使用：
 * 先用一句话说明原因，再跳转登录页，登录后回到当前页面。
 * 不再静默跳转，避免用户以为按钮坏了。
 */
export const useRequireLogin = () => {
  const router = useRouter()
  const route = useRoute()

  return (message: string) => {
    toast.info(message)
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
  }
}
