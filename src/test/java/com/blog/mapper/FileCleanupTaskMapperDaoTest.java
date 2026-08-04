package com.blog.mapper;

import com.blog.entity.FileCleanupTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("FileCleanupTaskMapper DAO 直测")
class FileCleanupTaskMapperDaoTest {

    @Autowired
    private FileCleanupTaskMapper fileCleanupTaskMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long taskId;

    @AfterEach
    void cleanup() {
        if (taskId != null) {
            jdbcTemplate.execute("DELETE FROM file_cleanup_tasks WHERE id = " + taskId);
        }
    }

    private FileCleanupTask buildTask() {
        FileCleanupTask task = new FileCleanupTask();
        task.setObjectKey("dao-test/object-" + System.nanoTime());
        task.setRetryCount(0);
        task.setNextRetryTime(LocalDateTime.now().plusHours(1));
        task.setStatus("pending");
        return task;
    }

    @Test
    @DisplayName("清理任务插入、查询、更新与删除")
    void fileCleanupTaskCRUD_shouldPersistAndReturnRow() {
        FileCleanupTask task = buildTask();

        int inserted = fileCleanupTaskMapper.insert(task);
        assertThat(inserted).isGreaterThan(0);
        taskId = task.getId();
        assertThat(taskId).isNotNull();

        FileCleanupTask selected = fileCleanupTaskMapper.selectById(taskId);
        assertThat(selected).isNotNull();
        assertThat(selected.getObjectKey()).isEqualTo(task.getObjectKey());
        assertThat(selected.getStatus()).isEqualTo("pending");
        assertThat(selected.getRetryCount()).isZero();

        selected.setStatus("failed");
        selected.setRetryCount(2);
        selected.setLastError("dao-test-error");
        assertThat(fileCleanupTaskMapper.updateById(selected)).isEqualTo(1);

        FileCleanupTask updated = fileCleanupTaskMapper.selectById(taskId);
        assertThat(updated.getStatus()).isEqualTo("failed");
        assertThat(updated.getRetryCount()).isEqualTo(2);
        assertThat(updated.getLastError()).isEqualTo("dao-test-error");

        assertThat(fileCleanupTaskMapper.deleteById(taskId)).isEqualTo(1);
        Long deletedId = taskId;
        taskId = null;
        assertThat(fileCleanupTaskMapper.selectById(deletedId)).isNull();
    }
}
