package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CommentControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("评论列表 - 公开接口应可匿名访问")
    void commentList_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/comment/list?articleId=1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("热门评论 - 公开接口应可匿名访问")
    void hotComments_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/comment/hot?articleId=1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("评论详情 - 需要认证")
    void commentById_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/comment/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("发表评论 - 需要认证")
    void createComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleId\":1,\"content\":\"test comment\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除评论 - 需要认证")
    void deleteComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/comment/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("评论点赞 - 需要认证")
    void likeComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/comment/1/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("取消评论点赞 - 需要认证")
    void unlikeComment_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/comment/1/like"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("敏感词检测 - 公开接口应可匿名访问")
    void checkSensitiveWords_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/comment/check-sensitive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("子评论列表 - 公开接口应可匿名访问")
    void childComments_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/comment/children?parentId=1"))
                .andExpect(status().isOk());
    }
}
