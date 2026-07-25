package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_cleanup_tasks")
public class FileCleanupTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String objectKey;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String status;
    private String lastError;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
