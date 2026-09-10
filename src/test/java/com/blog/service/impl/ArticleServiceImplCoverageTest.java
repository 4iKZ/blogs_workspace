package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.ArticleRankService;
import com.blog.service.FileUploadService;
import com.blog.service.NotificationService;
import com.blog.service.SensitiveWordService;
import com.blog.service.UserService;
import com.blog.service.ArticleModerationSubmissionService;
import com.blog.service.ArticleStatisticsService;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleServiceImplCoverageTest {

    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private UserFavoriteMapper userFavoriteMapper;
    @Mock
    private UserFollowMapper userFollowMapper;
    @Mock
    private com.blog.mapper.CommentMapper commentMapper;
    @Mock
    private com.blog.mapper.CommentLikeMapper commentLikeMapper;
    @Mock
    private com.blog.mapper.ArticleViewMapper articleViewMapper;
    @Mock
    private com.blog.mapper.ArticleModerationSubmissionMapper moderationSubmissionMapper;
    @Mock
    private com.blog.service.CommentService commentService;
    @Mock
    private UserService userService;
    @Mock
    private ArticleStatisticsService articleStatisticsService;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private RedisCacheUtils redisCacheUtils;
    @Mock
    private FileUploadService fileUploadService;
    @Mock
    private ArticleRankService articleRankService;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private ArticleModerationSubmissionService moderationSubmissionService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // ==================== 获取文章列表 ====================

    @Test
    @DisplayName("获取文章列表 - 默认仅查询已发布文章")
    void getArticleList_shouldDefaultToPublished() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, null, null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("获取文章列表 - null页码应修正为1")
    void getArticleList_nullPage_shouldUseDefault1() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(null, 10, null, null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article>> captor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        verify(articleMapper).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getCurrent()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取文章列表 - 页码小于1应修正为1")
    void getArticleList_pageLessThan1_shouldUseDefault1() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(0, 10, null, null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article>> captor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        verify(articleMapper).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getCurrent()).isEqualTo(1);
    }

    @Test
    @DisplayName("获取文章列表 - null页大小应修正为10")
    void getArticleList_nullSize_shouldUseDefault10() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, null, null, null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article>> captor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        verify(articleMapper).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("获取文章列表 - 页大小大于100应修正为100")
    void getArticleList_sizeOver100_shouldCapTo100() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 150, null, null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article>> captor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        verify(articleMapper).selectPage(captor.capture(), any());
        assertThat(captor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("获取文章列表 - 按分类过滤")
    void getArticleList_categoryFilter() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, null, 5L, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("获取文章列表 - 按标签过滤")
    void getArticleList_tagFilter() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, null, null, 3L, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("获取文章列表 - popular排序且无筛选条件时委托排行榜分页查询")
    void getArticleList_popularSort_delegatesToRankPage() {
        ArticleDTO hotDto = new ArticleDTO();
        hotDto.setId(2L);
        List<ArticleDTO> hotItems = List.of(hotDto);
        PageResult<ArticleDTO> hotPage = PageResult.of(hotItems, 1L, 1, 10);
        when(articleRankService.getHotArticlesPage(1, 10, "week")).thenReturn(Result.success(hotPage));

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, null, null, null, null, null, "popular");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getId()).isEqualTo(2L);
        verify(articleRankService).getHotArticlesPage(1, 10, "week");
        verify(articleMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("获取文章列表 - 关键词全文搜索")
    void getArticleList_keywordSearch() {
        Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(page.getRecords()).thenReturn(Collections.singletonList(article));
        when(page.getTotal()).thenReturn(1L);
        when(articleMapper.selectPublishedByFulltext(any(), eq(Article.STATUS_PUBLISHED), eq("关键词"), any(), any(), any()))
                .thenReturn(page);

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, "关键词", null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).hasSize(1);
        verify(articleMapper).selectPublishedByFulltext(any(), eq(Article.STATUS_PUBLISHED), eq("关键词"), any(), any(), any());
    }

    @Test
    @DisplayName("获取文章列表 - 空结果列表")
    void getArticleList_emptyResult() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(page.getRecords()).thenReturn(Collections.emptyList());
        when(page.getTotal()).thenReturn(0L);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, null, null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getItems()).isEmpty();
        assertThat(result.getData().getTotal()).isEqualTo(0);
    }

    // ==================== 获取文章详情 ====================

    @Nested
    @DisplayName("获取文章详情")
    class GetArticleDetail {

        @Test
        @DisplayName("文章不存在")
        void articleNotFound() {
            when(articleMapper.selectById(99L)).thenReturn(null);
            Result<ArticleDTO> result = articleService.getArticleDetail(99L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章不存在");
        }

        @Test
        @DisplayName("草稿文章 - 作者本人可访问")
        void draftArticle_authorCanView() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);

            Result<ArticleDTO> result = articleService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("草稿文章 - 非作者且非管理员应拒绝")
        void draftArticle_nonAuthorForbidden() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(3L);

            Result<ArticleDTO> result = articleService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章未发布或已删除");
        }

        @Test
        @DisplayName("草稿文章 - 管理员可访问")
        void draftArticle_adminCanView() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setAdmin(true);

            Result<ArticleDTO> result = articleService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("非发布文章 - 非作者且非管理员应拒绝")
        void nonPublishedArticle_nonAuthorForbidden() {
            Article article = createArticle(1L, "已下线", Article.STATUS_DELETED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(3L);

            Result<ArticleDTO> result = articleService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章未发布或已删除");
        }

        @Test
        @DisplayName("已发布文章 - 合并 Redis 浏览量")
        void publishedArticle_mergeRedisView() {
            Article article = createArticle(1L, "已发布", Article.STATUS_PUBLISHED, 2L);
            article.setViewCount(10);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(redisCacheUtils.getArticleRedisViewCount(1L)).thenReturn(5);

            Result<ArticleDTO> result = articleService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getViewCount()).isEqualTo(15);
        }
    }

    // ==================== 发布文章 ====================

    @Nested
    @DisplayName("发布文章")
    class PublishArticle {

        @Test
        @DisplayName("作者不存在")
        void authorNotFound() {
            ArticleCreateDTO dto = createArticleCreateDTO("标题");
            when(userService.getUserById(1L)).thenReturn(null);

            Result<Long> result = articleService.publishArticle(dto, 1L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("作者不存在");
        }

        @Test
        @DisplayName("未指定分类 - 使用默认分类 11")
        void defaultCategory() {
            ArticleCreateDTO dto = createArticleCreateDTO("标题");
            dto.setCategoryId(null);
            User author = createUser(1L, "作者");
            Category category = createCategory(11L, "技术分享");
            when(userService.getUserById(1L)).thenReturn(author);
            when(categoryMapper.selectById(11L)).thenReturn(category);
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
            when(articleMapper.insert(any())).thenReturn(1);
            when(moderationSubmissionService.submitNew(any())).thenReturn("token");

            Result<Long> result = articleService.publishArticle(dto, 1L);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("分类不存在")
        void categoryNotFound() {
            ArticleCreateDTO dto = createArticleCreateDTO("标题");
            dto.setCategoryId(99L);
            User author = createUser(1L, "作者");
            when(userService.getUserById(1L)).thenReturn(author);
            when(categoryMapper.selectById(99L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> articleService.publishArticle(dto, 1L));
            assertThat(ex.getMessage()).isEqualTo("分类不存在");
        }

        @Test
        @DisplayName("敏感词检测失败")
        void sensitiveWordFailed() {
            ArticleCreateDTO dto = createArticleCreateDTO("标题");
            User author = createUser(1L, "作者");
            Category category = createCategory(11L, "技术分享");
            when(userService.getUserById(1L)).thenReturn(author);
            when(categoryMapper.selectById(11L)).thenReturn(category);
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.error("包含敏感词"));

            Result<Long> result = articleService.publishArticle(dto, 1L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("包含敏感词");
        }

        @Test
        @DisplayName("插入文章失败")
        void insertFailed() {
            ArticleCreateDTO dto = createArticleCreateDTO("标题");
            User author = createUser(1L, "作者");
            Category category = createCategory(11L, "技术分享");
            when(userService.getUserById(1L)).thenReturn(author);
            when(categoryMapper.selectById(11L)).thenReturn(category);
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
            when(articleMapper.insert(any())).thenReturn(0);

            Result<Long> result = articleService.publishArticle(dto, 1L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("创建文章失败");
        }

        @Test
        @DisplayName("发布成功 - 提交审核并发布事件")
        void publishSuccess() {
            ArticleCreateDTO dto = createArticleCreateDTO("标题");
            User author = createUser(1L, "作者");
            Category category = createCategory(11L, "技术分享");
            when(userService.getUserById(1L)).thenReturn(author);
            when(categoryMapper.selectById(11L)).thenReturn(category);
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
            when(articleMapper.insert(any())).thenReturn(1);
            when(moderationSubmissionService.submitNew(any())).thenReturn("token");

            Result<Long> result = articleService.publishArticle(dto, 1L);
            assertThat(result.isSuccess()).isTrue();
            verify(eventPublisher).publishEvent(any());
        }
    }

    // ==================== 编辑文章 ====================

    @Nested
    @DisplayName("编辑文章")
    class EditArticle {

        @Test
        @DisplayName("文章不存在")
        void articleNotFound() {
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            when(articleMapper.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> articleService.editArticle(1L, dto, 2L));
            assertThat(ex.getMessage()).isEqualTo("文章不存在");
        }

        @Test
        @DisplayName("无权编辑")
        void noPermission() {
            Article article = createArticle(1L, "文章", Article.STATUS_DRAFT, 2L);
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(99L);

            Result<Void> result = articleService.editArticle(1L, dto, 99L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("无权编辑此文章，只有文章作者或管理员可以编辑");
        }

        @Test
        @DisplayName("分类不存在")
        void categoryNotFound() {
            Article article = createArticle(1L, "文章", Article.STATUS_DRAFT, 2L);
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            dto.setCategoryId(99L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);

            BusinessException ex = assertThrows(BusinessException.class, () -> articleService.editArticle(1L, dto, 2L));
            assertThat(ex.getMessage()).isEqualTo("分类不存在");
        }

        @Test
        @DisplayName("敏感词检测失败")
        void sensitiveWordFailed() {
            Article article = createArticle(1L, "文章", Article.STATUS_DRAFT, 2L);
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(categoryMapper.selectById(anyLong())).thenReturn(createCategory(11L, "技术分享"));
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.error("包含敏感词"));

            Result<Void> result = articleService.editArticle(1L, dto, 2L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("包含敏感词");
        }

        @Test
        @DisplayName("已发布文章 - 应提交编辑审核")
        void publishedArticle_submitEditModeration() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(categoryMapper.selectById(anyLong())).thenReturn(createCategory(11L, "技术分享"));
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
            when(moderationSubmissionService.submitEdit(any(), any())).thenReturn("edit-token");

            Result<Void> result = articleService.editArticle(1L, dto, 2L);
            assertThat(result.isSuccess()).isTrue();
            verify(moderationSubmissionService).submitEdit(eq(article), any());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("草稿文章 - 更新并提交新审核")
        void draftArticle_updateAndSubmit() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(categoryMapper.selectById(anyLong())).thenReturn(createCategory(11L, "技术分享"));
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
            when(articleMapper.updateById(any())).thenReturn(1);
            lenient().when(redisUtils.scanKeys(anyString())).thenReturn(Collections.emptySet());
            when(moderationSubmissionService.submitNew(any())).thenReturn("token");

            Result<Void> result = articleService.editArticle(1L, dto, 2L);
            assertThat(result.isSuccess()).isTrue();
            verify(articleMapper).updateById(any());
        }

        @Test
        @DisplayName("草稿文章 - 成功清除缓存")
        void draftArticle_clearCache() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(categoryMapper.selectById(anyLong())).thenReturn(createCategory(11L, "技术分享"));
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
            when(articleMapper.updateById(any())).thenReturn(1);
            Set<String> keys = new HashSet<>(Arrays.asList("recommended:articles:1", "recommended:articles:2"));
            when(redisUtils.scanKeys(anyString())).thenReturn(keys);
            when(moderationSubmissionService.submitNew(any())).thenReturn("token");

            Result<Void> result = articleService.editArticle(1L, dto, 2L);
            assertThat(result.isSuccess()).isTrue();
            verify(redisUtils).delete(eq(keys));
        }

        @Test
        @DisplayName("草稿文章 - 客户端传 status=2 不应绕过审核直接发布")
        void draftArticle_clientStatusShouldNotBypassModeration() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            ArticleCreateDTO dto = createArticleCreateDTO("新标题");
            dto.setStatus(Article.STATUS_PUBLISHED); // 恶意传入已发布状态
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(categoryMapper.selectById(anyLong())).thenReturn(createCategory(11L, "技术分享"));
            when(sensitiveWordService.validateContent(anyString())).thenReturn(Result.success());
            when(articleMapper.updateById(any())).thenReturn(1);
            lenient().when(redisUtils.scanKeys(anyString())).thenReturn(Collections.emptySet());
            when(moderationSubmissionService.submitNew(any())).thenReturn("token");

            Result<Void> result = articleService.editArticle(1L, dto, 2L);

            assertThat(result.isSuccess()).isTrue();
            // 落库的文章状态必须保持为草稿（由服务端控制），不能随客户端传入的 status=2 变为已发布
            ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);
            verify(articleMapper).updateById(articleCaptor.capture());
            assertThat(articleCaptor.getValue().getStatus()).isEqualTo(Article.STATUS_DRAFT);
            // 草稿仍应提交新文章审核
            verify(moderationSubmissionService).submitNew(any());
        }
    }

    // ==================== 删除文章 ====================

    @Nested
    @DisplayName("删除文章")
    class DeleteArticle {

        @Test
        @DisplayName("文章不存在")
        void articleNotFound() {
            when(articleMapper.selectById(1L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class, () -> articleService.deleteArticle(1L, 2L));
            assertThat(ex.getMessage()).isEqualTo("文章不存在");
        }

        @Test
        @DisplayName("无权删除")
        void noPermission() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(99L);

            Result<Void> result = articleService.deleteArticle(1L, 99L);
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("删除失败")
        void deleteFailure() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(articleMapper.deleteById(1L)).thenReturn(0);

            BusinessException ex = assertThrows(BusinessException.class, () -> articleService.deleteArticle(1L, 2L));
            assertThat(ex.getMessage()).isEqualTo("文章不存在");
        }

        @Test
        @DisplayName("删除成功 - 清理关联数据")
        void deleteSuccess() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(articleMapper.deleteById(1L)).thenReturn(1);
            when(userLikeMapper.deleteByArticleId(1L)).thenReturn(2);
            when(userFavoriteMapper.deleteByArticleId(1L)).thenReturn(1);
            doNothing().when(articleRankService).removeFromRank(1L);
            lenient().when(redisUtils.scanKeys(anyString())).thenReturn(Collections.emptySet());

            // 文章下的两条评论（含一条作者自己的评论）
            Comment comment1 = new Comment();
            comment1.setId(11L);
            comment1.setArticleId(1L);
            comment1.setUserId(3L);
            Comment comment2 = new Comment();
            comment2.setId(12L);
            comment2.setArticleId(1L);
            comment2.setUserId(2L);
            when(commentMapper.selectCommentsByArticleId(1L, null)).thenReturn(List.of(comment1, comment2));

            Result<Void> result = articleService.deleteArticle(1L, 2L);
            assertThat(result.isSuccess()).isTrue();
            verify(userLikeMapper).deleteByArticleId(1L);
            verify(articleRankService).removeFromRank(1L);
            // 评论及其点赞记录、详情缓存应被清理
            verify(commentMapper).selectCommentsByArticleId(1L, null);
            verify(commentLikeMapper).deleteByCommentId(11L);
            verify(commentLikeMapper).deleteByCommentId(12L);
            verify(commentMapper).deleteById(11L);
            verify(commentMapper).deleteById(12L);
            verify(redisCacheUtils).deleteCache(RedisCacheUtils.generateCommentDetailKey(11L));
            verify(redisCacheUtils).deleteCache(RedisCacheUtils.generateCommentDetailKey(12L));
            // 浏览记录与审核提交记录应被物理清理（外键约束要求先于文章删除）
            verify(articleViewMapper).deleteByArticleId(1L);
            verify(moderationSubmissionMapper).deleteByArticleId(1L);
            // 评论列表/计数/热门缓存应被清除
            verify(commentService).clearCommentCache(1L);
        }

        @Test
        @DisplayName("删除成功 - 文章无评论时不清评论数据")
        void deleteSuccess_noComments() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(articleMapper.deleteById(1L)).thenReturn(1);
            when(commentMapper.selectCommentsByArticleId(1L, null)).thenReturn(Collections.emptyList());

            Result<Void> result = articleService.deleteArticle(1L, 2L);
            assertThat(result.isSuccess()).isTrue();
            verify(commentLikeMapper, never()).deleteByCommentId(anyLong());
            verify(commentMapper).selectCommentsByArticleId(1L, null);
        }

        @Test
        @DisplayName("子表清理异常 - 中断删除（外键约束要求先清子表，失败不得强行删除文章）")
        void cleanupException() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(articleMapper.deleteById(1L)).thenReturn(1);
            doThrow(new RuntimeException()).when(userLikeMapper).deleteByArticleId(anyLong());

            assertThrows(RuntimeException.class, () -> articleService.deleteArticle(1L, 2L));
            // 文章不应被删除
            verify(articleMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("排行榜服务异常 - 不应影响删除结果")
        void rankServiceException() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);
            when(articleMapper.deleteById(1L)).thenReturn(1);
            doThrow(new RuntimeException()).when(articleRankService).removeFromRank(anyLong());

            Result<Void> result = articleService.deleteArticle(1L, 2L);
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ==================== 封面图片上传 ====================

    @Nested
    @DisplayName("封面图片上传")
    class UploadCoverImage {

        @Test
        @DisplayName("上传成功")
        void uploadSuccess() {
            MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "image".getBytes());
            when(fileUploadService.uploadImage(file)).thenReturn(Result.success("https://oss/cover.jpg"));

            Result<String> result = articleService.uploadCoverImage(file);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo("https://oss/cover.jpg");
        }

        @Test
        @DisplayName("上传失败")
        void uploadFailed() {
            MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", "image".getBytes());
            when(fileUploadService.uploadImage(file)).thenThrow(new RuntimeException("上传失败"));

            Result<String> result = articleService.uploadCoverImage(file);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("上传失败");
        }
    }

    // ==================== 推荐与关注 ====================

    @Nested
    @DisplayName("推荐与关注")
    class RecommendedAndFollowing {

        @Test
        @DisplayName("获取推荐文章")
        void getRecommendedArticles() {
            Article article = createArticle(1L, "推荐", Article.STATUS_PUBLISHED, 2L);
            article.setIsRecommended(2);
            when(articleMapper.selectList(any())).thenReturn(Collections.singletonList(article));

            Result<List<ArticleDTO>> result = articleService.getRecommendedArticles(10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
        }

        @Test
        @DisplayName("获取推荐文章 - Redis缓存命中")
        void getRecommendedArticles_cacheHit() {
            ArticleDTO cached = new ArticleDTO();
            cached.setId(1L);
            cached.setTitle("缓存推荐");
            when(redisUtils.get("recommended:articles:5")).thenReturn(Collections.singletonList(cached));

            Result<List<ArticleDTO>> result = articleService.getRecommendedArticles(5);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getTitle()).isEqualTo("缓存推荐");
            verify(articleMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("获取推荐文章 - 缓存未命中且结果为空")
        void getRecommendedArticles_cacheMiss_emptyResult() {
            when(redisUtils.get("recommended:articles:5")).thenReturn(null);
            when(articleMapper.selectList(any())).thenReturn(Collections.emptyList());

            Result<List<ArticleDTO>> result = articleService.getRecommendedArticles(5);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
            verify(redisUtils, never()).set(anyString(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("获取推荐文章 - 缓存未命中并回写缓存")
        void getRecommendedArticles_cacheMiss_writeBack() {
            Article article = createArticle(1L, "推荐", Article.STATUS_PUBLISHED, 2L);
            article.setIsRecommended(2);
            when(redisUtils.get("recommended:articles:5")).thenReturn(null);
            when(articleMapper.selectList(any())).thenReturn(Collections.singletonList(article));
            when(redisUtils.set(eq("recommended:articles:5"), any(), eq(1L), any())).thenReturn(true);

            Result<List<ArticleDTO>> result = articleService.getRecommendedArticles(5);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            verify(redisUtils).set(eq("recommended:articles:5"), any(), eq(1L), any());
        }

        @Test
        @DisplayName("获取关注文章 - 未关注时为空")
        void getFollowingArticles_empty() {
            setUserId(1L);
            when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());

            Result<PageResult<ArticleDTO>> result = articleService.getFollowingArticles(1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }

        @Test
        @DisplayName("获取关注文章 - 未登录应抛出异常")
        void getFollowingArticles_notLoggedIn_throwsException() {
            RequestContextHolder.resetRequestAttributes();
            SecurityContextHolder.clearContext();

            assertThrows(BusinessException.class, () -> articleService.getFollowingArticles(1, 10));
        }

        @Test
        @DisplayName("获取关注文章 - 异常处理")
        void getFollowingArticles_exceptionHandling() {
            setUserId(1L);
            when(userFollowMapper.selectList(any())).thenThrow(new RuntimeException("数据库异常"));

            Result<PageResult<ArticleDTO>> result = articleService.getFollowingArticles(1, 10);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("获取关注作者的文章列表失败");
        }

        @Test
        @DisplayName("获取关注文章 - 有关注作者时查询文章")
        void getFollowingArticles_withFollowedAuthors() {
            setUserId(1L);
            com.blog.entity.UserFollow follow = new com.blog.entity.UserFollow();
            follow.setFollowingId(2L);
            when(userFollowMapper.selectList(any())).thenReturn(List.of(follow));

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            Article article = new Article();
            article.setId(1L);
            article.setTitle("following article");
            article.setStatus(2);
            when(page.getRecords()).thenReturn(List.of(article));
            when(page.getTotal()).thenReturn(1L);
            when(articleMapper.selectPage(any(), any())).thenReturn(page);

            Result<PageResult<ArticleDTO>> result = articleService.getFollowingArticles(1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("更新文章浏览量 - 委托统计服务")
        void updateArticleViewCount_delegates() {
            articleService.updateArticleViewCount(1L);
            verify(articleStatisticsService).incrementViewCount(1L);
        }

        @Test
        @DisplayName("获取用户文章 - 无数据返回空页")
        void getUserArticles_empty() {
            setUserId(1L);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            when(page.getRecords()).thenReturn(Collections.emptyList());
            when(page.getTotal()).thenReturn(0L);
            when(articleMapper.selectPage(any(), any())).thenReturn(page);

            Result<PageResult<ArticleDTO>> result = articleService.getUserArticles(1L, 1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }

        @Test
        @DisplayName("获取用户点赞文章 - 无数据")
        void getUserLikedArticles_empty() {
            setUserId(1L);
            when(userLikeMapper.findArticleIdsByUserId(1L)).thenReturn(Collections.emptyList());

            Result<PageResult<ArticleDTO>> result = articleService.getUserLikedArticles(1L, 1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }

        @Test
        @DisplayName("获取用户点赞文章 - 仅查询已发布文章")
        void getUserLikedArticles_onlyPublished() {
            setUserId(1L);
            when(userLikeMapper.findArticleIdsByUserId(1L)).thenReturn(Collections.singletonList(1L));

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            when(page.getRecords()).thenReturn(Collections.emptyList());
            when(articleMapper.selectPage(any(), any())).thenReturn(page);

            articleService.getUserLikedArticles(1L, 1, 10);
            verify(articleMapper).selectPage(any(), any());
        }

        @Test
        @DisplayName("获取用户收藏文章 - 无数据")
        void getUserFavoriteArticles_empty() {
            setUserId(1L);
            when(userFavoriteMapper.findArticleIdsByUserId(1L)).thenReturn(Collections.emptyList());

            Result<PageResult<ArticleDTO>> result = articleService.getUserFavoriteArticles(1L, 1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }

        @Test
        @DisplayName("获取用户收藏文章 - 仅查询已发布文章")
        void getUserFavoriteArticles_onlyPublished() {
            setUserId(1L);
            when(userFavoriteMapper.findArticleIdsByUserId(1L)).thenReturn(Collections.singletonList(1L));

            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            when(page.getRecords()).thenReturn(Collections.emptyList());
            when(articleMapper.selectPage(any(), any())).thenReturn(page);

            articleService.getUserFavoriteArticles(1L, 1, 10);
            verify(articleMapper).selectPage(any(), any());
        }
    }

    // ==================== 批量转换 ====================

    @Nested
    @DisplayName("批量转换")
    class BatchConvert {

        @Test
        @DisplayName("空输入")
        void emptyInput() {
            List<ArticleDTO> result = articleService.batchConvertToDTO(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("异常分类查询 - 不应抛异常")
        void categoryQueryException() {
            Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
            when(userMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(createUser(2L, "作者")));
            when(categoryMapper.selectBatchIds(any())).thenThrow(new RuntimeException());
            setUserId(1L);

            List<ArticleDTO> result = articleService.batchConvertToDTO(Collections.singletonList(article));
            assertThat(result).hasSize(1);
        }
    }

    // ==================== 工具方法 ====================

    private void setUserId(Long userId) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("userId")).thenReturn(userId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    private void setAdmin(boolean isAdmin) {
        if (isAdmin) {
            org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin");
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin", "password", List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    private Article createArticle(Long id, String title, int status, Long authorId) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setStatus(status);
        article.setAuthorId(authorId);
        article.setViewCount(0);
        article.setCategoryId(11L);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        return article;
    }

    private ArticleCreateDTO createArticleCreateDTO(String title) {
        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle(title);
        dto.setContent("内容");
        dto.setCategoryId(11L);
        dto.setAllowComment(1);
        dto.setStatus(Article.STATUS_DRAFT);
        return dto;
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        user.setStatus(1);
        user.setRole(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
