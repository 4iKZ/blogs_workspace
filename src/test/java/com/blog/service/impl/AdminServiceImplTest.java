package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.ArticleDTO;
import com.blog.dto.CommentDTO;
import com.blog.dto.UserDTO;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.VisitStatisticsMapper;
import com.blog.mapper.WebsiteAccessLogMapper;
import com.blog.service.AdminService;
import com.blog.service.ArticleRankService;
import com.blog.service.ArticleStatisticsService;
import com.blog.service.AuthSessionRevocationService;
import com.blog.service.ArticleService;
import com.blog.service.FollowCountService;
import com.blog.utils.BusinessUtils;
import com.blog.utils.DTOConverter;
import com.blog.utils.HotArticleCacheEvictionService;
import com.blog.utils.PageUtils;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private UserLikeMapper userLikeMapper;

    @Mock
    private UserFavoriteMapper userFavoriteMapper;

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
    private ArticleDtoAssembler articleDtoAssembler;

    @Mock
    private VisitStatisticsMapper visitStatisticsMapper;

    @Mock
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    @Mock
    private ArticleRankService articleRankService;

    @Mock
    private AuthSessionRevocationService authSessionRevocationService;

    @Mock
    private FollowCountService followCountService;

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

    @Test
    @DisplayName("获取用户列表 - 关键词条件应分组，status 过滤不落入 OR 分支")
    void getUserList_keywordGrouped_statusShouldNotBeOred() {
        // 手动初始化 User 的 Lambda 列缓存，避免单测中 getSqlSegment 因反射缓存未初始化而失败
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new org.apache.ibatis.session.Configuration(), "test"),
                com.blog.entity.User.class);

        var page = org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(userMapper.selectPage(any(), any())).thenReturn(page);

        adminService.getUserList(1, 10, "abc", 1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(userMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        // OR 关键词条件被括号包裹，status 单独 AND，避免 status 只约束最后一个 like
        assertThat(sql).contains("(username LIKE").contains("OR nickname LIKE").contains("OR email LIKE");
        assertThat(sql).contains("status").contains("AND");
    }

    // ==================== updateUserStatus ====================

    @Test
    @DisplayName("更新用户状态 - 用户不存在应抛出异常")
    void updateUserStatus_userNotFound_shouldReturnError() {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> adminService.updateUserStatus(99L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在")
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("更新用户状态 - 会话吊销失败应抛出异常")
    void updateUserStatus_revokeFailed_shouldReturnError() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.updateStatusAndRevoke(1L, 1)).thenReturn(false);

        assertThatThrownBy(() -> adminService.updateUserStatus(1L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("修改用户状态失败")
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.ERROR.getCode()));
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
    @DisplayName("删除用户 - 用户不存在应抛出异常")
    void deleteUser_userNotFound_shouldReturnError() {
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> adminService.deleteUser(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在")
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.USER_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("删除用户 - 会话吊销失败应抛出异常")
    void deleteUser_revokeFailed_shouldReturnError() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("删除用户失败")
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.ERROR.getCode()));
    }

    @Test
    @DisplayName("删除用户 - 成功应更新关注计数并删除用户")
    void deleteUser_success_shouldUpdateFollowCountsAndDelete() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(true);
        when(userMapper.deleteById(1L)).thenReturn(1);

        var result = adminService.deleteUser(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(userMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除用户 - 删除失败应抛出异常")
    void deleteUser_deleteFailed_shouldReturnError() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(true);
        when(userMapper.deleteById(1L)).thenReturn(0);

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("删除用户失败")
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResultCode.ERROR.getCode()));
    }

    // ==================== getArticleList ====================

    @Test
    @DisplayName("获取文章列表 - 应返回分页结果")
    void getArticleList_shouldReturnPagedArticles() {
        var page = org.mockito.Mockito.mock(com.baomidou.mybatisplus.core.metadata.IPage.class);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);

        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("文章");
        dto.setAuthorNickname("作者");
        when(articleDtoAssembler.batchConvertToDTO(any())).thenReturn(List.of(dto));

        var result = adminService.getArticleList(1, 10, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getItems()).hasSize(1);
        assertThat(result.getData().getItems().get(0).getAuthorNickname()).isEqualTo("作者");
    }

    @Test
    @DisplayName("获取文章列表 - 空列表应委托组装器且不触达 Redis")
    void getArticleList_emptyArticles_shouldDelegateToAssemblerWithoutRedis() {
        var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Article>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0L);
        when(articleMapper.selectPage(any(), any())).thenReturn(page);
        when(articleDtoAssembler.batchConvertToDTO(Collections.emptyList())).thenReturn(Collections.emptyList());

        var result = adminService.getArticleList(1, 10, null, null);

        assertThat(result.isSuccess()).isTrue();
        verify(articleDtoAssembler).batchConvertToDTO(Collections.emptyList());
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
    // 管理员删除已委托给 ArticleService.deleteArticle，此处验证委托语义

    @Test
    @DisplayName("删除文章 - 委托 ArticleService 并透传结果（文章不存在）")
    void deleteArticle_articleNotFound_shouldReturnError() {
        when(articleService.deleteArticle(99L, null))
                .thenReturn(BusinessUtils.error("文章不存在"));

        var result = adminService.deleteArticle(99L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文章不存在");
        verify(articleService).deleteArticle(99L, null);
    }

    @Test
    @DisplayName("删除文章 - 委托 ArticleService 并透传结果（删除失败）")
    void deleteArticle_deleteFailed_shouldReturnError() {
        when(articleService.deleteArticle(1L, null))
                .thenReturn(BusinessUtils.error("删除文章失败"));

        var result = adminService.deleteArticle(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("删除文章失败");
    }

    @Test
    @DisplayName("删除文章 - 委托 ArticleService 成功透传成功结果")
    void deleteArticle_success_shouldClearRecommendedCache() {
        when(articleService.deleteArticle(1L, null)).thenReturn(BusinessUtils.success());

        var result = adminService.deleteArticle(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleService).deleteArticle(1L, null);
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

    @Test
    @DisplayName("获取访问统计 - 有数据时汇总所有指标")
    void getVisitStatistics_withData_shouldAggregate() {
        com.blog.entity.VisitStatistics vs = new com.blog.entity.VisitStatistics();
        vs.setPageViews(10);
        vs.setUniqueVisitors(5);
        vs.setNewUsers(2);
        vs.setNewArticles(1);
        vs.setNewComments(3);
        com.blog.entity.VisitStatistics vsNull = new com.blog.entity.VisitStatistics();
        when(visitStatisticsMapper.selectByDateRange(any(), any())).thenReturn(List.of(vs, vsNull));
        when(websiteAccessLogMapper.countTodayPv()).thenReturn(1);
        when(websiteAccessLogMapper.countTodayUv()).thenReturn(1);

        var result = adminService.getVisitStatistics("2026-01-01", "2026-01-31");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get("totalPageViews")).isEqualTo(10L);
        assertThat(result.getData().get("totalUniqueVisitors")).isEqualTo(5L);
        assertThat(result.getData().get("totalNewUsers")).isEqualTo(2L);
        assertThat(result.getData().get("totalNewArticles")).isEqualTo(1L);
        assertThat(result.getData().get("totalNewComments")).isEqualTo(3L);
    }

    // ==================== deleteUser 补充 ====================

    @Test
    @DisplayName("删除用户 - 有关注关系时委托 FollowCountService 修正计数")
    void deleteUser_withFollowRelations_shouldUpdateCounts() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(true);
        when(userMapper.deleteById(1L)).thenReturn(1);

        var result = adminService.deleteUser(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(followCountService).detachUserRelations(1L);
    }

    @Test
    @DisplayName("删除用户 - 运行时异常应传播")
    void deleteUser_runtimeException_shouldReturnError() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenThrow(new RuntimeException("db error"));

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db error");
    }

    @Test
    @DisplayName("删除用户 - 关注计数修正异常应传播且不执行删除")
    void deleteUser_decrementFollowingCountFailed_shouldPropagateAndNotDelete() {
        User user = new User();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(authSessionRevocationService.incrementVersionAndRevoke(1L)).thenReturn(true);
        doThrow(new RuntimeException("count error")).when(followCountService).detachUserRelations(1L);

        assertThatThrownBy(() -> adminService.deleteUser(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("count error");
        verify(userMapper, never()).deleteById(anyLong());
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
