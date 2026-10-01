package com.blog.service.impl;

import com.blog.entity.FileCleanupTask;
import com.blog.mapper.FileCleanupTaskMapper;
import com.blog.mapper.FileInfoMapper;
import com.blog.service.TOSService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceDeleteCleanupTest {

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private FileCleanupTaskMapper fileCleanupTaskMapper;

    @Mock
    private TOSService tosService;

    @InjectMocks
    private FileUploadServiceImpl service;

    @Test
    void deleteObjectAfterCommit_tosDeleteFails_shouldEnqueueCleanupTask() {
        when(tosService.deleteFile("orphan/key.png")).thenReturn(false);

        ReflectionTestUtils.invokeMethod(service, "deleteObjectAfterCommit", "orphan/key.png");

        org.mockito.ArgumentCaptor<FileCleanupTask> captor =
                org.mockito.ArgumentCaptor.forClass(FileCleanupTask.class);
        verify(fileCleanupTaskMapper).insert(captor.capture());
        assertThat(captor.getValue().getObjectKey()).isEqualTo("orphan/key.png");
        assertThat(captor.getValue().getLastError()).contains("TOS删除失败");
    }
}
