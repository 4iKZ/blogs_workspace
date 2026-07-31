package com.blog.mapper;

import com.blog.entity.UploadFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("UploadFileMapper DAO 直测")
class UploadFileMapperDaoTest {

    @Autowired
    private UploadFileMapper uploadFileMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM upload_files WHERE file_path LIKE '%dao-test%'");
    }

    @Test
    @DisplayName("用户文件列表、统计与大小汇总")
    void uploadFileQueries_shouldReturnInsertedFiles() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        jdbcTemplate.execute(
                "INSERT INTO upload_files (original_name, file_name, file_path, file_url, file_size, file_type, mime_type, upload_user_id, status, create_time, update_time) " +
                        "VALUES ('dao-test.txt','dao-test.txt','/tmp/dao-test.txt','http://example.com/dao-test.txt',1024,'text','text/plain'," + userId + ",1,NOW(),NOW())"
        );

        List<UploadFile> files = uploadFileMapper.selectFilesByUserId(userId, 1);
        assertThat(files).extracting(UploadFile::getFilePath).contains("/tmp/dao-test.txt");

        assertThat(uploadFileMapper.countUserFiles(userId)).isGreaterThanOrEqualTo(1);
        assertThat(uploadFileMapper.sumUserFileSize(userId)).isGreaterThan(0);

        List<UploadFileMapper.FileTypeStatistics> stats = uploadFileMapper.selectFileTypeStatistics(userId);
        assertThat(stats).anyMatch(item -> "text".equals(item.getFileType()));
    }
}
