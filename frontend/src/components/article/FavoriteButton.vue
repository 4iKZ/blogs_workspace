<template>
  <el-button 
    :type="favorited ? 'success' : 'default'"
    :loading="loading"
    @click="handleFavorite"
    size="small"
  >
    <el-icon v-if="!loading" :size="16">
      <component :is="favorited ? StarFilled : Star" />
    </el-icon>
    {{ favoriteCount }}
  </el-button>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { articleService } from '../../services/articleService'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'

interface Props {
  articleId: number | string;
  initialFavorited?: boolean
  initialCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  initialFavorited: false,
  initialCount: 0
})

const emit = defineEmits<{
  (e: 'update', favorited: boolean, count: number): void
}>()

const userStore = useUserStore()
const favorited = ref(props.initialFavorited)
const favoriteCount = ref(props.initialCount)
const loading = ref(false)
const checkingStatus = ref(false)

watch(() => props.initialFavorited, (val) => {
  favorited.value = val
})

watch(() => props.initialCount, (val) => {
  favoriteCount.value = val
})

// 组件挂载时，检查收藏状态（仅在articleId有效且用户已登录时）
onMounted(async () => {
  // Only check status if articleId is valid and user is logged in
  if (Number(props.articleId) > 0 && userStore.isLoggedIn) {
    await checkFavoriteStatus()
  }
})

// 将 articleId 转换为 number
const toNumber = (id: number | string): number => {
  return typeof id === 'string' ? parseInt(id, 10) : id
}

// 检查收藏状态
const checkFavoriteStatus = async () => {
  try {
    checkingStatus.value = true
    const isFavorited = await articleService.checkFavoriteStatus(toNumber(props.articleId))
    if (isFavorited !== favorited.value) {
      favorited.value = isFavorited
      emit('update', isFavorited, favoriteCount.value)
    }
  } catch (error: any) {
    console.error('检查收藏状态失败:', error)
    // 出错时保持初始状态，不影响用户体验
  } finally {
    checkingStatus.value = false
  }
}

// 处理收藏操作
const handleFavorite = async () => {
  if (loading.value || checkingStatus.value) return

  loading.value = true
  try {
    // 先检查收藏状态，确保准确性
    const currentIsFavorited = await articleService.checkFavoriteStatus(toNumber(props.articleId))

    if (currentIsFavorited === favorited.value) {
      // 状态一致，可以执行收藏/取消收藏操作
      if (favorited.value) {
        // 取消收藏
        await articleService.unfavoriteArticle(toNumber(props.articleId))
        favorited.value = false
        favoriteCount.value = Math.max(0, favoriteCount.value - 1)
        ElMessage.success('取消收藏成功')
      } else {
        // 收藏
        await articleService.favoriteArticle(toNumber(props.articleId))
        favorited.value = true
        favoriteCount.value++
        ElMessage.success('收藏成功')
      }

      // 向父组件发送更新事件
      emit('update', favorited.value, favoriteCount.value)
    } else {
      // 状态不一致，更新本地状态
      favorited.value = currentIsFavorited
      ElMessage.warning('收藏状态已更新，请重新操作')
    }
  } catch (error: any) {
    console.error('收藏操作失败:', error)
    // 更详细的错误处理
    if (error.response) {
      // 服务器返回了错误状态码
      ElMessage.error(error.response.data?.message || '操作失败，请稍后重试')
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      ElMessage.error('网络错误，请检查网络连接后重试')
    } else {
      // 请求配置出错
      ElMessage.error('操作失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.el-button {
  transition: all 0.3s ease;
}

.el-button:hover {
  transform: scale(1.05);
}
</style>
