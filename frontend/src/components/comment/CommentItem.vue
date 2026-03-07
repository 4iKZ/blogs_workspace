<template>
  <div class="comment-item" :class="{ 'is-reply': isReply }">
    <div class="comment-avatar">
      <el-avatar
        :size="isReply ? 32 : 40"
        :src="comment.avatar || '/default-avatar.png'"
      >
        {{ (comment.nickname || '').charAt(0) || '匿' }}
      </el-avatar>
    </div>

    <div class="comment-main">
      <div class="comment-card">
        <div class="comment-header">
          <div class="user-details">
            <span class="nickname">
              <template v-if="isReply">
                {{ comment.nickname }}
                <span class="reply-label">回复</span>
                <span class="reply-target">{{ comment.replyToNickname || '原评论' }}</span>
              </template>
              <template v-else>
                {{ comment.nickname }}
              </template>
            </span>
          </div>
        </div>

        <div class="comment-content">
          {{ comment.content }}
        </div>

        <div class="comment-actions comment-action-bar">
          <time class="action-time">{{ formatTime(comment.createTime) }}</time>

          <button
            :class="['comment-action-btn', 'like-btn', { active: isLiked }]"
            @click="toggleLike"
            :disabled="likingLoading"
          >
            <svg class="like-icon" viewBox="0 0 24 24" fill="none">
              <path
                d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
                fill="currentColor"
              />
            </svg>
            <span>{{ localLikeCount }}</span>
          </button>

          <button class="comment-action-btn reply-btn" @click="handleReply">
            <el-icon><ChatDotRound /></el-icon>
            <span>回复</span>
          </button>

          <button
            v-if="canDelete"
            class="comment-action-btn delete-btn"
            @click="handleDelete"
            :disabled="deleteLoading"
          >
            <el-icon><Delete /></el-icon>
            <span>删除</span>
          </button>
        </div>
      </div>

      <CommentForm
        v-if="showReplyForm"
        :article-id="comment.articleId"
        :parent-id="isReply ? rootId : comment.id"
        :reply-to-comment-id="comment.id"
        @submit="handleReplySubmit"
        @cancel="showReplyForm = false"
      />

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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessageBox } from "element-plus";
import { ChatDotRound, Delete } from "@element-plus/icons-vue";
import { useUserStore } from "../../store/user";
import { commentService } from "../../services/commentService";
import type { Comment } from "../../types/comment";
import CommentForm from "./CommentForm.vue";
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

const isLiked = ref(
  props.initialLiked !== undefined ? props.initialLiked : (props.comment.liked || false)
);
const localLikeCount = ref(props.comment.likeCount);

watch(
  () => props.initialLiked,
  (newValue) => {
    if (newValue !== undefined) {
      isLiked.value = newValue;
    }
  },
  { immediate: true }
);

watch(() => props.comment.liked, (newValue) => {
  if (newValue !== undefined && props.initialLiked === undefined) {
    isLiked.value = newValue;
  }
});

watch(() => props.comment.likeCount, (newValue) => {
  if (typeof newValue === "number") {
    localLikeCount.value = newValue;
  }
});

let debounceTimer: number | null = null;
let pendingApiCall = false;

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

const toggleLike = () => {
  if (likingLoading.value || pendingApiCall) {
    return;
  }

  if (!userStore.isLoggedIn) {
    router.push("/login");
    return;
  }

  pendingApiCall = true;

  if (debounceTimer !== null) {
    clearTimeout(debounceTimer);
  }

  const previousLiked = isLiked.value;
  const previousCount = localLikeCount.value;

  if (isLiked.value) {
    isLiked.value = false;
    localLikeCount.value = Math.max(0, localLikeCount.value - 1);
  } else {
    isLiked.value = true;
    localLikeCount.value = localLikeCount.value + 1;
  }

  emit("update:liked", props.comment.id, isLiked.value);
  emit("update:likeCount", props.comment.id, localLikeCount.value);

  debounceTimer = window.setTimeout(async () => {
    likingLoading.value = true;
    try {
      if (previousLiked) {
        await commentService.unlikeComment(props.comment.id);
        toast.success("取消点赞成功");
      } else {
        await commentService.likeComment(props.comment.id);
        toast.like("点赞成功");
      }
    } catch (error: any) {
      console.error("点赞操作失败:", error);
      isLiked.value = previousLiked;
      localLikeCount.value = previousCount;
      emit("update:liked", props.comment.id, previousLiked);
      emit("update:likeCount", props.comment.id, previousCount);

      const status = error.response?.status;
      const errorCode = error.response?.data?.code;

      if (status === 401 || errorCode === 401) {
        userStore.logout();
        router.push("/login");
      } else if (status === 403 || errorCode === 403) {
        toast.warning("没有权限执行此操作");
      } else if (status === 404 || errorCode === 404) {
        toast.error("评论不存在或已被删除");
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
  }, 300);
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

onUnmounted(() => {
  if (debounceTimer !== null) {
    clearTimeout(debounceTimer);
  }
});
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid var(--border-color);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  flex-shrink: 0;
  padding-top: 2px;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-card {
  min-width: 0;
}

.comment-header {
  margin-bottom: 10px;
}

.user-details {
  min-width: 0;
}

.nickname {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.55;
  word-break: break-word;
}

.reply-label {
  color: var(--text-tertiary);
  margin: 0 4px;
  font-weight: 400;
}

.reply-target {
  color: var(--color-blue-500);
}

.comment-content {
  color: var(--text-secondary);
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 12px;
  font-size: 14px;
}

.comment-actions {
  display: flex;
  gap: 14px;
}

.comment-action-bar {
  align-items: center;
  flex-wrap: wrap;
}

.action-time {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-right: 2px;
}

.comment-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-default), opacity var(--duration-fast) var(--ease-default);
  outline: none;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.comment-action-btn:hover:not(:disabled) {
  color: var(--color-blue-500);
}

.comment-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.like-btn .like-icon {
  width: 16px;
  height: 16px;
  color: currentColor;
  transition: transform var(--duration-normal) var(--ease-spring);
}

.like-btn.active {
  color: var(--color-blue-500);
}

.like-btn.active .like-icon {
  animation: heart-pop-mini 0.4s var(--ease-spring);
}

@keyframes heart-pop-mini {
  0% { transform: scale(1); }
  25% { transform: scale(1.3); }
  50% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

.delete-btn:hover:not(:disabled) {
  color: var(--color-rose-500);
}

.comment-action-btn .el-icon {
  font-size: 15px;
}

.comment-main :deep(.comment-form) {
  margin-top: 12px;
}

.comment-children {
  margin-top: 14px;
  padding-left: 12px;
  border-left: 2px solid rgba(59, 130, 246, 0.12);
}

.comment-item.is-reply {
  padding: 14px 0 0;
  border-bottom: none;
}

.comment-item.is-reply .comment-card {
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--bg-secondary);
}

.comment-item.is-reply .comment-content {
  margin-bottom: 10px;
  font-size: 13px;
}

.dark .comment-item.is-reply .comment-card {
  background: var(--bg-secondary);
}

.dark .like-btn.active {
  color: var(--color-blue-400);
}

.dark .comment-action-btn:hover:not(:disabled) {
  color: var(--color-blue-400);
}

.dark .delete-btn:hover:not(:disabled) {
  color: var(--color-rose-400);
}

@media (max-width: 768px) {
  .comment-item {
    gap: 10px;
    padding: 14px 0;
  }

  .comment-avatar {
    padding-top: 0;
  }

  .comment-item.is-reply {
    padding-top: 12px;
  }

  .user-details {
    width: 100%;
  }

  .nickname {
    font-size: 12px;
    max-width: 100%;
    overflow: hidden;
    white-space: normal;
    word-break: break-word;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .reply-label,
  .reply-target {
    font-size: 11px;
    word-break: break-word;
  }

  .comment-content {
    font-size: 13px;
    line-height: 1.65;
    margin-bottom: 10px;
  }

  .comment-item.is-reply .comment-card {
    padding: 10px 12px;
    border-radius: 12px;
  }

  .comment-action-bar {
    gap: 10px;
    flex-wrap: wrap;
  }

  .action-time {
    font-size: 11px;
  }

  .comment-action-btn {
    font-size: 12px;
  }

  .like-btn .like-icon {
    width: 14px;
    height: 14px;
  }

  .comment-action-btn .el-icon {
    font-size: 14px;
  }

  .comment-children {
    margin-top: 10px;
    padding-left: 10px;
  }
}

@media (max-width: 480px) {
  .comment-item {
    gap: 8px;
    padding: 12px 0;
  }

  .comment-item.is-reply .comment-card {
    padding: 10px;
  }

  .nickname {
    font-size: 11.5px;
  }

  .comment-content {
    font-size: 12.5px;
  }

  .comment-action-bar {
    gap: 8px 10px;
  }

  .comment-action-btn {
    font-size: 11px;
  }
}
</style>
