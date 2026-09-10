# Architecture Audit — blogs_workspace

> 审计依据：`architecture-refactoring` skill（客户化证据审计，未改动任何代码）。
> Audit date: 2026-09-10 · Commit: `4ca5dbe`

## Scope

- Target: `blogs_workspace` 全仓（Spring Boot 3.5.6 后端 + Vue 3 前端）
- Task scope: **Repository-wide**（用户请求整体分析）
- Reason: 识别最高价值的衔接/耦合问题，产出一份可执行的增量重构方向

## Baseline

- Build: `mvn -o -q compile -DskipTests` → **通过，exit code = 0**（本次已运行验证）
- Test: `mvn test` 全量存在既有失败（AGENTS.md 已记录，非本次审计引入）；定向回归命令见 AGENTS.md
- Frontend: `cd frontend && npm run check`（type-check + lint + Vitest + build）
- Typecheck/lint/static-analysis（架构层）：**不存在**。pom.xml 无 ArchUnit / checkstyle / pmd / spotbugs 配置，没有依赖方向或分层约束的自动检查
- Known pre-existing failures: 历史全量测试失败（AGENTS.md 声明，本次未重跑）
- Behavior that must remain unchanged: 全部 HTTP 契约（`Result<T>` 结构）、文章/评论状态语义、计数与缓存一致性语义、现有测试

## Current system map

### Relevant modules/components

| Module | Responsibility observed in code | Owns data? | Main callers | Main dependencies |
|---|---|---|---|---|
| `controller` (18 类) | HTTP 入口、校验、调 service | 否 | 前端 / 客户端 | service, dto, common |
| `service` + `service.impl` (31 接口 + 30 实现) | 业务编排与规则 | 是（各域） | controller, event, schedule | mapper, dto, entity, utils, event |
| `mapper` (21) | MyBatis-Plus 持久化 | 表读写入口 | service/schedule | entity |
| `event` (15) | 审核/通知/缓存失效的异步监听 | 否 | ApplicationEventPublisher | service, dto, utils |
| `utils` (14, 含 `HotArticleCacheEvictionService`) | 横切工具 + 缓存失效入口 | 部分（Redis key） | 全层（被 import 56 次） | common, event, mapper, config |
| `config` (17) | 安全、Redis、缓存、TOS 配置 | 否 | 容器启动 | security, interceptor, service(1) |
| `schedule` (5) | 计数校正、榜单重置、文件清理、审核重试 | 是（校正三域计数） | 定时器 | mapper, service |
| `security` (3) | JWT 过滤、认证入口、UserDetails | 否 | Spring Security | utils, mapper, service.impl |
| `frontend/src/views` (28) | 页面 | 否 | 路由 | services, store |
| `frontend/src/services` (11) | API 封装 | 否 | views | utils/axios |
| `frontend/src/utils/axios.ts` + `tokenRefreshQueue` + `crossTabRefresh` | HTTP 客户端 + 401 刷新编排 + 全局错误提示 | 否 | 全部前端调用 | store, router, composables |

### Representative runtime flow(s)

```text
1) 文章列表：GET /api/article/list
   ArticleController → ArticleQueryService(Impl)
   → ArticleRankService（popular 排序走 Redis ZSet）
   → ArticleDtoAssembler（作者/分类/点赞/收藏/Redis 浏览量合并）
   → ArticleMapper / UserMapper / CategoryMapper / UserLikeMapper / UserFavoriteMapper / Redis

2) 文章发布：POST /api/article
   ArticleController → ArticleService(Impl).publishArticle
   → ArticleModerationSubmissionService（落审核提交记录）
   → 事件监听（ModerationEventListener / NotificationEventListener）
   → ArticleMapper（status=1 草稿） + 审核通过后 ArticleModerationSubmissionServiceImpl 置 status=2

3) 登录/刷新：POST /api/user/login、/user/token/refresh
   UserController → UserServiceImpl（CaptchaService、JWTUtils、Redis 刷新令牌族、
   AuthSessionRevocationService、EmailTemplateService）
   → 前端 axios 拦截器负责 401 后单飞刷新 + 幂等请求重放（tokenRefreshQueue/crossTabRefresh）
```

### Dependency view

```text
controller ──> service ──> mapper ──> entity
     │            │  ▲
     │            ▼  │ (event 监听回调 service)
     └──────> dto │  event ──> service / dto / utils
                  │
service.impl ──> utils（56 次 import：RedisCacheUtils 12、AuthUtils 10、RedisUtils 9、BusinessUtils 9…）
service.impl ──> controller.CaptchaController  ← 反向依赖（Finding 6）
config ──> service（ArticleRankInitializer 启动时初始化榜单）
security ──> service.impl（CustomUserDetailsService）
```

- service 接口间依赖**无环**（最新提交 `4ca5dbe` 刚解除文章域循环）：Article 读/写已拆分（`ArticleService` 5 方法 / `ArticleQueryService` 9 方法），`ArticleDtoAssembler` 已抽出
- 现存方向性问题：`service → controller`（见 Finding 6）
- 近期 git 主题显示团队已在做“反异常吞噬 + 事务语义 + 去抽象”的架构治理，本审计应与该方向衔接而非另起炉灶

## Data ownership

| Data / table / state | Readers | Writers | Current invariant owner | Problem? |
|---|---|---|---|---|
| `articles` 行（标题/正文/分类） | 多域 | `ArticleServiceImpl`(增/改/删)、`AdminServiceImpl`(状态) | 不明确，状态机三处写 | **是（Finding 3）** |
| `articles` 计数（view/like/comment/favorite） | DTO 组装多处 | **仅** `ArticleStatisticsServiceImpl` 经 `ArticleMapper` 写 | `ArticleStatisticsService` | 否（单写者，良好） |
| Redis 浏览量增量 | `ArticleDtoAssembler`、`AdminServiceImpl`、`SearchServiceImpl`、`UserLikeServiceImpl`、`UserFavoriteServiceImpl` | `ArticleStatisticsServiceImpl` | 读合并逻辑 5 处重复 | **是（Finding 1）** |
| `ArticleDTO` 组装知识 | 前端各页 | 5 个 Java 类各自组装 | `ArticleDtoAssembler`（未被全部采用） | **是（Finding 1）** |
| `users` 行 | 多域 | `UserServiceImpl`、`AdminServiceImpl`（删除/状态经 `AuthSessionRevocationService`） | 基本单一 | 否 |
| 关注/粉丝计数 | 前端、DTO | `UserServiceImpl.follow/unfollow`、`AdminServiceImpl.deleteUser` 手写增减、`FollowCountCorrectionSchedule` 每日校正 | 无单一 owner，定时任务兜底 | **是（Finding 4）** |
| `comments` / 评论点赞 | 多域 | `CommentServiceImpl`（含 `applyModerationResult`）、`ArticleServiceImpl.deleteArticle` 级联删 | `CommentServiceImpl` | 否（级联清理已由删除方集中） |

## Findings

### Finding 1 — ArticleDTO 组装知识分散在 5 处，已抽出的 Assembler 只迁移了一半

Observed evidence:

- 规范实现：`ArticleDtoAssembler.batchConvertToDTO`（ArticleDtoAssembler.java:60），被 `ArticleQueryServiceImpl`（5 处调用）和 `ArticleRankServiceImpl`（2 处）使用
- 手工重复实现：
  - `AdminServiceImpl.getArticleList`（AdminServiceImpl.java:207-226）：`DTOConverter.convert` + 手工合并 Redis 浏览量，无作者/分类/互动状态
  - `SearchServiceImpl.convertToDTO`（SearchServiceImpl.java:167-171）：仅 `BeanUtils.copyProperties`
  - `UserLikeServiceImpl`（UserLikeServiceImpl.java:296-298）：仅 copy，无浏览量合并
  - `UserFavoriteServiceImpl`（UserFavoriteServiceImpl.java:261-262）：同上
- 最新提交 `4ca5dbe` 才抽出 Assembler，属于**未完成的迁移**

Architectural mechanism: DTO 组装是文章域的一条业务知识（哪些字段来自哪里、浏览量如何合并、互动状态如何注入）。它现在有 1 个“规范所有者”和 4 个影子实现。新增一个需要联表/缓存合并的字段（如标签、阅读时长、互动状态）需要同步改 2–5 个类，且各出口返回的 DTO 完整度已经不一致。

Why it matters: 典型的“改一处功能要改多个无关模块”。并且影子实现之间的不一致会以“同一篇文章在不同页面字段不同”的形式变成缺陷。

- Affected workflows: 文章列表、文章详情、搜索、用户点赞列表、用户收藏列表、管理后台文章列表
- Change frequency: Medium-High（近 6 个月 `AdminServiceImpl` 9 次、`ArticleServiceImpl` 10 次提交）
- Blast radius: High（5 个模块）
- Migration risk: Low（纯组装路径，测试覆盖多）
- Confidence: High

### Finding 2 — UserServiceImpl 是 1515 行/17 依赖的上帝服务，至少 6 个独立变更原因

Observed evidence:

- 规模：UserServiceImpl.java 1515 行（最大 impl），`@Autowired` 字段 17 个，接口 `UserService` 25 个方法，`UserController` 22 个端点
- 变更原因（方法清单直接可见）：注册+验证码+欢迎邮件、登录+令牌族轮换、登出+会话吊销、改密+邮箱验证码重置、资料读写、关注/粉丝/互关、GitHub OAuth、公开主页聚合
- 变更频率：近 6 个月 14 次提交，全仓第一热点
- 测试：`UserServiceImplSecurityTest`、`UserServiceImplCoverageTest` 等已用 Mockito 隔离，拆分有测试安全网

Architectural mechanism: 一个类对多个业务原因负责，认证策略、邮件模板、社交关注、OAuth 供应商的变更都会命中同一文件，合并冲突与回归面随功能线性增长。`ArticleServiceImpl` 只调用它的 `getUserById` 一个方法，却依赖整个 25 方法接口（公开面过宽）。

Why it matters: 最热文件上的任何改动都需要加载整个文件上下文，改一处影响面大。

- Affected workflows: 全部登录态相关流程 + 个人主页 + 关注关系
- Change frequency: High
- Blast radius: High
- Migration risk: Medium-High（涉及事务/令牌/安全，需要分阶段）
- Confidence: High

### Finding 3 — 文章状态机没有单一所有者，3 个模块直接写 `articles.status`

Observed evidence:

- `ArticleServiceImpl.java:126`（创建为草稿）、`ArticleServiceImpl.java:183`（编辑恢复原状态）
- `ArticleModerationSubmissionServiceImpl.java:116`（审核通过 → published）、`:149`（驳回 → draft）
- `AdminServiceImpl.java:241`（管理员直改任意状态，仅拦截“改为 published”这一条规则）

Architectural mechanism: 状态迁移规则（谁能把文章改成什么状态、迁移后哪些缓存/榜单要联动）分散在 3 个模块；管理端路径通过 `articleMapper.updateById` 绕过审核流程的状态守卫。规则一多（如新增“归档/定时下线”）就必须同步多处，且守卫容易漂移。`AdminServiceImpl.updateArticleStatus` 与 `ArticleServiceImpl.deleteArticle` 的缓存/榜单联动逻辑已经不一致（前者手动 removeFromRank，后者另有缓存清理流程）。

Why it matters: 状态是文章域的核心不变量，“部分调用方绕过守卫直写字段”是所有权分裂的典型信号。

- Affected workflows: 文章发布、审核、管理端下架/删除
- Change frequency: Medium
- Blast radius: Medium
- Migration risk: Medium
- Confidence: High（代码事实明确）；对“未来变更成本”的推断为 Medium

### Finding 4 — 关注/粉丝计数有两个手写更新方，外加每日校正任务兜底

Observed evidence:

- `UserServiceImpl.follow/unfollow` 自己增减计数
- `AdminServiceImpl.deleteUser`（AdminServiceImpl.java:152-173）为关注者/被关注者逐个调 `userMapper.decrementFollowingCount/decrementFollowerCount`
- `FollowCountCorrectionSchedule.correctFollowCounts`（FollowCountCorrectionSchedule.java:26-35）每天凌晨 2 点用 SQL 全量重算“确保数据一致性”

Architectural mechanism: 计数不变量有 2 个业务写入者 + 1 个修复者。存在全量校正任务本身说明历史上出现过漂移；两处业务逻辑对“删除用户时是否要修正计数”的理解必须永远保持一致。

Why it matters: 新增任何与关注关系相关的批量操作（如封禁用户、游客合并）都要记得同步计数，否则依赖夜间任务慢慢愈合。

- Affected workflows: 关注/取关、管理端删用户
- Change frequency: Medium
- Blast radius: Medium
- Migration risk: Low-Medium（把计增减收口到单一 Owner，管理端复用该 API）
- Confidence: High

### Finding 5 — UserServiceImpl 跨域直读 articles/comments 表组装公开主页

Observed evidence: `UserServiceImpl.getPublicUserInfo`（UserServiceImpl.java:813-822）直接用 `articleMapper.selectCount`（含 `Article.STATUS_PUBLISHED` 常量）和 `commentMapper.selectCount`（含评论 `status=2` 字面量）填 `articleCount/commentCount`。

Architectural mechanism: 用户域拥有并暴露文章域/评论域的持久化模型知识（表结构 + 状态编码）。文章或评论的状态模型调整会连带改到用户域。

Why it matters: 跨边界知识本应窄而稳定；这里泄漏了查询实现（状态常量、条件）。是 Finding 1/3 的同类问题在用户域的实例。

- Affected workflows: 公开用户主页
- Change frequency: Low
- Blast radius: Low
- Migration risk: Low
- Confidence: High

### Finding 6 — service 层反向依赖 controller 的嵌套 DTO

Observed evidence: `CaptchaService.java:4` 与 `CaptchaServiceImpl.java:4` 均 `import com.blog.controller.CaptchaController.CaptchaResponse`；`CaptchaResponse` 定义在 CaptchaController.java:51（controller 内部静态类）。

Architectural mechanism: 控制器嵌套类型被服务接口当返回值。分层方向 `service → controller` 反转，服务层无法脱离 Web 层复用/测试，且形成一个“谁都能 import controller”的先例。

Why it matters: 修复极小，不修则约束无从建立（是加架构测试前必须清掉的违规）。

- Affected workflows: 验证码获取
- Change frequency: Low
- Blast radius: Low
- Migration risk: Low（移动到 `dto` 包，前端契约不变）
- Confidence: High

### Finding 7 — 前端 axios.ts 混合三种职责，是全仓第 3 热点

Observed evidence: axios.ts 274 行，同时负责客户端配置（27-34）、401 刷新重试编排（97-168）、全局错误提示策略（170-271）；近 6 个月 11 次提交。已有部分拆分（`tokenRefreshQueue.ts`、`crossTabRefresh.ts`）。

Architectural mechanism: 认证失效策略（何时跳登录、写请求不重放）与错误展示策略（冷却时间、哪些错误静默）写在同一拦截器链里。任何认证 UX 调整都触碰全部错误处理逻辑。

Why it matters: 11 次/6 个月的变更频率 × 该文件承载全部 API 流量，是前端最高风险的耦合点。但当前拆出的两个队列模块说明 seam 已存在，进一步抽离“错误提示策略”是低风险动作。

- Affected workflows: 全部前端 API 调用
- Change frequency: High
- Blast radius: High（全部请求）
- Migration risk: Medium
- Confidence: Medium（未逐行审计 `tokenRefreshQueue`/`crossTabRefresh` 的边界是否已合理）

### Finding 8 — 缺少架构约束的自动检查

Observed evidence: pom.xml 无 ArchUnit/checkstyle/pmd/spotbugs；AGENTS.md 无架构检查命令；`service → controller` 泄漏无人拦截。

Architectural mechanism: 目前所有分层约束靠约定与评审。最近的循环依赖是“事后手工解除”的，没有机制防止回归。

Why it matters: 任何一次重构的架构收益都无法被验证或固化（skill 要求行为验证与架构验证分离）。

- Affected workflows: 全仓
- Change frequency: —
- Blast radius: 高（长期）
- Migration risk: Low（先加 2–3 条规则）
- Confidence: High

## Highest-value target

Problem to solve first: **统一 ArticleDTO 组装，完成 `4ca5dbe` 未完成的迁移（Finding 1）**。

Why this outranks other findings:

- 价值公式 `变更频率 × 波及面 × 缺陷成本 ÷ 迁移风险` 得分最优：5 个模块的重复知识 vs. 低迁移风险
- 它是“进行中重构的收尾”，团队上下文已经具备（Assembler 已在两个服务中稳定使用）
- 与 Finding 2（UserServiceImpl 拆分）相比，可在一次会话内完成并验证；与 Finding 3（状态机）相比，不需要改变行级行为
- 完成后为 Finding 8（加架构规则）提供第一条可固化的边界

战略级目标（第二步）：按 Finding 2 拆分 `UserServiceImpl` 为 认证 / 账户资料 / 社交关系 三个模块，`UserService` 保留为兼容门面。

## Proposed target boundary

### 第一步：ArticleDTO 组装边界

- Responsibility: 将 `Article` 聚合转换为对外 `ArticleDTO`（含作者/分类/互动状态/全量浏览量合并）
- Owned invariants: “同一篇文章在任意出口的 DTO 组装规则一致”
- Owned data/state: 无（只读聚合）
- Public contract: `convertToDTO(Article)`、`batchConvertToDTO(List<Article>)`（保留现有签名）
- Implementation details to hide: 各 mapper 的批量查询、Redis 浏览量增量合并、状态字段映射
- Allowed dependencies: article/user/category/like/favorite mappers、`RedisCacheUtils`、`AuthUtils`
- Forbidden dependencies: controller 层类型；任何写操作

### 第二步（战略）：UserServiceImpl 拆分边界

- Responsibility 拆分：`AuthService`（注册/登录/令牌/登出/改密/重置）、`UserProfileService`（资料、公开主页）、`UserSocialService`（关注关系与计数）
- Owned invariants: 令牌族轮换、密码策略、关注计数（唯一 Owner，供 Admin 复用）
- Public contract: 保留 `UserService` 作为兼容门面，先委托后迁移调用方
- Forbidden dependencies: 跨域 mapper（articles/comments 直读移出）

## Alternatives considered

### Option A — 只改 AdminServiceImpl 复用 Assembler

Change: `AdminServiceImpl.getArticleList` 改用 `articleDtoAssembler.batchConvertToDTO`
Benefits: 最小、无接口变化
Costs/risks: Search/UserLike/UserFavorite 的影子实现仍在，Finding 1 只解决 1/4

### Option B — 给 Assembler 加“轻量模式”，统一 5 处

Change: Assembler 增加可选的 enrichment 开关，搜索/点赞/收藏改用轻量模式
Benefits: 全量收口，各出口可按需组装
Costs/risks: 引入一个非请求驱动的参数分支；属于“为将来变化预留的抽象”，需确认 5 处确实会同步变化再采用

### Option C — 维持现状，只在 Assembler 处加注释说明唯一入口

Change: 不改代码
Benefits: 零风险
Costs/risks: 与“变更传播”目标冲突；`4ca5dbe` 的重构收益无法兑现

## Recommendation

Recommended option: **A 起步，验证后按证据推进 B**。即先让 `AdminServiceImpl` 走 Assembler（单点、可回归），同时用一次“新增 ArticleDTO 字段”的变更成本评估决定是否收口其余 3 处；若确认同步变化，再按 Option B 统一。

Coupling expected to disappear: 管理后台文章列表对 `DTOConverter` + 手工浏览量合并的私有知识；后续 3 处若推进则消除 5→1 的组装知识复制。

Coupling expected to be introduced: `AdminServiceImpl → ArticleDtoAssembler`（同一域内的显式读依赖，语义上本就应该存在）。

Why the trade-off is better: 把“文章 DTO 长什么样”收敛到文章域所有者；管理端不再各自解释浏览量合并规则。

Behavior verification: `mvn test -Dtest="AdminServiceImplTest,AdminControllerUnitTest,ArticleDtoAssemblerUnitTest,ArticleQueryServiceImplUnitTest"`；管理端文章列表接口人工对比字段。
Architecture verification: 全仓 `new ArticleDTO()` / `BeanUtils.copyProperties(article` 计数从 5 → 4（第一步），目标 5 → 1；新增 ArchUnit 规则禁止 `service → controller`（Finding 6 先修复）。

Remaining risks: Assembler 会额外查询作者/分类/互动状态，管理端列表响应字段会变多（兼容追加，但需确认前端与管理端测试无严格字段断言）。

## Uncertainties / assumptions to validate

- `SearchServiceImpl`/`UserLike`/`UserFavorite` 的轻量组装是否出于性能或契约刻意为之——需产品/测试确认后再收口（本次未发现注释或测试说明）
- 全量测试既有失败清单未在本审计中重跑（仅编译基线已验证）；执行迁移前需先跑定向回归确认绿
- `tokenRefreshQueue.ts`/`crossTabRefresh.ts` 的文件内聚是否已合理（前端 Finding 7 未逐行审计）
- `SensitiveWordFilter`（utils）依赖 `SensitiveWordMapper` 的持久化耦合未展开评估，优先级低但同类
