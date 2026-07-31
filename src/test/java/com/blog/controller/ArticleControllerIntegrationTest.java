package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArticleControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取文章列表 - 未登录应允许访问")
    void getArticleList_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/article/list")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取文章详情 - 未登录应允许访问")
    void getArticleDetail_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/article/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取热门文章 - 未登录应允许访问")
    void getHotArticles_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/article/hot")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取推荐文章 - 未登录应允许访问")
    void getRecommendedArticles_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/article/recommended")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("搜索文章 - 未登录应允许访问")
    void searchArticles_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/article/search")
                .param("keyword", "test")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("按分类获取文章 - 未登录应返回 401")
    void getArticlesByCategory_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/article/category/1")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取关注作者的文章 - 未登录应返回 401")
    void getFollowingArticles_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/article/following")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("发布文章 - 未登录应返回 401")
    void publishArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/article/publish")
                .contentType("application/json")
                .content("{\"title\":\"test\",\"content\":\"test\",\"categoryId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("编辑文章 - 未登录应返回 401")
    void editArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/article/1")
                .contentType("application/json")
                .content("{\"title\":\"test\",\"content\":\"test\",\"categoryId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除文章 - 未登录应返回 401")
    void deleteArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/article/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("发布文章 - 登录后应放行到控制器")
    void publishArticle_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/article/publish")
                .contentType("application/json")
                .content("{\"title\":\"test\",\"content\":\"test\",\"categoryId\":1}")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("编辑文章 - 登录后应放行到控制器")
    void editArticle_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(put("/api/article/1")
                .contentType("application/json")
                .content("{\"title\":\"test\",\"content\":\"test\",\"categoryId\":1}")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取用户的文章列表 - 登录后应放行到控制器")
    void getUserArticles_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/user/1")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取用户的文章列表 - 管理员可访问其他用户私有文章")
    void getUserArticles_admin_shouldAccessOtherUsersPrivateArticles() throws Exception {
        mockMvc.perform(get("/api/article/user/1")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("按分类获取文章 - 登录后应放行到控制器")
    void getArticlesByCategory_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/category/1")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("删除文章 - 登录后应放行到控制器")
    void deleteArticle_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/article/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取用户点赞的文章列表 - 登录后应放行到控制器")
    void getUserLikedArticles_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/user/1/liked")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取用户收藏的文章列表 - 登录后应放行到控制器")
    void getUserFavoriteArticles_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/user/1/favorite")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取用户点赞的文章列表 - 管理员可访问其他用户点赞文章")
    void getUserLikedArticles_admin_shouldAccessOtherUsersLikedArticles() throws Exception {
        mockMvc.perform(get("/api/article/user/1/liked")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取用户收藏的文章列表 - 管理员可访问其他用户收藏文章")
    void getUserFavoriteArticles_admin_shouldAccessOtherUsersFavoriteArticles() throws Exception {
        mockMvc.perform(get("/api/article/user/1/favorite")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取关注作者的文章 - 登录后应放行到控制器")
    void getFollowingArticles_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/following")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("搜索文章 - 登录后应放行到控制器")
    void searchArticles_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/search")
                .param("keyword", "test")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取文章详情 - 登录后应放行到控制器")
    void getArticleDetail_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取热门文章 - 登录后应放行到控制器")
    void getHotArticles_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/hot")
                .param("limit", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("获取推荐文章 - 登录后应放行到控制器")
    void getRecommendedArticles_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/recommended")
                .param("limit", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("初始化分片上传 - 未登录应返回 401")
    void initChunkedUpload_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/article/init-upload")
                .contentType("application/json")
                .content("{\"fileName\":\"test.mp4\",\"fileSize\":1048576,\"totalChunks\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("初始化分片上传 - 登录后应放行到控制器")
    void initChunkedUpload_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/article/init-upload")
                .contentType("application/json")
                .content("{\"fileName\":\"test.mp4\",\"fileSize\":1048576,\"totalChunks\":10}")
                .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("上传单个分片 - 未登录应返回 401")
    void uploadChunk_shouldRequireAuth() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        mockMvc.perform(multipart("/api/article/upload-chunk")
                .file(file)
                .param("uploadId", "upload-123")
                .param("index", "0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("上传单个分片 - 登录后应放行到控制器")
    void uploadChunk_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/article/upload-chunk")
                .param("uploadId", "upload-123")
                .param("index", "0")
                .requestAttr("userId", 1L))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("完成分片上传 - 未登录应返回 401")
    void completeChunkedUpload_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/article/complete-upload")
                .contentType("application/json")
                .content("{\"uploadId\":\"upload-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("完成分片上传 - 登录后应放行到控制器")
    void completeChunkedUpload_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/article/complete-upload")
                .contentType("application/json")
                .content("{\"uploadId\":\"upload-123\"}")
                .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("完成分片上传 - 缺少 uploadId")
    void completeChunkedUpload_missingUploadId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/article/complete-upload")
                .contentType("application/json")
                .content("{}")
                .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("取消分片上传 - 未登录应返回 401")
    void cancelChunkedUpload_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/article/cancel-upload")
                .contentType("application/json")
                .content("{\"uploadId\":\"upload-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("取消分片上传 - 登录后应放行到控制器")
    void cancelChunkedUpload_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/article/cancel-upload")
                .contentType("application/json")
                .content("{\"uploadId\":\"upload-123\"}")
                .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("检查可恢复上传 - 未登录应返回 401")
    void checkUpload_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/article/check-upload/fileHash123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("检查可恢复上传 - 登录后应放行到控制器")
    void checkUpload_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/check-upload/fileHash123")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取上传状态 - 未登录应返回 401")
    void getUploadStatus_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/article/upload-status/upload-123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("获取上传状态 - 登录后应放行到控制器")
    void getUploadStatus_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/article/upload-status/upload-123")
                .requestAttr("userId", 1L))
                .andExpect(status().isBadRequest());
    }
}
