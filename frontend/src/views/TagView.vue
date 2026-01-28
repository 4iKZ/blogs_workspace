<template>
  <Layout>
    <div class="tag">
      <!-- 重定向到首页 -->
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Layout from '../components/Layout.vue'

const router = useRouter()

// 初始化数据，重定向到首页
onMounted(() => {
  router.replace('/')
})
</script>

<style scoped>
.tag {
  padding: 20px 0;
}
</style>