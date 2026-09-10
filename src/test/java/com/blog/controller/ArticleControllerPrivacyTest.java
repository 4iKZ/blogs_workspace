package com.blog.controller;

import com.blog.exception.BusinessException;
import com.blog.service.ArticleQueryService;
import com.blog.service.ArticleRankService;
import com.blog.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ArticleControllerPrivacyTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void privateArticleLists_crossUser_shouldReturnForbidden() {
        ArticleService articleService = mock(ArticleService.class);
        ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
        ArticleController controller = createController(articleService, articleQueryService, 7L, "ROLE_user");

        assertThatThrownBy(() -> controller.getUserArticles(8L, 1, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限");
        assertThatThrownBy(() -> controller.getUserLikedArticles(8L, 1, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限");
        assertThatThrownBy(() -> controller.getUserFavoriteArticles(8L, 1, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限");

        verify(articleQueryService, never()).getUserArticles(8L, 1, 10);
        verify(articleQueryService, never()).getUserLikedArticles(8L, 1, 10);
        verify(articleQueryService, never()).getUserFavoriteArticles(8L, 1, 10);
    }

    @Test
    void privateArticleLists_admin_shouldBeAllowed() {
        ArticleService articleService = mock(ArticleService.class);
        ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
        ArticleController controller = createController(articleService, articleQueryService, 7L, "ROLE_admin");

        controller.getUserFavoriteArticles(8L, 1, 10);

        verify(articleQueryService).getUserFavoriteArticles(8L, 1, 10);
    }

    @Test
    void publicArticleList_byAuthor_shouldNotRequireOwnership() {
        ArticleService articleService = mock(ArticleService.class);
        ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
        ArticleController controller = createController(articleService, articleQueryService, 7L, "ROLE_user");
        Long targetAuthorId = 8L;

        // 非管理员、非作者访问者也应能按作者查询已发布文章（路径式公开接口），不触发 requireSelfOrAdmin
        controller.getPublishedArticlesByAuthor(targetAuthorId, 1, 10, "latest");

        verify(articleQueryService).getArticleList(1, 10, null, null, null, null, targetAuthorId, "latest");
    }

    private ArticleController createController(
            ArticleService articleService,
            ArticleQueryService articleQueryService,
            Long currentUserId,
            String role
    ) {
        ArticleController controller = new ArticleController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", currentUserId);
        ReflectionTestUtils.setField(controller, "articleService", articleService);
        ReflectionTestUtils.setField(controller, "articleQueryService", articleQueryService);
        ReflectionTestUtils.setField(controller, "articleRankService", mock(ArticleRankService.class));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        "password",
                        List.of(new SimpleGrantedAuthority(role))
                )
        );
        return controller;
    }
}
