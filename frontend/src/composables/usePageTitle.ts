import { watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteConfigStore } from '@/store/siteConfig'

export function usePageTitle(title?: string) {
  const route = useRoute()
  const siteConfigStore = useSiteConfigStore()

  const getRouteTitle = (): string | undefined => {
    const routeTitles: Record<string, string> = {
      '/': '首页',
      '/about': '关于',
      '/login': '登录',
      '/register': '注册',
      '/profile': '个人中心',
      '/admin': '管理后台',
      '/admin/users': '用户管理',
      '/search': '搜索结果',
    }

    const titleFromPath = routeTitles[route.path]
    if (titleFromPath) return titleFromPath
    if (route.path.startsWith('/article/')) return '文章详情'
    if (route.path.startsWith('/category/')) return '分类'
    if (route.path.startsWith('/tag/')) return '标签'
    return undefined
  }

  const updateTitle = (pageTitle?: string) => {
    const baseTitle = siteConfigStore.websiteName
    document.title = pageTitle ? `${pageTitle} - ${baseTitle}` : baseTitle
  }

  if (title) {
    updateTitle(title)
  } else {
    // 路由变化时刷新标题；未匹配路由回退为纯站点名，避免残留上一页标题
    watch(
      () => route.path,
      () => {
        updateTitle(getRouteTitle())
      },
      { immediate: true }
    )
    // 站点名加载完成后重刷标题（首次加载时 config 未就绪）
    watch(
      () => siteConfigStore.websiteName,
      () => {
        updateTitle(getRouteTitle())
      }
    )
  }

  return {
    updateTitle
  }
}
