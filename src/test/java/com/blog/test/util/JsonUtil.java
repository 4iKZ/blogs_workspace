package com.blog.test.util;

import com.blog.dto.ArticleCreateDTO;
import com.blog.dto.CommentCreateDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String toJson(Object obj) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
    }

    public static UserRegisterDTO aRegisterDTO() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("testuser_" + System.nanoTime());
        dto.setPassword("Password123!");
        dto.setConfirmPassword("Password123!");
        dto.setEmail("test_" + System.nanoTime() + "@example.com");
        dto.setNickname("Test User");
        dto.setEmailCode("123456");
        return dto;
    }

    public static UserLoginDTO aLoginDTO(String username, String password) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    public static ArticleCreateDTO anArticleDTO(Long categoryId) {
        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle("Test Article " + System.nanoTime());
        dto.setContent("Test content for article.");
        dto.setSummary("Test summary");
        dto.setCategoryId(categoryId);
        dto.setStatus(1);
        return dto;
    }

    public static CommentCreateDTO aCommentDTO(Long articleId) {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setArticleId(articleId);
        dto.setContent("Test comment " + System.nanoTime());
        dto.setNickname("Commenter");
        return dto;
    }
}
