<template>
  <header class="header">
    <div class="container">
      <div class="header-content">
        <!-- Logo -->
        <div class="logo">
          <router-link to="/" class="logo-link">
            <h1 class="logo-title">
              Lumina<span class="logo-dot">.</span>
            </h1>
          </router-link>
        </div>
        
        <!-- Mobile Menu Button -->
        <button class="mobile-menu-btn" @click="mobileMenuOpen = true">
          <el-icon :size="24"><IconMenu /></el-icon>
        </button>

        <!-- 导航菜单 -->
        <nav class="nav desktop-only">
          <ul class="nav-list">
            <li class="nav-item">
              <router-link to="/" class="nav-link" active-class="active">首页</router-link>
            </li>
            <li class="nav-item">
              <router-link to="/about" class="nav-link" active-class="active">关于</router-link>
            </li>
          </ul>
        </nav>
        
        <!-- 搜索框 -->
        <div class="search desktop-only">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章"
            clearable
            @keyup.enter="handleSearch"
            class="search-input"
            size="default"
          >
            <template #append>
              <el-button @click="handleSearch" class="search-btn">
                <SvgIcon name="search" size="16px" />
              </el-button>
            </template>
          </el-input>
        </div>
        
        <!-- 主题切换按钮 -->
        <button @click="toggleTheme" class="theme-toggle desktop-only p-2 rounded-full hover:bg-secondary transition-all" title="切换主题">
          <i v-if="isDark" class="fas fa-sun text-amber-400"></i>
          <i v-else class="fas fa-moon text-secondary"></i>
        </button>
        
        <!-- 消息通知铃铛 -->
        <div class="notification-wrapper desktop-only" v-if="isLoggedIn">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
            <button @click="toggleNotifications" class="notification-bell" title="消息通知">
              <i class="fas fa-bell"></i>
            </button>
          </el-badge>
          
          <!-- 通知下拉框 -->
          <div v-if="showNotifications" class="notification-dropdown" @click.stop>
            <div class="notification-header">
              <h3>消息通知</h3>
              <button @click="handleMarkAllRead" class="mark-all-read-btn">全部已读</button>
            </div>
            
            <div class="notification-tabs">
              <button 
                v-for="tab in notificationTabs" 
                :key="tab.key"
                :class="['notification-tab', { active: activeTab === tab.key }]"
                @click="activeTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>
            
            <div class="notification-list">
              <div v-if="loading" class="notification-loading">
                <i class="fas fa-spinner fa-spin"></i> 加载中...
              </div>
              <div v-else-if="filteredNotifications.length === 0" class="notification-empty">
                <i class="fas fa-inbox"></i>
                <p>暂无通知</p>
              </div>
              <div v-else class="notification-items">
                <div 
                  v-for="notification in filteredNotifications" 
                  :key="notification.id"
                  :class="['notification-item', { unread: notification.isRead === 0 }]"
                  @click="handleNotificationClick(notification)"
                >
                  <el-avatar :size="40" :src="notification.senderAvatar || ''">
                    {{ notification.senderNickname?.charAt(0) }}
                  </el-avatar>
                  <div class="notification-content">
                    <div class="notification-text">
                      <span class="sender-name">{{ notification.senderNickname }}</span>
                      <span class="action-text">{{ notification.typeName }}</span>
                    </div>
                    <div class="notification-target">{{ notification.targetTitle }}</div>
                    <div class="notification-time">{{ formatTime(notification.createTime) }}</div>
                  </div>
                  <div v-if="notification.isRead === 0" class="unread-dot"></div>
                </div>
              </div>
            </div>
            
            <div class="notification-footer">
              <router-link to="/notifications" class="view-all-link" @click="showNotifications = false">
                查看全部通知
              </router-link>
            </div>
          </div>
        </div>
        
        <!-- 用户菜单 -->
        <div class="user-menu desktop-only">
          <template v-if="isLoggedIn">
            <el-dropdown>
              <span class="user-info">
                <el-avatar :size="32" :src="userInfo?.avatar || ''">
                  {{ userInfo?.nickname?.charAt(0) || userInfo?.username?.charAt(0) }}
                </el-avatar>
                <span class="user-name">{{ userInfo?.nickname || userInfo?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <router-link to="/profile" class="dropdown-link">个人中心</router-link>
                  </el-dropdown-item>
                  <el-dropdown-item>
                    <router-link to="/article/create" class="dropdown-link">写文章</router-link>
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userInfo?.role === 'admin'">
                    <router-link to="/admin" class="dropdown-link">管理后台</router-link>
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="btn btn-primary">登录</router-link>
            <router-link to="/register" class="btn btn-default">注册</router-link>
          </template>
        </div>
      </div>
    </div>
    
    <!-- Mobile Menu Drawer -->
    <el-drawer
      v-model="mobileMenuOpen"
      direction="rtl"
      size="80%"
      :with-header="false"
      destroy-on-close
      class="mobile-menu-drawer"
    >
      <div class="mobile-menu-content">
        <!-- Mobile User Info / Login -->
        <div class="mobile-user-section">
          <template v-if="isLoggedIn">
            <div class="mobile-user-info">
              <el-avatar :size="48" :src="userInfo?.avatar || ''">
                {{ userInfo?.nickname?.charAt(0) || userInfo?.username?.charAt(0) }}
              </el-avatar>
              <div class="mobile-user-details">
                <span class="mobile-user-name">{{ userInfo?.nickname || userInfo?.username }}</span>
                <span class="mobile-user-role">{{ userInfo?.role === 'admin' ? '管理员' : '普通用户' }}</span>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="mobile-auth-buttons">
              <router-link to="/login" class="btn btn-primary btn-block" @click="mobileMenuOpen = false">登录</router-link>
              <router-link to="/register" class="btn btn-default btn-block" @click="mobileMenuOpen = false">注册</router-link>
            </div>
          </template>
        </div>

        <!-- Mobile Search -->
        <div class="mobile-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">
                <SvgIcon name="search" size="16px" />
              </el-button>
            </template>
          </el-input>
        </div>

        <!-- Mobile Nav Links -->
        <nav class="mobile-nav">
          <router-link to="/" class="mobile-nav-link" active-class="active" @click="mobileMenuOpen = false">
            <i class="fas fa-home"></i> 首页
          </router-link>
          <router-link to="/about" class="mobile-nav-link" active-class="active" @click="mobileMenuOpen = false">
            <i class="fas fa-info-circle"></i> 关于
          </router-link>
          
          <template v-if="isLoggedIn">
            <div class="mobile-divider"></div>
            <router-link to="/article/create" class="mobile-nav-link" active-class="active" @click="mobileMenuOpen = false">
              <i class="fas fa-pen"></i> 写文章
            </router-link>
            <router-link to="/profile" class="mobile-nav-link" active-class="active" @click="mobileMenuOpen = false">
              <i class="fas fa-user"></i> 个人中心
            </router-link>
            <router-link to="/notifications" class="mobile-nav-link" active-class="active" @click="mobileMenuOpen = false">
              <i class="fas fa-bell"></i> 消息通知
              <span v-if="unreadCount > 0" class="mobile-badge">{{ unreadCount }}</span>
            </router-link>
            <router-link v-if="userInfo?.role === 'admin'" to="/admin" class="mobile-nav-link" active-class="active" @click="mobileMenuOpen = false">
              <i class="fas fa-cog"></i> 管理后台
            </router-link>
            <a href="javascript:;" class="mobile-nav-link text-danger" @click="handleLogout; mobileMenuOpen = false">
              <i class="fas fa-sign-out-alt"></i> 退出登录
            </a>
          </template>
        </nav>

        <!-- Mobile Theme Toggle -->
        <div class="mobile-theme-toggle">
          <span>深色模式</span>
          <el-switch
            v-model="isDark"
            @change="toggleTheme"
            active-color="#409EFF"
          />
        </div>
      </div>
    </el-drawer>
  </header>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useNotificationStore } from '../store/notification'
import type { Notification } from '../types/notification'
import { ElMessage } from 'element-plus'
import { Menu as IconMenu } from '@element-plus/icons-vue'
import SvgIcon from './SvgIcon.vue'

const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const searchKeyword = ref('')
const isDark = ref(false)
const mobileMenuOpen = ref(false)

// 通知相关状态
const showNotifications = ref(false)
const activeTab = ref('all')

// 使用 store 中的通知状态
const unreadCount = computed(() => notificationStore.unreadCount)
const notifications = computed(() => notificationStore.notifications)
const loading = computed(() => notificationStore.loading)

// 通知分类标签
const notificationTabs = [
  { key: 'all', label: '全部' },
  { key: 'comment', label: '评论' },
  { key: 'like', label: '赞和收藏' },
  { key: 'follow', label: '新增粉丝' },
  { key: 'message', label: '私信' },
  { key: 'system', label: '系统通知' }
]

// 筛选通知
const filteredNotifications = computed(() => {
  if (activeTab.value === 'all') {
    return notifications.value
  }

  return notifications.value.filter(n => {
    switch (activeTab.value) {
      case 'comment':
        return n.type === 2 || n.type === 4 // 文章评论或评论回复
      case 'like':
        return n.type === 1 || n.type === 3 // 文章点赞或评论点赞
      case 'follow':
        return false // TODO: 关注通知类型
      case 'message':
        return false // TODO: 私信通知类型
      case 'system':
        return false // TODO: 系统通知类型
      default:
        return true
    }
  })
})

const isLoggedIn = computed(() => userStore.isLoggedIn)
const userInfo = computed(() => userStore.userInfo)

// 轮询间隔（毫秒）
let pollingInterval: number | null = null

// 初始化用户信息和主题
onMounted(() => {
  userStore.initUserInfo()
  initTheme()

  // 添加全局点击事件监听，点击外部关闭通知框
  document.addEventListener('click', handleClickOutside)
})

// 组件卸载时清理
onUnmounted(() => {
  stopPolling()
  document.removeEventListener('click', handleClickOutside)
})

// 初始化主题
const initTheme = () => {
  if (localStorage.theme === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    isDark.value = true
    document.documentElement.classList.add('dark')
  } else {
    isDark.value = false
    document.documentElement.classList.remove('dark')
  }
}

// 切换主题
const toggleTheme = () => {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  }
}

// 处理搜索
const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      name: 'Search',
      query: { keyword: searchKeyword.value.trim() }
    })
  }
}

// 处理退出登录
const handleLogout = async () => {
  try {
    // 清空通知数据
    notificationStore.clearNotifications()
    await userStore.logout()
  } finally {
    // 无论logout方法是否成功，都确保导航到登录页面
    router.push('/login')
  }
}

// 加载通知列表
const fetchNotifications = async () => {
  try {
    await notificationStore.fetchNotifications()
  } catch (error) {
    console.error('加载通知列表失败:', error)
    ElMessage.error('加载通知失败')
  }
}

// 切换通知下拉框显示
const toggleNotifications = async () => {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value && notifications.value.length === 0) {
    await fetchNotifications()
  }
}

// 点击外部关闭通知框
const handleClickOutside = (event: Event) => {
  const target = event.target as HTMLElement
  if (!target.closest('.notification-wrapper')) {
    showNotifications.value = false
  }
}

// 处理通知点击
const handleNotificationClick = async (notification: Notification) => {
  // 标记为已读
  if (notification.isRead === 0) {
    try {
      await notificationStore.markAsRead(notification.id)
    } catch (error) {
      console.error('标记消息已读失败:', error)
    }
  }

  // 根据通知类型跳转
  if (notification.targetType === 1) {
    // 文章相关通知
    router.push(`/article/${notification.targetId}`)
  }

  showNotifications.value = false
}

// 标记所有消息为已读
const handleMarkAllRead = async () => {
  try {
    await notificationStore.markAllAsRead()
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    console.error('标记所有消息已读失败:', error)
    ElMessage.error('操作失败')
  }
}

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN')
}

// 开始轮询
const startPolling = () => {
  // 清除已有的轮询
  stopPolling()
  // 每30秒轮询一次未读消息数量
  pollingInterval = window.setInterval(() => {
    notificationStore.fetchUnreadCount()
  }, 30000)
}

// 停止轮询
const stopPolling = () => {
  if (pollingInterval) {
    clearInterval(pollingInterval)
    pollingInterval = null
  }
}

// 监听登录状态变化，自动启动/停止轮询
watch(() => isLoggedIn.value, (newValue) => {
  if (newValue) {
    // 用户登录，立即获取未读数并开始轮询
    notificationStore.fetchUnreadCount()
    startPolling()
  } else {
    // 用户登出，停止轮询并清空通知
    stopPolling()
    notificationStore.clearNotifications()
  }
}, { immediate: true })
</script>

<style scoped>
.header {
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-color);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  transition: all var(--duration-normal) var(--ease-default);
}

.dark .header {
  background-color: rgba(15, 23, 42, 0.8);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  gap: var(--space-6);
}

.logo-link {
  color: var(--text-primary);
  text-decoration: none;
  display: flex;
  align-items: center;
  height: 100%;
}

.logo-title {
  font-family: var(--font-serif);
  font-size: var(--text-2xl);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  line-height: 1;
  display: flex;
  align-items: center;
  letter-spacing: -0.5px;
}

.logo-dot {
  color: var(--color-blue-500);
  margin-left: 2px;
  display: inline-block;
  transition: transform var(--duration-normal) var(--ease-default);
}

.logo-link:hover .logo-dot {
  animation: pulse 0.6s ease-in-out;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

.nav-list {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: var(--space-2);
  align-items: center;
  height: 100%;
}

.nav-item {
  height: 100%;
  display: flex;
  align-items: center;
}

.nav-link {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: var(--text-sm);
  font-weight: 500;
  transition: all var(--duration-fast) var(--ease-default);
  position: relative;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  height: 100%;
}

.nav-link:hover {
  color: var(--text-primary);
  background-color: var(--bg-secondary);
}

.dark .nav-link:hover {
  color: var(--text-primary);
}

.nav-link.active {
  color: var(--color-blue-500);
  font-weight: 600;
  background-color: rgba(59, 130, 246, 0.08);
}

.dark .nav-link.active {
  background-color: rgba(96, 165, 250, 0.15);
}

.user-menu {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: 100%;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.user-info:hover {
  background-color: var(--bg-secondary);
}

.user-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.dropdown-link {
  color: var(--text-primary);
  text-decoration: none;
  display: block;
  width: 100%;
  height: 100%;
  transition: color var(--duration-fast) var(--ease-default);
}

.dropdown-link:hover {
  color: var(--color-blue-500);
}

.btn {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: 500;
  text-decoration: none;
  transition: all var(--duration-fast) var(--ease-default);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
}

.btn-primary {
  background-color: var(--color-blue-500);
  color: white;
}

.btn-primary:hover {
  background-color: var(--color-blue-600);
  transform: translateY(-1px);
}

.btn-default {
  background-color: transparent;
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.btn-default:hover {
  background-color: var(--bg-secondary);
  border-color: var(--border-hover);
  transform: translateY(-1px);
}

/* 搜索框样式 */
.search {
  display: flex;
  align-items: center;
  height: 100%;
}

.search-input {
  width: 240px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
  transition: all var(--duration-normal) var(--ease-default);
  border: 1px solid var(--border-color);
}

.search-input :deep(.el-input__wrapper:hover) {
  border-color: var(--border-hover);
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--color-blue-500);
  box-shadow: var(--shadow-focus);
}

/* Mobile Responsive Styles */
.mobile-menu-btn {
  display: none;
  background: transparent;
  border: none;
  padding: var(--space-2);
  color: var(--text-primary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
}

.mobile-menu-btn:hover {
  background-color: var(--bg-secondary);
  transform: scale(1.05);
}

.mobile-menu-btn:active {
  transform: scale(0.95);
}

@media (max-width: 768px) {
  .desktop-only {
    display: none !important;
  }

  .mobile-menu-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .header-content {
    padding: 0 var(--space-4);
  }
}

/* Mobile Drawer Styles */
.mobile-menu-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: var(--space-4);
  gap: var(--space-6);
}

.mobile-user-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.mobile-user-info {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.mobile-user-details {
  display: flex;
  flex-direction: column;
}

.mobile-user-name {
  font-weight: 600;
  font-size: var(--text-lg);
  color: var(--text-primary);
}

.mobile-user-role {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 2px;
}

.mobile-auth-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.btn-block {
  width: 100%;
  text-align: center;
  justify-content: center;
  padding: var(--space-3);
}

.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex: 1;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  color: var(--text-primary);
  text-decoration: none;
  border-radius: var(--radius-md);
  transition: background-color 0.2s;
  font-size: var(--text-base);
}

.mobile-nav-link:hover, .mobile-nav-link.active {
  background-color: var(--bg-secondary);
  color: var(--color-blue-500);
}

.mobile-nav-link i {
  width: 20px;
  text-align: center;
}

.mobile-divider {
  height: 1px;
  background-color: var(--border-color);
  margin: var(--space-2) 0;
}

.mobile-badge {
  background-color: #ef4444;
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: auto;
}

.mobile-theme-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3);
  border-top: 1px solid var(--border-color);
  color: var(--text-primary);
}

.text-danger {
  color: #ef4444;
}

.text-danger:hover {
  color: #dc2626;
  background-color: #fef2f2;
}

.search-btn {
  padding: 0 var(--space-3);
  background-color: var(--bg-secondary);
  border-color: var(--border-color);
  transition: all var(--duration-fast) var(--ease-default);
}

.search-btn:hover {
  background-color: var(--color-blue-600);
  border-color: var(--color-blue-600);
  color: white;
}

/* 主题切换按钮 */
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
}

.theme-toggle:hover {
  background-color: var(--bg-secondary);
}

.theme-toggle:active {
  transform: scale(0.95);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-wrap: wrap;
    gap: var(--space-3);
    padding: var(--space-2) 0;
  }
  
  .nav {
    order: 3;
    width: 100%;
  }
  
  .nav-list {
    justify-content: center;
    gap: var(--space-4);
  }
  
  .search {
    order: 2;
  }
  
  .search-input {
    width: 150px;
  }
}

/* 消息通知样式 */
.notification-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.notification-bell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-default);
  color: var(--text-secondary);
}

.notification-bell:hover {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.notification-bell:active {
  transform: scale(0.95);
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 380px;
  max-height: 600px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  overflow: hidden;
}

.dark .notification-dropdown {
  background: var(--bg-secondary);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.notification-header h3 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--text-primary);
}

.mark-all-read-btn {
  padding: var(--space-1) var(--space-3);
  background: transparent;
  border: none;
  color: var(--color-blue-500);
  font-size: var(--text-sm);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast);
}

.mark-all-read-btn:hover {
  background: var(--bg-secondary);
}

.notification-tabs {
  display: flex;
  padding: var(--space-2) var(--space-4);
  gap: var(--space-2);
  overflow-x: auto;
  border-bottom: 1px solid var(--border-color);
}

.notification-tab {
  padding: var(--space-2) var(--space-3);
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  border-radius: var(--radius-sm);
  white-space: nowrap;
  transition: all var(--duration-fast);
}

.notification-tab:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.notification-tab.active {
  background: var(--color-blue-500);
  color: white;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-loading,
.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.notification-empty i {
  font-size: 48px;
  margin-bottom: var(--space-3);
  opacity: 0.3;
}

.notification-items {
  padding: var(--space-2) 0;
}

.notification-item {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  position: relative;
  transition: background-color var(--duration-fast);
}

.notification-item:hover {
  background-color: var(--bg-secondary);
}

.notification-item.unread {
  background-color: rgba(59, 130, 246, 0.05);
}

.dark .notification-item.unread {
  background-color: rgba(96, 165, 250, 0.1);
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  font-size: var(--text-sm);
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}

.sender-name {
  font-weight: 600;
  margin-right: var(--space-1);
}

.action-text {
  color: var(--text-secondary);
}

.notification-target {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-time {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: var(--color-blue-500);
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: var(--space-2);
}

.notification-footer {
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--border-color);
  text-align: center;
}

.view-all-link {
  color: var(--color-blue-500);
  text-decoration: none;
  font-size: var(--text-sm);
  transition: color var(--duration-fast);
}

.view-all-link:hover {
  color: var(--color-blue-600);
  text-decoration: underline;
}
</style>