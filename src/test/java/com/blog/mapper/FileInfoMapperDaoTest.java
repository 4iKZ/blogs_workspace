package com.blog.mapper;

import com.blog.entity.FileInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("FileInfoMapper DAO 直测")
class FileInfoMapperDaoTest {

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long fileId;

    @AfterEach
    void cleanup() {
        if (fileId != null) {
            jdbcTemplate.execute("DELETE FROM file_info WHERE id = " + fileId);
        }
    }

    private FileInfo buildFileInfo(Long uploadUserId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName("dao-test.txt");
        fileInfo.setFileName("dao-test-1.txt");
        fileInfo.setFilePath("/tmp/dao-test.txt");
        fileInfo.setFileUrl("http://example.com/dao-test.txt");
        fileInfo.setContentHash("dao-test-content-hash");
        fileInfo.setFileSize(1024L);
        fileInfo.setFileType("text");
        fileInfo.setMimeType("text/plain");
        fileInfo.setFileExtension("txt");
        fileInfo.setFileCategory("attachment");
        fileInfo.setUploadUserId(uploadUserId);
        fileInfo.setStatus("active");
        return fileInfo;
    }

    @Test
    @DisplayName("文件信息插入、查询、更新与删除")
    void fileInfoCRUD_shouldPersistAndReturnRow() {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        FileInfo fileInfo = buildFileInfo(userId);

        int inserted = fileInfoMapper.insert(fileInfo);
        assertThat(inserted).isGreaterThan(0);
        fileId = fileInfo.getId();
        assertThat(fileId).isNotNull();

        FileInfo selected = fileInfoMapper.selectById(fileId);
        assertThat(selected).isNotNull();
        assertThat(selected.getOriginalName()).isEqualTo("dao-test.txt");
        assertThat(selected.getContentHash()).isEqualTo("dao-test-content-hash");
        assertThat(selected.getFileSize()).isEqualTo(1024L);

        selected.setStatus("deleted");
        selected.setOriginalName("dao-test-updated.txt");
        assertThat(fileInfoMapper.updateById(selected)).isEqualTo(1);

        FileInfo updated = fileInfoMapper.selectById(fileId);
        assertThat(updated.getStatus()).isEqualTo("deleted");
        assertThat(updated.getOriginalName()).isEqualTo("dao-test-updated.txt");

        assertThat(fileInfoMapper.deleteById(fileId)).isEqualTo(1);
        Long deletedId = fileId;
        fileId = null;
        assertThat(fileInfoMapper.selectById(deletedId)).isNull();
    }
}
