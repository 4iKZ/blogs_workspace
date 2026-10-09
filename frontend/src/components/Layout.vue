<template>
  <div class="layout">
    <!-- 顶部导航栏 -->
    <Header />
    
    <div class="layout-main">
      <div class="container">
        <div
          class="layout-content"
          :class="{ 'no-left-sidebar': !showLeftSidebar }"
        >
          <!-- 左侧分类导航 -->
          <aside
            v-if="showLeftSidebar"
            class="left-sidebar"
          >
            <slot name="left-sidebar">
              <LeftSidebar />
            </slot>
          </aside>
          
          <!-- 中间主内容区域 -->
          <main class="main-content">
            <!-- 子路由页面在这里切换，顶栏、侧栏、页脚保持不动 -->
            <router-view v-slot="{ Component, route: pageRoute }">
              <Transition
                name="page"
                mode="out-in"
              >
                <component
                  :is="Component"
                  :key="pageRoute.path"
                />
              </Transition>
            </router-view>
          </main>
          
          <!-- 右侧边栏 -->
          <aside class="right-sidebar">
            <!-- 页面通过 Teleport 投递右栏内容时使用的目标节点 -->
            <div
              v-if="rightRail === 'custom'"
              id="shell-right-rail"
            />
            <Aside v-else-if="rightRail === 'default'" />
          </aside>
        </div>
      </div>
    </div>
    
    <!-- 移动端底部导航栏 -->
    <nav class="mobile-bottom-nav">
      <router-link
        to="/"
        class="mobile-nav-item"
        active-class="active"
      >
        <i class="fas fa-home" />
        <span>首页</span>
      </router-link>
      <router-link
        to="/category"
        class="mobile-nav-item"
        active-class="active"
      >
        <i class="fas fa-folder" />
        <span>分类</span>
      </router-link>
      <router-link
        v-if="isLoggedIn"
        to="/profile"
        class="mobile-nav-item"
        active-class="active"
      >
        <i class="fas fa-user" />
        <span>我的</span>
      </router-link>
      <router-link
        v-else
        to="/login"
        class="mobile-nav-item"
        active-class="active"
      >
        <i class="fas fa-sign-in-alt" />
        <span>登录</span>
      </router-link>
    </nav>
    
    <!-- 页脚 -->
    <Footer />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AppLayout' })

import { computed } from 'vue'
import { useRoute } from 'vue-router'
// 导入子组件
import Header from './Header.vue'
import LeftSidebar from './LeftSidebar.vue'
import Aside from './Aside.vue'
import Footer from './Footer.vue'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const route = useRoute()
// 由路由 meta 决定：leftSidebar: false 隐藏左栏；rightRail 为 'custom' 时右栏交给页面，'none' 不显示，默认显示榜单
const showLeftSidebar = computed(() => route.meta.leftSidebar !== false)
const rightRail = computed(() => (route.meta.rightRail as string | undefined) ?? 'default')
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
  color: var(--text-primary);
  transition: background-color var(--duration-normal) var(--ease-default),
              color var(--duration-normal) var(--ease-default);
}

.layout-main {
  flex: 1;
  padding: calc(64px + var(--space-6)) 0 var(--space-12);
  transition: var(--transition-surface);
  background-color: var(--bg-secondary);
}

.container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 var(--space-8);
}

.layout-content {
  display: grid;
  grid-template-columns: 200px 1fr 260px;
  gap: var(--space-12);
  align-items: flex-start;
}

/* 无左侧栏时的布局 */
.layout-content.no-left-sidebar {
  grid-template-columns: 1fr 260px;
}

/* 左侧分类导航栏 */
.left-sidebar {
  position: sticky;
  top: calc(64px + var(--space-6));
  background-color: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-3) 0;
  transition: var(--transition-surface);
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  box-shadow: var(--shadow-sm);
}

/* 中间主内容区 */
.main-content {
  background-color: var(--bg-primary);
  border-radius: var(--radius-lg);
  min-height: 600px;
  padding: var(--space-8);
  transition: var(--transition-surface);
  box-shadow: var(--shadow-sm);
  overflow: auto;
}

/* 右侧边栏 */
.right-sidebar {
  position: sticky;
  top: calc(64px + var(--space-6));
  transition: var(--transition-surface);
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

/* 隐藏滚动条以获得清洁外观 */
.left-sidebar::-webkit-scrollbar,
.right-sidebar::-webkit-scrollbar {
  width: 6px;
}

.left-sidebar::-webkit-scrollbar-track,
.right-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.left-sidebar::-webkit-scrollbar-thumb,
.right-sidebar::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: var(--radius-full);
}

.left-sidebar::-webkit-scrollbar-thumb:hover,
.right-sidebar::-webkit-scrollbar-thumb:hover {
  background: var(--border-hover);
}

/* 响应式设计 */
@media (max-width: 1600px) {
  .container {
    max-width: 1440px;
  }
  
  .layout-content {
    grid-template-columns: 200px 1fr 260px;
    gap: var(--space-8);
  }
}

@media (max-width: 1440px) {
  .container {
    max-width: 1200px;
  }
  
  .layout-content {
    grid-template-columns: 180px 1fr 240px;
    gap: var(--space-6);
  }
}

@media (max-width: 1024px) {
  .layout-content {
    grid-template-columns: 1fr 280px;
    gap: var(--space-4);
  }
  
  .left-sidebar {
    display: none;
  }
}

@media (max-width: 768px) {
  .layout-content {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }
  
  .layout-content.no-left-sidebar {
    grid-template-columns: 1fr;
  }
  
  .left-sidebar,
  .right-sidebar {
    display: none;
  }
  
  .layout-main {
    padding: calc(64px + var(--space-4)) 0 calc(60px + var(--space-8));
  }
  
  .container {
    padding: 0 var(--space-4);
  }
  
  .main-content {
    padding: var(--space-4);
  }
}

/* 移动端底部导航栏 */
.mobile-bottom-nav {
  display: none;
}

@media (max-width: 768px) {
  .mobile-bottom-nav {
    display: flex;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 60px;
    background: var(--bg-primary);
    box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
    z-index: 1000;
    justify-content: space-around;
    align-items: center;
    border-top: 1px solid var(--border-color);
  }

  .mobile-nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    flex: 1;
    height: 100%;
    color: var(--text-secondary);
    text-decoration: none;
    font-size: 12px;
    transition: color 0.2s ease;
  }

  .mobile-nav-item i {
    font-size: 18px;
  }

  .mobile-nav-item.active {
    color: var(--color-primary);
  }

  .mobile-nav-item:active {
    background-color: var(--bg-secondary);
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .layout-main {
    padding: calc(64px + var(--space-4)) 0 var(--space-8);
  }
}
</style>

<style>
/* 站点壳内的页面切换：只动 <main> 里的页面，顶栏、侧栏、页脚不参与。
   类名作用在子页面的根元素上，所以不能写进 scoped 样式。 */
.page-enter-active {
  transition: opacity 220ms cubic-bezier(0.2, 0.8, 0.2, 1),
    transform 220ms cubic-bezier(0.2, 0.8, 0.2, 1);
}

.page-leave-active {
  transition: opacity 120ms ease-in;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
}
</style>
