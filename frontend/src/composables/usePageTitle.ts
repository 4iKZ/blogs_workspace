import { watch } from 'vue'
import { useRoute } from 'vue-router'

export function usePageTitle(title?: string) {
  const route = useRoute()

  const updateTitle = (pageTitle?: string) => {
    const baseTitle = 'Lumina'
    document.title = pageTitle ? `${pageTitle} - ${baseTitle}` : baseTitle
  }

  if (title) {
    updateTitle(title)
  } else {
    // 根据路由自动设置标题
    watch(
      () => route.path,
      () => {
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
        if (titleFromPath) {
          updateTitle(titleFromPath)
        } else if (route.path.startsWith('/article/')) {
          updateTitle('文章详情')
        } else if (route.path.startsWith('/category/')) {
          updateTitle('分类')
        } else if (route.path.startsWith('/tag/')) {
          updateTitle('标签')
        }
      },
      { immediate: true }
    )
  }

  return {
    updateTitle
  }
}
