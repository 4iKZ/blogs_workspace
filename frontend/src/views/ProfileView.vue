<template>
  <Layout :show-left-sidebar="false">
    <div class="profile-container">
      <!-- 1. User Info Header -->
      <div class="user-header shadow-sm">
        <div class="user-info-block">
          <div class="avatar-section">
            <el-avatar :size="96" :src="userInfo.avatar || ''" class="avatar">
              {{ userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0) }}
            </el-avatar>
          </div>
          <div class="info-content">
            <h1 class="username">
              {{ userInfo.nickname || userInfo.username }}
            </h1>
            <div class="position-info">
              <SvgIcon name="user" size="14px" />
              <span>{{ userInfo.position || "职位未填写" }}</span>
              <span class="divider">|</span>
              <span>{{ userInfo.company || "公司未填写" }}</span>
            </div>
            <div class="intro">
              {{ userInfo.bio || "这个用户很懒，什么都没写" }}
            </div>
          </div>
          <div class="action-section">
            <el-button class="settings-btn" plain @click="showSettings = true">
              <SvgIcon name="settings" size="16px" style="margin-right: 6px" />
              设置
            </el-button>
          </div>
        </div>
      </div>

      <!-- 2. Main Navigation Tabs -->
      <div class="main-content shadow-sm">
        <el-tabs
          v-model="activeMainTab"
          class="profile-tabs"
          @tab-change="handleMainTabChange"
        >
          <!-- Articles Tab -->
          <el-tab-pane label="文章" name="articles">
            <div v-if="loadingArticles" class="loading">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else-if="userArticles.length > 0" class="articles-list">
              <div
                class="article-item"
                v-for="article in userArticles"
                :key="article.id"
              >
                <div v-if="article.coverImage" class="article-cover">
                  <img :src="article.coverImage" :alt="article.title" />
                </div>
                <div class="article-item-content">
                  <h4 class="article-item-title">
                    <router-link :to="`/article/${article.id}`">{{
                      article.title
                    }}</router-link>
                    <el-tag
                      v-if="article.status === 1"
                      type="info"
                      size="small"
                      class="status-tag"
                      >草稿</el-tag
                    >
                    <el-tag
                      v-else-if="article.status === 2"
                      type="success"
                      size="small"
                      class="status-tag"
                      >已发布</el-tag
                    >
                  </h4>
                  <div v-if="article.summary" class="article-item-summary">
                    {{ article.summary }}
                  </div>
                  <div class="article-item-meta">
                    <span v-if="article.status === 2 && article.publishTime"
                      >发布于 {{ formatDate(article.publishTime) }}</span
                    >
                    <span v-else
                      >创建于 {{ formatDate(article.createTime) }}</span
                    >
                    <span v-if="article.status === 2" class="meta-stats"
                      >浏览 {{ article.viewCount }} · 点赞
                      {{ article.likeCount }} · 评论
                      {{ article.commentCount }}</span
                    >
                  </div>
                </div>
                <div class="article-item-actions">
                  <el-button
                    type="primary"
                    link
                    @click="editArticle(article.id)"
                    class="action-btn"
                  >
                    <SvgIcon name="edit" size="14px" /> 编辑
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    @click="deleteArticle(article.id)"
                    class="action-btn"
                  >
                    <SvgIcon name="delete" size="14px" /> 删除
                  </el-button>
                </div>
              </div>
            </div>
            <div v-else class="empty">
              <el-empty description="暂无文章">
                <router-link to="/article/create">
                  <el-button type="primary">开始创作</el-button>
                </router-link>
              </el-empty>
            </div>
          </el-tab-pane>

          <!-- Dynamic Tab (Placeholder) -->
          <el-tab-pane label="动态" name="dynamic">
            <div class="empty">
              <el-empty description="暂无动态" />
            </div>
          </el-tab-pane>

          <!-- Favorites Tab -->
          <el-tab-pane label="收藏" name="favorites">
            <div v-if="loadingFavorited" class="loading">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else-if="favoritedArticles.length > 0" class="articles-list">
              <div
                class="article-item"
                v-for="item in favoritedArticles"
                :key="item.favoriteId"
              >
                <div v-if="item.article.coverImage" class="article-cover">
                  <img
                    :src="item.article.coverImage"
                    :alt="item.article.title"
                  />
                </div>
                <div class="article-item-content">
                  <h4 class="article-item-title">
                    <router-link :to="`/article/${item.article.id}`">{{
                      item.article.title
                    }}</router-link>
                  </h4>
                  <div class="article-item-meta">
                    <span>收藏于 {{ formatDate(item.createdAt) }}</span>
                    <span class="meta-stats"
                      >浏览 {{ item.article.viewCount }} · 点赞
                      {{ item.article.likeCount }}</span
                    >
                  </div>
                </div>
                <div class="article-item-actions">
                  <el-button
                    type="warning"
                    link
                    @click="unfavoriteArticle(item.articleId)"
                    class="action-btn"
                    >取消收藏</el-button
                  >
                </div>
              </div>
              <div
                v-if="favoritedTotal > favoritedArticles.length"
                class="load-more"
              >
                <el-button
                  @click="loadMoreFavorited"
                  :loading="loadingFavorited"
                  class="load-more-btn"
                  >加载更多</el-button
                >
              </div>
            </div>
            <div v-else class="empty">
              <el-empty description="暂无收藏的文章" />
            </div>
          </el-tab-pane>

          <!-- Liked Tab -->
          <el-tab-pane label="赞过的文章" name="liked">
            <div v-if="loadingLiked" class="loading">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else-if="likedArticles.length > 0" class="articles-list">
              <div
                class="article-item"
                v-for="item in likedArticles"
                :key="item.id"
              >
                <div v-if="item.article.coverImage" class="article-cover">
                  <img
                    :src="item.article.coverImage"
                    :alt="item.article.title"
                  />
                </div>
                <div class="article-item-content">
                  <h4 class="article-item-title">
                    <router-link :to="`/article/${item.article.id}`">{{
                      item.article.title
                    }}</router-link>
                  </h4>
                  <div class="article-item-meta">
                    <span>点赞于 {{ formatDate(item.createdAt) }}</span>
                    <span class="meta-stats"
                      >浏览 {{ item.article.viewCount }} · 点赞
                      {{ item.article.likeCount }}</span
                    >
                  </div>
                </div>
                <div class="article-item-actions">
                  <el-button
                    type="danger"
                    link
                    @click="unlikeArticle(item.articleId)"
                    class="action-btn"
                    >取消点赞</el-button
                  >
                </div>
              </div>
              <div v-if="likedTotal > likedArticles.length" class="load-more">
                <el-button
                  @click="loadMoreLiked"
                  :loading="loadingLiked"
                  class="load-more-btn"
                  >加载更多</el-button
                >
              </div>
            </div>
            <div v-else class="empty">
              <el-empty description="暂无点赞的文章" />
            </div>
          </el-tab-pane>

          <!-- Following Tab -->
          <el-tab-pane label="关注的人" name="following">
            <div v-if="loadingFollowing" class="loading">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else-if="followingUsers.length > 0" class="user-list">
              <div class="user-item" v-for="u in followingUsers" :key="u.id">
                <router-link :to="`/user/${u.id}`" class="user-link">
                  <el-avatar :size="44" :src="u.avatar || ''">
                    {{ u.nickname?.charAt(0) || u.username?.charAt(0) }}
                  </el-avatar>
                  <div class="user-info-text">
                    <div class="user-name">{{ u.nickname || u.username }}</div>
                    <div class="user-stats">
                      粉丝 {{ u.followerCount || 0 }}
                    </div>
                  </div>
                </router-link>
              </div>
            </div>
            <div v-else class="empty">
              <el-empty description="暂无关注的人" />
            </div>
          </el-tab-pane>

          <!-- Followers Tab -->
          <el-tab-pane label="粉丝" name="followers">
            <div v-if="loadingFollowers" class="loading">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else-if="followerUsers.length > 0" class="user-list">
              <div class="user-item" v-for="u in followerUsers" :key="u.id">
                <router-link :to="`/user/${u.id}`" class="user-link">
                  <el-avatar :size="44" :src="u.avatar || ''">
                    {{ u.nickname?.charAt(0) || u.username?.charAt(0) }}
                  </el-avatar>
                  <div class="user-info-text">
                    <div class="user-name">{{ u.nickname || u.username }}</div>
                    <div class="user-stats">
                      粉丝 {{ u.followerCount || 0 }}
                    </div>
                  </div>
                </router-link>
              </div>
            </div>
            <div v-else class="empty">
              <el-empty description="暂无粉丝" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 3. Settings Dialog -->
      <el-dialog
        v-model="showSettings"
        title="设置"
        width="520px"
        destroy-on-close
        class="settings-dialog"
      >
        <el-tabs>
          <el-tab-pane label="个人资料">
            <el-form
              ref="userInfoFormRef"
              :model="userInfo"
              :rules="userInfoRules"
              label-width="80px"
              class="setting-form"
            >
              <div class="avatar-edit-section">
                <el-avatar :size="64" :src="userInfo.avatar || ''">
                  {{
                    userInfo.nickname?.charAt(0) || userInfo.username?.charAt(0)
                  }}
                </el-avatar>
                <el-button type="primary" link @click="showAvatarUpload = true"
                  >修改头像</el-button
                >
              </div>
              <el-form-item label="用户名">
                <el-input v-model="userInfo.username" disabled />
              </el-form-item>
              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="userInfo.nickname" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="userInfo.email" />
              </el-form-item>
              <el-form-item label="简介" prop="bio">
                <el-input v-model="userInfo.bio" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item label="职位" prop="position">
                <el-input v-model="userInfo.position" />
              </el-form-item>
              <el-form-item label="公司" prop="company">
                <el-input v-model="userInfo.company" />
              </el-form-item>
              <el-form-item label="网站" prop="website">
                <el-input v-model="userInfo.website" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleUpdateUserInfo"
                  >保存修改</el-button
                >
              </el-form-item>
            </el-form>
          </el-tab-pane>
          <el-tab-pane label="修改密码">
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-width="100px"
              class="setting-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  show-password
                  placeholder="请输入当前密码"
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  show-password
                  placeholder="请输入新密码"
                />
                <div class="password-requirements">
                  <p class="requirements-title">密码要求：</p>
                  <ul class="requirements-list">
                    <li>长度 8-20 位</li>
                    <li>至少一个大写字母</li>
                    <li>至少一个小写字母</li>
                    <li>至少一个数字</li>
                    <li>至少一个特殊字符 (!@#$%^&*(),.?:{}|&lt;&gt;)</li>
                  </ul>
                </div>
              </el-form-item>
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入新密码"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  @click="handleChangePassword"
                  :loading="changingPassword"
                  >确认修改</el-button
                >
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-dialog>

      <!-- Avatar Upload Dialog -->
      <el-dialog v-model="showAvatarUpload" title="修改头像" width="420px">
        <el-upload
          class="avatar-uploader"
          :show-file-list="false"
          :on-change="handleAvatarUpload"
          :before-upload="beforeAvatarUpload"
          :auto-upload="false"
          drag
        >
          <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持JPG/PNG格式，文件大小不超过2MB</div>
          </template>
        </el-upload>
      </el-dialog>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import Layout from "../components/Layout.vue";
import SvgIcon from "../components/SvgIcon.vue";
import { articleService } from "../services/articleService";
import { authorService, type Author } from "../services/authorService";
import type { UpdateUserInfoRequest } from "../types/user";
import type { UserLike, UserFavorite } from "../types/comment";
import axios from "../utils/axios";
import { useUserStore } from "@/store/user";

const router = useRouter();
const userStore = useUserStore();

// 用户信息
const userInfo = ref({
  id: 0,
  username: "",
  email: "",
  nickname: "",
  avatar: "",
  bio: "",
  website: "",
  position: "",
  company: "",
  role: "",
  createTime: "",
});

// 状态控制
const showSettings = ref(false);
const userInfoFormRef = ref();
const changingPassword = ref(false);
const showAvatarUpload = ref(false);
const uploadingAvatar = ref(false);

// 标签页控制
const activeMainTab = ref("articles");

// 表单验证规则 - 为所有字段添加验证规则，确保表单状态正确更新
const userInfoRules = {
  nickname: [
    { max: 20, message: "昵称长度不能超过20个字符", trigger: "blur" }
  ],
  email: [
    { type: "email", message: "请输入正确的邮箱地址", trigger: "blur" }
  ],
  bio: [
    { max: 500, message: "简介长度不能超过500个字符", trigger: "blur" },
    { validator: (_rule: any, value: any, callback: any) => {
        // 允许空值，但如果输入了内容则检查长度
        if (value && value.length > 500) {
          callback(new Error('简介长度不能超过500个字符'));
        } else {
          callback();
        }
      }, trigger: 'blur' }
  ],
  website: [
    { type: "url", message: "请输入正确的网址格式（以http://或https://开头）", trigger: "blur" },
    { validator: (_rule: any, value: any, callback: any) => {
        // 允许空值
        if (!value) {
          callback();
          return;
        }
        // 简单的URL格式检查
        const urlPattern = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
        if (!urlPattern.test(value)) {
          callback(new Error('请输入正确的网址格式'));
        } else {
          callback();
        }
      }, trigger: 'blur' }
  ],
  position: [
    { max: 50, message: "职位长度不能超过50个字符", trigger: "blur" }
  ],
  company: [
    { max: 100, message: "公司名称长度不能超过100个字符", trigger: "blur" },
    { validator: (_rule: any, _value: any, callback: any) => {
        // 允许空值，触发验证以确保表单状态更新
        callback();
      }, trigger: ['blur', 'change'] }
  ],
};

// 密码修改表单
const passwordForm = ref({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const passwordFormRef = ref();

const passwordRules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    {
      min: 8,
      max: 20,
      message: "密码长度必须在 8-20 位之间",
      trigger: "blur",
    },
    {
      pattern: /[A-Z]/,
      message: "密码必须包含至少一个大写字母",
      trigger: "blur",
    },
    {
      pattern: /[a-z]/,
      message: "密码必须包含至少一个小写字母",
      trigger: "blur",
    },
    {
      pattern: /[0-9]/,
      message: "密码必须包含至少一个数字",
      trigger: "blur",
    },
    {
      pattern: /[!@#$%^&*(),.?":{}|<>]/,
      message: "密码必须包含至少一个特殊字符",
      trigger: "blur",
    },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    ({ value, form }: { value: string; form: typeof passwordForm.value }) => {
      if (value !== form.newPassword) {
        return new Error("两次输入的密码不一致");
      }
      return true;
    },
  ],
};

// 我的文章
const userArticles = ref<any[]>([]);
const loadingArticles = ref(false);

// 关注/粉丝列表
const followingUsers = ref<Author[]>([]);
const followerUsers = ref<Author[]>([]);
const loadingFollowing = ref(false);
const loadingFollowers = ref(false);

// 点赞的文章
const likedArticles = ref<UserLike[]>([]);
const loadingLiked = ref(false);
const likedPage = ref(1);
const likedTotal = ref(0);

// 收藏的文章
const favoritedArticles = ref<UserFavorite[]>([]);
const loadingFavorited = ref(false);
const favoritedPage = ref(1);
const favoritedTotal = ref(0);

// 获取用户信息
const getUserInfo = async () => {
  try {
    const response = await axios.get("/user/info");
    userInfo.value = response;
  } catch (error) {
    console.error("获取用户信息失败:", error);
    ElMessage.error("获取用户信息失败");
  }
};

// 获取用户文章
const getUserArticles = async () => {
  loadingArticles.value = true;
  try {
    const response = await axios.get("/article/user/" + userInfo.value.id);
    userArticles.value = response.items || response;
  } catch (error: any) {
    console.error("获取用户文章失败:", error);
    ElMessage.error(error.response?.data?.message || "加载文章失败");
  } finally {
    loadingArticles.value = false;
  }
};

// 获取点赞的文章
const getLikedArticles = async (loadMore = false) => {
  loadingLiked.value = true;
  try {
    const response = await articleService.getUserLikedArticles(
      loadMore ? likedPage.value + 1 : 1,
      10
    );

    // 直接使用 UserLikeDTO 数据，保持完整结构（包含 id, userId, articleId, createdAt, article）
    const rawItems = response.items || [];
    // 过滤掉 article 为 undefined 的项，并强制转换为 UserLike 类型
    const items = rawItems.filter(item => item.article) as unknown as UserLike[];

    if (loadMore) {
      likedArticles.value.push(...items);
      likedPage.value++;
    } else {
      likedArticles.value = items;
      likedPage.value = 1;
    }

    likedTotal.value = response.total;
  } catch (error: any) {
    console.error("获取点赞文章失败:", error);
    ElMessage.error(error.response?.data?.message || "加载失败");
  } finally {
    loadingLiked.value = false;
  }
};

// 获取收藏的文章
const getFavoritedArticles = async (loadMore = false) => {
  loadingFavorited.value = true;
  try {
    const response = await articleService.getUserFavorites(
      loadMore ? favoritedPage.value + 1 : 1,
      10
    );

    const items = response?.items || [];
    const total = response?.total || 0;

    if (loadMore) {
      favoritedArticles.value.push(...items);
      favoritedPage.value++;
    } else {
      favoritedArticles.value = items;
      favoritedPage.value = 1;
    }

    favoritedTotal.value = total;
  } catch (error: any) {
    console.error("获取收藏文章失败:", error);
    ElMessage.error(error.response?.data?.message || "加载失败");
  } finally {
    loadingFavorited.value = false;
  }
};

// 获取关注列表
const getFollowings = async () => {
  loadingFollowing.value = true;
  try {
    const list = await authorService.getFollowings(1, 10);
    followingUsers.value = list;
  } catch (error: any) {
    console.error("获取关注列表失败:", error);
    ElMessage.error(error.response?.data?.message || "加载失败");
  } finally {
    loadingFollowing.value = false;
  }
};

// 获取粉丝列表
const getFollowers = async () => {
  loadingFollowers.value = true;
  try {
    const list = await authorService.getFollowers(1, 10);
    followerUsers.value = list;
  } catch (error: any) {
    console.error("获取粉丝列表失败:", error);
    ElMessage.error(error.response?.data?.message || "加载失败");
  } finally {
    loadingFollowers.value = false;
  }
};

// 主标签页切换
const handleMainTabChange = (tabName: string | number) => {
  if (tabName === "articles" && userArticles.value.length === 0) {
    getUserArticles();
  } else if (tabName === "favorites" && favoritedArticles.value.length === 0) {
    getFavoritedArticles();
  } else if (tabName === "liked" && likedArticles.value.length === 0) {
    getLikedArticles();
  } else if (tabName === "following" && followingUsers.value.length === 0) {
    getFollowings();
  } else if (tabName === "followers" && followerUsers.value.length === 0) {
    getFollowers();
  }
};

// 加载更多点赞
const loadMoreLiked = () => {
  getLikedArticles(true);
};

// 加载更多收藏
const loadMoreFavorited = () => {
  getFavoritedArticles(true);
};

// 取消点赞
const unlikeArticle = async (articleId: number) => {
  try {
    await articleService.unlikeArticle(articleId);
    ElMessage.success("已取消点赞");
    likedArticles.value = likedArticles.value.filter(
      (item) => item.articleId !== articleId
    );
  } catch (error: any) {
    console.error("取消点赞失败:", error);
    ElMessage.error(error.response?.data?.message || "操作失败");
  }
};

// 取消收藏
const unfavoriteArticle = async (articleId: number) => {
  try {
    await articleService.unfavoriteArticle(articleId);
    ElMessage.success("已取消收藏");
    favoritedArticles.value = favoritedArticles.value.filter(
      (item) => item.articleId !== articleId
    );
  } catch (error: any) {
    console.error("取消收藏失败:", error);
    ElMessage.error(error.response?.data?.message || "操作失败");
  }
};

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleString();
};

// 更新用户信息
const handleUpdateUserInfo = async () => {
  try {
    await userInfoFormRef.value.validate();

    // 辅助函数：将空字符串转换为 null，空格字符串转换为 null
    const toNullIfEmpty = (value: string | undefined | null): string | null => {
      if (value === undefined || value === null) {
        return null;
      }
      const trimmed = value.trim();
      return trimmed === "" ? null : trimmed;
    };

    // 构建更新数据，空字符串转为 null 以便后端正确处理清空操作
    const updateData: UpdateUserInfoRequest = {
      nickname: toNullIfEmpty(userInfo.value.nickname) || undefined,
      email: toNullIfEmpty(userInfo.value.email) || undefined,
      bio: toNullIfEmpty(userInfo.value.bio) || undefined,
      website: toNullIfEmpty(userInfo.value.website) || undefined,
      position: toNullIfEmpty(userInfo.value.position) || undefined,
      company: toNullIfEmpty(userInfo.value.company) || undefined,
    };

    // 移除 undefined 字段，只发送有值的字段
    const cleanData = Object.fromEntries(
      Object.entries(updateData).filter(([_, v]) => v !== undefined)
    ) as UpdateUserInfoRequest;

    await axios.put("/user/info", cleanData);
    ElMessage.success("个人信息更新成功");
    showSettings.value = false;

    // 刷新显示，确保前端展示最新数据
    await getUserInfo();

    // 同步更新 localStorage 中的用户信息
    const userInfoStr = localStorage.getItem("userInfo");
    if (userInfoStr) {
      try {
        const storedUserInfo = JSON.parse(userInfoStr);
        // 合并更新后的数据
        const updatedUserInfo = { ...storedUserInfo, ...userInfo.value };
        localStorage.setItem("userInfo", JSON.stringify(updatedUserInfo));
      } catch (e) {
        console.warn("同步本地用户信息失败:", e);
      }
    }
  } catch (error: any) {
    console.error("更新个人信息失败:", error);
    ElMessage.error(error.message || "更新失败");
  }
};

// 修改密码
const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate();

    changingPassword.value = true;

    await axios.put("/user/password", {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    });

    ElMessage.success("密码修改成功");

    // 重置表单
    passwordForm.value = {
      oldPassword: "",
      newPassword: "",
      confirmPassword: "",
    };
    showSettings.value = false;
  } catch (error: any) {
    console.error("修改密码失败:", error);
    // 优先显示后端返回的错误信息
    const errorMessage = error.response?.data?.message || error.message || "修改密码失败";
    ElMessage.error(errorMessage);
  } finally {
    changingPassword.value = false;
  }
};

// 编辑文章
const editArticle = (articleId: number) => {
  router.push(`/article/edit/${articleId}`);
};

// 删除文章
const deleteArticle = (articleId: number) => {
  ElMessageBox.confirm("确定要删除这篇文章吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await axios.delete(`/article/${articleId}`);
        ElMessage.success("文章删除成功");
        getUserArticles();
      } catch (error: any) {
        console.error("删除文章失败:", error);
        ElMessage.error(error.message || "删除失败");
      }
    })
    .catch(() => {
      // 取消删除
    });
};

// 处理头像上传
const handleAvatarUpload = async (file: any) => {
  const formData = new FormData();
  formData.append("file", file.raw);

  uploadingAvatar.value = true;
  try {
    const response = await axios.post("/user/avatar/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });

    const avatarUrl = response;

    // 更新用户头像
    await axios.put("/user/info", { avatar: avatarUrl });

    userInfo.value.avatar = avatarUrl;

    // 更新localStorage中的用户信息
    const userInfoStr = localStorage.getItem("userInfo");
    if (userInfoStr) {
      const storedUserInfo = JSON.parse(userInfoStr);
      storedUserInfo.avatar = avatarUrl;
      localStorage.setItem("userInfo", JSON.stringify(storedUserInfo));
    }

    ElMessage.success("头像上传成功");
    showAvatarUpload.value = false;
  } catch (error: any) {
    console.error("头像上传失败:", error);
    ElMessage.error(error.response?.data?.message || "头像上传失败");
  } finally {
    uploadingAvatar.value = false;
  }
};

// 头像文件验证
const beforeAvatarUpload = (file: any) => {
  const isImage = file.type.startsWith("image/");
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isImage) {
    ElMessage.error("只能上传图片文件！");
    return false;
  }
  if (!isLt2M) {
    ElMessage.error("头像文件大小不能超过2MB！");
    return false;
  }
  return true;
};

// 初始化数据
onMounted(async () => {
  // 登录状态检查
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login' })
    return
  }
  await getUserInfo();
  await getUserArticles();
});
</script>

<style scoped>
/* ===== Profile Container ===== */
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: var(--space-8) var(--space-6);
  animation: fade-in-up var(--duration-normal) var(--ease-default);
}

/* ===== User Header Section ===== */
.user-header {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-8);
  margin-bottom: var(--space-6);
  display: flex;
  align-items: center;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  transition: box-shadow var(--duration-normal) var(--ease-default);
}

.user-header:hover {
  box-shadow: var(--shadow-md);
}

.user-info-block {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.avatar-section {
  margin-right: var(--space-6);
}

.avatar {
  border: 3px solid var(--bg-secondary);
  box-shadow: var(--shadow-sm);
  transition: transform var(--duration-normal) var(--ease-default);
  cursor: pointer;
}

.avatar:hover {
  transform: scale(1.05);
}

.info-content {
  flex: 1;
}

.username {
  margin: 0 0 var(--space-3);
  font-family: var(--font-serif);
  font-size: var(--text-3xl);
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.position-info {
  display: flex;
  align-items: center;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  margin-bottom: var(--space-4);
}

.divider {
  margin: 0 var(--space-3);
  color: var(--border-color);
}

.intro {
  color: var(--text-secondary);
  font-size: var(--text-base);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
  max-width: 600px;
}

.action-section {
  margin-left: var(--space-6);
}

.settings-btn {
  font-weight: 500;
  padding: 8px 20px;
  border-radius: var(--radius-full);
}

/* ===== Main Content & Tabs ===== */
.main-content {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 0 var(--space-6);
  min-height: 500px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
}

/* Tabs Styling */
.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border-color);
}

.profile-tabs :deep(.el-tabs__item) {
  font-size: var(--text-base);
  padding: 0 var(--space-6);
  height: 60px;
  line-height: 60px;
  color: var(--text-secondary);
  transition: color var(--duration-fast);
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: var(--color-blue-600);
  font-weight: 600;
}

.profile-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--color-blue-600);
  height: 3px;
  border-radius: 3px 3px 0 0;
}

/* ===== List Styles ===== */
.articles-list,
.user-list {
  padding: var(--space-4) 0;
  animation: fade-in-up var(--duration-normal) var(--ease-default);
}

/* Article Item Styles */
.article-item {
  display: flex;
  padding: var(--space-6);
  border-bottom: 1px solid var(--border-color);
  transition: background-color var(--duration-fast) var(--ease-default);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-2);
}

.article-item:hover {
  background-color: var(--bg-secondary);
}

.article-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.article-cover {
  width: 160px;
  height: 100px;
  border-radius: var(--radius-md);
  overflow: hidden;
  margin-right: var(--space-6);
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-slow) var(--ease-default);
}

.article-item:hover .article-cover img {
  transform: scale(1.05);
}

.article-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.article-item-title {
  font-family: var(--font-sans);
  font-size: var(--text-lg);
  font-weight: 600;
  margin: 0 0 var(--space-2);
  line-height: 1.4;
  display: flex;
  align-items: center;
}

.article-item-title a {
  color: var(--text-primary);
  text-decoration: none;
  transition: color var(--duration-fast);
}

.article-item-title a:hover {
  color: var(--color-blue-600);
}

.status-tag {
  margin-left: var(--space-2);
}

.article-item-summary {
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.6;
  margin-bottom: var(--space-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-item-meta {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.meta-stats {
  color: var(--text-tertiary);
}

.article-item-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin-left: var(--space-6);
  opacity: 0;
  transition: opacity var(--duration-fast);
}

.article-item:hover .article-item-actions {
  opacity: 1;
}

.action-btn {
  transition: all var(--duration-fast) var(--ease-default);
  margin-bottom: var(--space-2);
}

/* User Item Styles */
.user-item {
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-color);
  transition: background-color var(--duration-fast) var(--ease-default);
  border-radius: var(--radius-md);
}

.user-item:hover {
  background-color: var(--bg-secondary);
}

.user-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: inherit;
}

.user-info-text {
  margin-left: var(--space-4);
}

.user-name {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-stats {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* ===== Settings Dialog ===== */
.avatar-edit-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: var(--space-6);
  padding: var(--space-4);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.setting-form {
  padding: var(--space-4) var(--space-4) 0 0;
}

/* ===== Password Requirements ===== */
.password-requirements {
  margin-top: 8px;
  padding: 12px;
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-blue-500);
}

.requirements-title {
  margin: 0 0 8px 0;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.requirements-list {
  margin: 0;
  padding-left: 20px;
  list-style: disc;
}

.requirements-list li {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: 2px;
}

.empty,
.loading {
  padding: var(--space-12) 0;
  text-align: center;
}

.load-more {
  text-align: center;
  margin-top: var(--space-6);
  padding-bottom: var(--space-6);
}

.load-more-btn {
  padding: 10px 28px;
  border-radius: var(--radius-full);
  transition: all var(--duration-fast) var(--ease-default);
}

.load-more-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* ===== Avatar Uploader ===== */
.avatar-uploader {
  text-align: center;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: var(--text-tertiary);
  width: 178px;
  height: 178px;
  text-align: center;
  line-height: 178px;
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-md);
  transition: border-color var(--duration-fast);
}

.avatar-uploader-icon:hover {
  border-color: var(--color-blue-500);
}

/* ===== Animations ===== */
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

/* ===== Responsive Design ===== */
@media (max-width: 768px) {
  .profile-container {
    padding: var(--space-4) var(--space-2);
  }

  .user-header {
    flex-direction: column;
    padding: var(--space-6);
    text-align: center;
  }

  .user-info-block {
    flex-direction: column;
    align-items: center;
  }

  .avatar-section {
    margin-right: 0;
    margin-bottom: var(--space-4);
  }

  .position-info {
    justify-content: center;
  }

  .action-section {
    margin-left: 0;
    margin-top: var(--space-6);
    width: 100%;
  }

  .settings-btn {
    width: 100%;
  }

  .article-item {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 180px;
    margin-right: 0;
    margin-bottom: var(--space-4);
  }

  .article-item-actions {
    margin-left: 0;
    margin-top: var(--space-4);
    flex-direction: row;
    gap: var(--space-4);
    opacity: 1; /* Always show actions on mobile */
  }

  .action-btn {
    margin-bottom: 0;
  }

  .main-content {
    padding: 0 var(--space-4);
  }

  .profile-tabs :deep(.el-tabs__item) {
    padding: 0 var(--space-4);
    height: 56px;
    line-height: 56px;
    font-size: var(--text-sm);
  }
}

@media (max-width: 480px) {
  .username {
    font-size: var(--text-2xl);
  }

  .article-cover {
    height: 150px;
  }
}
</style>
