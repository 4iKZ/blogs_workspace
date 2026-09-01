package com.blog.controller;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class CommentControllerReflectionTest {

    @Autowired
    private CommentController commentController;

    @MockBean
    private CommentService commentService;

    @Test
    @DisplayName("getCommentList - 用户未登录时应返回成功")
    void getCommentList_missingUserContext_shouldReturnSuccess() throws Exception {
        Field requestField = CommentController.class.getDeclaredField("request");
        requestField.setAccessible(true);
        requestField.set(commentController, mock(HttpServletRequest.class));

        when(commentService.getCommentList(anyLong(), anyInt(), anyInt(), isNull(), anyString(), any()))
                .thenReturn(Result.success(PageResult.empty(1, 10)));

        Result<?> result = commentController.getCommentList(1L, 1, 10, null, "time");

        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("batchCheckCommentLikeStatus - 用户未登录时应返回成功")
    void batchCheckCommentLikeStatus_missingUserContext_shouldReturnSuccess() throws Exception {
        Field requestField = CommentController.class.getDeclaredField("request");
        requestField.setAccessible(true);
        requestField.set(commentController, mock(HttpServletRequest.class));

        when(commentService.batchCheckCommentLikeStatus(any(), any()))
                .thenReturn(Result.success(Map.of()));

        Result<?> result = commentController.batchCheckCommentLikeStatus(List.of(1L, 2L));

        assertEquals(200, result.getCode());
    }
}
