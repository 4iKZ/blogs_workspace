package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("评论列表 - 未登录应允许访问")
    void commentList_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/comment/list")
                .param("articleId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("热门评论 - 未登录应允许访问")
    void hotComments_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/comment/hot")
                .param("articleId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("子评论列表 - 未登录应允许访问")
    void childComments_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/comment/children")
                .param("parentId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("敏感词检测 - 未登录应允许访问")
    void checkSensitiveWords_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/comment/check-sensitive")
                .contentType("application/json")
                .content("{\"content\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("替换敏感词 - 未登录应允许访问")
    void replaceSensitiveWords_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/comment/replace-sensitive")
                .contentType("application/json")
                .content("{\"content\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取文章评论数量 - 未登录应允许访问")
    void getArticleCommentCount_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/comment/article/1/count"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("发表评论 - 未登录应返回 401")
    void createComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/comment")
                .contentType("application/json")
                .content("{\"articleId\":1,\"content\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除评论 - 未登录应返回 401")
    void deleteComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/comment/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("评论点赞 - 未登录应返回 401")
    void likeComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/comment/1/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("取消评论点赞 - 未登录应返回 401")
    void unlikeComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/comment/1/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查评论点赞状态 - 未登录应返回 401")
    void checkCommentLikeStatus_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/comment/1/like-status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("发表评论 - 登录后应放行到控制器")
    void createComment_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/comment")
                .contentType("application/json")
                .content("{\"articleId\":1,\"content\":\"test\"}")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("删除评论 - 登录后应放行到控制器")
    void deleteComment_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/comment/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("评论点赞 - 登录后应放行到控制器")
    void likeComment_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/comment/1/like")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("检查评论点赞状态 - 登录后应放行到控制器")
    void checkCommentLikeStatus_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/comment/1/like-status")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }
}
