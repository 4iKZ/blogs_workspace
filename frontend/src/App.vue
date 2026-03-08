<template>
  <div>
    <router-view />
    <!-- Lumina Toast 通知组件 -->
    <LuminaToast />
  </div>
</template>

<script setup lang="ts">
import { usePageTitle } from './composables/usePageTitle'
import { useUserStore } from '@/store/user'
import { useSiteConfigStore } from '@/store/siteConfig'
import LuminaToast from '@/components/LuminaToast.vue'

// 自动管理页面标题
usePageTitle()

// 初始化用户状态
const userStore = useUserStore()
userStore.initUserInfo()

// 初始化网站配置
const siteConfigStore = useSiteConfigStore()
siteConfigStore.fetchConfig().then(() => {
  siteConfigStore.updateFavicon()
  siteConfigStore.updateMetaTags()
})
</script>

<style scoped>
</style>
