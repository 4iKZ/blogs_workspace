import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 公共路由
    { path: '/', name: 'Home', component: () => import('../views/HomeView.vue') },
    { path: '/article/:id', name: 'ArticleDetail', component: () => import('../views/ArticleDetailView.vue') },
    { path: '/category/:id', name: 'Category', component: () => import('../views/CategoryView.vue') },
    { path: '/tag/:id', name: 'Tag', component: () => import('../views/TagView.vue') },
    { path: '/search', name: 'Search', component: () => import('../views/SearchView.vue') },
    { path: '/about', name: 'About', component: () => import('../views/AboutView.vue') },
    { path: '/following', name: 'Following', component: () => import('../views/HomeView.vue') },
    { path: '/user/:id', name: 'UserProfile', component: () => import('../views/UserProfileView.vue') },

    // 认证路由
    { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue'), meta: { requiresAuth: false } },
    { path: '/register', name: 'Register', component: () => import('../views/RegisterView.vue'), meta: { requiresAuth: false } },
    { path: '/reset-password', name: 'ResetPassword', component: () => import('../views/ResetPasswordView.vue'), meta: { requiresAuth: false } },

    // 需要认证的路由
    { path: '/profile', name: 'Profile', component: () => import('../views/ProfileView.vue'), meta: { requiresAuth: true } },
    { path: '/article/create', name: 'ArticleCreate', component: () => import('../views/ArticleEditView.vue'), meta: { requiresAuth: true } },
    { path: '/article/edit/:id', name: 'ArticleEdit', component: () => import('../views/ArticleEditView.vue'), meta: { requiresAuth: true } },
    { path: '/notifications', name: 'Notifications', component: () => import('../views/NotificationView.vue'), meta: { requiresAuth: true } },

    // 管理员路由
    { path: '/admin', name: 'Admin', component: () => import('../views/admin/AdminHomeView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/users', name: 'AdminUsers', component: () => import('../views/admin/AdminUsersView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/articles', name: 'AdminArticles', component: () => import('../views/admin/AdminArticlesView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/comments', name: 'AdminComments', component: () => import('../views/admin/AdminCommentsView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/categories', name: 'AdminCategories', component: () => import('../views/admin/AdminCategoriesView.vue'), meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/settings', name: 'AdminSettings', component: () => import('../views/admin/AdminSettingsView.vue'), meta: { requiresAuth: true, requiresAdmin: true } }
  ]
})

// 路由守卫
router.beforeEach((to, _, next) => {
  // 路由切换前清理所有 Message 弹窗
  ElMessage.closeAll()

  const userStore = useUserStore()

  // 检查是否需要认证
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      next({ name: 'Login' })
      return
    }

    // 检查是否需要管理员权限
    if (to.meta.requiresAdmin && userStore.getRole !== 'admin') {
      next({ name: 'Home' })
      return
    }
  }

  // 如果用户已登录，但尝试访问登录/注册页面，则重定向到首页
  if (!to.meta.requiresAuth && userStore.isLoggedIn) {
    if (to.name === 'Login' || to.name === 'Register') {
      next({ name: 'Home' })
      return
    }
  }

  next()
})

export default router