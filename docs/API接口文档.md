# 博客网站 API 接口文档

## 接口概述
- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token
- **数据格式**: JSON

## Admin 模块

### getUserList
**GET** `/api/admin/users`

**参数说明:**
`@Parameter(description = "页码"`

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<PageResult<UserDTO>>`

---

### updateUserStatus
**PUT** `/api/admin/users/{userId}/status`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回类型:** `Result<Void>`

---

### deleteUser
**DELETE** `/api/admin/users/{userId}`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回类型:** `Result<Void>`

---

### getArticleList
**GET** `/api/admin/articles`

**参数说明:**
`@Parameter(description = "页码"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### updateArticleStatus
**PUT** `/api/admin/articles/{articleId}/status`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### deleteArticle
**DELETE** `/api/admin/articles/{articleId}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### getCommentList
**GET** `/api/admin/comments`

**参数说明:**
`@Parameter(description = "页码"`

**返回 CommentDTO 字段:**
- id (Long): 
- articleId (Long): 
- parentId (Long): 
- content (String): 
- userId (Long): 
- nickname (String): 
- email (String): 
- website (String): 
- avatar (String): 
- status (Integer): 
- likeCount (Integer): 
- liked (Boolean): 
- replyCount (Integer): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- children (List<CommentDTO>): 
- replyToCommentId (Long): 
- replyToUserId (Long): 
- replyToNickname (String): 

**返回类型:** `Result<PageResult<CommentDTO>>`

---

### updateSystemConfig
**PUT** `/api/admin/config`

**参数说明:**
`@RequestBody Map<String, String> config`

**返回类型:** `Result<Void>`

---

### backupDatabase
**POST** `/api/admin/backup`

**参数说明:**
``

**返回类型:** `Result<String>`

---

### clearCache
**POST** `/api/admin/cache/clear`

**参数说明:**
``

**返回类型:** `Result<Void>`

---

## Article 模块

### publishArticle
**POST** `/api/article/publish`

**参数说明:**
`@Valid @RequestBody ArticleCreateDTO articleCreateDTO`

**ArticleCreateDTO 字段:**
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- categoryId (Long): 
- topicId (Long): 

**返回类型:** `Result<Long>`

---

### editArticle
**PUT** `/api/article/{articleId:[0-9]+}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### deleteArticle
**DELETE** `/api/article/{articleId:[0-9]+}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### getArticleList
**GET** `/api/article/list`

**参数说明:**
`@Parameter(description = "页码"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### getArticleDetail
**GET** `/api/article/{articleId:[0-9]+}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<ArticleDTO>`

---

### getHotArticles
**GET** `/api/article/hot`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<List<ArticleDTO>>`

---

### getRecommendedArticles
**GET** `/api/article/recommended`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<List<ArticleDTO>>`

---

### getUserArticles
**GET** `/api/article/user/{userId}`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### getUserLikedArticles
**GET** `/api/article/user/{userId}/liked`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### getUserFavoriteArticles
**GET** `/api/article/user/{userId}/favorite`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### uploadCoverImage
**POST** `/api/article/upload-cover`

**参数说明:**
`@Parameter(description = "图片文件"`

**返回类型:** `Result<String>`

---

### getPresignedUploadUrl
**POST** `/api/article/upload-presign`

**参数说明:**
`@Valid @RequestBody PreSignedUploadRequestDTO request`

**PreSignedUploadRequestDTO 字段:**
- fileName (String): 
- contentType (String): 
- fileSize (Long): 

**返回 PreSignedUploadResponseDTO 字段:**
- signedUrl (String): 
- publicUrl (String): 
- objectKey (String): 
- expiresIn (long): 

**返回类型:** `Result<PreSignedUploadResponseDTO>`

---

### searchArticles
**GET** `/api/article/search`

**参数说明:**
`@Parameter(description = "搜索关键词"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### getArticlesByCategory
**GET** `/api/article/category/{categoryId}`

**参数说明:**
`@Parameter(description = "分类ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### getFollowingArticles
**GET** `/api/article/following`

**参数说明:**
`@Parameter(description = "页码"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<PageResult<ArticleDTO>>`

---

### cancelChunkedUpload
**POST** `/api/article/cancel-upload`

**参数说明:**
`@Parameter(description = "上传ID"`

**返回类型:** `Result<Void>`

---

### getUploadStatus
**GET** `/api/article/upload-status/{uploadId}`

**参数说明:**
`@Parameter(description = "上传ID"`

**返回类型:** `Result<ChunkedUploadService.ChunkedUploadStatus>`

---

## ArticleSearch 模块

### searchArticles
**POST** `/api/search/article`

**参数说明:**
`@Parameter(description = "搜索请求参数"`

**返回 SearchResultDTO 字段:**
- articleId (Long): 
- title (String): 
- summary (String): 
- content (String): 
- coverImage (String): 
- authorId (Long): 
- authorName (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- publishTime (String): 
- relevanceScore (Double): 
- matchedField (String): 

**返回类型:** `Result<List<SearchResultDTO>>`

---

### quickSearch
**GET** `/api/search/quick`

**参数说明:**
`@Parameter(description = "搜索关键词"`

**返回 SearchResultDTO 字段:**
- articleId (Long): 
- title (String): 
- summary (String): 
- content (String): 
- coverImage (String): 
- authorId (Long): 
- authorName (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- publishTime (String): 
- relevanceScore (Double): 
- matchedField (String): 

**返回类型:** `Result<List<SearchResultDTO>>`

---

### getSearchSuggestions
**GET** `/api/search/suggestions`

**参数说明:**
`@Parameter(description = "搜索关键词"`

**返回类型:** `Result<List<String>>`

---

### getHotKeywords
**GET** `/api/search/hot-keywords`

**参数说明:**
`@Parameter(description = "返回数量"`

**返回类型:** `Result<List<String>>`

---

### getSearchStatistics
**GET** `/api/search/statistics`

**参数说明:**
`@Parameter(description = "搜索关键词"`

**返回 SearchStatisticsDTO 字段:**
- totalResults (Long): 
- searchTime (Long): 
- keyword (String): 
- categoryId (Long): 
- categoryName (String): 
- count (Long): 
- tagId (Long): 
- tagName (String): 
- count (Long): 
- timeRange (String): 
- count (Long): 

**返回类型:** `Result<SearchStatisticsDTO>`

---

### searchByCategory
**GET** `/api/search/category/{categoryId}`

**参数说明:**
`@Parameter(description = "分类ID"`

**返回 SearchResultDTO 字段:**
- articleId (Long): 
- title (String): 
- summary (String): 
- content (String): 
- coverImage (String): 
- authorId (Long): 
- authorName (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- publishTime (String): 
- relevanceScore (Double): 
- matchedField (String): 

**返回类型:** `Result<List<SearchResultDTO>>`

---

### searchByTag
**GET** `/api/search/tag/{tagId}`

**参数说明:**
`@Parameter(description = "标签ID"`

**返回 SearchResultDTO 字段:**
- articleId (Long): 
- title (String): 
- summary (String): 
- content (String): 
- coverImage (String): 
- authorId (Long): 
- authorName (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- publishTime (String): 
- relevanceScore (Double): 
- matchedField (String): 

**返回类型:** `Result<List<SearchResultDTO>>`

---

### searchByAuthor
**GET** `/api/search/author/{authorId}`

**参数说明:**
`@Parameter(description = "作者ID"`

**返回 SearchResultDTO 字段:**
- articleId (Long): 
- title (String): 
- summary (String): 
- content (String): 
- coverImage (String): 
- authorId (Long): 
- authorName (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- publishTime (String): 
- relevanceScore (Double): 
- matchedField (String): 

**返回类型:** `Result<List<SearchResultDTO>>`

---

## ArticleStatistics 模块

### getHotArticleStatistics
**GET** `/api/statistics/article/hot`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 ArticleStatisticsDTO 字段:**
- articleId (Long): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- shareCount (Integer): 
- lastStatisticsTime (String): 

**返回类型:** `Result<List<ArticleStatisticsDTO>>`

---

### getTopArticleStatistics
**GET** `/api/statistics/article/top`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 ArticleStatisticsDTO 字段:**
- articleId (Long): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- shareCount (Integer): 
- lastStatisticsTime (String): 

**返回类型:** `Result<List<ArticleStatisticsDTO>>`

---

### getRecommendedArticleStatistics
**GET** `/api/statistics/article/recommended`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 ArticleStatisticsDTO 字段:**
- articleId (Long): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- shareCount (Integer): 
- lastStatisticsTime (String): 

**返回类型:** `Result<List<ArticleStatisticsDTO>>`

---

### getArticleStatistics
**GET** `/api/statistics/article/{articleId}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回 ArticleStatisticsDTO 字段:**
- articleId (Long): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- shareCount (Integer): 
- lastStatisticsTime (String): 

**返回类型:** `Result<ArticleStatisticsDTO>`

---

### incrementViewCount
**POST** `/api/statistics/article/view/{articleId}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### incrementLikeCount
**POST** `/api/statistics/article/like/{articleId}/increment`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### decrementLikeCount
**POST** `/api/statistics/article/like/{articleId}/decrement`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### incrementCommentCount
**POST** `/api/statistics/article/comment/{articleId}/increment`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### decrementCommentCount
**POST** `/api/statistics/article/comment/{articleId}/decrement`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### incrementFavoriteCount
**POST** `/api/statistics/article/favorite/{articleId}/increment`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### decrementFavoriteCount
**POST** `/api/statistics/article/favorite/{articleId}/decrement`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

## Captcha 模块

### generateCaptcha
**POST** `/api/captcha/generate`

**参数说明:**
``

**返回类型:** `Result<String>`

---

### getCaptchaImage
**GET** `/api/captcha`

**参数说明:**
``

**返回类型:** `Result<CaptchaResponse>`

---

## Category 模块

### getCategoryList
**GET** `/api/category/list`

**参数说明:**
``

**返回 CategoryDTO 字段:**
- id (Long): 
- name (String): 
- description (String): 
- sortOrder (Integer): 
- articleCount (Long): 

**返回类型:** `Result<List<CategoryDTO>>`

---

### getCategoryById
**GET** `/api/category/{categoryId}`

**参数说明:**
`@Parameter(description = "分类ID"`

**返回 CategoryDTO 字段:**
- id (Long): 
- name (String): 
- description (String): 
- sortOrder (Integer): 
- articleCount (Long): 

**返回类型:** `Result<CategoryDTO>`

---

### addCategory
**POST** `/api/category`

**参数说明:**
`@Valid @RequestBody CategoryCreateDTO categoryCreateDTO`

**CategoryCreateDTO 字段:**
- name (String): 
- description (String): 
- sortOrder (Integer): 

**返回类型:** `Result<Long>`

---

### updateCategory
**PUT** `/api/category/{categoryId}`

**参数说明:**
`@Parameter(description = "分类ID"`

**返回类型:** `Result<Void>`

---

### deleteCategory
**DELETE** `/api/category/{categoryId}`

**参数说明:**
`@Parameter(description = "分类ID"`

**返回类型:** `Result<Void>`

---

### getCategoryArticleCount
**GET** `/api/category/{categoryId}/count`

**参数说明:**
`@Parameter(description = "分类ID"`

**返回类型:** `Result<Integer>`

---

## Comment 模块

### createComment
**POST** `/api/comment`

**参数说明:**
`@Valid @RequestBody CommentCreateDTO commentCreateDTO`

**CommentCreateDTO 字段:**
- articleId (Long): 
- parentId (Long): 
- replyToCommentId (Long): 
- content (String): 
- nickname (String): 
- email (String): 
- website (String): 
- userId (Long): 

**返回类型:** `Result<Long>`

---

### getCommentList
**GET** `/api/comment/list`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回 CommentDTO 字段:**
- id (Long): 
- articleId (Long): 
- parentId (Long): 
- content (String): 
- userId (Long): 
- nickname (String): 
- email (String): 
- website (String): 
- avatar (String): 
- status (Integer): 
- likeCount (Integer): 
- liked (Boolean): 
- replyCount (Integer): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- children (List<CommentDTO>): 
- replyToCommentId (Long): 
- replyToUserId (Long): 
- replyToNickname (String): 

**返回类型:** `Result<List<CommentDTO>>`

---

### checkCommentLikeStatus
**GET** `/api/comment/{commentId}/like-status`

**参数说明:**
`@Parameter(description = "评论ID"`

**返回类型:** `Result<Boolean>`

---

### getHotComments
**GET** `/api/comment/hot`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回 CommentDTO 字段:**
- id (Long): 
- articleId (Long): 
- parentId (Long): 
- content (String): 
- userId (Long): 
- nickname (String): 
- email (String): 
- website (String): 
- avatar (String): 
- status (Integer): 
- likeCount (Integer): 
- liked (Boolean): 
- replyCount (Integer): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- children (List<CommentDTO>): 
- replyToCommentId (Long): 
- replyToUserId (Long): 
- replyToNickname (String): 

**返回类型:** `Result<List<CommentDTO>>`

---

### checkSensitiveWords
**POST** `/api/comment/check-sensitive`

**参数说明:**
`@RequestBody ContentRequest contentRequest`

**返回类型:** `Result<Boolean>`

---

### replaceSensitiveWords
**POST** `/api/comment/replace-sensitive`

**参数说明:**
`@RequestBody ContentRequest contentRequest`

**返回类型:** `Result<String>`

---

### getChildComments
**GET** `/api/comment/children`

**参数说明:**
`@Parameter(description = "父评论ID"`

**返回 CommentDTO 字段:**
- id (Long): 
- articleId (Long): 
- parentId (Long): 
- content (String): 
- userId (Long): 
- nickname (String): 
- email (String): 
- website (String): 
- avatar (String): 
- status (Integer): 
- likeCount (Integer): 
- liked (Boolean): 
- replyCount (Integer): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- children (List<CommentDTO>): 
- replyToCommentId (Long): 
- replyToUserId (Long): 
- replyToNickname (String): 

**返回类型:** `Result<List<CommentDTO>>`

---

### getCommentById
**GET** `/api/comment/{commentId}`

**参数说明:**
`@Parameter(description = "评论ID"`

**返回 CommentDTO 字段:**
- id (Long): 
- articleId (Long): 
- parentId (Long): 
- content (String): 
- userId (Long): 
- nickname (String): 
- email (String): 
- website (String): 
- avatar (String): 
- status (Integer): 
- likeCount (Integer): 
- liked (Boolean): 
- replyCount (Integer): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- children (List<CommentDTO>): 
- replyToCommentId (Long): 
- replyToUserId (Long): 
- replyToNickname (String): 

**返回类型:** `Result<CommentDTO>`

---

### deleteComment
**DELETE** `/api/comment/{commentId}`

**参数说明:**
`@Parameter(description = "评论ID"`

**返回类型:** `Result<Void>`

---

### getArticleCommentCount
**GET** `/api/comment/article/{articleId}/count`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Integer>`

---

### getUserComments
**GET** `/api/comment/user/{userId}`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回 CommentDTO 字段:**
- id (Long): 
- articleId (Long): 
- parentId (Long): 
- content (String): 
- userId (Long): 
- nickname (String): 
- email (String): 
- website (String): 
- avatar (String): 
- status (Integer): 
- likeCount (Integer): 
- liked (Boolean): 
- replyCount (Integer): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- children (List<CommentDTO>): 
- replyToCommentId (Long): 
- replyToUserId (Long): 
- replyToNickname (String): 

**返回类型:** `Result<List<CommentDTO>>`

---

### likeComment
**POST** `/api/comment/{commentId}/like`

**参数说明:**
`@Parameter(description = "评论ID"`

**返回类型:** `Result<Void>`

---

### unlikeComment
**DELETE** `/api/comment/{commentId}/like`

**参数说明:**
`@Parameter(description = "评论ID"`

**返回类型:** `Result<Void>`

---

## DataBackup 模块

### createDatabaseBackup
**POST** `/api/system/backup/database`

**参数说明:**
`@Parameter(description = "备份名称"`

**返回 BackupInfoDTO 字段:**
- backupId (Long): 
- fileName (String): 
- filePath (String): 
- fileSize (Long): 
- backupType (String): 
- description (String): 
- createTime (LocalDateTime): 
- status (String): 

**返回类型:** `Result<BackupInfoDTO>`

---

### getBackupList
**GET** `/api/system/backup/list`

**参数说明:**
``

**返回 BackupInfoDTO 字段:**
- backupId (Long): 
- fileName (String): 
- filePath (String): 
- fileSize (Long): 
- backupType (String): 
- description (String): 
- createTime (LocalDateTime): 
- status (String): 

**返回类型:** `Result<List<BackupInfoDTO>>`

---

### deleteBackup
**DELETE** `/api/system/backup/{backupId}`

**参数说明:**
`@Parameter(description = "备份ID"`

**返回类型:** `Result<Void>`

---

### restoreDatabase
**POST** `/api/system/backup/restore/{backupId}`

**参数说明:**
`@Parameter(description = "备份ID"`

**返回类型:** `Result<Void>`

---

### exportUserData
**POST** `/api/system/backup/export/user`

**参数说明:**
`@Parameter(description = "用户ID，可选"`

**返回 ExportInfoDTO 字段:**
- exportId (Long): 
- fileName (String): 
- filePath (String): 
- fileSize (Long): 
- exportType (String): 
- recordCount (Long): 
- createTime (LocalDateTime): 
- status (String): 

**返回类型:** `Result<ExportInfoDTO>`

---

### exportArticleData
**POST** `/api/system/backup/export/article`

**参数说明:**
`@Parameter(description = "分类ID，可选"`

**返回 ExportInfoDTO 字段:**
- exportId (Long): 
- fileName (String): 
- filePath (String): 
- fileSize (Long): 
- exportType (String): 
- recordCount (Long): 
- createTime (LocalDateTime): 
- status (String): 

**返回类型:** `Result<ExportInfoDTO>`

---

### exportCommentData
**POST** `/api/system/backup/export/comment`

**参数说明:**
`@Parameter(description = "文章ID，可选"`

**返回 ExportInfoDTO 字段:**
- exportId (Long): 
- fileName (String): 
- filePath (String): 
- fileSize (Long): 
- exportType (String): 
- recordCount (Long): 
- createTime (LocalDateTime): 
- status (String): 

**返回类型:** `Result<ExportInfoDTO>`

---

### getExportFileList
**GET** `/api/system/backup/export/list`

**参数说明:**
``

**返回 ExportInfoDTO 字段:**
- exportId (Long): 
- fileName (String): 
- filePath (String): 
- fileSize (Long): 
- exportType (String): 
- recordCount (Long): 
- createTime (LocalDateTime): 
- status (String): 

**返回类型:** `Result<List<ExportInfoDTO>>`

---

### deleteExportFile
**DELETE** `/api/system/backup/export/{exportId}`

**参数说明:**
`@Parameter(description = "导出ID"`

**返回类型:** `Result<Void>`

---

### downloadBackup
**GET** `/api/system/backup/download/{backupId}`

**参数说明:**
`@Parameter(description = "备份ID"`

**返回类型:** `ResponseEntity<Resource>`

---

### downloadExportFile
**GET** `/api/system/backup/export/download/{exportId}`

**参数说明:**
`@Parameter(description = "导出ID"`

**返回类型:** `ResponseEntity<Resource>`

---

## FileUpload 模块

### uploadImage
**POST** `/api/file/upload/image`

**参数说明:**
`@Parameter(description = "图片文件"`

**返回类型:** `Result<String>`

---

### uploadFile
**POST** `/api/file/upload/file`

**参数说明:**
`@Parameter(description = "文件"`

**返回 FileInfoDTO 字段:**
- id (Long): 
- fileName (String): 
- fileType (String): 
- fileSize (Long): 
- fileMd5 (String): 
- fileUrl (String): 
- filePath (String): 
- uploadUserId (Long): 
- uploadUserName (String): 
- uploadTime (String): 
- status (Integer): 

**返回类型:** `Result<FileInfoDTO>`

---

### batchUploadFiles
**POST** `/api/file/upload/batch`

**参数说明:**
`@Parameter(description = "文件列表"`

**返回 FileInfoDTO 字段:**
- id (Long): 
- fileName (String): 
- fileType (String): 
- fileSize (Long): 
- fileMd5 (String): 
- fileUrl (String): 
- filePath (String): 
- uploadUserId (Long): 
- uploadUserName (String): 
- uploadTime (String): 
- status (Integer): 

**返回类型:** `Result<List<FileInfoDTO>>`

---

### getFileList
**GET** `/api/file/list`

**参数说明:**
`@Parameter(description = "页码"`

**返回 FileInfoDTO 字段:**
- id (Long): 
- fileName (String): 
- fileType (String): 
- fileSize (Long): 
- fileMd5 (String): 
- fileUrl (String): 
- filePath (String): 
- uploadUserId (Long): 
- uploadUserName (String): 
- uploadTime (String): 
- status (Integer): 

**返回类型:** `Result<List<FileInfoDTO>>`

---

### getFileInfo
**GET** `/api/file/{fileId}`

**参数说明:**
`@Parameter(description = "文件ID"`

**返回 FileInfoDTO 字段:**
- id (Long): 
- fileName (String): 
- fileType (String): 
- fileSize (Long): 
- fileMd5 (String): 
- fileUrl (String): 
- filePath (String): 
- uploadUserId (Long): 
- uploadUserName (String): 
- uploadTime (String): 
- status (Integer): 

**返回类型:** `Result<FileInfoDTO>`

---

### deleteFile
**DELETE** `/api/file/{fileId}`

**参数说明:**
`@Parameter(description = "文件ID"`

**返回类型:** `Result<Void>`

---

### checkFileByMd5
**GET** `/api/file/check/md5/{md5}`

**参数说明:**
`@Parameter(description = "文件MD5值"`

**返回 FileInfoDTO 字段:**
- id (Long): 
- fileName (String): 
- fileType (String): 
- fileSize (Long): 
- fileMd5 (String): 
- fileUrl (String): 
- filePath (String): 
- uploadUserId (Long): 
- uploadUserName (String): 
- uploadTime (String): 
- status (Integer): 

**返回类型:** `Result<FileInfoDTO>`

---

## Image 模块

### extractMetadata
**POST** `/api/image/metadata`

**参数说明:**
`@Parameter(description = "图片文件"`

**返回 ImageMetadataDTO 字段:**
- fileName (String): 
- fileSize (Long): 
- width (Integer): 
- height (Integer): 
- aspectRatio (Double): 
- totalPixels (Long): 
- format (String): 
- mimeType (String): 
- colorType (String): 
- metadataFormat (String): 
- printWidthCm (Double): 
- printHeightCm (Double): 

**返回类型:** `Result<ImageMetadataDTO>`

---

### convertFormat
**POST** `/api/image/convert`

**参数说明:**
`@Parameter(description = "图片文件"`

**返回 ImageConvertDTO 字段:**
- originalFormat (String): 
- targetFormat (String): 
- originalSize (Long): 
- convertedSize (Long): 
- compressionRatio (Double): 
- width (Integer): 
- height (Integer): 
- mimeType (String): 

**返回类型:** `Result<ImageConvertDTO>`

---

### convertAndDownload
**POST** `/api/image/convert/download`

**参数说明:**
`@Parameter(description = "图片文件"`

**返回类型:** `ResponseEntity<byte[]>`

---

### batchConvertFormat
**POST** `/api/image/batch-convert`

**参数说明:**
`@Parameter(description = "图片文件列表"`

**返回 ImageConvertDTO 字段:**
- originalFormat (String): 
- targetFormat (String): 
- originalSize (Long): 
- convertedSize (Long): 
- compressionRatio (Double): 
- width (Integer): 
- height (Integer): 
- mimeType (String): 

**返回类型:** `Result<List<ImageConvertDTO>>`

---

### getSupportedFormats
**GET** `/api/image/formats`

**参数说明:**
``

**返回类型:** `Result<List<String>>`

---

### compressImage
**POST** `/api/image/compress`

**参数说明:**
`@Parameter(description = "图片文件"`

**返回类型:** `ResponseEntity<byte[]>`

---

### validateImage
**POST** `/api/image/validate`

**参数说明:**
`@Parameter(description = "图片文件"`

**返回类型:** `Result<Boolean>`

---

## Notification 模块

### getUnreadCount
**GET** `/api/notification/unread-count`

**参数说明:**
``

**返回类型:** `Result<Integer>`

---

### getNotificationList
**GET** `/api/notification/list`

**参数说明:**
`@Parameter(description = "页码"`

**返回 NotificationDTO 字段:**
- id (Long): 
- userId (Long): 
- senderId (Long): 
- senderNickname (String): 
- senderAvatar (String): 
- type (Integer): 
- typeName (String): 
- targetId (Long): 
- targetType (Integer): 
- targetTitle (String): 
- content (String): 
- isRead (Integer): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 

**返回类型:** `Result<PageResult<NotificationDTO>>`

---

### markAsRead
**PUT** `/api/notification/{id}/read`

**参数说明:**
`@Parameter(description = "消息ID"`

**返回类型:** `Result<Void>`

---

### markAllAsRead
**PUT** `/api/notification/read-all`

**参数说明:**
``

**返回类型:** `Result<Void>`

---

### deleteNotification
**DELETE** `/api/notification/{id}`

**参数说明:**
`@Parameter(description = "消息ID"`

**返回类型:** `Result<Void>`

---

## Search 模块

### searchByKeyword
**GET** `/api/search/legacy/keyword`

**参数说明:**
`@Parameter(description = "搜索关键词"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<List<ArticleDTO>>`

---

### searchByCategory
**GET** `/api/search/legacy/category/{categoryId}`

**参数说明:**
`@Parameter(description = "分类ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<List<ArticleDTO>>`

---

### searchByTag
**GET** `/api/search/legacy/tag/{tagId}`

**参数说明:**
`@Parameter(description = "标签ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<List<ArticleDTO>>`

---

### searchByAuthor
**GET** `/api/search/legacy/author/{authorId}`

**参数说明:**
`@Parameter(description = "作者ID"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<List<ArticleDTO>>`

---

### advancedSearch
**GET** `/api/search/legacy/advanced`

**参数说明:**
`@Parameter(description = "关键词"`

**返回 ArticleDTO 字段:**
- id (Long): 
- title (String): 
- content (String): 
- summary (String): 
- coverImage (String): 
- status (Integer): 
- allowComment (Integer): 
- viewCount (Integer): 
- likeCount (Integer): 
- commentCount (Integer): 
- favoriteCount (Integer): 
- authorId (Long): 
- authorNickname (String): 
- authorAvatar (String): 
- categoryId (Long): 
- categoryName (String): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 
- publishTime (LocalDateTime): 
- category (CategoryDTO): 
- liked (Boolean): 
- favorited (Boolean): 
- hotScore (Double): 

**返回类型:** `Result<List<ArticleDTO>>`

---

### getSearchSuggestions
**GET** `/api/search/legacy/suggestion`

**参数说明:**
`@Parameter(description = "搜索关键词"`

**返回类型:** `Result<List<String>>`

---

### getHotKeywordsLegacy
**GET** `/api/search/legacy`

**参数说明:**
``

**返回类型:** `Result<List<String>>`

---

## SystemConfig 模块

### getSystemConfig
**GET** `/api/system/config/{configKey}`

**参数说明:**
`@Parameter(description = "配置键"`

**返回 SystemConfigDTO 字段:**
- configId (Long): 
- configKey (String): 
- configValue (String): 
- description (String): 
- configType (String): 
- isEditable (Integer): 
- createdAt (String): 
- updatedAt (String): 

**返回类型:** `Result<SystemConfigDTO>`

---

### getAllSystemConfigs
**GET** `/api/system/config/all`

**参数说明:**
``

**返回 SystemConfigDTO 字段:**
- configId (Long): 
- configKey (String): 
- configValue (String): 
- description (String): 
- configType (String): 
- isEditable (Integer): 
- createdAt (String): 
- updatedAt (String): 

**返回类型:** `Result<List<SystemConfigDTO>>`

---

### getSystemConfigsByType
**GET** `/api/system/config/type/{configType}`

**参数说明:**
`@Parameter(description = "配置类型"`

**返回 SystemConfigDTO 字段:**
- configId (Long): 
- configKey (String): 
- configValue (String): 
- description (String): 
- configType (String): 
- isEditable (Integer): 
- createdAt (String): 
- updatedAt (String): 

**返回类型:** `Result<List<SystemConfigDTO>>`

---

### updateSystemConfig
**PUT** `/api/system/config`

**参数说明:**
`@Parameter(description = "系统配置信息"`

**返回类型:** `Result<Void>`

---

### batchUpdateSystemConfigs
**PUT** `/api/system/config/batch`

**参数说明:**
`@Parameter(description = "系统配置列表"`

**返回类型:** `Result<Void>`

---

### getWebsiteConfig
**GET** `/api/system/config/website`

**参数说明:**
``

**返回 WebsiteConfigDTO 字段:**
- websiteName (String): 
- websiteDescription (String): 
- websiteKeywords (String): 
- websiteLogo (String): 
- websiteFavicon (String): 
- websiteIcp (String): 
- websiteAnalytics (String): 
- websiteStatus (Integer): 
- closeMessage (String): 
- pageSize (Integer): 
- rssLimit (Integer): 
- commentStatus (Integer): 
- registerStatus (Integer): 

**返回类型:** `Result<WebsiteConfigDTO>`

---

### updateWebsiteConfig
**PUT** `/api/system/config/website`

**参数说明:**
`@Parameter(description = "网站配置信息"`

**返回类型:** `Result<Void>`

---

### getEmailConfig
**GET** `/api/system/config/email`

**参数说明:**
``

**返回 EmailConfigDTO 字段:**
- smtpHost (String): 
- smtpPort (Integer): 
- smtpUsername (String): 
- smtpPassword (String): 
- enableSsl (Integer): 
- fromEmail (String): 
- fromName (String): 
- emailEnabled (Integer): 

**返回类型:** `Result<EmailConfigDTO>`

---

### updateEmailConfig
**PUT** `/api/system/config/email`

**参数说明:**
`@Parameter(description = "邮件配置信息"`

**返回类型:** `Result<Void>`

---

### getFileUploadConfig
**GET** `/api/system/config/file-upload`

**参数说明:**
``

**返回 FileUploadConfigDTO 字段:**
- maxFileSize (Integer): 
- allowedImageTypes (String): 
- allowedFileTypes (String): 
- imageUploadPath (String): 
- fileUploadPath (String): 
- enableLocalStorage (Integer): 
- enableOssStorage (Integer): 
- ossAccessKey (String): 
- ossSecretKey (String): 
- ossBucketName (String): 
- ossEndpoint (String): 

**返回类型:** `Result<FileUploadConfigDTO>`

---

### updateFileUploadConfig
**PUT** `/api/system/config/file-upload`

**参数说明:**
`@Parameter(description = "文件上传配置信息"`

**返回类型:** `Result<Void>`

---

## User 模块

### register
**POST** `/api/user/register`

**参数说明:**
`@Valid @RequestBody UserRegisterDTO registerDTO`

**UserRegisterDTO 字段:**
- username (String): 
- password (String): 
- confirmPassword (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- position (String): 
- company (String): 
- captcha (String): 
- captchaKey (String): 

**返回类型:** `Result<String>`

---

### login
**POST** `/api/user/login`

**参数说明:**
`@Valid @RequestBody UserLoginDTO loginDTO`

**UserLoginDTO 字段:**
- username (String): 
- password (String): 
- captcha (String): 
- captchaKey (String): 

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<UserDTO>`

---

### logout
**POST** `/api/user/logout`

**参数说明:**
`@RequestHeader(value = "X-Refresh-Token", required = false`

**返回类型:** `Result<Void>`

---

### refreshToken
**POST** `/api/user/refresh-token`

**参数说明:**
`@Parameter(description = "刷新令牌"`

**返回 TokenRefreshResponseDTO 字段:**
- token (String): 
- refreshToken (String): 

**返回类型:** `Result<TokenRefreshResponseDTO>`

---

### validateToken
**GET** `/api/user/token/validate`

**参数说明:**
`@RequestHeader(value = "Authorization", required = false`

**返回类型:** `Result<Boolean>`

---

### getUserInfo
**GET** `/api/user/info`

**参数说明:**
``

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<UserDTO>`

---

### getPublicUserInfo
**GET** `/api/user/{userId}`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<UserDTO>`

---

### updateUserInfo
**PUT** `/api/user/info`

**参数说明:**
`@Valid @RequestBody UserUpdateDTO updateDTO`

**UserUpdateDTO 字段:**
- nickname (String): 
- email (String): 
- phone (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 

**返回类型:** `Result<Void>`

---

### changePassword
**PUT** `/api/user/password`

**参数说明:**
`@Valid @RequestBody ChangePasswordDTO changePasswordDTO,             @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false`

**ChangePasswordDTO 字段:**
- oldPassword (String): 
- newPassword (String): 

**返回类型:** `Result<Void>`

---

### resetPassword
**POST** `/api/user/reset-password`

**参数说明:**
`@Parameter(description = "邮箱地址"`

**返回类型:** `Result<Void>`

---

### sendResetCode
**POST** `/api/user/password/reset/send`

**参数说明:**
`@Valid @RequestBody SendResetCodeDTO sendResetCodeDTO`

**SendResetCodeDTO 字段:**
- email (String): 

**返回类型:** `Result<Void>`

---

### resetPasswordByCode
**POST** `/api/user/password/reset`

**参数说明:**
`@Valid @RequestBody ResetPasswordByCodeDTO resetPasswordByCodeDTO`

**ResetPasswordByCodeDTO 字段:**
- email (String): 
- code (String): 
- newPassword (String): 

**返回类型:** `Result<Void>`

---

### getUserList
**GET** `/api/user/admin/list`

**参数说明:**
`@Parameter(description = "页码"`

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<PageResult<UserDTO>>`

---

### updateUserStatus
**PUT** `/api/user/admin/status/{userId}`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回类型:** `Result<Void>`

---

### deleteUser
**DELETE** `/api/user/admin/{userId}`

**参数说明:**
`@Parameter(description = "用户ID"`

**返回类型:** `Result<Void>`

---

### followUser
**POST** `/api/user/follow/{followingId}`

**参数说明:**
`@Parameter(description = "被关注者ID"`

**返回类型:** `Result<Void>`

---

### unfollowUser
**DELETE** `/api/user/unfollow/{followingId}`

**参数说明:**
`@Parameter(description = "被关注者ID"`

**返回类型:** `Result<Void>`

---

### isFollowing
**GET** `/api/user/is-following/{followingId}`

**参数说明:**
`@Parameter(description = "被关注者ID"`

**返回类型:** `Result<Boolean>`

---

### getTopAuthors
**GET** `/api/user/top-authors`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<List<UserDTO>>`

---

### getFollowings
**GET** `/api/user/followings`

**参数说明:**
`@Parameter(description = "页码"`

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<List<UserDTO>>`

---

### getFollowers
**GET** `/api/user/followers`

**参数说明:**
`@Parameter(description = "页码"`

**返回 UserDTO 字段:**
- id (Long): 
- username (String): 
- email (String): 
- phone (String): 
- nickname (String): 
- avatar (String): 
- bio (String): 
- website (String): 
- position (String): 
- company (String): 
- status (Integer): 
- role (String): 
- createTime (LocalDateTime): 
- lastLoginTime (LocalDateTime): 
- lastLoginIp (String): 
- articleCount (Integer): 
- commentCount (Integer): 
- followerCount (Integer): 
- followingCount (Integer): 
- isFollowed (Boolean): 
- accessToken (String): 
- refreshToken (String): 

**返回类型:** `Result<List<UserDTO>>`

---

### uploadAvatar
**POST** `/api/user/avatar/upload`

**参数说明:**
`@Parameter(description = "头像文件"`

**返回类型:** `Result<String>`

---

## UserFavorite 模块

### favoriteArticle
**POST** `/api/user/favorite/{articleId}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Long>`

---

### unfavoriteArticle
**DELETE** `/api/user/favorite/{articleId}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### getUserFavorites
**GET** `/api/user/favorite/list`

**参数说明:**
`@Parameter(description = "页码"`

**返回 UserFavoriteDTO 字段:**
- favoriteId (Long): 
- userId (Long): 
- articleId (Long): 
- createdAt (String): 
- article (ArticleDTO): 

**返回类型:** `Result<PageResult<UserFavoriteDTO>>`

---

### isArticleFavorited
**GET** `/api/user/favorite/{articleId}/check`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Boolean>`

---

### getUserFavoriteCount
**GET** `/api/user/favorite/count`

**参数说明:**
``

**返回类型:** `Result<Integer>`

---

## UserLike 模块

### likeArticle
**POST** `/api/user/like/{articleId}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Long>`

---

### unlikeArticle
**DELETE** `/api/user/like/{articleId}`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Void>`

---

### getUserLikes
**GET** `/api/user/like/list`

**参数说明:**
`@Parameter(description = "页码"`

**返回 UserLikeDTO 字段:**
- id (Long): 
- userId (Long): 
- articleId (Long): 
- createdAt (String): 
- article (ArticleDTO): 

**返回类型:** `Result<PageResult<UserLikeDTO>>`

---

### isArticleLiked
**GET** `/api/user/like/{articleId}/check`

**参数说明:**
`@Parameter(description = "文章ID"`

**返回类型:** `Result<Boolean>`

---

### getUserLikeCount
**GET** `/api/user/like/count`

**参数说明:**
``

**返回类型:** `Result<Integer>`

---

## WebsiteStatistics 模块

### recordPageView
**POST** `/api/statistics/website/record`

**参数说明:**
`@RequestParam @Parameter(description = "页面URL"`

**返回类型:** `Result<Void>`

---

### getWebsiteStatistics
**GET** `/api/statistics/website/overview`

**参数说明:**
``

**返回 WebsiteStatisticsDTO 字段:**
- totalPageViews (Long): 
- totalUniqueVisitors (Long): 
- todayPageViews (Long): 
- todayUniqueVisitors (Long): 
- yesterdayPageViews (Long): 
- yesterdayUniqueVisitors (Long): 
- averageVisitDuration (Double): 
- bounceRate (Double): 
- statisticsDate (LocalDateTime): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 

**返回类型:** `Result<WebsiteStatisticsDTO>`

---

### getVisitTrend
**GET** `/api/statistics/website/trend`

**参数说明:**
`@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd"`

**返回 VisitTrendDTO 字段:**
- date (LocalDate): 
- pageViews (Long): 
- uniqueVisitors (Long): 
- newVisitors (Long): 
- returningVisitors (Long): 
- averageVisitDuration (Double): 
- bounceRate (Double): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 

**返回类型:** `Result<List<VisitTrendDTO>>`

---

### getTodayStatistics
**GET** `/api/statistics/website/today`

**参数说明:**
``

**返回 WebsiteStatisticsDTO 字段:**
- totalPageViews (Long): 
- totalUniqueVisitors (Long): 
- todayPageViews (Long): 
- todayUniqueVisitors (Long): 
- yesterdayPageViews (Long): 
- yesterdayUniqueVisitors (Long): 
- averageVisitDuration (Double): 
- bounceRate (Double): 
- statisticsDate (LocalDateTime): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 

**返回类型:** `Result<WebsiteStatisticsDTO>`

---

### getWeekStatistics
**GET** `/api/statistics/website/week`

**参数说明:**
``

**返回 WebsiteStatisticsDTO 字段:**
- totalPageViews (Long): 
- totalUniqueVisitors (Long): 
- todayPageViews (Long): 
- todayUniqueVisitors (Long): 
- yesterdayPageViews (Long): 
- yesterdayUniqueVisitors (Long): 
- averageVisitDuration (Double): 
- bounceRate (Double): 
- statisticsDate (LocalDateTime): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 

**返回类型:** `Result<WebsiteStatisticsDTO>`

---

### getMonthStatistics
**GET** `/api/statistics/website/month`

**参数说明:**
``

**返回 WebsiteStatisticsDTO 字段:**
- totalPageViews (Long): 
- totalUniqueVisitors (Long): 
- todayPageViews (Long): 
- todayUniqueVisitors (Long): 
- yesterdayPageViews (Long): 
- yesterdayUniqueVisitors (Long): 
- averageVisitDuration (Double): 
- bounceRate (Double): 
- statisticsDate (LocalDateTime): 
- createTime (LocalDateTime): 
- updateTime (LocalDateTime): 

**返回类型:** `Result<WebsiteStatisticsDTO>`

---

### cleanExpiredStatistics
**DELETE** `/api/statistics/website/clean`

**参数说明:**
`@RequestParam(defaultValue = "90"`

**返回类型:** `Result<Void>`

---

## WebsiteVisit 模块

### recordPageVisit
**POST** `/api/statistics/website/visit`

**参数说明:**
`HttpServletRequest request`

**返回类型:** `Result<Void>`

---

### getWebsiteVisitStatistics
**GET** `/api/statistics/website/statistics`

**参数说明:**
`@Parameter(description = "统计类型：day-日统计，week-周统计，month-月统计"`

**返回 WebsiteVisitDTO 字段:**
- date (String): 
- pageView (Long): 
- uniqueVisitor (Long): 
- visitCount (Long): 
- avgVisitTime (Long): 
- bounceRate (Double): 
- newVisitor (Long): 
- oldVisitor (Long): 

**返回类型:** `Result<List<WebsiteVisitDTO>>`

---

### getRealTimeStatistics
**GET** `/api/statistics/website/realtime`

**参数说明:**
``

**返回 WebsiteVisitDTO 字段:**
- date (String): 
- pageView (Long): 
- uniqueVisitor (Long): 
- visitCount (Long): 
- avgVisitTime (Long): 
- bounceRate (Double): 
- newVisitor (Long): 
- oldVisitor (Long): 

**返回类型:** `Result<WebsiteVisitDTO>`

---

### getHotPageStatistics
**GET** `/api/statistics/website/hot-pages`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 PageVisitDTO 字段:**
- pageUrl (String): 
- pageTitle (String): 
- visitCount (Long): 
- uniqueVisitor (Long): 
- avgStayTime (Long): 
- bounceRate (Double): 

**返回类型:** `Result<List<PageVisitDTO>>`

---

### getVisitorSourceStatistics
**GET** `/api/statistics/website/visitor-sources`

**参数说明:**
`@Parameter(description = "数量限制"`

**返回 VisitorSourceDTO 字段:**
- sourceType (String): 
- sourceName (String): 
- visitCount (Long): 
- uniqueVisitor (Long): 
- percentage (Double): 

**返回类型:** `Result<List<VisitorSourceDTO>>`

---

### getDeviceStatistics
**GET** `/api/statistics/website/devices`

**参数说明:**
``

**返回 DeviceStatisticsDTO 字段:**
- deviceType (DeviceTypeStat): 
- operatingSystem (OperatingSystemStat): 
- browser (BrowserStat): 
- desktop (Double): 
- mobile (Double): 
- tablet (Double): 
- windows (Double): 
- macos (Double): 
- linux (Double): 
- android (Double): 
- ios (Double): 
- other (Double): 
- chrome (Double): 
- firefox (Double): 
- safari (Double): 
- edge (Double): 
- other (Double): 

**返回类型:** `Result<DeviceStatisticsDTO>`

---

