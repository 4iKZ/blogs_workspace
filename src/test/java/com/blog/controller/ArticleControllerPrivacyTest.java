package com.blog.controller;

import com.blog.exception.BusinessException;
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
        ArticleController controller = createController(articleService, 7L, "ROLE_user");

        assertThatThrownBy(() -> controller.getUserArticles(8L, 1, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限");
        assertThatThrownBy(() -> controller.getUserLikedArticles(8L, 1, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限");
        assertThatThrownBy(() -> controller.getUserFavoriteArticles(8L, 1, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限");

        verify(articleService, never()).getUserArticles(8L, 1, 10);
        verify(articleService, never()).getUserLikedArticles(8L, 1, 10);
        verify(articleService, never()).getUserFavoriteArticles(8L, 1, 10);
    }

    @Test
    void privateArticleLists_admin_shouldBeAllowed() {
        ArticleService articleService = mock(ArticleService.class);
        ArticleController controller = createController(articleService, 7L, "ROLE_admin");

        controller.getUserFavoriteArticles(8L, 1, 10);

        verify(articleService).getUserFavoriteArticles(8L, 1, 10);
    }

    private ArticleController createController(
            ArticleService articleService,
            Long currentUserId,
            String role
    ) {
        ArticleController controller = new ArticleController();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", currentUserId);
        ReflectionTestUtils.setField(controller, "articleService", articleService);
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
