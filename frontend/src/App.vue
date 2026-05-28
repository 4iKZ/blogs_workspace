<template>
  <div>
    <router-view v-slot="{ Component, route }">
      <Transition
        :name="transitionName"
        mode="out-in"
      >
        <component
          :is="Component"
          :key="route.path"
        />
      </Transition>
    </router-view>
    <LuminaToast />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { usePageTitle } from './composables/usePageTitle'
import { useUserStore } from '@/store/user'
import { useSiteConfigStore } from '@/store/siteConfig'
import LuminaToast from '@/components/LuminaToast.vue'

const router = useRouter()
const transitionName = ref('page-fade')

watch(
  () => router.currentRoute.value,
  (to, from) => {
    if (!from || !from.path) {
      transitionName.value = 'page-fade'
      return
    }
    const toDepth = to.path.split('/').filter(Boolean).length
    const fromDepth = from.path.split('/').filter(Boolean).length
    transitionName.value = toDepth >= fromDepth ? 'page-slide-left' : 'page-slide-right'
  }
)

usePageTitle()

const userStore = useUserStore()
userStore.initUserInfo()

const siteConfigStore = useSiteConfigStore()
siteConfigStore.fetchConfig().then(() => {
  siteConfigStore.updateFavicon()
  siteConfigStore.updateMetaTags()
})
</script>

<style>
/* Page transition animations */
.page-fade-enter-active,
.page-fade-leave-active,
.page-slide-left-enter-active,
.page-slide-left-leave-active,
.page-slide-right-enter-active,
.page-slide-right-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}

.page-slide-left-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.page-slide-left-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.page-slide-right-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.page-slide-right-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
