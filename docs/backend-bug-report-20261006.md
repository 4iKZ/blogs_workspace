# Lumina 博客后端代码复审报告（2026-10-06）

- **日期**：2026-10-06
- **审查基线**：`main` 分支 @ `60ed78f`（上轮 18 项 P1/P2 修复已合入）
- **方法**：5 个并行子代理分域审查（认证用户 / 文章排行 / 评论互动 / 搜索统计 / 文件基础设施），主代理对全部 30 项发现逐条读源码打假（含修复是否完整的复核）
- **结果**：原始 30 项 → 确认 **30 项**（P1 × 0、P2 × 10、P3 × 20），其中 **7 项属上轮修复不完整/同类遗漏**；降级 2 项、修正描述 2 项、维持存疑 3 项
- **未发现 P0/P1**：权限链路（`/api/admin/**`、`/api/statistics/**`、`/api/system/**` 均 `hasRole('admin')`）、JWT 过滤器链与 tokenVersion/黑名单检查、refresh 轮换、文件上传 fail-closed、分布式锁语义经核对未见绕过
- **上游上下文**：上轮报告见 `docs/backend-bug-report-20261004.md`（37 项，其中 18 项已修、19 项 P3 未修）

---

## 修复状态（2026-10-06 更新）

- **修复分支**：`fix/backend-review-20261006`（基于 `main@60ed78f`，改动未提交/未合并）
- **已修复**：P2 × 10 全部 + P3 × 17 中确认存在的全部，合计 27 项（其中 P3-18 为实施期发现的漏列项、于联调阶段补做，其余 26 项为批量修复轮完成）
- **验证**：排除 `*DaoTest` 的全量单测——首轮 **1795 tests / 18 failures / 0 errors**，P3-18 补修后复核 **1800 tests / 18 failures / 0 errors**；基线 `60ed78f` 同一命令为 **1748 tests / 18 failures / 0 errors**（失败集合完全一致，均为历史遗留：9 项 `/api/statistics/**` "shouldBePublic" 陈旧断言 + 9 项依赖外部 Redis 的 `ArticleStatisticsIntegrationTest`）。零新增失败
- **打包**：`.\mvnw.cmd -DskipTests package` 成功（`target/blog-backend-0.0.1-SNAPSHOT.jar`）
- **API 级联调（2026-10-06 晚，按项目规范跳过浏览器 UI）**：后端在 8080（连 `blog_test`）实跑，覆盖 12 组场景全部通过——
  - 公开/分页：`hot?limit=999999`→100 条、`=-1`→10 条；`recommended?limit=-1` 不再报错；排行 `page=MAX_VALUE`/评论 `page=22M` 返回空页；按作者搜索 `pageNum=22M` 200；
  - 评论：负 parentId 拒绝、`allow_comment=0` 拒绝、正常评论成功、已删评论不可见；
  - 通知 `page=0` 200（原 500）；取关两次均成功（幂等）；登出后 `validate=false`；
  - 登录锁定：5 次错误后正确密码被拒、邮箱形式共享计数、清理后恢复；
  - 配置：OSS 密钥不回显且"不带字段 PUT"库值保留；`clean?daysToKeep=0` 400；
  - 导出：comments 200 条流式导出成功、文件为合法 JSON 数组；
  - 敏感词：变体录入判重、检测命中、评论同步拦截；分类：禁用分类匿名不可见/管理员可见；
  - 图片（P3-18）：PNG→`image/png`（魔数 `89504E47`）、JPG→`image/jpeg`；
  - `[Follow Debug]` 日志出现 0 次。
  - 联调账号：`lt_user`/`lt_admin`（密码 `Liantiao!2026`，测试库新增、保留复用）；验证码经 Redis 直连注入（修复后的 `StringRedisTemplate` 通道可用）。
  - 联调观察（范围外，未修）：`blog_test` 缺 `article_tags` 表 → 含 keyword 的全文搜索报 SQL 错误（"标签从未落地"的半残留契约，非本轮改动）；`getCommentById` 对不存在评论返回通用文案。
- **仍未修（本轮范围外）**：存疑 3 项（P3-9 / P3-17 / P3-19）、上轮 19 项 P3 遗留、上述 18 项历史遗留失败

### 实施期事实纠正（重要）

- **P3-5（`allowComment`）的原始描述有误**：报告称"`articles` 表已有 `allow_comment` 列（`schema.sql:401`）、仅实体未映射"。核实后确认 `schema.sql:401` 属于 `article_moderation_submissions`（审核快照表），`articles` 表**从未有**该列——该开关从未落库，属"从未实现"而非"映射遗漏"。
- 用户确认按"**加迁移、真正生效**"处理：新增加法迁移 `database/migrations/20261006_p2_articles_allow_comment.sql`（`ALTER TABLE articles ADD COLUMN allow_comment tinyint DEFAULT 1 COMMENT '是否允许评论：0-不允许，1-允许'`），并同步 `database/schema.sql`、`src/test/resources/schema-h2.sql`、`CLAUDE.md` 迁移清单。
- 既有库需在维护窗口按文件名顺序执行该加法迁移（默认 1，存量文章行为不变）；测试库 `blog_test` 已应用并验证（`allow_comment tinyint default 1`，0 篇文章为 0）。
- 连带修正的回归：`editArticle` 定向更新对 `summary/coverImage/categoryId/topicId` 改为"非 null 才 set"，对齐原 `updateById` 的 NOT_NULL 语义，避免前端不提交 `topicId` 时清空 `topic_id`。
- 死代码清理：`CommentMapper.selectDirectChildComments` 因评论删除路径改 BFS 批量查询而失去唯一生产调用方，按项目规范删除（含 DAO 测试对应断言）。

---

## 一、P2（10 项，建议尽快修复）

### P2-1 敏感词过滤可被大小写/变体绕过，且无法通过录词自愈（内容审核 fail-open）
- **位置**：`src/main/java/com/blog/utils/SensitiveWordFilter.java:62-92`（Trie 构建）、`:111-137`（匹配）；`SensitiveWordServiceImpl.java:110-114`；`mapper/SensitiveWordMapper.java:45`
- **问题**：Trie 构建与匹配全程逐字符精确比较，无大小写/全角归一化；种子词本身含大小写混排（`src/main/resources/sql/sensitive_words_data.sql:10` `('傻B',...)`、`:35` `('二B',...)`）。用户输入 `傻b` 即不命中。加剧点：`sensitive_words` 表 `collate = utf8mb4_unicode_ci`（`database/schema.sql:384`，大小写不敏感），管理员想补录 `傻b` 会被 `existsSensitiveWord`（`WHERE word = #{word}`）判为已存在而拒绝 → 内存 Trie 不匹配、库又录不进，无法自愈。
- **影响**：文章创建/编辑（`ArticleServiceImpl.java:122、169`）与评论创建（`CommentServiceImpl.java:116`）的**同步敏感词关卡**可被轻量变体绕过（AI 审核为异步二道防线，不影响本项成立）。
- **打假**：✅ 已读源码与种子数据确认；表 collation、Mapper SQL、两处调用点均逐行复核。

### P2-2 评论详情缓存仍在事务提交前清除，已删评论可被反缓存 24h（P2-10 修复不完整）
- **位置**：`CommentServiceImpl.java:422-424`（删除循环内直接清详情缓存）对照 `:460-461`（列表缓存已改 afterCommit）、`:1097-1116`（afterCommit 工具方法）、`:355`（详情缓存 TTL 24h）
- **问题**：P2-10 只把文章级列表缓存改为提交后清除；`deleteComment` 循环内的 `redisCacheUtils.deleteCache(generateCommentDetailKey(...))` 仍处于 `@Transactional` 内。清缓存与提交之间的窗口内，并发 `getCommentById`（`:327-357`）回源读到未提交的旧行并重新写入 24h 缓存，提交后无二次清除（全仓仅 `:424` 一处清详情键）。
- **影响**：已删除评论可通过 `GET /api/comment/{id}` 继续返回最长 24 小时（隐私/一致性）。
- **打假**：✅ 已读删除链路、缓存读写两侧与 TTL 确认；竞态触发依赖时序（毫秒窗口），与上轮 P2-10 同性质。

### P2-3 系统配置接口仍明文回显 OSS 密钥（P1-4 修复不完整）
- **位置**：`SystemConfigServiceImpl.java:330-331`（`getFileUploadConfig` 直接回填 oss key/secret）对照 `:410-419`（`toDTO` 仅对 `smtp_password` 脱敏）
- **问题**：P1-4 修复只改了日志行（不再打印 DTO），密钥回显口径未统一；`getAllSystemConfigs`/`getSystemConfigsByType` 走 `toDTO` 同样原样回显 `oss_access_key`/`oss_secret_key`。
- **影响**：管理员 GET 配置接口即拿回明文密钥（接口 `hasRole('admin')`，`SecurityConfig.java:69`）；一旦日志/响应被留档或越权读取即为密钥泄露。
- **打假**：✅ 已读两处源码确认；与上轮 P1-4 修复建议（"对 secret 类配置统一脱敏"）对照，确属未完成部分。

### P2-4 登录失败锁定仅按账号维度，可被用于账户锁定 DoS（P2-2 修复不完整）
- **位置**：`UserServiceImpl.java:280-285`（按输入 username 判锁定）、`:341-343`（累加）
- **问题**：锁定键为 `login:fail:{输入的账号字符串}`，无 IP 维度、不区分用户名/邮箱/手机号。攻击者仅需过图形验证码（`/api/captcha/**` 公开，可自动化获取）后对目标用户名连打 5 次错误密码，即可让受害人在 15 分钟内无法登录（正确密码也被拦截）；反向地，同一 IP 扫多个账号完全不受限。
- **影响**：可用性攻击（对特定账号）；同时防护方向单一，绕过多账号字典攻击不受阻。
- **打假**：✅ 锁定/累加逻辑已读确认；上轮 P2-2 修复建议原文"按用户/IP"，仅实现用户维度。

### P2-5 `githubLogin` 在 `@Transactional` 内发起 GitHub 外部 HTTP，事务内占用 DB 连接
- **位置**：`UserServiceImpl.java:1305-1307`（`@Transactional` 覆盖整个方法）、`:1330-1395`（restTemplate 调用换取 token/拉取用户信息）
- **问题**：事务边界内执行多次外部 HTTP（GitHub 慢/超时时无上限等待），期间持有 HikariCP 连接。
- **影响**：GitHub 侧抖动时并发登录可耗尽连接池，拖垮全部 DB 请求（可用性）。
- **打假**：✅ 事务注解范围与 HTTP 调用位置已读确认。

### P2-6 `PageUtils.calculateOffset` 整型溢出，7 处调用方含公开接口（P2-4/P2-6 同类遗漏）
- **位置**：`utils/PageUtils.java:48-50`（`getValidPage` 无上界）、`:103-104`（`(page-1)*size` int 溢出）；调用点：`CommentServiceImpl.java:234、545、918`（含公开评论列表）、`UserFavoriteServiceImpl.java:192`、`UserLikeServiceImpl.java:221`、`WebsiteStatisticsServiceImpl.java:141`
- **问题**：`page≈2.15 亿`（size=10）即溢出为负 offset → `LIMIT -x,y` → MySQL 1064 → 接口统一错误（公开端点可被匿名触发错误响应与错误日志）。
- **影响**：合法大页码请求返回错误；上轮 P2-4/P2-6 只修了具体调用点，公共工具类未收口。
- **打假**：✅ 工具类与全部 7 处调用点已 grep + 抽读确认。

### P2-7 `ArticleSearchServiceImpl` 偏移量溢出（P2-6 修复不完整）
- **位置**：`ArticleSearchServiceImpl.java:65-74`（快速搜索）、`:189-199`（按作者搜索）
- **问题**：只钳了 `pageNum` 下界与 `pageSize` 上限，`pageNum` 无上界，`(pageNum-1)*pageSize` int 溢出为负 → SQL 偏移异常。
- **影响**：`/api/search/quick`、按作者搜索大页码请求报错。
- **打假**：✅ 两处均已读源码确认。

### P2-8 `/api/article/hot` 的 `limit` 无上限，公开端点可放大资源消耗
- **位置**：`ArticleRankServiceImpl.java:122-135`；`ArticleController.java:113-119`（默认 10、直接透传）；`SecurityConfig.java:71`（GET permitAll）
- **问题**：仅归一化下界，`fetchLimit=(int)(limit*1.5)+10` 后一次取 ZSet 全量并 `selectBatchIds` 全量查询、全量组装响应；且 `@Cacheable` 键含 limit（`:120`），不同 limit 值可膨胀缓存条目。
- **影响**：匿名单请求可触发大范围 Redis+DB+响应体，配合多 limit 值可用于拖垮实例。
- **打假**：✅ 归一化缺失与缓存键构成已读确认。

### P2-9 数据导出接口仍全量缓冲，大表易 OOM（P2-14 修复不完整）
- **位置**：`DataBackupServiceImpl.java:205/207、222/224、239/241`（`queryForList` 整表载入 `List<Map>`）对照 `:365-370`（P2-14 已给 `.sql` 导出的 `exportTableData` 加流式 `setFetchSize(Integer.MIN_VALUE)`）
- **问题**：JSON 导出三条链路（用户/文章/评论）仍一次性载入；无过滤参数时全表导出。
- **影响**：大表导出 OOM → 500，核心运维功能不可用（接口 admin-only）。
- **打假**：✅ 已读未修与已修两处对照确认。

### P2-10 清理统计接口 `daysToKeep` 无下限校验，`0` 即清空全部访问日志
- **位置**：`WebsiteStatisticsServiceImpl.java:164-171`；`WebsiteStatisticsController.java:89-94`（`defaultValue="90"`，无 `@Min`）
- **问题**：`daysToKeep=0` → `cutoff=now` → `deleteBeforeDate(now)` 删除全部历史日志；负数更甚。
- **影响**：管理员误传即不可逆清空访问日志与统计基础数据（接口 admin-only）。
- **打假**：✅ 已读实现与控制器确认；建议加 `@Min(1)`。

---

## 二、P3（20 项，择机修复）

| # | 问题 | 位置 | 打假结论 |
|---|------|------|----------|
| 1 | GitHub OAuth 兜底 catch 把原始异常文本回传公开回调（`throw new BusinessException(ERROR, e.getMessage())`，含 DuplicateKeyException 表/约束名；并发双击时 state 的 get/delete 非原子可放大触发） | `UserServiceImpl.java:1489-1497`、`GlobalExceptionHandler.java:79-84` | ✅ 确认；程度有限，对齐上轮 P3-9 判例 |
| 2 | `/api/user/token/validate` 不检查 access token 黑名单（登出后旧 token 仍报 valid=true），与过滤器行为不一致 | `UserServiceImpl.java:521-544` 对照 `JwtAuthenticationFilter.java:50` | ✅ 确认；**前端当前无调用方**（grep 仅定义），故降级 |
| 3 | 取消关注非幂等：重复取消抛"未关注该用户"（点赞/收藏已幂等，此处未对齐） | `UserServiceImpl.java:972-980` | ✅ 确认 |
| 4 | 作者排行榜残留 `[Follow Debug]` 调试日志（6+ 处 INFO），permitAll 端点可被匿名高频刷日志 | `UserServiceImpl.java:1007-1064` | ✅ 确认；内容为公开昵称，非 PII |
| 5 | `Article.allowComment` 标记 `@TableField(exist=false)`，而表列存在（`schema.sql:401`）→ 文章级"允许评论"开关从未持久化、无消费方，前端设置被静默丢弃 | `entity/Article.java:107-112`；快照链路 `ArticleModerationSubmissionServiceImpl.java:120-122` | ✅ 确认功能失效；存疑是否刻意废弃（需产品确认） |
| 6 | `/api/article/recommended` 裸拼 `LIMIT + limit`：负值 → SQL 报错 500，超大值 → 全量返回 + 缓存键膨胀 | `ArticleQueryServiceImpl.java:318-344`；`ArticleController.java:121-126` | ✅ 确认 |
| 7 | `editArticle` 非发布分支整实体 `updateById`：① status=3（已删除/下线）文章可被作者直写内容且不走审核提交 ② 沿用 NOT_NULL 全列回写会覆盖毫秒窗口内的计数增量（P2-8 同类遗漏） | `ArticleServiceImpl.java:176-189`；`Article.java:284-286` | ✅ 确认；前者不可见危害有限、后者低概率 |
| 8 | `getHotArticlesPage` 超大 `page` 溢出（`(page-1)*size` → 负 `subList` → IndexOutOfBounds，被 catch 转错误文案） | `ArticleRankServiceImpl.java:231-238、293-298` | ✅ 确认；同类分页修复遗漏 |
| 9 | 评论审核 `process()` 在 `@Transactional` 内 catch 后不重抛：`pass()` 抛"审核任务已被处理"（`changed!=1`）时，此前已应用的快照/状态更新不随异常回滚，仅任务被标 RETRY，调度器可重复处理 | `ArticleModerationSubmissionServiceImpl.java:65-88、134-137` | ⚠️ 存疑；重试状态机或有意的容错设计，但违反"事务内禁止吞异常"约定，触发窗口窄 |
| 10 | 评论列表/删除存在 N+1：组装子评论逐条 `selectById(replyToId)`（为取 `replyToUserId`，可批量化）；`collectAllChildComments` 递归对已压平的二级评论再发一次查询 | `CommentServiceImpl.java:287-301、490-501`；`CommentMapper.java:159` | ✅ 确认；修正子代理"dict 已含所需信息"的表述 |
| 11 | 通知列表分页不校验：`(page-1)*size` 直拼，`page=0` → 负 LIMIT 报错（controller 仅 defaultValue） | `NotificationServiceImpl.java:99-105`；`NotificationController.java:35-42` | ✅ 确认 |
| 12 | 负 `parentId` 评论入库为"幽灵评论"：`parent_id<0` 不归零也不校验，列表永不显示，但审核通过后仍 `incrementCommentCount`、创建时已加热度分 | `CommentServiceImpl.java:121-145、893-895`；`CommentCreateDTO.java:20-21`（无 `@Min`） | ✅ 确认 |
| 13 | 热门评论/子评论"全量加载后内存切片"：SQL 无 LIMIT，取全量后内存排序/`subList` 分页（热门文章下大内存分配） | `CommentServiceImpl.java:852-859、921-927`；`CommentMapper.java:25-33`（无 LIMIT） | ✅ 确认 |
| 14 | 应用关闭刷写访问日志遇首个失败批次即 `break`，剩余缓冲日志静默丢弃（与 flush 的重试+回灌策略不一致） | `AccessLogBufferService.java:131-148` | ✅ 确认 |
| 15 | 公开分类列表不过滤 status：`selectList(null)` 返回含禁用分类，现成的 `selectAllActiveCategories()` 未被使用 | `CategoryServiceImpl.java:34-46`；`CategoryMapper.java:37-38` | ✅ 确认 |
| 16 | 每次浏览量事件都同步 `evictAll`（热榜 Caffeine 缓存 30s TTL 被高频清空，命中率趋近 0）并追加 2 条 INFO 日志 | `ArticleEventListener.java:22-27、43-46`；`ArticleStatisticsServiceImpl.java:152` | ✅ 确认；缓存近乎失效 + 日志噪声 |
| 17 | 访问日志日期口径不一致：写入用应用时区 `LocalDate.now()`，查询用 DB `CAST(NOW() AS DATE)`，时区不一致时跨零点窗口错位 | `AccessLogInterceptor.java:57`；`WebsiteAccessLogMapper.java:79-88` | ⚠️ 存疑；取决于部署时区配置 |
| 18 | 压缩图片响应硬编码 `Content-Type: image/jpeg`，而输出格式跟随源 MIME（PNG/GIF 输入 → 字节与头不符） | `ImageController.java:153-162`；`ImageProcessor.java:376-390`；`ImageProcessingServiceImpl.java:190` | ✅ 确认 |
| 19 | `TOSConfig.acl`（默认 `public-read`）定义后从未被上传路径读取，公共读 URL 的可用性完全依赖桶级策略 | `TOSConfig.java:48-51`；`TOSServiceImpl` 无 acl 使用（grep 证实） | ⚠️ 存疑；取决于桶配置，属配置死项 |
| 20 | `hotArticlesTtl` 死配置：独立声明后无任何消费方（`CacheConfig` 统一用 `defaultTtl`），全仓仅定义处 1 处引用 | `CaffeineCacheConfig.java:35-38`；`CacheConfig.java:48-53` | ✅ 确认；当前两者同为 30s 无行为差异 |

---

## 三、打假记录（降级 / 修正 / 合并 / 维持存疑）

- **降级 2 项**：`token/validate` 黑名单漏检（P2→P3：前端 grep 无调用方，且受保护端点已被过滤器 401 拦截，无实际绕过）；OAuth 异常回传（P2→P3：信息泄露程度有限，与上轮 P3-9"搜索回显 e.getMessage"判例对齐）。
- **修正描述 2 项**：评论 N+1 的动机是取 `replyToUserId`（子代理"`nicknameDict` 已含所需信息"不准确，dict 仅存昵称）；作者榜日志含公开昵称（非子代理所称 PII），但日志刷屏本身成立。
- **维持存疑 3 项**：评论审核 `process` 吞异常（B6/P3-9）、访问日志时区口径（D7/P3-17）、TOS `acl` 未使用（E4/P3-19）。
- **合并说明**：P2-6 与 P2-7 同根源（int 偏移溢出）但分属工具类与搜索实现，分列便于修复；B5（P3-8）为同类分页溢出在排行分页处的第三处遗漏。
- **复读确认修复正确、未见回归**：P1-2 楼中楼通知（`replyToCommentId` 取值）、P1-3 删评论扣分口径（status∈{1,2}）、P2-7 排行"全量过滤后分页"（total=过滤后总数）、P2-5 搜索建议 SQL、P2-9 收藏幂等、P2-11 Trie 局部构建 + volatile 赋值均已按修复方案落地。
- **剔除误报 0 项**：本轮 30 项均有源码证据，无完全误报。

## 四、上轮遗留状态（2026-10-04 报告的 P3 × 19）

- 上轮 19 项 P3 在本轮基线 `60ed78f` 中**均未修复**（该提交只含 18 项 P1/P2 修复）。
- 本轮抽查：上轮 P3-1（子评论 `Collectors.toMap` 昵称 NULL NPE，`CommentServiceImpl.java:279` 虽有 merge 函数但仍不防 null value）仍存在；其余 18 项未逐一复核，维持上轮报告描述。
- 本轮 7 项"修复不完整/同类遗漏"（P2-2、P2-3、P2-4、P2-6、P2-7、P2-9、以及 P3-7）建议与对应上轮条目合并处理。

## 五、建议修复顺序

1. **第一批（安全/隐私，成本低）**：P2-1 敏感词归一化、P2-2 详情缓存 afterCommit、P2-3 OSS 脱敏、P2-10 `@Min(1)`。
2. **第二批（可用性/稳定性）**：P2-4 锁定维度、P2-5 事务外置、P2-6/P2-7 分页钳位收口（含 P3-8）、P2-8 limit 钳位。
3. **第三批（数据/运维）**：P2-9 导出流式化、P3-16 缓存失效节流、P3-14 关闭刷写兜底、P3-15 分类过滤。
4. **第四批（P3 清理与契约）**：P3-5 allowComment 契约决策、P3-9 事务内吞异常复核、P3-4 调试日志清理、P3-19/P3-20 死配置处理。

> 依据项目工作规范：修复前先建 `fix/<scope>` 分支；每批修复后跑 AGENTS.md 中的定向测试集；P2-1、P2-2、P2-8、P3-13 建议补针对性测试。