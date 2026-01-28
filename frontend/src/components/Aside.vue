<template>
  <aside class="aside">
    <!-- 文章榜单 -->
    <div class="aside-card">
      <h3 class="card-title">文章榜单</h3>
      <div class="rank-tabs">
        <div
          class="rank-tab"
          :class="{ active: rankTab === 'day' }"
          @click="
            rankTab = 'day';
            getHotArticles();
          "
        >
          今日
        </div>
        <div
          class="rank-tab"
          :class="{ active: rankTab === 'week' }"
          @click="
            rankTab = 'week';
            getHotArticles();
          "
        >
          本周
        </div>
      </div>
      <div v-if="hotArticlesLoading" class="loading-text">
        <i class="fas fa-spinner fa-spin"></i>
        加载中...
      </div>
      <div v-else-if="hotArticlesError" class="empty-text">
        <i class="fas fa-wifi"></i>
        网络异常，
        <el-button size="small" type="primary" @click="retryHotArticles"
          >重试</el-button
        >
      </div>
      <div v-else-if="hotArticles.length === 0" class="empty-text">
        <i class="fas fa-coffee"></i>
        暂无热门文章
      </div>
      <ul v-else class="rank-list">
        <li
          class="rank-item"
          v-for="(article, index) in hotArticles"
          :key="article.id"
        >
          <span class="rank-num" :class="`rank-${index + 1}`">{{
            index + 1
          }}</span>
          <router-link :to="`/article/${article.id}`" class="rank-title">
            {{ article.title }}
          </router-link>
        </li>
      </ul>
    </div>

    <!-- 作者榜 -->
    <div class="aside-card">
      <h3 class="card-title">作者榜</h3>
      <div v-if="authorsLoading" class="loading-text">
        <i class="fas fa-spinner fa-spin"></i>
        加载中...
      </div>
      <div v-else-if="authorsError" class="empty-text">
        <i class="fas fa-wifi"></i>
        网络异常或暂无数据
        <el-button size="small" type="primary" @click="retryTopAuthors"
          >重试</el-button
        >
      </div>
      <div v-else-if="topAuthors.length === 0" class="empty-text">
        <i class="fas fa-user-slash"></i>
        暂无数据
      </div>
      <div v-else class="author-list">
        <div class="author-item" v-for="author in topAuthors" :key="author.id">
          <router-link :to="`/user/${author.id}`" class="author-link">
            <el-avatar :size="36" :src="author.avatar || ''">
              {{ author.nickname?.charAt(0) || author.username?.charAt(0) }}
            </el-avatar>
            <div class="author-info">
              <div class="author-name">
                {{ author.nickname || author.username }}
              </div>
              <div class="author-stats">
                粉丝 {{ author.followerCount || 0 }}
              </div>
            </div>
          </router-link>
          <button
            v-if="userStore.userInfo?.id !== author.id"
            class="follow-btn"
            :class="{ followed: author.isFollowed }"
            @click.prevent="toggleFollow(author)"
          >
            {{ author.isFollowed ? "已关注" : "关注" }}
          </button>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from "vue";
import { useRouter } from "vue-router";
import { useUserStore } from "../store/user";
import { articleService } from "../services/articleService";
import { authorService, type Author } from "../services/authorService";
import type { Article } from "../types/article";
import { ElMessage } from "element-plus";
import axios from "../utils/axios";
import { withRetry } from "../utils/retry";

const router = useRouter();
const userStore = useUserStore();

// 榜单切换标签
const rankTab = ref<"day" | "week">("week");

// 热门文章数据
const hotArticles = ref<Article[]>([]);
const hotArticlesLoading = ref(false);
const hotArticlesError = ref(false);

// 作者排行榜数据
const topAuthors = ref<Author[]>([]);
const authorsLoading = ref(false);
const authorsError = ref(false);
const abortController = ref<AbortController | null>(null);

// 获取热门文章
const getHotArticles = async () => {
  hotArticlesLoading.value = true;
  hotArticlesError.value = false;
  abortController.value?.abort();
  abortController.value = new AbortController();
  try {
    const data = await withRetry(
      () =>
        articleService.getHotArticles(5, rankTab.value, {
          signal: abortController.value!.signal,
        }),
      (list) => Array.isArray(list),
      3,
      1000
    );
    hotArticles.value = Array.isArray(data) ? data : [];
  } catch (error: any) {
    if (error && error.code === "ERR_CANCELED") return;
    console.warn("获取热门文章失败:", error);
    hotArticlesError.value = true;
    hotArticles.value = [];
  } finally {
    hotArticlesLoading.value = false;
  }
};

const retryHotArticles = async () => {
  if (hotArticlesLoading.value) return;
  ElMessage.info("正在重试获取热门文章");
  await getHotArticles();
};

// 获取作者排行榜
const getTopAuthors = async () => {
  authorsLoading.value = true;
  authorsError.value = false;
  abortController.value?.abort();
  abortController.value = new AbortController();
  try {
    console.log("[Author] 开始获取作者排行榜...");
    const response = await authorService.getTopAuthors(10, {
      signal: abortController.value!.signal,
    });
    console.log("[Author] 原始响应:", response);
    console.log("[Author] 响应类型:", typeof response, Array.isArray(response));
    console.log("[Author] 响应长度:", response?.length);

    const data = Array.isArray(response) ? response : [];
    console.log("[Author] 处理后数据:", data);
    topAuthors.value = data;
  } catch (error: any) {
    if (error && error.code === "ERR_CANCELED") return;
    console.error("[Author] 获取作者排行榜失败:", error);
    authorsError.value = true;
    topAuthors.value = [];
  } finally {
    authorsLoading.value = false;
  }
};

const retryTopAuthors = async () => {
  if (authorsLoading.value) return;
  ElMessage.info("正在重试获取作者榜");
  await getTopAuthors();
};

// 切换关注状态
const toggleFollow = async (author: Author) => {
  console.log("切换关注状态:", author);
  // 检查是否登录
  if (!userStore.isLoggedIn) {
    console.log("用户未登录，跳转到登录页面");
    router.push("/login");
    return;
  }

  try {
    if (author.isFollowed) {
      // 取消关注
      console.log("取消关注作者:", author.id);
      await authorService.unfollow(author.id);

      // 更新本地状态
      author.isFollowed = false;
      author.followerCount = Math.max((author.followerCount || 0) - 1, 0);
      console.log("取消关注成功:", author);

      // 刷新当前登录用户信息以同步 followingCount
      try {
        const me = await axios.get("/user/info");
        userStore.setUserInfo(me as any);
      } catch (e) {
        console.warn("刷新用户信息失败:", e);
      }
    } else {
      // 关注
      console.log("关注作者:", author.id);
      await authorService.follow(author.id);

      // 更新本地状态
      author.isFollowed = true;
      author.followerCount = (author.followerCount || 0) + 1;
      console.log("关注成功:", author);

      // 刷新当前登录用户信息以同步 followingCount
      try {
        const me = await axios.get("/user/info");
        userStore.setUserInfo(me as any);
      } catch (e) {
        console.warn("刷新用户信息失败:", e);
      }
    }
  } catch (error: any) {
    console.error("关注操作失败:", error);
    ElMessage.error(error.response?.data?.message || "操作失败");

    // 如果是状态不同步错误，刷新作者列表
    const errorMessage = error?.response?.data?.message || error?.message || error?.toString() || '';
    if (errorMessage.includes('已关注') || errorMessage.includes('未关注')) {
      console.warn("检测到关注状态不同步，刷新作者列表");
      await getTopAuthors();
    }
  }
};

// 监听登录状态变化，重新获取作者列表
watch(
  () => userStore.isLoggedIn,
  (newVal, oldVal) => {
    if (newVal !== oldVal) {
      // 登录或登出时，重新获取作者列表以刷新关注状态
      getTopAuthors();
    }
  }
);

// 组件挂载时获取数据
onMounted(() => {
  getHotArticles();
  getTopAuthors();
});

onUnmounted(() => {
  abortController.value?.abort();
});
</script>

<style scoped>
.aside {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.aside-card {
  background-color: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  transition: all var(--duration-normal) var(--ease-default);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

.card-title {
  font-family: var(--font-serif);
  font-size: var(--text-xl);
  font-weight: 600;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

/* 榜单切换标签 */
.rank-tabs {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
  border-bottom: 1px solid var(--border-color);
}

.rank-tab {
  padding: var(--space-2) 0;
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  position: relative;
}

.rank-tab:hover {
  color: var(--text-primary);
}

.rank-tab.active {
  color: var(--color-blue-500);
  font-weight: 600;
}

.rank-tab.active::after {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background-color: var(--color-blue-500);
}

/* 榜单列表 */
.rank-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.rank-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border-color);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-item:hover {
  background-color: var(--bg-secondary);
}

.rank-num {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-tertiary);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.rank-num.rank-1 {
  background-color: #ff6b6b;
  color: white;
}

.rank-num.rank-2 {
  background-color: #ffa502;
  color: white;
}

.rank-num.rank-3 {
  background-color: #ffd700;
  color: white;
}

.rank-title {
  flex: 1;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--duration-fast) var(--ease-default);
}

.rank-title:hover {
  color: var(--color-blue-500);
  text-decoration: underline;
  text-underline-offset: 2px;
}

/* 作者列表 */
.author-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.author-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.author-item:hover {
  background-color: var(--bg-secondary);
}

.author-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
  text-decoration: none;
  min-width: 0;
}

.author-info {
  flex: 1;
  min-width: 0;
}

.author-name {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color var(--duration-fast) var(--ease-default);
}

.author-link:hover .author-name {
  color: var(--color-blue-500);
}

.author-stats {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-1);
}

.follow-btn {
  padding: var(--space-1) var(--space-3);
  font-size: var(--text-xs);
  font-weight: 500;
  border: 1px solid var(--color-blue-500);
  background-color: transparent;
  color: var(--color-blue-500);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  white-space: nowrap;
}

.follow-btn:hover:not(:disabled) {
  background-color: var(--color-blue-500);
  color: white;
  transform: translateY(-1px);
}

.follow-btn:active:not(:disabled) {
  transform: translateY(0);
}

.follow-btn.followed {
  background-color: var(--bg-secondary);
  border-color: var(--border-color);
  color: var(--text-secondary);
}

/* 已关注状态样式：禁用点击，保持灰色 */
.follow-btn:disabled {
  cursor: not-allowed;
  opacity: 0.8;
  border-color: var(--border-color);
  background-color: var(--bg-secondary);
  color: var(--text-tertiary);
}

.follow-btn.followed:hover {
  /* 覆盖之前的红色样式，保持禁用状态 */
  border-color: var(--border-color);
  color: var(--text-tertiary);
  background-color: var(--bg-secondary);
}

/* 加载和空状态 */
.loading-text,
.empty-text {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  text-align: center;
  padding: var(--space-8) var(--space-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.loading-text i,
.empty-text i {
  font-size: var(--text-3xl);
  color: var(--text-disabled);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .aside-card {
    padding: var(--space-4);
  }

  .card-title {
    font-size: var(--text-lg);
  }
}
</style>
