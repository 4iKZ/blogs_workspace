# AGENTS.md — Lumina 博客全栈项目

## 项目结构

- 后端在仓库根目录（`src/main/java/com/blog`）：Spring Boot 3.5.6 + Java 21 + MyBatis Plus 3.5.5。
- 前端在 `frontend/`：Vue 3.4 + TypeScript + Vite 5 + Element Plus + Pinia。
- 存储：MySQL（库名 `blog_db`）+ Redis（计数器/分布式锁/验证码/浏览量队列/ZSet 排行，不承担 Spring Cache 层）+ Caffeine 本地缓存（Spring Cache 层，仅热榜查询结果，30s TTL）+ 火山云 TOS；测试用 H2 内存库。
- 其他指令来源：`.specify/memory/constitution.md`（架构最高权威）、`CLAUDE.md`（架构速览）、`docs/architecture-audit.md`（既有架构问题清单）。

## 命令

### 后端（仓库根目录，优先用 Maven Wrapper）

```bash
mvnw.cmd spring-boot:run              # Windows；Linux/macOS 用 ./mvnw
mvnw.cmd -DskipTests package          # 打包
mvnw.cmd test -Dtest=ArticleControllerTest#testXxx   # 单个测试/方法
```

定向安全与文件回归（H2 + Mockito，不连外部服务，改代码后优先跑这个）：

```bash
mvnw.cmd -Dtest="SecurityConfigTest,UserServiceImplSecurityTest,ArticleServiceImplUnitTest,FileUploadServiceImplSecurityTest,FileUploadDeduplicationTest,*FileCleanup*Test,ArticleControllerPrivacyTest" test
```

- `mvn test` 全量存在历史失败，不得在未修复前宣称全量通过。
- `mvn verify` 会触发 JaCoCo 覆盖率门禁（instruction 35% / branch 12% / line 38% / method 43% / class 80%），可能因此失败。

### 前端（`frontend/`）

```bash
npm ci
npm run dev      # http://127.0.0.1:3000，/api 代理到 8080
npm run check    # 完整门禁：type-check → eslint → vitest → knip → build
npx vitest run src/path/xx.test.ts   # 单个测试
```

- `npm run build` 会先跑 `vue-tsc`，类型错误直接失败。
- ESLint 禁止 `console.log`（仅允许 `console.warn/error`）；Vitest 为 jsdom，setup 文件 `src/test/setup.ts`。

## 测试环境坑

- `src/test/resources/application.yml` 被 gitignore；已提交的 `application.yml.example` 是过期模板（缺 H2 `spring.sql.init`、mail、AI mock 等）。H2 测试必须依赖本地实际存在的 `application.yml`。
- 已知全量测试噪声（非回归，勿重复排查）：`UserServiceImplCoverageTest` 的 login/GitHub OAuth 用例在纯 Mockito 下报 `can not find lambda cache for this entity [com.blog.entity.User]`。
- `*DaoTest` 使用 `@DaoTestContext`（`DaoTestConfig` 最小上下文 + `dao-test` profile），默认内存 H2，是 hermetic 的，`./mvnw -Dtest='*DaoTest' test` 即可运行，不需要任何外部服务或密码。
- 在真实 MySQL 上运行：先按 `database/schema.sql`、`database/data.sql` 初始化一个**专用的测试库**（不要指向开发库或生产库），再设置 `DAO_DB_URL`、`DAO_DB_USERNAME`、`DAO_DB_PASSWORD`、`DAO_DB_DRIVER=com.mysql.cj.jdbc.Driver`、`DAO_SQL_INIT_MODE=never`，运行 `./mvnw -Pdao-mysql -Dtest='*DaoTest' test`。CI 的 `backend-mysql` 作业即按此方式在 MySQL 8 服务容器上运行。
- `@Tag("mysql")` 的用例依赖 MySQL 方言（`CURDATE`、`DATE_SUB`、`MATCH…AGAINST`、递归 CTE 等），H2 下不可用，默认由 pom 的 `surefire.excludedGroups` 排除，只在 `-Pdao-mysql` 下运行。
- H2 表结构与数据来自 `src/test/resources/schema-h2.sql`、`data-h2.sql`。

## 数据库

- 新库初始化顺序：先 `database/schema.sql`（含触发器等），再 `database/data.sql`。
- 既有库升级：`database/migrations/` 下按文件名顺序执行加法迁移，必须在维护窗口内停止旧节点后完成。
- 认证与分块上传协议为破坏性变更，禁止新旧版本混跑；回滚应用代码时不要删除新增列/唯一索引、`file_cleanup_tasks`、`users.token_version`、`article_moderation_submissions`。
- 逻辑删除统一用 `deleted` 字段。

## 架构要点（容易踩坑的）

- 后端严格 Controller → Service → Mapper。文章 DTO 组装统一走 `src/main/java/com/blog/service/impl/ArticleDtoAssembler.java`（管理端、点赞、收藏、列表均已收口），不要在 Service 中另写组装逻辑。
- 鉴权唯一入口是 SecurityConfig 过滤器链中的 `JwtAuthenticationFilter`。Access Token 有效期 900s 且仅存 Pinia 内存；Refresh Token 有效期 604800s 仅通过 `HttpOnly; Secure; SameSite=Strict; Path=/api/user` Cookie 下发；JWT 含 `jti`、`tokenVersion`。
- 禁止在 `@Transactional` 方法内 catch 后 `return Result.error(...)`（吞异常会导致事务不回滚）——近期多个提交专门修复了这类问题；业务失败统一 `throw new BusinessException(...)`。
- 异步走 Spring Event（浏览量/点赞计数、通知、评论审核、缓存失效）；访问日志批量异步写；文章浏览量在应用关闭时刷库。
- TOS 上传按用户做 SHA-256 查重；DB 写入失败必须删除新对象；删除失败进入 `file_cleanup_tasks` 退避重试（最多 5 次）。AI 审核为持久化状态机：AI 失败按 1/5/15 分钟重试，耗尽转人工；已发布文章编辑在审核通过前继续展示旧版本。评论审核失败/事件丢失由 `CommentModerationRetryScheduler` 每 5 分钟兜底重投（单轮 ≤50 条，落库仅接受待审核状态）。
- Redis 双模板陷阱：`RedisTemplate`（Jackson 值序列化）与 `StringRedisTemplate`（裸字符串）不可混用同一键的值通道；凡值要与 Lua/原生命令比较、拼键或解析（验证码、计数器、队列成员）必须走 `StringRedisTemplate`。
- 前端所有请求必须走 `frontend/src/utils/axios.ts`（自动附加内存 JWT 并携带 Cookie；并发 401 共享一次刷新）；首次导航由 `store/user.ts::initializeSession()` 恢复会话。

## 约定

- 提交信息使用 conventional 前缀（`feat:` / `fix:` / `refactor:` / `docs:`），中英混合均可，参考 `git log`。
- 禁止提交私有配置：`src/main/resources/application.yml`、`src/test/resources/application.yml`、`scripts/prod-env.sh`、`scripts/private-values.env`。
