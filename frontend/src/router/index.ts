import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import Layout from '../components/Layout.vue'
import AdminShell from '../components/AdminShell.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 独立页面：不显示站点顶栏、侧栏和页脚
    { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue'), meta: { requiresAuth: false } },
    { path: '/register', name: 'Register', component: () => import('../views/RegisterView.vue'), meta: { requiresAuth: false } },
    { path: '/reset-password', name: 'ResetPassword', component: () => import('../views/ResetPasswordView.vue'), meta: { requiresAuth: false } },
    { path: '/github/callback', name: 'GithubCallback', component: () => import('../views/GithubCallbackView.vue'), meta: { requiresAuth: false } },
    { path: '/article/create', name: 'ArticleCreate', component: () => import('../views/ArticleEditView.vue'), meta: { requiresAuth: true } },
    { path: '/article/edit/:id', name: 'ArticleEdit', component: () => import('../views/ArticleEditView.vue'), meta: { requiresAuth: true } },

    // 管理后台：独立壳（顶栏 + 左侧竖向导航），不显示公共左右栏
    {
      path: '/admin',
      component: AdminShell,
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', name: 'Admin', component: () => import('../views/admin/AdminHomeView.vue') },
        { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/AdminUsersView.vue') },
        { path: 'articles', name: 'AdminArticles', component: () => import('../views/admin/AdminArticlesView.vue') },
        { path: 'moderation', name: 'AdminModeration', component: () => import('../views/admin/AdminModerationView.vue') },
        { path: 'comments', name: 'AdminComments', component: () => import('../views/admin/AdminCommentsView.vue') },
        { path: 'categories', name: 'AdminCategories', component: () => import('../views/admin/AdminCategoriesView.vue') },
        { path: 'settings', name: 'AdminSettings', component: () => import('../views/admin/AdminSettingsView.vue') },
        { path: 'backup', name: 'AdminBackup', component: () => import('../views/admin/AdminBackupView.vue') },
        { path: 'files', name: 'AdminFiles', component: () => import('../views/admin/AdminFilesView.vue') },
        { path: 'sensitive-words', name: 'AdminSensitiveWords', component: () => import('../views/admin/AdminSensitiveWordsView.vue') },
      ]
    },

    // 站点壳：顶栏、左右栏和页脚由 Layout 提供，子路由只替换 <main> 内的页面
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', name: 'Home', component: () => import('../views/HomeView.vue') },
        { path: 'article/:id', name: 'ArticleDetail', component: () => import('../views/ArticleDetailView.vue'), meta: { rightRail: 'custom' } },
        { path: 'category', name: 'CategoryList', component: () => import('../views/CategoryView.vue') },
        { path: 'category/:id', name: 'Category', component: () => import('../views/CategoryView.vue') },
        { path: 'search', name: 'Search', component: () => import('../views/SearchView.vue') },
        { path: 'about', name: 'About', component: () => import('../views/AboutView.vue'), meta: { rightRail: 'none' } },
        { path: 'following', name: 'Following', component: () => import('../views/HomeView.vue'), meta: { requiresAuth: true } },
        { path: 'user/:id', name: 'UserProfile', component: () => import('../views/UserProfileView.vue'), meta: { leftSidebar: false } },

        // 需要认证的页面
        { path: 'profile', name: 'Profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true, leftSidebar: false } },
        { path: 'profile/following', name: 'ProfileFollowing', component: () => import('../views/FollowListPage.vue'), meta: { requiresAuth: true, leftSidebar: false }, props: { mode: 'following' } },
        { path: 'profile/followers', name: 'ProfileFollowers', component: () => import('../views/FollowListPage.vue'), meta: { requiresAuth: true, leftSidebar: false }, props: { mode: 'followers' } },
        { path: 'notifications', name: 'Notifications', component: () => import('../views/NotificationView.vue'), meta: { requiresAuth: true } },


        // 404 页面（必须放在站点壳子路由的最后）
        { path: ':pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFoundView.vue'), meta: { leftSidebar: false } }
      ]
    }
  ],
  scrollBehavior(to, from, savedPosition) {
    // 前进/后退恢复原位置；等旧页面淡出后再滚动，避免被裁剪到旧页高度
    if (savedPosition) {
      return new Promise((resolve) => setTimeout(() => resolve(savedPosition), 150))
    }
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    // 同一页面只改查询参数（如搜索翻页）时保持当前位置
    if (to.path === from.path) return false
    return { top: 0 }
  }
})

interface RouteAccessTarget {
  name?: unknown
  fullPath?: string
  meta: {
    requiresAuth?: unknown
    requiresAdmin?: unknown
  }
}

interface RouteAccessStore {
  isLoggedIn: boolean
  getRole?: string
  initializeSession: () => Promise<void>
}

export const resolveRouteAccess = async (
  to: RouteAccessTarget,
  userStore: RouteAccessStore
) => {
  await userStore.initializeSession()

  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      // 记住原目标，登录成功后回到这里（地址由 utils/redirect 校验）
      return { name: 'Login', query: { redirect: to.fullPath } }
    }
    if (to.meta.requiresAdmin && userStore.getRole !== 'admin') {
      return { name: 'Home' }
    }
  }

  if (!to.meta.requiresAuth && userStore.isLoggedIn) {
    if (to.name === 'Login' || to.name === 'Register' || to.name === 'ResetPassword') {
      return { name: 'Home' }
    }
    // 已登录用户命中 GitHub 回调会静默换号，直接回首页（与 Login/Register 一致）
    if (to.name === 'GithubCallback') {
      return { name: 'Home' }
    }
  }
  return true
}

// 路由守卫
router.beforeEach(async (to) => {
  // 路由切换前清理所有 Message 弹窗
  ElMessage.closeAll()

  const userStore = useUserStore()
  return resolveRouteAccess(to, userStore)
})

export default router
