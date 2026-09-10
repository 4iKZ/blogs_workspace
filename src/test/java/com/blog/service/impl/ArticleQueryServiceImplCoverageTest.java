package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.ArticleRankService;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisUtils;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleQueryServiceImplCoverageTest {

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
    private RedisUtils redisUtils;
    @Mock
    private RedisCacheUtils redisCacheUtils;
    @Mock
    private ArticleRankService articleRankService;

    @InjectMocks
    private ArticleQueryServiceImpl articleQueryService;

    private ArticleDtoAssembler articleDtoAssembler;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();

        articleDtoAssembler = new ArticleDtoAssembler();
        ReflectionTestUtils.setField(articleDtoAssembler, "userMapper", userMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "userLikeMapper", userLikeMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "userFavoriteMapper", userFavoriteMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "redisCacheUtils", redisCacheUtils);
        ReflectionTestUtils.setField(articleQueryService, "articleDtoAssembler", articleDtoAssembler);
    }

    // ==================== 获取文章列表 ====================

    @Test
    @DisplayName("获取文章列表 - 默认仅查询已发布文章")
    void getArticleList_shouldDefaultToPublished() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, 10, null, null, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("获取文章列表 - null页码应修正为1")
    void getArticleList_nullPage_shouldUseDefault1() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(null, 10, null, null, null, null, null, null);

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

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(0, 10, null, null, null, null, null, null);

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

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, null, null, null, null, null, null, null);

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

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, 150, null, null, null, null, null, null);

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

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, 10, null, 5L, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("获取文章列表 - 按标签过滤")
    void getArticleList_tagFilter() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(page.getRecords()).thenReturn(Collections.emptyList());

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, 10, null, null, 3L, null, null, null);

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

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, 10, null, null, null, null, null, "popular");

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

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, 10, "关键词", null, null, null, null, null);

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

        Result<PageResult<ArticleDTO>> result = articleQueryService.getArticleList(1, 10, null, null, null, null, null, null);

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
            Result<ArticleDTO> result = articleQueryService.getArticleDetail(99L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章不存在");
        }

        @Test
        @DisplayName("草稿文章 - 作者本人可访问")
        void draftArticle_authorCanView() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(2L);

            Result<ArticleDTO> result = articleQueryService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("草稿文章 - 非作者且非管理员应拒绝")
        void draftArticle_nonAuthorForbidden() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(3L);

            Result<ArticleDTO> result = articleQueryService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).isEqualTo("文章未发布或已删除");
        }

        @Test
        @DisplayName("草稿文章 - 管理员可访问")
        void draftArticle_adminCanView() {
            Article article = createArticle(1L, "草稿", Article.STATUS_DRAFT, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setAdmin(true);

            Result<ArticleDTO> result = articleQueryService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("非发布文章 - 非作者且非管理员应拒绝")
        void nonPublishedArticle_nonAuthorForbidden() {
            Article article = createArticle(1L, "已下线", Article.STATUS_DELETED, 2L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            setUserId(3L);

            Result<ArticleDTO> result = articleQueryService.getArticleDetail(1L);
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

            Result<ArticleDTO> result = articleQueryService.getArticleDetail(1L);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getViewCount()).isEqualTo(15);
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

            Result<List<ArticleDTO>> result = articleQueryService.getRecommendedArticles(10);
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

            Result<List<ArticleDTO>> result = articleQueryService.getRecommendedArticles(5);
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

            Result<List<ArticleDTO>> result = articleQueryService.getRecommendedArticles(5);
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

            Result<List<ArticleDTO>> result = articleQueryService.getRecommendedArticles(5);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            verify(redisUtils).set(eq("recommended:articles:5"), any(), eq(1L), any());
        }

        @Test
        @DisplayName("获取关注文章 - 未关注时为空")
        void getFollowingArticles_empty() {
            setUserId(1L);
            when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());

            Result<PageResult<ArticleDTO>> result = articleQueryService.getFollowingArticles(1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }

        @Test
        @DisplayName("获取关注文章 - 未登录应抛出异常")
        void getFollowingArticles_notLoggedIn_throwsException() {
            RequestContextHolder.resetRequestAttributes();
            SecurityContextHolder.clearContext();

            assertThrows(BusinessException.class, () -> articleQueryService.getFollowingArticles(1, 10));
        }

        @Test
        @DisplayName("获取关注文章 - 异常处理")
        void getFollowingArticles_exceptionHandling() {
            setUserId(1L);
            when(userFollowMapper.selectList(any())).thenThrow(new RuntimeException("数据库异常"));

            Result<PageResult<ArticleDTO>> result = articleQueryService.getFollowingArticles(1, 10);
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

            Result<PageResult<ArticleDTO>> result = articleQueryService.getFollowingArticles(1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("获取用户文章 - 无数据返回空页")
        void getUserArticles_empty() {
            setUserId(1L);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            when(page.getRecords()).thenReturn(Collections.emptyList());
            when(page.getTotal()).thenReturn(0L);
            when(articleMapper.selectPage(any(), any())).thenReturn(page);

            Result<PageResult<ArticleDTO>> result = articleQueryService.getUserArticles(1L, 1, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }

        @Test
        @DisplayName("获取用户点赞文章 - 无数据")
        void getUserLikedArticles_empty() {
            setUserId(1L);
            when(userLikeMapper.findArticleIdsByUserId(1L)).thenReturn(Collections.emptyList());

            Result<PageResult<ArticleDTO>> result = articleQueryService.getUserLikedArticles(1L, 1, 10);
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

            articleQueryService.getUserLikedArticles(1L, 1, 10);
            verify(articleMapper).selectPage(any(), any());
        }

        @Test
        @DisplayName("获取用户收藏文章 - 无数据")
        void getUserFavoriteArticles_empty() {
            setUserId(1L);
            when(userFavoriteMapper.findArticleIdsByUserId(1L)).thenReturn(Collections.emptyList());

            Result<PageResult<ArticleDTO>> result = articleQueryService.getUserFavoriteArticles(1L, 1, 10);
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

            articleQueryService.getUserFavoriteArticles(1L, 1, 10);
            verify(articleMapper).selectPage(any(), any());
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
}
