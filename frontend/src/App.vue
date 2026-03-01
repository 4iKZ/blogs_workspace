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
import LuminaToast from '@/components/LuminaToast.vue'

// 自动管理页面标题
usePageTitle()

// 初始化用户状态
const userStore = useUserStore()
userStore.initUserInfo()
</script>

<style scoped>
</style>
