<template>
  <el-card class="statistics-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span>文章统计</span>
        <el-button 
          type="text" 
          :icon="Refresh" 
          @click="refreshStats" 
          :loading="loading"
          size="small"
        />
      </div>
    </template>
    
    <div class="stats-grid">
      <div class="stat-item">
        <el-icon class="stat-icon" color="#409eff">
          <View />
        </el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.viewCount) }}</div>
          <div class="stat-label">浏览</div>
        </div>
      </div>
      
      <div class="stat-item">
        <el-icon class="stat-icon" color="#f56c6c">
          <Star />
        </el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.likeCount) }}</div>
          <div class="stat-label">点赞</div>
        </div>
      </div>
      
      <div class="stat-item">
        <el-icon class="stat-icon" color="#67c23a">
          <ChatDotRound />
        </el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.commentCount) }}</div>
          <div class="stat-label">评论</div>
        </div>
      </div>
      
      <div class="stat-item">
        <el-icon class="stat-icon" color="#e6a23c">
          <Collection />
        </el-icon>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(stats.favoriteCount) }}</div>
          <div class="stat-label">收藏</div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { View, Star, ChatDotRound, Collection, Refresh } from '@element-plus/icons-vue'
import { statisticsService } from '../../services/statisticsService'
import type { ArticleStats } from '../../types/statistics'

interface Props {
  articleId: number
  autoRefresh?: boolean
  refreshInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  autoRefresh: false,
  refreshInterval: 30000 // 30 seconds
})

const loading = ref(false)
const stats = ref<ArticleStats>({
  articleId: props.articleId,
  viewCount: 0,
  likeCount: 0,
  commentCount: 0,
  favoriteCount: 0
})

let refreshTimer: number | null = null

// 获取统计数据
const fetchStats = async () => {
  if (loading.value) return
  
  loading.value = true
  try {
    const response = await statisticsService.getArticleStats(props.articleId)
    stats.value = response
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 刷新统计数据
const refreshStats = () => {
  fetchStats()
}

// 格式化数字
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 监听articleId变化
watch(() => props.articleId, () => {
  fetchStats()
})

// 组件挂载时获取数据
onMounted(() => {
  fetchStats()
  
  // 如果开启自动刷新
  if (props.autoRefresh && props.refreshInterval > 0) {
    refreshTimer = window.setInterval(() => {
      fetchStats()
    }, props.refreshInterval)
  }
})

// 组件卸载时清理定时器
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})

// 暴露refresh方法供父组件调用
defineExpose({
  refresh: refreshStats
})
</script>

<script lang="ts">
export default {
  name: 'StatisticsCard'
}
</script>

<style scoped>
.statistics-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.stat-item:hover {
  background-color: #ecf5ff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  font-size: 24px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  line-height: 1;
}
</style>
