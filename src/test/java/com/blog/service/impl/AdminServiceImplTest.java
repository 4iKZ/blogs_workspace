package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.UserDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.VisitStatisticsMapper;
import com.blog.mapper.WebsiteAccessLogMapper;
import com.blog.service.AdminService;
import com.blog.service.ArticleRankService;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.AuthSessionRevocationService;
import com.blog.service.ArticleService;
import com.blog.utils.BusinessUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.HotArticleCacheEvictionService;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserFollowMapper userFollowMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private ArticleService articleService;

    @Mock
    private ArticleStatisticsService articleStatisticsService;

    @Mock
    private HotArticleCacheEvictionService hotArticleCacheEvictionService;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @Mock
    private VisitStatisticsMapper visitStatisticsMapper;

    @Mock
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    @Mock
    private ArticleRankService articleRankService;

    @Mock
    private AuthSessionRevocationService authSessionRevocationService;

    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // ==================== getUserList ====================

    @Test
    @DisplayName("获取用户列表 - 应返回分页结果")
    void getUserList_shouldReturnPagedUsers() {
        var page = org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        var result = adminService.getUserList(1, 10, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("获取用户列表 - 带关键词和状态应拼接条件")
    void getUserList_withKeywordAndStatus_shouldQueryWithConditions() {
        var page = org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        var result = adminService.getUserList(1, 10, "abc", 1);

        assertThat(result.isSuccess()).isTrue();
        verify(userMapper).selectPage(any(), any());
    }

    // ==================== updateUserStatus ====================

    @Test
    @DisplayName("更新用户状态 - 用户不存在应返回错误")
    void updateUserStatus_userNotFound_shouldReturnError() {
        when(userMapper.selectById(99L)).thenReturn(null);

        var result = adminService.updateUserStatus(99L, 1);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("用户不存在");
    }

    @Test
    @DisplayName("更新用户状态 - 会话吊销失败应返回错误")
    void updateUserStatus_revokeFailed_shouldReturnError() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.updateStatusAndRevoke(1L, 1)).thenReturn(false);

        var result = adminService.updateUserStatus(1L, 1);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("修改用户状态失败");
    }

    @Test
    @DisplayName("更新用户状态 - 成功应更新并吊销会话")
    void updateUserStatus_success_shouldUpdateAndRevoke() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.updateStatusAndRevoke(1L, 1)).thenReturn(true);

        var result = adminService.updateUserStatus(1L, 1);

        assertThat(result.isSuccess()).isTrue();
        verify(authSessionRevocationService).updateStatusAndRevoke(1L, 1);
    }

    // ==================== deleteUser ====================

    @Test
    @DisplayName("删除用户 - 用户不存在应返回错误")
    void deleteUser_userNotFound_shouldReturnError() {
        when(userMapper.selectById(99L)).thenReturn(null);

        var result = adminService.deleteUser(99L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("用户不存在");
    }

    @Test
    @DisplayName("删除用户 - 会话吊销失败应返回错误")
    void deleteUser_revokeFailed_shouldReturnError() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(false);
        when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());

        var result = adminService.deleteUser(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("删除用户失败");
    }

    @Test
    @DisplayName("删除用户 - 成功应更新关注计数并删除用户")
    void deleteUser_success_shouldUpdateFollowCountsAndDelete() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(true);
        when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.deleteById(1L)).thenReturn(1);

        var result = adminService.deleteUser(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(userMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除用户 - 删除失败应返回错误")
    void deleteUser_deleteFailed_shouldReturnError() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(true);
        when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userMapper.deleteById(1L)).thenReturn(0);

        var result = adminService.deleteUser(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("删除用户失败");
    }

    // ==================== getArticleList ====================

    @Test
    @DisplayName("获取文章列表 - 应返回分页结果")
    void getArticleList_shouldReturnPagedArticles() {
        var page = org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);

        var result = adminService.getArticleList(1, 10, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("获取文章列表 - 空列表应不调用批量查询浏览量")
    void getArticleList_emptyArticles_shouldNotBatchQueryViewCount() {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0L);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);

        var result = adminService.getArticleList(1, 10, null, null);

        assertThat(result.isSuccess()).isTrue();
        verify(redisCacheUtils, never()).batchGetArticleRedisViewCount(any());
    }

    // ==================== updateArticleStatus ====================

    @Test
    @DisplayName("更新文章状态 - 不允许直接发布应返回错误")
    void updateArticleStatus_publishWithoutModeration_shouldReturnError() {
        var result = adminService.updateArticleStatus(1L, 2);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文章发布必须通过审核决定");
        verify(articleMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("更新文章状态 - 文章不存在应返回错误")
    void updateArticleStatus_articleNotFound_shouldReturnError() {
        when(articleMapper.selectById(99L)).thenReturn(null);

        var result = adminService.updateArticleStatus(99L, 1);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文章不存在");
    }

    @Test
    @DisplayName("更新文章状态 - 更新失败应返回错误")
    void updateArticleStatus_updateFailed_shouldReturnError() {
        Article article = new Article();
        article.setId(1L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(0);

        var result = adminService.updateArticleStatus(1L, 1);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("修改文章状态失败");
    }

    @Test
    @DisplayName("更新文章状态 - 非发布状态应移除热度榜")
    void updateArticleStatus_nonPublished_shouldRemoveFromRank() {
        Article article = new Article();
        article.setId(1L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.updateById(article)).thenReturn(1);

        var result = adminService.updateArticleStatus(1L, 1);

        assertThat(result.isSuccess()).isTrue();
        verify(articleRankService).removeFromRank(1L);
    }

    // ==================== deleteArticle ====================

    @Test
    @DisplayName("删除文章 - 文章不存在应返回错误")
    void deleteArticle_articleNotFound_shouldReturnError() {
        when(articleMapper.selectById(99L)).thenReturn(null);

        var result = adminService.deleteArticle(99L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文章不存在");
    }

    @Test
    @DisplayName("删除文章 - 删除失败应返回错误")
    void deleteArticle_deleteFailed_shouldReturnError() {
        Article article = new Article();
        article.setId(1L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.deleteById(1L)).thenReturn(0);

        var result = adminService.deleteArticle(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("删除文章失败");
    }

    @Test
    @DisplayName("删除文章 - 成功应清除推荐缓存")
    void deleteArticle_success_shouldClearRecommendedCache() {
        Article article = new Article();
        article.setId(1L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.deleteById(1L)).thenReturn(1);
        when(redisUtils.scanKeys(any())).thenReturn(Collections.emptySet());

        var result = adminService.deleteArticle(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(redisUtils).scanKeys("recommended:articles:*");
    }

    // ==================== getCommentList ====================

    @Test
    @DisplayName("获取评论列表 - 应返回分页结果")
    void getCommentList_shouldReturnPagedComments() {
        var page = org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(commentMapper.selectPage(any(), any())).thenReturn(page);

        var result = adminService.getCommentList(1, 10, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("获取评论列表 - 带文章ID应拼接条件")
    void getCommentList_withArticleId_shouldQueryByArticle() {
        var page = org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(commentMapper.selectPage(any(), any())).thenReturn(page);

        var result = adminService.getCommentList(1, 10, null, 5L);

        assertThat(result.isSuccess()).isTrue();
        verify(commentMapper).selectPage(any(), any());
    }

    // ==================== getWebsiteStatistics ====================

    @Test
    @DisplayName("获取网站统计 - 应返回统计信息")
    void getWebsiteStatistics_shouldReturnStats() {
        when(userMapper.selectCount(any())).thenReturn(10L);
        when(articleMapper.selectCount(any())).thenReturn(5L);

        var result = adminService.getWebsiteStatistics();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsKeys("totalUsers", "totalArticles", "publishedArticles", "draftArticles", "activeUsers");
    }

    // ==================== getVisitStatistics ====================

    @Test
    @DisplayName("获取访问统计 - 应返回统计信息")
    void getVisitStatistics_shouldReturnVisitStats() {
        when(visitStatisticsMapper.selectByDateRange(any(), any())).thenReturn(Collections.emptyList());
        when(websiteAccessLogMapper.countTodayPv()).thenReturn(100);
        when(websiteAccessLogMapper.countTodayUv()).thenReturn(50);

        var result = adminService.getVisitStatistics("2026-01-01", "2026-01-31");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsKeys("totalPageViews", "todayPageViews");
    }

    // ==================== clearCache ====================

    @Test
    @DisplayName("清理缓存 - 应清除多种缓存")
    void clearCache_shouldClearMultipleCaches() {
        when(redisUtils.scanKeys("hot:articles:*")).thenReturn(Collections.singleton("hot:articles:1"));
        when(redisUtils.scanKeys("recommended:articles:*")).thenReturn(Collections.emptySet());
        when(redisUtils.scanKeys("captcha:*")).thenReturn(Collections.emptySet());
        when(redisUtils.delete(any(Set.class))).thenReturn(1L);

        var result = adminService.clearCache();

        assertThat(result.isSuccess()).isTrue();
        verify(hotArticleCacheEvictionService).evictAll();
    }

    @Test
    @DisplayName("清理缓存 - 发生异常应返回错误")
    void clearCache_exception_shouldReturnError() {
        when(redisUtils.scanKeys(any())).thenThrow(new RuntimeException("redis error"));

        var result = adminService.clearCache();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("清理缓存失败");
    }
}
