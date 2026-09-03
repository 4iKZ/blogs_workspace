<template>
  <Layout :show-left-sidebar="false">
    <div class="follow-page">
      <div class="page-header">
        <button class="back-btn" @click="goBack">
          <el-icon :size="14"><ArrowLeft /></el-icon>
          <span>返回</span>
        </button>
        <h2 class="page-title">{{ title }}</h2>
      </div>

      <div class="list-card shadow-sm">
        <div v-if="loading && !list.length" class="loading">
          <el-skeleton :rows="4" animated />
        </div>

        <div v-else-if="list.length" class="user-list">
          <div v-for="u in list" :key="u.id" class="user-item">
            <router-link :to="`/user/${u.id}`" class="user-link">
              <el-avatar :size="44" :src="u.avatar || ''">
                {{ u.nickname?.charAt(0) || u.username?.charAt(0) }}
              </el-avatar>
              <div class="user-info-text">
                <div class="user-name-row">
                  <span class="user-name">{{ u.nickname || u.username }}</span>
                  <span v-if="u.isMutual" class="mutual-badge">互相关注</span>
                </div>
                <div class="user-stats">粉丝 {{ u.followerCount || 0 }}</div>
              </div>
            </router-link>

            <el-button
              class="follow-btn"
              :type="u.isFollowed ? 'default' : 'primary'"
              :plain="u.isFollowed"
              size="small"
              round
              @click="toggleFollow(u)"
            >
              {{ u.isFollowed ? '已关注' : '关注' }}
            </el-button>
          </div>

          <div v-if="hasMore" class="load-more">
            <el-button
              class="load-more-btn"
              :loading="loadingMore"
              @click="loadList(false)"
            >
              加载更多
            </el-button>
          </div>
        </div>

        <div v-else class="empty">
          <el-empty :description="`暂无${title}`" />
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import Layout from '../components/Layout.vue'
import { authorService, follow, unfollow, type Author } from '../services/authorService'
import { toast } from '@/composables/useLuminaToast'

const props = defineProps<{ mode: 'following' | 'followers' }>()

const router = useRouter()

const list = ref<Author[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const page = ref(1)
const pageSize = 20
const hasMore = ref(true)

const isFollowers = computed(() => props.mode === 'followers')
const title = computed(() => (isFollowers.value ? '粉丝' : '关注'))

const loadList = async (reset = false) => {
  if (reset) {
    page.value = 1
    hasMore.value = true
    list.value = []
  }
  if (!hasMore.value) return

  if (reset) loading.value = true
  else loadingMore.value = true

  try {
    const data = isFollowers.value
      ? await authorService.getFollowers(page.value, pageSize)
      : await authorService.getFollowings(page.value, pageSize)
    if (reset) list.value = data
    else list.value.push(...data)
    hasMore.value = data.length >= pageSize
    if (!reset) page.value++
  } catch (error: any) {
    if (!error._handled) {
      toast.error(error.response?.data?.message || '加载失败')
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const toggleFollow = async (u: Author) => {
  try {
    if (u.isFollowed) {
      await unfollow(u.id)
      toast.success('已取消关注')
    } else {
      await follow(u.id)
      toast.success('已关注')
    }
    // 重新拉取首屏数据，保证互关状态与关注按钮准确
    await loadList(true)
  } catch (error: any) {
    if (!error._handled) {
      toast.error(error.response?.data?.message || '操作失败')
    }
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadList(true)
})
</script>

<style scoped>
.follow-page {
  max-width: 700px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-6) var(--space-8);
  animation: fade-in-up var(--duration-normal) var(--ease-default);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: var(--space-4);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1.5px solid var(--border-color);
  border-radius: 999px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.back-btn:hover {
  color: var(--color-blue-500);
  border-color: var(--color-blue-400);
}

.page-title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--text-primary);
}

.list-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  padding: 0 var(--space-4);
}

.user-list {
  padding: var(--space-2) 0;
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-color);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.user-item:hover {
  background-color: var(--bg-secondary);
}

.user-item:last-child {
  border-bottom: none;
}

.user-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: inherit;
  min-width: 0;
}

.user-info-text {
  margin-left: var(--space-4);
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mutual-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 0.75rem;
  color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.2);
  white-space: nowrap;
  flex-shrink: 0;
}

.user-stats {
  margin-top: 4px;
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.follow-btn {
  flex-shrink: 0;
  margin-left: var(--space-4);
}

.loading,
.empty {
  padding: var(--space-12) 0;
  text-align: center;
}

.load-more {
  text-align: center;
  padding: var(--space-4) 0 var(--space-6);
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .follow-page {
    padding: var(--space-3);
  }

  .page-title {
    font-size: var(--text-lg);
  }
}
</style>
