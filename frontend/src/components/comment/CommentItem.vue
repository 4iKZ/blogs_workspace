<template>
  <div class="comment-item" :class="{ 'is-reply': isReply }">
    <div class="comment-header">
      <div class="user-info">
        <el-avatar
          :size="isReply ? 32 : 40"
          :src="comment.avatar || '/default-avatar.png'"
        />
        <div class="user-details">
          <span class="nickname">
            <template v-if="isReply">
              {{ comment.nickname }}
              <span class="reply-label"> 回复 </span>
              <span class="reply-target">{{
                comment.replyToNickname || "评论"
              }}</span>
            </template>
            <template v-else>
              {{ comment.nickname }}
            </template>
          </span>
          <span class="time">{{ formatTime(comment.createTime) }}</span>
        </div>
      </div>
      <div class="comment-actions">
        <el-button
          type="text"
          size="small"
          @click="toggleLike"
          :loading="likingLoading"
        >
          <SvgIcon :name="isLiked ? 'like-filled' : 'like'" size="small" />
          {{ comment.likeCount }}
        </el-button>
        <el-button type="text" size="small" @click="handleReply">
          <el-icon><ChatDotRound /></el-icon>
          回复
        </el-button>
        <el-button
          v-if="canDelete"
          type="text"
          size="small"
          @click="handleDelete"
          :loading="deleteLoading"
        >
          <el-icon><Delete /></el-icon>
          删除
        </el-button>
      </div>
    </div>

    <div class="comment-content">
      {{ comment.content }}
    </div>

    <!-- Reply form -->
    <CommentForm
      v-if="showReplyForm"
      :article-id="comment.articleId"
      :parent-id="isReply ? rootId : comment.id"
      :reply-to-comment-id="comment.id"
      @submit="handleReplySubmit"
      @cancel="showReplyForm = false"
    />

    <!-- 二级回复：仅在顶层显示，不再继续递归 -->
    <div
      v-if="!isReply && comment.children && comment.children.length > 0"
      class="comment-children"
    >
      <CommentItem
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        :is-reply="true"
        :root-id="comment.id"
        :initial-liked="child.liked"
        @delete="handleChildDelete"
        @refresh="handleRefresh"
        @update:liked="(id, val) => $emit('update:liked', id, val)"
        @update:likeCount="(id, val) => $emit('update:likeCount', id, val)"
      />
    </div>
  </div>
</template><script setup lang="ts">
import { ref, computed, onUnmounted, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import { ChatDotRound, Delete } from "@element-plus/icons-vue";
import { useUserStore } from "../../store/user";
import { commentService } from "../../services/commentService";
import type { Comment } from "../../types/comment";
import CommentForm from "./CommentForm.vue";
import SvgIcon from "../SvgIcon.vue";
import { toast } from "@/composables/useLuminaToast";

const router = useRouter();

interface Props {
  comment: Comment;
  isReply?: boolean;
  rootId?: number;
  initialLiked?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isReply: false,
  rootId: undefined,
  initialLiked: false,
});

const emit = defineEmits<{
  (e: "delete", commentId: number): void;
  (e: "refresh"): void;
  (e: "update:liked", commentId: number, value: boolean): void;
  (e: "update:likeCount", commentId: number, value: number): void;
}>();

const userStore = useUserStore();
const showReplyForm = ref(false);
const likingLoading = ref(false);
const deleteLoading = ref(false);

// Initialize like status from prop, fallback to comment.liked
const isLiked = ref(props.initialLiked !== undefined ? props.initialLiked : (props.comment.liked || false));
const localLikeCount = ref(props.comment.likeCount);

// Watch for changes in initialLiked prop or comment.liked
watch(() => props.initialLiked, (newValue) => {
  if (newValue !== undefined) {
    isLiked.value = newValue;
  }
}, { immediate: true });

watch(() => props.comment.liked, (newValue) => {
  if (newValue !== undefined && props.initialLiked === undefined) {
    isLiked.value = newValue;
  }
});

// 防抖定时器
let debounceTimer: number | null = null;

const canDelete = computed(() => {
  return (
    userStore.isLoggedIn &&
    (userStore.userInfo?.id === props.comment.userId ||
      userStore.userInfo?.role === "admin")
  );
});

const formatTime = (time: string) => {
  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;

  return date.toLocaleDateString();
};

// 标记是否有待处理的 API 请求（用于防止快速连续点击）
let pendingApiCall = false;

const toggleLike = () => {
  // 如果正在加载或有待处理的 API 请求，忽略点击
  if (likingLoading.value || pendingApiCall) {
    return;
  }

  if (!userStore.isLoggedIn) {
    router.push("/login");
    return;
  }

  // 立即标记有待处理的请求，防止快速连续点击
  pendingApiCall = true;

  // 清除之前的定时器
  if (debounceTimer !== null) {
    clearTimeout(debounceTimer);
  }

  // 保存之前的状态（用于失败时回滚）
  const previousLiked = isLiked.value;
  const previousCount = localLikeCount.value;

  // 1. 立即更新 UI（真正的乐观更新）
  if (isLiked.value) {
    isLiked.value = false;
    localLikeCount.value = Math.max(0, localLikeCount.value - 1);
  } else {
    isLiked.value = true;
    localLikeCount.value = localLikeCount.value + 1;
  }
  // 立即同步状态到父组件
  emit('update:liked', props.comment.id, isLiked.value);
  emit('update:likeCount', props.comment.id, localLikeCount.value);

  // 2. 防抖发送 API 请求（避免频繁请求）
  debounceTimer = window.setTimeout(async () => {
    likingLoading.value = true;
    try {
      // 调用 API
      if (previousLiked) {
        await commentService.unlikeComment(props.comment.id);
        toast.success("取消点赞成功");
      } else {
        await commentService.likeComment(props.comment.id);
        toast.like("点赞成功");
      }
    } catch (error: any) {
      console.error("点赞操作失败:", error);
      // 回滚到之前的状态
      isLiked.value = previousLiked;
      localLikeCount.value = previousCount;
      emit('update:liked', props.comment.id, previousLiked);
      emit('update:likeCount', props.comment.id, previousCount);

      // 更详细的错误处理
      const status = error.response?.status;
      const errorCode = error.response?.data?.code;

      if (status === 401 || errorCode === 401) {
        // Token 过期，直接跳转登录页
        userStore.logout();
        router.push("/login");
      } else if (status === 403 || errorCode === 403) {
        toast.warning("没有权限执行此操作");
      } else if (status === 404 || errorCode === 404) {
        toast.error("评论不存在或已被删除");
        // 刷新评论列表
        emit("refresh");
      } else if (status === 409 || errorCode === 409) {
        toast.info("您已点赞过该评论");
      } else if (status >= 500) {
        toast.error("服务器错误，请稍后重试");
      } else if (error.response?.data?.message) {
        toast.error(error.response.data.message);
      } else if (error.message) {
        toast.error(`操作失败: ${error.message}`);
      } else {
        toast.error("网络错误，请检查网络连接后重试");
      }
    } finally {
      likingLoading.value = false;
      pendingApiCall = false;
      debounceTimer = null;
    }
  }, 300); // 300ms防抖
};

const handleReply = () => {
  if (!userStore.isLoggedIn) {
    router.push("/login");
    return;
  }
  showReplyForm.value = !showReplyForm.value;
};

const handleReplySubmit = () => {
  showReplyForm.value = false;
  emit("refresh");
};

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm("确定要删除这条评论吗？", "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消"
    });

    deleteLoading.value = true;
    try {
      await commentService.delete(props.comment.id);
      toast.success("删除成功", { duration: 2000 });
      emit("delete", props.comment.id);
    } catch (error: any) {
      // 详细错误处理
      const status = error.response?.status;
      const errorCode = error.response?.data?.code;

      if (status === 401 || errorCode === 401) {
        toast.error("登录已过期，请重新登录");
        userStore.logout();
      } else if (status === 403 || errorCode === 403) {
        toast.warning("没有权限删除此评论");
      } else if (status === 404 || errorCode === 404) {
        toast.error("评论不存在或已被删除");
        emit("refresh");
      } else if (status >= 500) {
        toast.error("服务器错误，请稍后重试");
      } else if (error.response?.data?.message) {
        toast.error(error.response.data.message);
      } else {
        toast.error("删除失败，请稍后重试");
      }
    }
  } catch (error) {
    // 用户取消删除操作
    if (error !== "cancel" && error !== "close") {
      console.error("删除确认框错误:", error);
    }
  } finally {
    deleteLoading.value = false;
  }
};

const handleChildDelete = (commentId: number) => {
  emit("delete", commentId);
};

const handleRefresh = () => {
  emit("refresh");
};

// 清理定时器
onUnmounted(() => {
  if (debounceTimer !== null) {
    clearTimeout(debounceTimer);
  }
});
</script>

<style scoped>
.comment-item {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.comment-item.is-reply {
  padding-left: 48px;
  background-color: #f7f7f7;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nickname {
  font-weight: 600;
  color: #333;
}
.reply-label {
  color: #999;
  margin: 0 4px;
}
.reply-target {
  color: #409eff;
}

.time {
  font-size: 12px;
  color: #999;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.comment-content {
  color: #666;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 12px;
}

.comment-children {
  margin-top: 12px;
}
</style>
