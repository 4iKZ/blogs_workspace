<template>
  <div>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { usePageTitle } from './composables/usePageTitle'
import { useUserStore } from '@/store/user'

// 自动管理页面标题
usePageTitle()

// 初始化用户状态
const userStore = useUserStore()
userStore.initUserInfo()
</script>

<style scoped>
</style>