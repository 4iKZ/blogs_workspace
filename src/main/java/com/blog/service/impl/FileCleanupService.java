package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.entity.FileCleanupTask;
import com.blog.mapper.FileCleanupTaskMapper;
import com.blog.service.TOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private static final int MAX_RETRIES = 5;
    private static final long[] BACKOFF_MINUTES = {1, 5, 30, 120, 360};

    private final FileCleanupTaskMapper taskMapper;
    private final TOSService tosService;

    @Scheduled(fixedDelay = 60_000)
    public void processPendingTasks() {
        List<FileCleanupTask> tasks = taskMapper.selectList(
                new QueryWrapper<FileCleanupTask>()
                        .eq("status", "pending")
                        .le("next_retry_time", LocalDateTime.now())
                        .orderByAsc("next_retry_time")
                        .last("LIMIT 50")
        );
        tasks.forEach(this::processTask);
    }

    private void processTask(FileCleanupTask task) {
        String error = "TOS删除返回失败，objectKey=" + task.getObjectKey();
        try {
            if (tosService.deleteFile(task.getObjectKey())) {
                taskMapper.deleteById(task.getId());
                return;
            }
        } catch (Exception exception) {
            error = exception.getMessage() + "，objectKey=" + task.getObjectKey();
        }

        int retryCount = task.getRetryCount() + 1;
        task.setRetryCount(retryCount);
        task.setLastError(error);
        task.setUpdateTime(LocalDateTime.now());
        if (retryCount >= MAX_RETRIES) {
            task.setStatus("failed");
            log.error("TOS对象清理已达最大重试次数，objectKey={}", task.getObjectKey());
        } else {
            task.setNextRetryTime(
                    LocalDateTime.now().plusMinutes(BACKOFF_MINUTES[retryCount])
            );
        }
        taskMapper.updateById(task);
    }
}
