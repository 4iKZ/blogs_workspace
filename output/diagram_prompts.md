---

## [D1] 图3-1 系统整体架构图

**Prompt:**
A professional academic architecture diagram showing the layered structure of a full-stack blog system. The diagram is a top-to-bottom directed graph rendered in clean, top-journal style.

Five horizontal layers, each as a rounded light-gray background band with a subtle label on the left:
- Top: "客户端" (single blue node: "浏览器 — Vue 3 SPA")
- "反向代理层" (single orange node: "Nginx")
- "业务服务层" (three green nodes in a row: "Spring Boot 3 RESTful API", "Spring Security JWT认证", "事件驱动异步处理")
- "缓存层" (two connected nodes: "Caffeine L1 本地缓存" arrow-> "Redis L2 分布式缓存")
- Bottom: "持久层" (blue node: "MySQL 8.0") + "存储层" (gray node: "火山引擎 TOS")

Arrows connect layers: HTTP/HTTPS from client to Nginx, reverse-proxy arrow from Nginx to API, query arrows from API down through Caffeine->Redis->MySQL (labeled "未命中" on fall-through edges), and a direct arrow from API to TOS. Node shapes are rounded rectangles with thin borders, using muted professional colors (navy #1a237e, teal #00695c, warm gray #616161). The composite cache manager chain (Caffeine→Redis→MySQL) should be visually emphasized as the key innovation. White background, no decorative elements, generous spacing between layers. 16:9, vector-diagram quality suitable for top CS journal publication.

**中文简述:** 五层架构图——客户端→Nginx→业务服务(Spring Boot/Security/事件)→缓存(Caffeine→Redis)→持久(MySQL)/存储(TOS)。缓存级联链路重点突出，顶刊风格干净矢量图。

---

## [D2] 图3-2 系统功能模块图

**Prompt:**
A professional functional module diagram for a blog system, rendered as a clean top-journal-style directed graph. Eight rounded-rectangle nodes arranged in a logical layout, each containing a module name (bold) and a subtitle describing its responsibility:

- "用户模块" (blue, subtitle: "注册/登录/个人信息")
- "文章模块" (green, subtitle: "创作/发布/浏览")
- "分类模块" (teal, subtitle: "多级分类管理")
- "评论模块" (purple, subtitle: "嵌套评论/点赞")
- "互动模块" (orange, subtitle: "点赞/收藏/关注")
- "通知模块" (amber, subtitle: "消息推送/已读标记")
- "后台管理" (dark gray, subtitle: "数据统计/内容管理")
- "文件模块" (indigo, subtitle: "上传/分片/存储")

All 8 nodes are enclosed in a large dashed-border container labeled "Lumina博客系统". Directed dependency arrows connect modules showing their relationships: 用户→文章/评论/互动/通知; 文章→分类/文件; 评论→通知; 互动→通知; 后台→用户/文章/评论. Arrow lines are thin (#888) with small arrowheads. Nodes have 8px border radius and subtle 1px borders. The overall layout should be balanced and readable, with modules positioned to minimize arrow crossing. White background, clean typography in a sans-serif font (bold module names at 14pt, subtitles at 10pt). 16:9, professional quality.

**中文简述:** 八大功能模块关系图——用户/文章/分类/评论/互动/通知/后台/文件，虚线大框围住，箭头表示模块间依赖，配色区分，布局整洁最小化交叉。

---

## [D4] 图3-4 系统全局E-R图

**Prompt:**
A professional Entity-Relationship diagram for a blog database, rendered in clean academic top-journal style. Six entity rectangles connected by crow's-foot relationship lines:

Entities (each as a clean rectangle with title bar and attribute list):
1. "users" — title bar in dark blue, attributes: id (PK), username (UK), email (UK), password, nickname, role, status
2. "articles" — title bar in dark green, attributes: id (PK), user_id (FK), category_id (FK), title, content, view_count, like_count, comment_count, status
3. "comments" — title bar in teal, attributes: id (PK), article_id (FK), user_id (FK), parent_id, content, like_count
4. "categories" — title bar in purple, attributes: id (PK), name, parent_id, sort_order, article_count
5. "notifications" — title bar in amber, attributes: id (PK), user_id (FK), sender_id (FK), type, target_id, content, is_read
6. "article_views" + "user_likes" + "user_favorites" + "user_follows" + "comment_likes" — smaller auxiliary entity boxes in light gray

Relationships shown with standard crow's-foot notation: ||--o{ (one-to-many) and relationship labels in Chinese on the lines ("撰写", "发表", "接收", "点赞", "收藏", "关注", "包含", "属于", "被点赞", "被收藏", "被浏览"). Primary keys marked with underline, foreign keys with (FK) suffix. The layout should position users and articles as the central hub entities with others radiating outward. White background, thin 1px entity borders, clean monospace font for attribute names. The entire diagram should look like a textbook-quality database schema illustration. 16:9.

**中文简述:** 全局数据库E-R图——6个核心实体(users/articles/comments/categories/notifications)+辅助实体，标准鸡爪线表示一对多关系，中文关系标签，教科书级别数据库模式图。

---

## [D5] 图3-5 核心业务表E-R图

**Prompt:**
A focused Entity-Relationship diagram zooming into the core business tables of a blog system, rendered in top-journal academic style. This is a simplified version showing only the essential interactive relationships. Four main entity rectangles and three join tables:

Main entities:
1. "users" (dark blue title bar): id (PK), username, email, password, nickname
2. "articles" (dark green title bar): id (PK), user_id (FK), category_id (FK), title, content, view_count, like_count, comment_count
3. "comments" (teal title bar): id (PK), article_id (FK), user_id (FK), parent_id, content, like_count

Join/association tables (light gray, slightly smaller):
4. "user_likes": id (PK), user_id (FK), article_id (FK), create_time
5. "user_favorites": id (PK), user_id (FK), article_id (FK), create_time
6. "comment_likes": id (PK), user_id (FK), comment_id (FK), create_time

Crow's-foot relationships: users||--o{articles ("创作"), users||--o{comments ("评论"), users||--o{user_likes ("点赞操作"), users||--o{user_favorites ("收藏操作"), articles||--o{comments ("包含"), articles||--o{user_likes ("被点赞"), articles||--o{user_favorites ("被收藏"), comments||--o{comment_likes ("被点赞"), users||--o{comment_likes ("点赞评论"). Clean, spacious layout with users and articles as the central pair. Professional monospace font for attributes, thin borders, no decoration. 16:9, textbook-quality. Different from D4—this one is simpler, focusing on the core interaction flow. Make it visually distinct from the global E-R diagram.

**中文简述:** 核心业务表E-R图——聚焦users/articles/comments及user_likes/user_favorites/comment_likes三张关联表，鸡爪线+中文标签，比全局图更精简，突出互动核心链。

---

## [D6] 图4-1 JWT认证流程图

**Prompt:**
A professional UML-style sequence diagram showing the JWT authentication flow in a blog system, rendered in clean top-journal vector style. Four vertical lifelines with headers:

- "客户端" (leftmost, blue header)
- "认证过滤器" (center-left, green header, labeled "JwtAuthenticationFilter")
- "SecurityContext" (center-right, purple header)
- "Redis" (rightmost, red header)

The sequence of interactions (top to bottom):
1. Client → Filter: horizontal arrow labeled "请求带Authorization头"
2. Filter self-loop: "提取Bearer Token" 
3. Filter self-loop: "验证Token签名与有效期"
4. Filter → Redis: arrow labeled "检查Token是否在黑名单"
5. Redis → Filter: dashed return arrow "黑名单状态"
6. An ALT block (rectangular frame with dashed border covering two branches):
   - Upper branch (labeled "Token有效且不在黑名单"): Filter → SecurityContext arrow "设置认证上下文", SecurityContext → Client dashed return "放行请求" (green success path)
   - Lower branch (labeled "Token无效或已被加入黑名单"): Filter → Client dashed arrow "返回401未认证" (red failure path)

Lifelines are thin vertical dotted lines. Activation bars (thin rectangles on lifelines) show active periods. Arrows have clean labels in sans-serif 10pt. The ALT block frame is light gray with subtle rounded corners. White background, professional typography. 16:9, top-journal software engineering diagram quality. The image should convey the elegance of the stateless JWT authentication architecture.

**中文简述:** UML序列图——客户端→过滤器→SecurityContext→Redis四级交互，提取Token→验证→黑名单检查→认证决策(ALT分叉:放行/401)，顶刊软件工程图风格。
---

## [D7] 图4-3 多级缓存架构图

**Prompt:**
A top-journal quality architecture diagram illustrating a two-level cache architecture (Caffeine L1 + Redis L2) with a cascaded query chain and write-around consistency strategy. The diagram should be clean, vector-style, white background, 16:9.

The diagram has TWO panels side by side:

--- LEFT PANEL (70% width): READ PATH — 级联查询流程 ---

A horizontal flow from left to right with 5 stages, each as a rounded rectangular node:

Stage 1: "客户端请求" (light gray box, labeled "Browser / API Call")
  ↓ arrow labeled "HTTP Request"
Stage 2: "Spring Cache 切面" (blue box #1565c0, white text) with sub-label "@Cacheable 拦截"
  ↓ arrow
Stage 3: "CompositeCacheManager" (dark teal box #00695c, white text) — this is a LARGER container that visually wraps two sub-nodes:
    - "Caffeine L1" (green box #2e7d32, white text) — sub-label: "maxSize=1000 · TTL=30s · recordStats"
    - "Redis L2" (red box #c62828, white text) — sub-label: "hotArticles: 3min · hotArticlesPage: 2min · default: 5min"
  
  The Caffeine L1 box has a RIGHT arrow labeled "HIT →" going upward to a "返回结果" node.
  Below Caffeine L1, an arrow labeled "MISS ↓" points to Redis L2.
  From Redis L2, two outgoing paths:
    - Arrow RIGHT labeled "HIT → 回填 Caffeine" leading up to "返回结果" (green success path)
    - Arrow DOWN labeled "MISS ↓"

Stage 4: "MySQL 8.0" (dark blue box #0d47a1, white text) — sub-label: "HikariCP 连接池"
  ↓ arrow labeled "DB Query"
Stage 5: "回填 Redis → 回填 Caffeine → 返回结果" (three stacked arrows in light gray, converging to "返回结果")

Final node on far right: "返回结果" (green filled circle/rounded rect #4caf50, white text)

--- RIGHT PANEL (30% width): WRITE PATH — 缓存一致性策略 ---

A vertical flow with 3 strategies shown as stacked labeled sections:

Strategy 1 (top): "延迟双删"
  - Icon: clock
  - Text: "写DB前删除缓存 → 写DB → 延迟1500ms再删缓存"
  - Config: "enableDoubleDelete=true · delayedDeleteMs=1500"
  - Visual: A small timeline with 3 dots (delete→write→delay→delete)

Strategy 2 (middle): "异步缓存失效"
  - Icon: lightning bolt
  - Text: "@CacheEvict → Spring Event → 异步Listener → 清理远端实例缓存"
  - Config: "enableAsyncInvalidation=true"
  - Visual: Small event bus diagram (publisher → event → async listener → cache clear)

Strategy 3 (bottom): "定期一致性校验"
  - Icon: checkmark / clipboard
  - Text: "每5分钟抽样100条缓存Key · 对比DB当前值 · 不一致则主动失效"
  - Config: "enableVerification=true · interval=5min · sampleSize=100"
  - Visual: Small batch process icon with sample counter

--- STYLING ---

- All text in sans-serif font (similar to Helvetica or Noto Sans)
- Stage boxes: 8px border radius, thin 1.5px borders
- Arrow lines: 2px width, clean triangular arrowheads
- Color palette strictly: blue #1565c0, green #2e7d32, red #c62828, teal #00695c, dark blue #0d47a1, gray #757575
- Labels on arrows in 9pt gray italic
- The CompositeCacheManager container should have a subtle dashed border to show it wraps both L1 and L2
- NO decorative gradients, NO drop shadows, NO 3D effects — flat academic vector style
- White background throughout
- Title at top: "图4-3 多级缓存架构图" in 14pt bold
- Subtitle: "Caffeine (L1 本地缓存) + Redis (L2 分布式缓存) 级联查询与一致性保障" in 10pt gray

**中文简述:** 双栏布局——左侧为主（Caffeine→Redis→MySQL 三级级联查询链，含命中/未命中分支和回填逻辑，标注TTL/容量参数），右侧为辅（延迟双删+异步失效+定期校验三种一致性策略，含源码配置值）。CompositeCacheManager作为包裹L1/L2的虚线大框。顶刊矢量图风格。

**源码依据:**
- CompositeCacheManager(caffeineCacheManager, redisCacheManager) — CacheConfig.java:45
- Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(30s).recordStats() — CacheConfig.java:65-68
- Redis TTL: hotArticles=3min, hotArticlesPage=2min, default=5min — CacheConfig.java:95-97
- 延迟双删: enableDoubleDelete=true, delayedDeleteMs=1500 — CacheConsistencyConfig.java
- 异步失效: enableAsyncInvalidation=true, 通过 PersistentCacheInvalidationScheduler 投递
- 定期校验: enableVerification=true, interval=5min, sampleSize=100 — CacheConsistencyConfig.java
---

## [D3] 图3-3 系统部署架构图

**Prompt:**
A professional deployment architecture diagram for the "Lumina" blog system, rendered in flat vector top-journal style. Six horizontal layers stack top-to-bottom, each as a full-width colored band with a left-edge label and nodes inside. White background, 16:9, clean sans-serif typography.

LAYER 1 — "用户层" (light blue band #e3f2fd, label in dark blue #1565c0):
  Single node: "浏览器" (solid blue rounded rect #1565c0, white text, 14pt bold).

LAYER 2 — "接入层" (light orange band #fff3e0, label in dark orange #e65100):
  Two nodes side by side:
  - "Nginx 反向代理" (solid orange rounded rect #e65100, white text, slightly larger/thicker border as the single entry point)
  - "静态资源服务" (medium orange #ff9800, white text, same height but narrower)

LAYER 3 — "应用层" (light green band #e8f5e9, label in dark green #2e7d32):
  Three nodes in a row:
  - "Spring Boot 实例 #1" (solid green #2e7d32, white text)
  - "Spring Boot 实例 #2" (same style, identical)
  - "... ×N 水平扩展" (lighter green #a5d6a7, dark green text, smaller, dashed border — indicating elastic scaling beyond 2 instances)

LAYER 4 — "中间件层" (light red band #ffebee, label in dark red #c62828):
  Single node: "Redis 7.0" (solid red #c62828, white text)
  Sub-label below node in 8pt gray italic: "缓存 · 分布式锁 · ZSet 排行 · 会话管理"

LAYER 5 — "数据持久层" (light indigo band #e8eaf6, label in dark indigo #283593):
  Single node: "MySQL 8.0" (solid dark blue #283593, white text)
  Sub-label below node in 8pt gray italic: "InnoDB · HikariCP 连接池"

LAYER 6 — "对象存储层" (light purple band #f3e5f5, label in dark purple #6a1b9a):
  Single node: "火山引擎 TOS" (solid purple #6a1b9a, white text)
  Sub-label: "图片/文件 · CDN 加速"

ARROWS (2px lines, clean triangular arrowheads):
- Browser → Nginx: solid arrow labeled "HTTPS"
- Nginx → Static Server: solid arrow labeled "静态资源"
- Nginx → Instance #1: solid arrow labeled "API 代理 (负载均衡)"
- Nginx → Instance #2: same style, identical label
- Instance #1 → Redis: DOTTED arrow labeled "缓存读写" (dotted to indicate lighter/faster interaction)
- Instance #2 → Redis: same dotted style
- Instance #1 → MySQL: DOTTED arrow labeled "数据读写"
- Instance #2 → MySQL: same dotted style
- Instance #1 → TOS: solid arrow labeled "文件上传"
- Instance #2 → TOS: same solid style

The two Spring Boot instances and the Nginx are visually connected: Nginx has two outgoing arrows splitting left and right to the two instances, creating a clean "load distribution" visual. The two instance nodes should be equidistant from center, with the Redis and MySQL nodes centered below them.

STYLING RULES:
- All nodes: 6px border radius, 1.5px borders
- Layer bands: full-width, 2px top/bottom borders only, translucent fills
- NO gradients, NO drop shadows, NO 3D effects
- Layer labels: 11pt bold, positioned at the left edge of each band, rotated -90° or placed as a vertical text label
- Node text: 12pt bold for main label, 8pt light for sub-labels
- Arrow labels: 8pt italic gray #757575
- Title at top center: "图3-3 系统部署架构图" in 16pt bold #333
- Subtitle: "Nginx 反向代理 + 多实例水平扩展 + 独立中间件与持久层" in 10pt gray #888

**中文简述:** 六层部署架构——用户层(浏览器)→接入层(Nginx+静态资源)→应用层(双Spring Boot实例+×N弹性扩展)→中间件层(Redis 7.0)→数据持久层(MySQL 8.0)→对象存储层(火山引擎TOS)。实线表示HTTP/文件流，虚线表示缓存/数据读写。Nginx略大加粗作为唯一入口。扁平矢量学术风格。
