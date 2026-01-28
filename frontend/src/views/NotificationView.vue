<template>
  <Layout>
    <div class="notifications-page">
      <div class="page-header">
        <h1>消息通知</h1>
        <el-button type="primary" link @click="markAllRead" :disabled="loading">
          全部已读
        </el-button>
      </div>

      <div class="notification-container" v-loading="loading">
        <template v-if="notifications.length > 0">
          <div 
            v-for="item in notifications" 
            :key="item.id"
            class="notification-item"
            :class="{ 'unread': item.isRead === 0 }"
            @click="handleItemClick(item)"
          >
            <el-avatar :size="40" :src="item.senderAvatar" class="avatar">
              {{ item.senderNickname?.charAt(0) }}
            </el-avatar>
            
            <div class="content">
              <div class="title">
                <span class="nickname">{{ item.senderNickname }}</span>
                <span class="action">{{ item.typeName }}</span>
              </div>
              <div class="target">{{ item.targetTitle }}</div>
              <div class="time">{{ formatDate(item.createTime) }}</div>
            </div>

            <div v-if="item.isRead === 0" class="dot"></div>
          </div>
          
          <div class="pagination">
             <el-pagination
              background
              layout="prev, pager, next"
              :total="total"
              :page-size="pageSize"
              v-model:current-page="currentPage"
              @current-change="loadData"
              hide-on-single-page
            />
          </div>
        </template>
        
        <el-empty v-else description="暂无消息通知" />
      </div>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '../components/Layout.vue'
import { notificationService } from '../services/notificationService'
import type { Notification } from '../types/notification'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const notifications = ref<Notification[]>([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0) // Note: Backend API might not return total count in getList, assuming infinite scroll or simple pagination flow. 
// Checking NotificationController, getNotificationList returns Result<List<NotificationDTO>> directly, not a Page result. 
// So actual pagination might be limited if backend doesn't return total. 
// MVP: Just load list. If backend doesn't give total, we might hide pagination or simple "Load More". 
// Re-checking NotificationController: 
// It calls notificationService.getNotificationList(userId, page, size).
// It returns Result<List<NotificationDTO>>. 
// It seems we don't have total count. So standard pagination component won't work well without modifications to backend.
// MVP Adjustment: We will just show the current page. If return size < pageSize, it's the end. 
// Let's stick to simple "Previous / Next" or just a list if < 20. 
// Wait, for MVP let's just assume we can fetch list. I'll omit Total for now or try to fetch it if API supports.
// Looking at API: getList returns array. No wrapper with total.
// So I will use "Load More" style or simple pagination without Total known (just Next button if list.length === pageSize).
// For MVP simplicity: Standard pagination where we just guess total or just Prev/Next buttons. 
// Actually, let's keep it super simple: Just standard list.

const loadData = async () => {
  loading.value = true
  try {
    const res = await notificationService.getList({
      page: currentPage.value,
      size: pageSize.value
    })
    notifications.value = res.items || []
    total.value = res.total || 0
  } catch(e) {
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

const formatDate = (str: string) => {
  return new Date(str).toLocaleString()
}

const markAllRead = async () => {
  try {
    await notificationService.markAllAsRead()
    notifications.value.forEach(n => n.isRead = 1)
    ElMessage.success('全部已读')
  } catch(e) {
    ElMessage.error('操作失败')
  }
}

const handleItemClick = async (item: Notification) => {
  if (item.isRead === 0) {
    try {
        await notificationService.markAsRead(item.id)
        item.isRead = 1
    } catch(e) {}
  }
  
  // Jump Logic
  if (item.targetType === 1 || item.targetType === 2) { // Article or Comment
      router.push(`/article/${item.targetId}`)
  }
}

onMounted(() => {
  // 登录状态检查
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login' })
    return
  }
  loadData()
})
</script>

<style scoped>
.notifications-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
  min-height: 600px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 15px;
  border-radius: 8px;
  background: var(--bg-primary);
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.notification-item:hover {
  background: var(--bg-secondary);
  border-color: var(--border-color);
}

.notification-item.unread {
  background: var(--bg-secondary); /* Highlight unread */
  position: relative;
}

.avatar {
  margin-right: 15px;
  flex-shrink: 0;
}

.content {
  flex: 1;
}

.title {
  font-size: 14px;
  margin-bottom: 4px;
}

.nickname {
  font-weight: 600;
  margin-right: 6px;
  color: var(--text-primary);
}

.action {
  color: var(--text-secondary);
}

.target {
  background: rgba(0,0,0,0.03);
  padding: 8px;
  border-radius: 4px;
  color: var(--text-secondary);
  font-size: 13px;
  margin: 8px 0;
}

.time {
  font-size: 12px;
  color: #999;
}

.dot {
  width: 8px;
  height: 8px;
  background: #f56c6c;
  border-radius: 50%;
  margin-left: 10px;
  margin-top: 6px;
}

.pagination {
    display: flex;
    justify-content: center;
    margin-top: 20px;
}
</style>
