package com.blog.service.impl;

import com.blog.entity.FileCleanupTask;
import com.blog.mapper.FileCleanupTaskMapper;
import com.blog.service.TOSService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTest {

    @Mock
    private FileCleanupTaskMapper taskMapper;

    @Mock
    private TOSService tosService;

    @InjectMocks
    private FileCleanupService service;

    @Test
    void successfulCleanup_shouldDeleteTheTask() {
        FileCleanupTask task = pendingTask(1);
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(tosService.deleteFile(task.getObjectKey())).thenReturn(true);

        service.processPendingTasks();

        verify(taskMapper).deleteById(task.getId());
    }

    @Test
    void cleanupFailure_shouldUseBackoffAndEventuallyMarkFailed() {
        FileCleanupTask retryable = pendingTask(1);
        retryable.setRetryCount(1);
        FileCleanupTask exhausted = pendingTask(2);
        exhausted.setRetryCount(4);
        when(taskMapper.selectList(any())).thenReturn(List.of(retryable, exhausted));
        when(tosService.deleteFile(any())).thenReturn(false);

        service.processPendingTasks();

        ArgumentCaptor<FileCleanupTask> updates = ArgumentCaptor.forClass(FileCleanupTask.class);
        verify(taskMapper, org.mockito.Mockito.times(2)).updateById(updates.capture());
        assertThat(updates.getAllValues().get(0).getRetryCount()).isEqualTo(2);
        assertThat(updates.getAllValues().get(0).getStatus()).isEqualTo("pending");
        assertThat(updates.getAllValues().get(0).getNextRetryTime())
                .isAfter(LocalDateTime.now().plusMinutes(29));
        assertThat(updates.getAllValues().get(1).getRetryCount()).isEqualTo(5);
        assertThat(updates.getAllValues().get(1).getStatus()).isEqualTo("failed");
        assertThat(updates.getAllValues().get(1).getLastError())
                .contains(exhausted.getObjectKey());
    }

    private static FileCleanupTask pendingTask(long id) {
        FileCleanupTask task = new FileCleanupTask();
        task.setId(id);
        task.setObjectKey("attachments/orphan-" + id);
        task.setRetryCount(0);
        task.setStatus("pending");
        task.setNextRetryTime(LocalDateTime.now().minusMinutes(1));
        return task;
    }
}
