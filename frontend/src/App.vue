<template>
  <div>
    <router-view v-slot="{ Component, route }">
      <!-- 站点壳内的页面切换由 Layout 处理；壳与独立页（登录、注册等）之间用短淡入淡出 -->
      <Transition
        name="fade"
        mode="out-in"
      >
        <component
          :is="Component"
          :key="route.matched[0]?.path ?? route.path"
        />
      </Transition>
    </router-view>
    <LuminaToast />
  </div>
</template>

<script setup lang="ts">
import { usePageTitle } from './composables/usePageTitle'
import { useSiteConfigStore } from '@/store/siteConfig'
import LuminaToast from '@/components/LuminaToast.vue'

usePageTitle()

const siteConfigStore = useSiteConfigStore()
siteConfigStore.fetchConfig().then(() => {
  siteConfigStore.updateFavicon()
  siteConfigStore.updateMetaTags()
})
</script>

<style>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 150ms ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
