# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language Preference

**Important**: When responding to this user, you should answer in Chinese (中文) as specified in `.qoder/rules/language.md`.

## Project Overview

This is a full-stack blog application with a Spring Boot backend (Java 21) and Vue 3 frontend (TypeScript + Vite). The application uses MySQL for persistence, Redis for caching/sessions, and Volcengine TOS for file storage.

### Website Identity
- **Title**: Lumina - 代码与思考
- **Author**: 4iKZ
- **GitHub**: https://github.com/4iKZ
- **Email**: syhaox@outlook.com

## Development Commands

### Windows Quick Start (后台运行)
```bash
# 启动后端 (使用 Maven，推荐)
mvn spring-boot:run

# 启动前端
cd frontend && npm run dev
```

### Backend (Spring Boot + Maven)
```bash
# Run development server (requires Java 21)
mvnw.cmd spring-boot:run
# or
mvn spring-boot:run

# Build JAR
mvnw.cmd clean package
# or
mvn clean package

# Run packaged JAR
java -jar target/blog-backend-0.0.1-SNAPSHOT.jar

# Run tests
mvnw.cmd test

# Run specific test class
mvnw.cmd test -Dtest=UserServiceTest

# Run specific test method
mvnw.cmd test -Dtest=UserServiceTest#testSpecificMethod
```

### Frontend (Vue 3 + Vite)
```bash
cd frontend

# Install dependencies
npm install

# Development server (runs on port 3000, proxies /api to backend:8080)
npm run dev

# Build for production (includes TypeScript type checking via vue-tsc)
npm run build

# Build without type checking (faster, for development)
npx vite build

# Preview production build
npm run preview
```

### Database Setup
```bash
# Initialize database tables
mysql -u root -p < database/create_tables.sql
mysql -u root -p < database/init_data.sql
# Additional migrations in db/migrations/ directory
```

## Architecture

### Backend Structure (`src/main/java/com/blog/`)
- **controller/** - REST API endpoints (Article, User, Comment, Category, Admin, FileUpload, etc.)
- **service/** - Business logic layer with JWT-based authentication
- **mapper/** - MyBatis Plus data access layer
- **entity/** - JPA entities (Article, Category, User, Comment, etc.)
- **dto/** - Data transfer objects for API requests/responses
- **config/** - Spring configuration (Security, CORS, Redis, MyBatis Plus, Swagger/OpenAPI)
- **security/** - JWT authentication filter, CustomUserDetailsService
- **exception/** - Global exception handling with BusinessException
- **interceptor/** - JWT interceptor (note: JwtAuthenticationFilter is preferred in SecurityConfig)
- **event/** - Domain events (ArticleViewCountChangeEvent, ArticleLikeCountChangeEvent)
- **util/** - Utilities (JWT, file upload, sensitive word filtering, AES encryption, Redis cache)
- **common/** - Result wrapper, ResultCode enums, PageResult

### Frontend Structure (`frontend/src/`)
- **services/** - API service layer (authService, articleService, commentService, etc.)
- **components/** - Reusable Vue components (ArticleCard, CommentSection, Layout, etc.)
- **views/** - Page-level components (HomeView, ArticleDetailView, admin views, etc.)
- **router/** - Vue Router configuration with auth guards
- **store/** - Pinia state management (user, article, notification stores)
- **types/** - TypeScript type definitions
- **utils/** - Utility functions (axios instance with interceptors)
- **composables/** - Vue composables (usePageTitle)
- **images/** - Static images (favicon.png, about-avatar.jpg)

### Key Technologies
- **Backend**: Spring Boot 3.5.6, Spring Security, MyBatis Plus 3.5.5, JWT (io.jsonwebtoken 0.11.5), SpringDoc OpenAPI 2.5.0, HikariCP, Hutool 5.8.16
- **Frontend**: Vue 3 Composition API, TypeScript 5.2.2, Vite 5.2.0, Element Plus 2.7.6, Pinia 2.1.7, Vue Router 4.3.0, MD Editor v3
- **Database**: MySQL with HikariCP connection pool (min 5, max 15 connections)
- **Cache**: Redis (Lettuce, max 8 connections)
- **Storage**: Volcengine TOS (volces.com, not Tencent)

## Authentication & Authorization

### Dual Authentication Architecture
The project uses a dual-layer authentication approach:

1. **Spring Security Filter Chain** (`JwtAuthenticationFilter`) - Primary authentication mechanism
   - Validates JWT tokens and sets Spring Security context
   - Configured in `SecurityConfig.java`
   - Stateless session management
   - Public endpoints: `/api/user/register`, `/api/user/login`, `/api/captcha/**`, article listing, etc.
   - Protected endpoints: article publishing, user profile management, admin APIs

2. **JWT Interceptor** (`JwtInterceptor`) - Secondary validation layer
   - Can be applied via `@Interceptor` annotation on specific controllers
   - Extracts userId/username from token and sets request attributes

### Getting Current User in Controllers
```java
// Method 1: From request attributes (set by JwtAuthenticationFilter)
Long currentUserId = (Long) request.getAttribute("userId");
String username = (String) request.getAttribute("username");

// Method 2: From SecurityContext
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
// Use CustomUserDetailsServiceImpl to load user details
```

### Frontend Authentication Flow
- Token stored in localStorage and Pinia store (`useUserStore`)
- Axios interceptor automatically adds `Authorization: Bearer <token>` header (see `frontend/src/utils/axios.ts`)
- Router guards redirect unauthenticated users to login page
- 401 responses trigger automatic logout and redirect
- **401 errors are exempt from the 10-second cooldown** - login state changes must always show immediate feedback
- Other errors have a 10-second toast cooldown to prevent spam

## Configuration

### Backend Configuration (`src/main/resources/application.yml`)
- Server port: 8080
- Database: MySQL on 59.110.22.74:3306 (configurable via env vars)
- Redis: 59.110.22.74:6379
- JWT expiration: 7 days (604800000ms)
- File upload limit: 10MB per file, 50MB total request
- MyBatis Plus: Logic delete field `deleted`, camelCase mapping enabled

### Environment Variables
- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret key

### Frontend Development Server
- Runs on port 3000
- Proxies `/api` requests to `http://localhost:8080`
- Path alias `@` maps to `frontend/src`
- Frontend code is located in the root `frontend/` directory (not in `data/blogs_workspace/frontend/`)
- Static assets (favicon, images) go in `frontend/public/` or `frontend/src/images/`

### Nginx Configuration (Production)
- Serves static files from `/data/blogs_workspace/frontend/dist`
- Proxies `/api` requests to backend (localhost:8080)
- Serves uploaded files from `/data/uploads/blog/`

## Important Notes

- JWT tokens use the `Authorization: Bearer <token>` header format
- All timestamps use Asia/Shanghai timezone (GMT+8)
- The project uses Lombok 1.18.32 - ensure annotation processing is enabled in your IDE
- MyBatis Plus mappers are in `src/main/resources/mapper/*.xml`
- API documentation available at `/swagger-ui.html` when running (SpringDoc OpenAPI)
- Logical deletion is enabled on `deleted` field (1=deleted, 0=active)
- Sensitive word filtering is implemented via `SensitiveWordFilter` utility
- TOS (Volcengine Object Storage) credentials are in application.yml - should be moved to env vars for production
- Jackson is configured to ignore null properties (`default-property-inclusion: non_null`)
- Log files are written to `logs/blog-backend.log` with 10MB max size and 30-day retention
- **TypeScript strict mode is enabled** with `noUnusedParameters: true` - prefix intentionally unused parameters with underscore (`_param`)

## Special Implementation Patterns

### Distributed Locking for Comment Likes
The project uses `RedisDistributedLock` to handle concurrent like/unlike operations:
```java
// In CommentServiceImpl
String lockKey = RedisDistributedLock.generateCommentLikeLockKey(commentId, userId);
String lockValue = redisDistributedLock.tryLock(lockKey);
try {
    // Perform like/unlike operation
} finally {
    redisDistributedLock.unlock(lockKey, lockValue);
}
```

### Transaction Synchronization for Cache Consistency
When updating comment likes, cache updates happen after DB commit via `TransactionSynchronizationManager`:
```java
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        // Update Redis cache after successful DB commit
        redisCacheUtils.setCache(likeCacheKey, true, 7, TimeUnit.DAYS);
    }
});
```

### Frontend Optimistic UI Updates
Comment like operations use optimistic updates with debouncing:
- UI updates immediately on user click
- API request debounced by 300ms
- Rollback on error with detailed error handling

### Vue Component Watch Pattern for Prop Changes
When components need to react to prop changes (like articleId in CommentSection):
```typescript
watch(() => props.articleId, (newId, oldId) => {
  if (newId && newId !== oldId) {
    // Reset state and reload data
    loadComments()
  }
}, { immediate: false })
```

### Component-Level Login Checks
Some pages (NotificationView, ProfileView) use component-level login checks in `onMounted` as a defense-in-depth measure beyond router guards:
```typescript
const userStore = useUserStore()

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'Login' })
    return
  }
  // Load data...
})
```

### Article Author Filtering
HomeView supports filtering articles by author via `useArticleStore`:
```typescript
const articleStore = useArticleStore()

// Set filter author
articleStore.setFilterAuthor(author)

// Clear filter
articleStore.clearFilterAuthor()

// Watch for changes
watch(() => articleStore.filterAuthor, (newVal) => {
  currentPage.value = 1
  getArticles()
}, { deep: true })
```

### Homepage Default Display
The homepage defaults to showing "推荐文章" (popular) rather than "最新文章" (latest). To change this behavior, modify `activeTab` default value in `HomeView.vue`.

D:\software\Redis 如果想要去连接到远程Redis服务器，可以去做这个路径下寻找redis-cli.exe程序，否则你直接在终端中使用redis-cli会提示找不到命令。
