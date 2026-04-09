<template>
  <div class="callback-page">
    <div class="callback-container">
      <div v-if="loading" class="loading-content">
        <div class="spinner"></div>
        <p class="loading-text">正在处理 GitHub 登录...</p>
      </div>
      <div v-else-if="error" class="error-content">
        <div class="error-icon">✕</div>
        <h2 class="error-title">登录失败</h2>
        <p class="error-message">{{ errorMessage }}</p>
        <el-button type="primary" @click="goToLogin" class="retry-btn">
          返回登录页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/composables/useLuminaToast'
import { useUserStore } from '../store/user'
import { authService } from '../services/authService'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const error = ref(false)
const errorMessage = ref('')

const handleGithubCallback = async () => {
  const urlParams = new URLSearchParams(window.location.search)
  const code = urlParams.get('code')
  const errorParam = urlParams.get('error')

  if (errorParam) {
    error.value = true
    errorMessage.value = '您取消了 GitHub 授权'
    loading.value = false
    return
  }

  if (!code) {
    error.value = true
    errorMessage.value = '未获取到授权码，请重试'
    loading.value = false
    return
  }

  try {
    const response = await authService.githubCallback(code)

    const userInfo = {
      id: response.id,
      username: response.username,
      email: response.email || '',
      phone: response.phone || null,
      nickname: response.nickname || response.username,
      avatar: response.avatar || null,
      bio: response.bio || null,
      website: response.website || null,
      status: response.status || 1,
      role: response.role || 'user',
      position: (response as any).position || null,
      company: (response as any).company || null,
      createTime: response.createTime || new Date().toISOString(),
      lastLoginTime: response.lastLoginTime || new Date().toISOString(),
      lastLoginIp: response.lastLoginIp || null,
      articleCount: response.articleCount || 0,
      likeCount: 0,
      viewCount: 0,
      commentCount: response.commentCount || 0
    }
    userStore.setUserInfo(userInfo)
    userStore.setTokens(response.accessToken, response.refreshToken)

    toast.success('登录成功')
    router.push('/')
  } catch (err: any) {
    console.error('GitHub 登录失败:', err)
    error.value = true
    errorMessage.value = err.response?.data?.message || 'GitHub 登录失败，请重试'
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}

onMounted(() => {
  handleGithubCallback()
})
</script>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--bg-secondary);
  padding: 20px;
}

.callback-container {
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  padding: 60px 40px;
  text-align: center;
  max-width: 400px;
  width: 100%;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--border-color);
  border-top-color: var(--color-blue-500);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  color: var(--text-secondary);
  font-size: 16px;
  margin: 0;
}

.error-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.error-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: var(--color-red-100);
  color: var(--color-red-600);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
}

.error-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 700;
}

.error-message {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.retry-btn {
  margin-top: 8px;
}

@media (max-width: 480px) {
  .callback-container {
    padding: 40px 24px;
  }

  .error-title {
    font-size: 20px;
  }
}
</style>
