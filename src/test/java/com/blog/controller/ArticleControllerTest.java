package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.ChunkedUploadService;
import com.blog.utils.RedisDistributedLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleController 分片上传完成接口测试")
class ArticleControllerTest {

    @Mock
    private com.blog.service.ArticleService articleService;

    @Mock
    private com.blog.service.ArticleRankService articleRankService;

    @Mock
    private ChunkedUploadService chunkedUploadService;

    @Mock
    private RedisDistributedLock redisDistributedLock;

    @InjectMocks
    private ArticleController articleController;

    @Test
    @DisplayName("completeChunkedUpload 成功后即使解锁失败也返回成功")
    void testCompleteChunkedUpload_SuccessDespiteUnlockFailure() {
        when(redisDistributedLock.tryLockWithWatchdog("upload:complete:upload-1", 30L, TimeUnit.SECONDS, 3L, TimeUnit.SECONDS))
                .thenReturn("lock-token");
        when(chunkedUploadService.completeUpload("upload-1")).thenReturn("https://example.com/file.jpg");
        doThrow(new RuntimeException("unlock failed"))
                .when(redisDistributedLock).unlock("upload:complete:upload-1", "lock-token");

        Result<Map<String, String>> result = articleController.completeChunkedUpload("upload-1", "cover.jpg", 3);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("https://example.com/file.jpg", result.getData().get("url"));
        verify(redisDistributedLock).unlock("upload:complete:upload-1", "lock-token");
    }

    @Test
    @DisplayName("completeChunkedUpload 主流程失败时解锁异常不覆盖原错误")
    void testCompleteChunkedUpload_ErrorDespiteUnlockFailure() {
        when(redisDistributedLock.tryLockWithWatchdog("upload:complete:upload-2", 30L, TimeUnit.SECONDS, 3L, TimeUnit.SECONDS))
                .thenReturn("lock-token");
        when(chunkedUploadService.completeUpload("upload-2"))
                .thenThrow(new RuntimeException("merge failed"));
        doThrow(new RuntimeException("unlock failed"))
                .when(redisDistributedLock).unlock("upload:complete:upload-2", "lock-token");

        Result<Map<String, String>> result = articleController.completeChunkedUpload("upload-2", "cover.jpg", 3);

        assertEquals(500, result.getCode());
        assertEquals("完成上传失败: merge failed", result.getMessage());
        verify(redisDistributedLock).unlock("upload:complete:upload-2", "lock-token");
    }
}
