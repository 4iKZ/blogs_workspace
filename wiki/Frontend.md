# 前端实现详解

[← 返回 Wiki 首页](./Home.md)

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4.x | 渐进式前端框架（Composition API） |
| TypeScript | 5.2.x | 类型安全 |
| Vite | 5.2.x | 构建工具（极速 HMR） |
| Element Plus | 2.7.6 | UI 组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.3.0 | 前端路由 |
| MD Editor v3 | 6.2.0 | Markdown 编辑器 |
| Axios | 1.6.8 | HTTP 客户端 |

---

## 应用入口

```typescript
// frontend/src/main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

---

## 路由配置

路由文件：`frontend/src/router/index.ts`

### 路由表

| 路径 | 组件 | 权限 |
|------|------|------|
| `/` | `HomeView` | 公开 |
| `/article/:id` | `ArticleDetailView` | 公开 |
| `/category` | `CategoryView` | 公开 |
| `/category/:id` | `CategoryView` | 公开 |
| `/tag/:id` | `TagView` | 公开 |
| `/search` | `SearchView` | 公开 |
| `/about` | `AboutView` | 公开 |
| `/user/:id` | `UserProfileView` | 公开 |
| `/login` | `LoginView` | 未登录 |
| `/register` | `RegisterView` | 未登录 |
| `/reset-password` | `ResetPasswordView` | 未登录 |
| `/profile` | `ProfileView` | **需登录** |
| `/article/create` | `ArticleEditView` | **需登录** |
| `/article/edit/:id` | `ArticleEditView` | **需登录** |
| `/notifications` | `NotificationView` | **需登录** |
| `/admin` | `AdminHomeView` | **需管理员** |
| `/admin/users` | `AdminUsersView` | **需管理员** |
| `/admin/articles` | `AdminArticlesView` | **需管理员** |
| `/admin/comments` | `AdminCommentsView` | **需管理员** |
| `/admin/categories` | `AdminCategoriesView` | **需管理员** |
| `/admin/settings` | `AdminSettingsView` | **需管理员** |

### 路由守卫逻辑

```typescript
router.beforeEach((to, _, next) => {
  ElMessage.closeAll()  // 路由跳转前清理所有提示
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login' })
    return
  }
  if (to.meta.requiresAdmin && userStore.getRole !== 'admin') {
    next({ name: 'Home' })
    return
  }
  // 已登录用户访问 /login 或 /register → 重定向到首页
  if (userStore.isLoggedIn && (to.name === 'Login' || to.name === 'Register')) {
    next({ name: 'Home' })
    return
  }
  next()
})
```

---

## 状态管理（Pinia Store）

### userStore (`store/user.ts`)

| 状态 | 类型 | 说明 |
|------|------|------|
| `user` | `UserInfo \| null` | 当前登录用户信息 |
| `token` | `string \| null` | Access Token |
| `refreshToken` | `string \| null` | Refresh Token |

| 计算属性 | 说明 |
|---------|------|
| `isLoggedIn` | 是否已登录 |
| `getRole` | 用户角色（'admin' / 'user'） |
| `getUserId` | 当前用户 ID |

| 方法 | 说明 |
|------|------|
| `setUserInfo(user, token, refreshToken)` | 登录后保存用户信息 |
| `clearUserInfo()` | 退出登录，清空状态 |
| `setTokens(token, refreshToken)` | 更新 Token（刷新时用） |
| `loadFromStorage()` | 从 localStorage 恢复登录状态 |

### articleStore (`store/article.ts`)

| 方法/状态 | 说明 |
|---------|------|
| `filterAuthor` | 当前过滤的作者 |
| `setFilterAuthor(author)` | 设置作者筛选 |
| `clearFilterAuthor()` | 清除作者筛选 |

### notificationStore (`store/notification.ts`)

| 状态 | 说明 |
|------|------|
| `unreadCount` | 未读通知数量（顶部导航显示） |
| `fetchUnreadCount()` | 从后端拉取未读数 |

---

## API 服务层（Services）

所有 API 调用统一通过 `utils/axios.ts` 中的 axios 实例发起。

| 服务文件 | 主要功能 |
|---------|---------|
| `authService.ts` | 登录、注册、登出、Token 刷新、密码重置 |
| `articleService.ts` | 文章 CRUD、列表、详情、点赞、收藏 |
| `commentService.ts` | 评论列表、发表、删除、点赞 |
| `categoryService.ts` | 分类列表、创建、编辑 |
| `notificationService.ts` | 通知列表、已读标记 |
| `authorService.ts` | 作者信息、关注/取关 |
| `adminService.ts` | 管理后台所有操作 |
| `statisticsService.ts` | 统计数据查询 |
| `systemConfigService.ts` | 系统配置读写 |

### Axios 实例配置

```typescript
// frontend/src/utils/axios.ts
const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：自动附加 JWT Token
service.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器：自动解包 data，处理 401 自动刷新 Token
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 401) {
      return tryRefreshAndRetry(response.config)  // 自动刷新 Token
    }
    if (res.code !== 200) {
      // 10秒错误提示冷却（防止刷屏）
      toast.error(res.message)
      return Promise.reject(error)
    }
    return res.data  // 直接返回解包后的 data
  }
)
```

**关键特性：**
- 响应自动解包：服务层拿到的直接是 `data` 字段内容
- Token 自动刷新：401 响应触发 Refresh Token 换取新 Access Token
- 错误提示冷却：非 401 错误显示 toast，10秒内不重复弹窗
- 并发刷新防重：多个 401 请求只触发一次刷新，其余请求排队等待

---

## 主要页面组件

### HomeView（首页）

- 展示文章列表，支持「推荐文章」/「最新文章」Tab 切换
- 默认显示「推荐文章」（`activeTab` 默认值为 `popular`）
- 支持按作者筛选（通过 `articleStore.filterAuthor`）
- 左侧边栏：分类导航、标签云、热门排行

### ArticleDetailView（文章详情）

- MD Editor v3 渲染 Markdown（只读模式）
- 文章点赞/收藏按钮（乐观 UI 更新）
- 评论区（CommentSection 组件）
- 自动上报浏览量

### ArticleEditView（文章编辑）

- MD Editor v3 全功能编辑（代码高亮、数学公式、流程图）
- 封面图片上传（图片压缩 + TOS 存储）
- 发布抽屉（PublishDrawer）：设置分类、摘要、封面、发布状态
- 支持草稿自动保存

### CommentSection（评论区组件）

- 展示评论树（顶级评论 + 嵌套子评论）
- 支持最新/最热排序
- 评论点赞（乐观更新 + 300ms 防抖）
- `watch` 监听 `articleId` prop 变化，重新加载

```typescript
// 监听文章ID变化，重置评论列表
watch(() => props.articleId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    loadComments()
  }
}, { immediate: false })
```

### AdminHomeView（管理后台首页）

- 统计大屏：总文章数、总用户数、总评论数、总浏览量
- 访问趋势折线图（PV/UV）
- 热门文章排行
- 近期用户注册列表

---

## 可复用组件

### LuminaToast（全局提示）

自定义提示框，替代 Element Plus ElMessage：

```typescript
// 使用方式（通过 composable）
import { toast } from '@/composables/useLuminaToast'

toast.success('操作成功')
toast.error('操作失败')
toast.warning('请注意')
toast.info('提示信息')
```

### ArticleCard（文章卡片）

显示文章摘要、封面图、标题、标签、统计数据（浏览/点赞/评论/收藏）。

### LikeButton / FavoriteButton

文章点赞/收藏按钮，支持：
- 乐观 UI 更新（点击立即更新界面）
- 未登录时提示登录
- 防抖处理

---

## 组合式函数（Composables）

### usePageTitle

动态设置浏览器标题：

```typescript
const { setTitle } = usePageTitle()
setTitle('文章详情')  // → "文章详情 - Lumina"
```

### useLuminaToast

提供 `toast` 实例，参见上面 LuminaToast 章节。

---

## 图片压缩与上传

### Web Worker 压缩（`workers/compression.worker.ts`）

图片上传前在 Web Worker 中压缩，不阻塞主线程：

```
用户选择图片文件
    │
    ▼
imageCompressor.ts → 创建 Web Worker
    │  发送 File 对象
    ▼
compression.worker.ts
    │  Canvas API 压缩（降低分辨率 / 质量）
    │  返回压缩后的 Blob
    ▼
chunkedUploader.ts（大文件分片上传）
    │  文件 < 阈值 → 直接上传
    │  文件 ≥ 阈值 → 分片上传
    ▼
POST /api/upload/image
    └─ 返回文件 URL
```

### enhancedImageCompressor（增强压缩）

支持压缩结果缓存（`compressionCache.ts`），相同文件不重复压缩。

---

## TypeScript 类型定义

类型文件位于 `frontend/src/types/`：

| 文件 | 主要类型 |
|------|---------|
| `article.ts` | `Article`、`ArticleCreateRequest`、`ArticleListResponse` |
| `user.ts` | `UserInfo`、`LoginRequest`、`RegisterRequest` |
| `comment.ts` | `Comment`、`CommentCreateRequest` |
| `notification.ts` | `Notification`、`NotificationType` |
| `statistics.ts` | `WebsiteStatistics`、`VisitTrend` |

---

## Vite 配置

```typescript
// frontend/vite.config.ts
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': '/src' }   // @ 指向 src 目录
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

---

## TypeScript 编译配置

```json
// frontend/tsconfig.json（关键配置）
{
  "compilerOptions": {
    "strict": true,
    "noUnusedParameters": true   // 未使用的参数需以 _ 前缀命名
  }
}
```

> **注意**: TypeScript 严格模式已启用。未使用的函数参数必须以下划线前缀命名（如 `_param`），否则编译报错。
