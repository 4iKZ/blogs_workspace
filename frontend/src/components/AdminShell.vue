<template>
  <div class="admin-shell">
    <Header />

    <div class="admin-body">
      <!-- 左侧竖向导航：后台页面之间的主要切换方式 -->
      <aside class="admin-nav">
        <el-menu
          :default-active="route.path"
          class="admin-menu"
          router
        >
          <el-menu-item
            v-for="item in navItems"
            :key="item.path"
            :index="item.path"
          >
            <i
              :class="item.icon"
              aria-hidden="true"
            />
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="admin-main">
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
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AppAdminShell' })

import { useRoute } from 'vue-router'
import Header from './Header.vue'

const route = useRoute()

const navItems = [
  { path: '/admin', title: '仪表盘', icon: 'fas fa-chart-line' },
  { path: '/admin/users', title: '用户', icon: 'fas fa-users' },
  { path: '/admin/articles', title: '文章', icon: 'fas fa-file-alt' },
  { path: '/admin/moderation', title: '审核', icon: 'fas fa-check-double' },
  { path: '/admin/comments', title: '评论', icon: 'fas fa-comments' },
  { path: '/admin/categories', title: '分类', icon: 'fas fa-folder' },
  { path: '/admin/sensitive-words', title: '敏感词', icon: 'fas fa-shield-alt' },
  { path: '/admin/files', title: '文件', icon: 'fas fa-folder-open' },
  { path: '/admin/backup', title: '备份', icon: 'fas fa-database' },
  { path: '/admin/settings', title: '设置', icon: 'fas fa-cog' },
]
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background-color: var(--bg-secondary);
  color: var(--text-primary);
}

.admin-body {
  display: flex;
  align-items: flex-start;
  gap: var(--space-6);
  max-width: 1440px;
  margin: 0 auto;
  padding: calc(64px + var(--space-6)) var(--space-8) var(--space-12);
}

.admin-nav {
  width: 200px;
  flex-shrink: 0;
  position: sticky;
  top: calc(64px + var(--space-6));
}

.admin-menu {
  border-right: none;
  border-radius: var(--radius-lg);
  background-color: var(--bg-primary);
  box-shadow: var(--shadow-sm);
  padding: var(--space-2) 0;
}

.admin-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  gap: 10px;
}

.admin-menu :deep(.el-menu-item i) {
  width: 18px;
  text-align: center;
}

.admin-main {
  flex: 1;
  min-width: 0;
  background-color: var(--bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-6);
}

/* 手机端：导航改为顶部可横向滚动的一行 */
@media (max-width: 768px) {
  .admin-body {
    flex-direction: column;
    align-items: stretch;
    padding: calc(64px + var(--space-4)) var(--space-4) var(--space-8);
    gap: var(--space-4);
  }

  .admin-nav {
    width: 100%;
    position: static;
    overflow-x: auto;
  }

  .admin-menu {
    display: flex;
    white-space: nowrap;
  }

  .admin-menu :deep(.el-menu-item) {
    padding: 0 var(--space-3);
  }

  .admin-main {
    padding: var(--space-4);
  }
}
</style>

