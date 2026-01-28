<template>
  <el-button
    :type="liked ? 'danger' : 'default'"
    :loading="loading"
    @click="handleLike"
    size="small"
  >
    <el-icon v-if="!loading" :size="16">
      <!-- 使用自定义 SVG 图标 -->
      <img
        v-if="liked"
        src="/images/icons/like-filled.svg"
        alt="已点赞"
        style="width: 16px; height: 16px"
      />
      <img
        v-else
        src="/images/icons/like.svg"
        alt="点赞"
        style="width: 16px; height: 16px"
      />
    </el-icon>
    <span v-if="loading">处理中</span>
    <span v-else>{{ likeCount }}</span>
  </el-button>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { articleService } from "../../services/articleService";
import { useUserStore } from "../../store/user";

// 移除 Element Plus 图标导入
interface Props {
  articleId: number | string;
  initialLiked?: boolean;
  initialCount?: number;
}

const props = withDefaults(defineProps<Props>(), {
  initialLiked: false,
  initialCount: 0,
});

const emit = defineEmits<{
  (e: "update", liked: boolean, count: number): void;
}>();

const userStore = useUserStore();
const liked = ref(props.initialLiked);
const likeCount = ref(props.initialCount);
const loading = ref(false);

watch(
  () => props.initialLiked,
  (val) => {
    liked.value = val;
  }
);

watch(
  () => props.initialCount,
  (val) => {
    likeCount.value = val;
  }
);

// 监听用户登录状态变化，登录后刷新点赞状态
watch(
  () => userStore.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn && Number(props.articleId) > 0) {
      checkLikeStatus();
    }
  }
);

// 组件挂载时，检查点赞状态（仅在articleId有效且用户已登录时）
onMounted(async () => {
  // Only check status if articleId is valid and user is logged in
  if (Number(props.articleId) > 0 && userStore.isLoggedIn) {
    await checkLikeStatus();
  }
});

// 检查点赞状态
const checkLikeStatus = async () => {
  try {
    const articleId = Number(props.articleId);
    const isLiked = await articleService.checkLikeStatus(articleId);
    if (isLiked !== liked.value) {
      liked.value = isLiked;
      emit("update", isLiked, likeCount.value);
    }
  } catch (error: any) {
    console.error("检查点赞状态失败:", error);
    // 出错时保持初始状态，不影响用户体验
  }
};

// 处理点赞操作
const handleLike = async () => {
  if (loading.value) return;

  const previousLiked = liked.value;
  const previousCount = likeCount.value;

  loading.value = true;
  try {
    // 确保 articleId 是 number 类型
    const articleId = Number(props.articleId);

    // 直接执行点赞/取消点赞操作，信任后端的幂等性设计
    if (liked.value) {
      // 取消点赞
      await articleService.unlikeArticle(articleId);
      liked.value = false;
      likeCount.value = Math.max(0, likeCount.value - 1);
      ElMessage.success("取消点赞成功");
    } else {
      // 点赞
      await articleService.likeArticle(articleId);
      liked.value = true;
      likeCount.value++;
      ElMessage.success("点赞成功");
    }

    // 向父组件发送更新事件
    emit("update", liked.value, likeCount.value);
  } catch (error: any) {
    console.error("点赞操作失败:", error);

    // 恢复原始状态
    liked.value = previousLiked;
    likeCount.value = previousCount;

    // 更详细的错误处理
    if (error.response) {
      if (error.response.status === 401) {
        ElMessage.error("请先登录后操作");
        // 可以考虑跳转到登录页
      } else if (error.response.status === 403) {
        ElMessage.error("没有权限执行此操作");
      } else {
        ElMessage.error(error.response.data?.message || "操作失败，请稍后重试");
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应
      ElMessage.error("网络错误，请检查网络连接后重试");
    } else {
      // 请求配置出错
      ElMessage.error("操作失败，请稍后重试");
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.el-button {
  transition: all var(--duration-fast) var(--ease-default);
  border-radius: var(--radius-sm);
}

.el-button:hover {
  transform: translateY(-1px);
}

.el-button:active {
  transform: scale(0.95);
}

/* 点赞动画效果 */
.el-button .el-icon {
  transition: transform var(--duration-normal) var(--ease-spring);
}

.el-button:hover .el-icon {
  transform: scale(1.1);
}

.el-button:active .el-icon {
  transform: scale(0.9);
}

/* 已点赞状态的心跳动画 */
.el-button--danger .el-icon {
  animation: heartbeat 1.5s ease-in-out infinite;
}

@keyframes heartbeat {
  0%, 100% {
    transform: scale(1);
  }
  10%, 30% {
    transform: scale(1.1);
  }
  20%, 40% {
    transform: scale(1);
  }
}

/* 加载状态 */
.el-button.is-loading {
  pointer-events: none;
}
</style>
