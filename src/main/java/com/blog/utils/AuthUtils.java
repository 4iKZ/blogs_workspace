package com.blog.utils;

import com.blog.common.ResultCode;
import com.blog.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证相关工具类：从请求上下文安全获取当前登录用户信息
 */
public class AuthUtils {

    /**
     * 获取当前登录用户ID
     * - 依赖 JwtInterceptor 将 userId 注入到请求属性
     */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取请求上下文");
        }
        HttpServletRequest request = attrs.getRequest();
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        try {
            return Long.valueOf(userIdAttr.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.ERROR, "用户ID格式错误");
        }
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无法获取请求上下文");
        }
        HttpServletRequest request = attrs.getRequest();
        Object usernameAttr = request.getAttribute("username");
        if (usernameAttr == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return usernameAttr.toString();
    }

    /**
     * 检查当前用户是否为管理员
     * @return 是否为管理员
     */
    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // 检查是否有ROLE_admin权限
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_admin".equals(role));
    }

    /**
     * 检查当前用户是否有权限操作指定的文章
     * @param articleAuthorId 文章作者ID
     * @return 是否有权限(管理员或文章作者本人)
     * @throws BusinessException 如果用户未登录
     */
    public static boolean canManageArticle(Long articleAuthorId) {
        if (articleAuthorId == null) {
            return false;
        }

        // 管理员可以管理所有文章
        if (isAdmin()) {
            return true;
        }

        // 文章作者可以管理自己的文章
        // 如果未登录，getCurrentUserId() 会抛出 BusinessException，不应被吞没
        Long currentUserId = getCurrentUserId();
        return articleAuthorId.equals(currentUserId);
    }
}