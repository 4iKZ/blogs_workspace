# 论文截图生成 Prompt 清单
# 论文: 基于SpringBoot和Redis的博客系统的设计与实现
# 风格要求: 顶刊风格 — 干净留白、低饱和配色、信息层级清晰、字号对比明确、无冗余装饰

---

## [01] 图4-1-1 用户登录界面

**Prompt:**
A clean professional login page UI for a modern blog platform named "Lumina".
The layout is centered on a white/light-gray background. At the top-center, the "Lumina" logo and tagline "让代码发光，让思考留痕" are displayed in a dark serif font. Below that, a login card with subtle shadow contains: a "登录" heading, a username/email input field with a user icon placeholder, a password input field with a lock icon, a "忘记密码?" link aligned right, a full-width dark blue "登 录" button with rounded corners, and a divider "或" with "还没有账号？立即注册" link below. The overall color palette uses a dark navy (#1a237e) accent, white cards, and muted gray text (#666). No cluttered elements—minimal, academic-journal style. The screenshot should show the form in its unfilled state. 16:9 aspect ratio, high resolution, no browser chrome visible.

**中文简述:** 干净的居中登录卡片，白色背景，深蓝主色调，品牌名"Lumina"+标语，用户名/密码输入框，登录按钮，底部注册引导链接。

---

## [02] 图4-1-2 用户注册界面

**Prompt:**
A clean registration page UI for the "Lumina" blog platform. White/light-gray background with a centered registration card. The card contains: a "创建账号" heading at the top, followed by form fields in sequence—username, email, password, confirm password (each with a subtle input label above and a light gray border, rounded 8px). Below the password fields, a small captcha/image-verification section with a 4-character distorted text image and a refresh button on the right. A "发送验证码" button next to the email field. Checkbox for "我已阅读并同意《用户协议》". A full-width dark navy "注 册" button. The overall style is minimal, academic, with generous whitespace between form elements. No background imagery, no gradient decoration. High resolution, 16:9, professional journal figure quality.

**中文简述:** 白色背景居中注册卡片，依次排列用户名/邮箱/密码/确认密码输入框，邮箱旁"发送验证码"按钮，图形验证码区域，协议勾选框，注册按钮。

---

## [03] 图4-2-1 Markdown编辑器创作界面

**Prompt:**
A split-screen Markdown editor interface for academic blog writing. The left half shows raw Markdown text with code blocks, headings (# and ##), bold markers (**text**), and inline links—using a monospace font on dark/light editor background. The right half shows the rendered preview: formatted headings in a serif font, syntax-highlighted code blocks with a dark background and colored keywords, properly styled paragraphs with comfortable line-height. A thin toolbar is visible at the top with icons for Bold, Italic, Heading, Code, Image, and Table. The overall interface uses a professional dark sidebar on the far left for navigation (minimal icons), a light content area. The color scheme is restrained: off-white content background, charcoal text, muted blue accent for toolbar icons. 16:9 ratio, crisp rendering, no real text needed—use placeholder lorem-ipsum-style content in Chinese. Top journal figure quality.

**中文简述:** 分屏Markdown编辑器，左侧源码/右侧预览，工具栏在最上方，左侧深色导航栏，整体干净学术风，代码块带语法高亮。

---

## [04] 图4-2-2 文章详情浏览界面

**Prompt:**
An article detail page for a modern academic-style blog. The layout: top navigation bar with "Lumina" logo, Home, Categories, Search icon, and user avatar on the right. Below, a large hero area showing the article title in bold 28pt serif font, author name + avatar + publish date on one line, and a cover image (abstract geometric gradient in teal-purple). The article body below is clean Markdown-rendered content: section headings, body paragraphs in 16px font with comfortable 1.8 line-height, a syntax-highlighted code block in a light gray rounded box, and an inline KaTeX math formula. At the bottom, like/heart button with count (e.g., "128"), bookmark icon with count, and share button. The page has generous side margins (about 200px on each side on desktop). Right sidebar shows author card (avatar, name, bio) and related articles list. Color palette: #fff background, #1a1a2e headings, #333 body text, #e8e8e8 separators. Professional, readable, journal-quality screenshot. 16:9.

**中文简述:** 文章详情页，顶部导航栏，大标题+作者信息+封面图，正文含Markdown渲染内容、代码块、数学公式，底部点赞/收藏按钮，右侧作者卡片+相关文章。

---

## [05] 图4-3-1 首页热门文章列表（缓存加速效果）

**Prompt:**
A blog homepage showing a grid of article cards, designed in a clean academic-journal style. Top: a minimal navigation bar with logo and category links. Below, a hero banner area with a subtle gradient (light blue to white), reading "热门文章" with a small flame icon and a tab switcher showing "日榜 | 周榜". Below the banner, a 2-column grid of article cards (6 cards visible). Each card has: a small rectangular cover thumbnail on the left (abstract geometric pattern in muted colors), title in dark 18pt font (2 lines max), author name and avatar on one line, a brief excerpt in gray (#888) 14pt, and bottom row stats—views (eye icon), likes (heart icon), comments (bubble icon) with numbers, and a category tag (small rounded pill, light blue background). Cards have subtle 1px gray borders and 8px border-radius, gentle shadow on hover state (show one card with hover shadow). Overall clean, editorial, top-journal figure quality. The UI should convey "fast, cached content loading" through its polished, instantly-rendered appearance. 16:9.

**中文简述:** 博客首页，顶部"热门文章"日榜/周榜切换标签，2列文章卡片网格，每张卡片含缩略图/标题/作者/摘要/统计数据/分类标签，干净编辑风格。

---

## [06] 图4-4-1 文章热度排行榜界面

**Prompt:**
A dedicated "热门排行" page showing ranked articles in a list format with ranking numbers. At the top: "热门排行" heading with subtitle "基于浏览、点赞、评论和收藏的加权热度计算". Below, a ranked list of 8 articles. Each row has: a large ranking number on the left (#1 in gold/amber, #2 in silver/gray, #3 in bronze/copper, #4-8 in muted gray), article title (18pt, dark), a brief excerpt (14pt, gray), and on the right side a vertical "热度" score bar (like a mini horizontal bar chart, dark blue fill proportional to score, with numeric score next to it). The #1 article row has a subtle warm highlight background (#fff8e1). Between the list and header, a tab switcher for "日榜" and "周榜" with an underline active indicator. The design is data-visualization-influenced but minimal—like a well-designed table in a top CS journal. Professional, clean, 16:9.

**中文简述:** 热门排行页面，前3名金银铜数字标识，每条含排名编号/文章标题/摘要/热度分数条，日榜周榜切换标签，数据可视化风格。

---

## [07] 图4-5-1 后台敏感词管理界面

**Prompt:**
An admin dashboard page for sensitive word management in the "Lumina" blog system. A clean admin panel with a dark sidebar on the left (containing menu items: Dashboard, Users, Articles, Comments, Categories, Sensitive Words, Settings—with "Sensitive Words" highlighted). The main content area shows a white card containing: a top action bar with "+ 添加敏感词" primary button, a search input, a category filter dropdown, and a "批量导入" secondary button. Below, a data table with columns: 序号, 敏感词, 分类, 级别 (with colored badges: 禁止=red, 审核=orange, 提醒=yellow), 创建时间, 操作 (编辑/删除 icon buttons). The table has alternating white/light-gray row backgrounds, 1px separator lines. Pagination controls at the bottom right. Overall professional CMS-style interface, clean and functional, top journal figure quality. 16:9.

**中文简述:** 后台敏感词管理页面，左侧深色菜单栏（敏感词高亮），右侧白色内容区，含添加/搜索/筛选/批量导入操作栏，敏感词数据表格（含级别彩色标签），底部分页。

---

## [08] 图4-6-1 图片上传界面（带进度条）

**Prompt:**
A file upload dialog/area within the Markdown editor showing a large file being uploaded with chunked progress. The interface shows a dashed-border drop zone area (light gray, 2px dashed #bbb) with text "拖拽图片到此处或点击上传". Below, an active upload item card showing: a thumbnail preview (small 80x80 px, showing a landscape photo placeholder), file name ("DSC_2847.jpg"), file size ("12.8 MB"), a horizontal progress bar (about 65% filled, dark blue #1a237e fill with subtle animation suggestion), percentage text ("65%"), uploaded size detail ("8.3 MB / 12.8 MB"), a chunk counter ("分片 7/11"), and upload speed ("2.4 MB/s"). The design is clean and technical, like a developer tool UI—no decorative elements, just clear information hierarchy. The progress bar has rounded ends and a subtle shimmer effect on the filled portion. Professional, top journal quality. 16:9.

**中文简述:** Markdown编辑器内的图片上传区域，虚线拖拽框，上传进度卡片显示缩略图、文件名、文件大小、蓝色进度条(65%)、分片计数(7/11)、上传速度，干净技术工具风格。

---

## [09] 图4-7-1 文章评论区界面

**Prompt:**
An article comment section below an article, showing nested (threaded) comments. At the top: "评论 (36)" heading with a sort selector ("最新 | 最热"). A comment input box at the top with a user avatar placeholder (40px circle) and a text area with "写下你的评论..." placeholder, with a "发表" button on the right. Below, the comment list showing 3 threaded levels: Level 1—a top-level comment with user avatar, username, timestamp ("2小时前"), comment body text, and action row (like icon with count "12", reply button). Level 2—two indented reply comments (with a vertical thread line on the left), smaller font, showing replied-to username in blue (@username), reply text, and like count. Level 3—one further indented reply. Commenter avatars show abstract geometric profile icons in varied muted colors (teal, coral, indigo). The design is clean, similar to academic discussion boards, with generous spacing. White background, subtle gray separators. 16:9, top journal quality.

**中文简述:** 文章评论区，顶部评论数+排序选择，评论输入框，下方三级嵌套评论（楼中楼），左侧缩进线，头像+用户名+时间+回复文本+点赞数，学术讨论风格。

---

## [10] 图4-7-2 消息通知中心界面

**Prompt:**
A notification center page for the "Lumina" blog platform. Page heading: "消息通知" with an "全部标记为已读" link on the right and an unread count badge ("3条未读"). Below, a list of notification cards, each showing: a small colored icon on the left indicating notification type (a comment bubble for comment replies, a heart for likes, a user-plus for follows, a star for system notifications), notification title in bold (e.g., "张三 评论了你的文章"), timestamp in gray ("3小时前"), a brief content preview ("写得很好，受益匪浅！"), and a subtle blue dot indicator for unread notifications. The unread cards have a very subtle light blue background tint (#f0f7ff), while read cards have a pure white background. Cards have 1px separator lines between them. The design is clean, minimal, like an email inbox but more modern. Color palette: white, #1a237e accent, #888 secondary text. 16:9, professional journal quality.

**中文简述:** 通知中心页面，"消息通知"标题+已读标记+未读数，通知卡片列表含类型图标/标题/时间/内容预览，未读项淡蓝色背景+蓝色圆点标记，邮箱收件箱风格。

---

## [11] 图4-8-1 后台数据统计仪表板

**Prompt:**
An admin statistics dashboard showing data visualization charts for a blog platform. The layout: top row has 4 stat cards in a horizontal row—"今日PV (12,847)", "今日UV (3,421)", "文章总数 (256)", "注册用户 (1,892)"—each card with a large number, a label below, and a small trend arrow indicator (green up arrow with percentage). Below, a 2-column grid: left column shows a line chart ("PV/UV 趋势 — 近7天") with two overlapping lines (dark blue for PV, teal for UV), date labels on X-axis, clean grid lines. Right column shows a horizontal bar chart ("热门文章 Top 5") with article titles truncated on the left and bar lengths proportional to view count, with numbers at the end of each bar. Bottom row: a pie/donut chart ("设备分布") showing Desktop (52%), Mobile (38%), Tablet (10%) in muted professional colors, with a legend below. The overall design is business-intelligence style but restrained—think academic data visualization in top journals. White cards with subtle shadows, dark sidebar, professional chart colors. 16:9.

**中文简述:** 后台统计仪表板，顶部4个核心指标卡片（PV/UV/文章数/用户数含趋势箭头），左侧PV/UV折线图，右侧热门文章横向柱状图，底部设备分布饼图，学术数据可视化风格。

---

## [12] 图4-9-1 系统首页（浅色主题）

**Prompt:**
The full homepage of the "Lumina" blog platform in light mode. Top: a sticky navigation bar with white background, "Lumina" logo in dark serif font on the left, navigation links (首页, 分类, 热门, 关于) in the center, and a search icon + user avatar menu on the right. Below the nav: a hero section with a subtle gradient background (light gray to white), featuring a large welcoming headline "探索知识与思想的边界" in 32pt dark serif font, a subtitle in lighter gray, and a prominent search bar in the center. Below that, a "热门文章" section header with a flame emoji, followed by a 3-column grid of featured article cards (each with a colorful abstract cover image, title, author, and stats). Further below, a "最新发布" section with a 2-column list layout showing recent articles in a more compact card style. Footer with copyright and links. The overall aesthetic is light, airy, editorial—white and off-white backgrounds, generous whitespace, dark navy accent color. Top journal figure quality, showing the full page layout. 16:9.

**中文简述:** 浅色主题首页全貌，顶部白色固定导航栏含Logo/链接/搜索/头像，Hero区域大标题+搜索栏，"热门文章"3列卡片网格，"最新发布"2列列表，底部页脚。宽松留白，编辑风格。

---

## [13] 图4-9-2 系统首页（深色主题）

**Prompt:**
The same "Lumina" blog homepage but in dark mode/theme. The navigation bar is now dark charcoal (#1e1e2e) with white text. The hero section background is a deep dark gradient (#1a1a2e to #16213e). The welcoming headline reads "探索知识与思想的边界" in off-white (#e0e0e0) serif font. The article cards have a dark slate background (#252540) with 1px subtle borders (#333). Card cover images remain colorful but with reduced brightness. Text colors: primary text in #e8e8e8, secondary text in #999, accent color is a muted teal (#64ffda). The search bar has a dark background with light border. Stats numbers (views, likes) use the teal accent. The overall dark theme should feel sophisticated and modern—like a premium code editor's dark mode, not just inverted colors. The footer uses a slightly darker shade (#111122). Professional, top journal dark-mode figure quality. 16:9.

**中文简述:** 深色主题首页，深炭色导航栏白色文字，Hero区域深色渐变背景，文章卡片深灰底色，青色强调色，文字层次分明，高端代码编辑器暗色风格。

---

## [14] 图4-9-3 移动端适配效果

**Prompt:**
A mobile-responsive view of the "Lumina" blog homepage displayed on a smartphone mockup. The phone screen shows the blog in a single-column responsive layout: a compact hamburger-menu navigation bar at the top with the "Lumina" logo centered, a simplified hero section with smaller heading text, the search bar spanning full width below, article cards stacked vertically in a single column (each card full-width with the cover image on top, title below, author row, and stat icons), bottom fixed tab bar with Home/Search/Notifications/Profile icons. The design adapts the desktop layout gracefully—font sizes are proportionally scaled, cards are edge-to-edge with 16px horizontal padding, interactive elements (buttons, links) have adequate touch targets (minimum 44px). The phone mockup should have a minimal bezel, showing the UI in a realistic device context. Light mode shown. Professional, top journal responsive-design figure. Portrait orientation, clean presentation.

**中文简述:** 手机模型展示移动端响应式首页，单列布局，汉堡菜单+居中Logo导航，Hero文字缩小，文章卡片全宽竖向堆叠，底部固定Tab栏，触摸友好的点击区域，真实手机壳外观。
