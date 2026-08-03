package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.Result;
import com.blog.dto.FileInfoDTO;
import com.blog.entity.FileCleanupTask;
import com.blog.entity.FileInfo;
import com.blog.exception.BusinessException;
import com.blog.mapper.FileCleanupTaskMapper;
import com.blog.mapper.FileInfoMapper;
import com.blog.service.TOSService;
import com.blog.utils.AuthUtils;
import com.volcengine.tos.TosClientException;
import com.volcengine.tos.TosServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceImplTest {

    private static final String HELLO_SHA256 =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private FileCleanupTaskMapper fileCleanupTaskMapper;

    @Mock
    private TOSService tosService;

    @InjectMocks
    private FileUploadServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxFileSize", 10_485_760L);
    }

    // ---- uploadImage ----

    @Test
    void uploadImage_tosServerException_shouldReturnErrorWithStatusCodeAndCode() {
        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            MockMultipartFile file = imageFile();
            when(tosService.uploadFileWithStyle(any(), eq("covers"), anyBoolean()))
                    .thenThrow(new TosServerException(500, "server error", "InternalError", "TosServerException", null));

            Result<String> result = service.uploadImage(file);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("500", "InternalError");
        }
    }

    @Test
    void uploadImage_tosClientException_shouldReturnError() {
        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            MockMultipartFile file = imageFile();
            when(tosService.uploadFileWithStyle(any(), eq("covers"), anyBoolean()))
                    .thenThrow(new TosClientException("client error", new Exception()));

            Result<String> result = service.uploadImage(file);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("client error");
        }
    }

    @Test
    void uploadImage_genericException_shouldReturnError() {
        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            MockMultipartFile file = imageFile();
            when(tosService.uploadFileWithStyle(any(), eq("covers"), anyBoolean()))
                    .thenThrow(new RuntimeException("unexpected"));

            Result<String> result = service.uploadImage(file);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("unexpected");
        }
    }

    // ---- uploadFile ----

    @Test
    void uploadFile_fileIsEmpty_shouldReturnError() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        Result<FileInfoDTO> result = service.uploadFile(emptyFile);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文件不能为空");
    }

    @Test
    void uploadFile_existingFileByUserAndHash_shouldReturnExistingWithoutTosUpload() {
        FileInfo existing = existingFile(7L);
        when(fileInfoMapper.selectOne(any())).thenReturn(existing);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<FileInfoDTO> result = service.uploadFile(file("hello.txt"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(existing.getId());
        }

        verify(tosService, never()).uploadFile(any(), anyString());
    }

    @Test
    void uploadFile_successCase_shouldUploadToTosAndInsertDb() {
        when(fileInfoMapper.selectOne(any())).thenReturn(null);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/new-file.txt");
        when(fileInfoMapper.insert(any())).thenReturn(1);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<FileInfoDTO> result = service.uploadFile(file("new.txt"));

            assertThat(result.isSuccess()).isTrue();
        }

        verify(tosService).uploadFile(any(), anyString());
        verify(fileInfoMapper).insert(any());
    }

    @Test
    void uploadFile_dbInsertReturnsZero_shouldCompensateTosObject() {
        when(fileInfoMapper.selectOne(any())).thenReturn(null);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/fail-insert.txt");
        when(fileInfoMapper.insert(any())).thenReturn(0);
        when(tosService.deleteFile("attachments/fail-insert.txt")).thenReturn(true);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<FileInfoDTO> result = service.uploadFile(file("fail-insert.txt"));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("保存文件信息失败");
        }

        verify(tosService).deleteFile("attachments/fail-insert.txt");
    }

    @Test
    void uploadFile_duplicateKeyException_shouldFindFallbackWinner() {
        FileInfo winner = existingFile(7L);
        when(fileInfoMapper.selectOne(any())).thenReturn(null, winner);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/dup.txt");
        when(fileInfoMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("uk_file_info_user_hash"));
        when(tosService.deleteFile("attachments/dup.txt")).thenReturn(true);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<FileInfoDTO> result = service.uploadFile(file("dup.txt"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(winner.getId());
        }

        verify(tosService).deleteFile("attachments/dup.txt");
    }

    @Test
    void uploadFile_duplicateKeyExceptionNoFallbackWinner_shouldReturnError() {
        when(fileInfoMapper.selectOne(any())).thenReturn(null, null);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/dup-no-winner.txt");
        when(fileInfoMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("uk_file_info_user_hash"));
        when(tosService.deleteFile("attachments/dup-no-winner.txt")).thenReturn(true);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<FileInfoDTO> result = service.uploadFile(file("dup-no-winner.txt"));

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("文件已存在，请重试");
        }

        verify(tosService).deleteFile("attachments/dup-no-winner.txt");
    }

    // ---- batchUploadFiles ----

    @Test
    void batchUploadFiles_emptyList_shouldReturnSuccessWithEmptyData() {
        Result<List<FileInfoDTO>> result = service.batchUploadFiles(List.of());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void batchUploadFiles_mixOfSuccessAndFailure_shouldFilterOnlySuccess() {
        when(fileInfoMapper.selectOne(any())).thenReturn(null);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/ok.txt");
        when(fileInfoMapper.insert(any())).thenReturn(1);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<List<FileInfoDTO>> result = service.batchUploadFiles(List.of(
                    file("a.txt"),
                    new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]),
                    file("b.txt")
            ));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(2);
        }
    }

    // ---- getFileList ----

    @Test
    void getFileList_withFileTypeFilter_shouldApplyFileTypeCondition() {
        Page<FileInfo> emptyPage = new Page<>(1, 10);
        when(fileInfoMapper.selectPage(any(), any())).thenReturn(emptyPage);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            service.getFileList(1, 10, "image");
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<FileInfo>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(fileInfoMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .containsIgnoringCase("file_type")
                .containsIgnoringCase("upload_user_id");
    }

    @Test
    void getFileList_adminUser_shouldNotFilterByOwner() {
        Page<FileInfo> emptyPage = new Page<>(1, 10);
        when(fileInfoMapper.selectPage(any(), any())).thenReturn(emptyPage);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            auth.when(AuthUtils::isAdmin).thenReturn(true);

            service.getFileList(1, 10, null);
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<FileInfo>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(fileInfoMapper).selectPage(any(), captor.capture());
        assertThat(captor.getValue().getSqlSegment()).doesNotContain("upload_user_id");
    }

    @Test
    void getFileList_exception_shouldReturnError() {
        when(fileInfoMapper.selectPage(any(), any())).thenThrow(new RuntimeException("db error"));

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            Result<List<FileInfoDTO>> result = service.getFileList(1, 10, null);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("获取文件列表失败");
        }
    }

    // ---- deleteFile ----

    @Test
    void deleteFile_fileNotFound_shouldReturnError() {
        when(fileInfoMapper.selectById(1L)).thenReturn(null);

        Result<Void> result = service.deleteFile(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文件不存在");
    }

    @Test
    void deleteFile_success_shouldDeleteDbAndScheduleTosCleanup() {
        FileInfo file = ownedFile(7L, "attachments/to-delete.txt",
                "https://bucket.example/attachments/to-delete.txt");
        when(fileInfoMapper.selectById(1L)).thenReturn(file);
        when(fileInfoMapper.deleteById(1L)).thenReturn(1);
        when(tosService.deleteFile("attachments/to-delete.txt")).thenReturn(true);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            Result<Void> result = service.deleteFile(1L);

            assertThat(result.isSuccess()).isTrue();
        }

        verify(fileInfoMapper).deleteById(1L);
        verify(tosService).deleteFile("attachments/to-delete.txt");
    }

    @Test
    void deleteFile_businessException_shouldPropagate() {
        FileInfo file = ownedFile(1L, "attachments/other.txt",
                "https://bucket.example/attachments/other.txt");
        when(fileInfoMapper.selectById(1L)).thenReturn(file);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            assertThatThrownBy(() -> service.deleteFile(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("权限");
        }

        verify(fileInfoMapper, never()).deleteById(anyLong());
    }

    // ---- getFileById ----

    @Test
    void getFileById_fileNotFound_shouldReturnError() {
        when(fileInfoMapper.selectById(1L)).thenReturn(null);

        Result<FileInfoDTO> result = service.getFileById(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("文件不存在");
    }

    @Test
    void getFileById_success_shouldReturnDTO() {
        FileInfo file = ownedFile(7L, "attachments/exists.txt",
                "https://bucket.example/attachments/exists.txt");
        file.setId(1L);
        file.setOriginalName("exists.txt");
        file.setFileName("uuid-exists.txt");
        file.setMimeType("text/plain");
        file.setFileSize(100L);
        file.setFileCategory("attachment");
        file.setContentHash(HELLO_SHA256);
        when(fileInfoMapper.selectById(1L)).thenReturn(file);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            Result<FileInfoDTO> result = service.getFileById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(1L);
            assertThat(result.getData().getFileUrl()).isEqualTo("https://bucket.example/attachments/exists.txt");
        }
    }

    // ---- checkFileExists ----

    @Test
    void checkFileExists_fileExists_shouldReturnDTO() {
        FileInfo file = ownedFile(7L, "attachments/checked.txt",
                "https://bucket.example/attachments/checked.txt");
        file.setId(1L);
        file.setOriginalName("checked.txt");
        file.setContentHash(HELLO_SHA256);
        when(fileInfoMapper.selectOne(any())).thenReturn(file);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            Result<FileInfoDTO> result = service.checkFileExists(HELLO_SHA256);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(1L);
        }
    }

    @Test
    void checkFileExists_fileNotFound_shouldReturnError() {
        when(fileInfoMapper.selectOne(any())).thenReturn(null);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<FileInfoDTO> result = service.checkFileExists(HELLO_SHA256);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("文件不存在");
        }
    }

    @Test
    void checkFileExists_exception_shouldReturnError() {
        when(fileInfoMapper.selectOne(any())).thenThrow(new RuntimeException("db error"));

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            Result<FileInfoDTO> result = service.checkFileExists(HELLO_SHA256);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("检查文件是否存在失败");
        }
    }

    // ---- extractObjectKeyFromUrl ----

    @Test
    void extractObjectKeyFromUrl_nullUrl_shouldReturnNull() {
        // Access private method via reflection
        String result = invokeExtractObjectKeyFromUrl(null);
        assertThat(result).isNull();
    }

    @Test
    void extractObjectKeyFromUrl_validUrl_shouldExtractKeyAfterThirdSlash() {
        String url = "https://syhaox.tos-cn-beijing.volces.com/old_book_system/covers/2025/12/08/uuid.jpg";
        String result = invokeExtractObjectKeyFromUrl(url);
        assertThat(result).isEqualTo("old_book_system/covers/2025/12/08/uuid.jpg");
    }

    @Test
    void extractObjectKeyFromUrl_urlWithoutEnoughSlashes_shouldReturnAsIs() {
        String url = "https://example.com";
        String result = invokeExtractObjectKeyFromUrl(url);
        assertThat(result).isEqualTo(url);
    }

    // ---- getFileExtension ----

    @Test
    void getFileExtension_fileNameWithExtension_shouldReturnExtension() {
        String result = invokeGetFileExtension("photo.jpg");
        assertThat(result).isEqualTo(".jpg");
    }

    @Test
    void getFileExtension_fileNameWithoutExtension_shouldReturnEmpty() {
        String result = invokeGetFileExtension("photo");
        assertThat(result).isEmpty();
    }

    // ---- helpers ----

    private static MockMultipartFile file(String filename) {
        return new MockMultipartFile("file", filename, "text/plain", "hello".getBytes());
    }

    private static MockMultipartFile imageFile() {
        // Create a real valid PNG using ImageIO
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new MockMultipartFile("file", "test.png", "image/png", baos.toByteArray());
    }

    private static FileInfo existingFile(Long ownerId) {
        FileInfo file = new FileInfo();
        file.setId(99L);
        file.setUploadUserId(ownerId);
        file.setContentHash(HELLO_SHA256);
        file.setFilePath("attachments/existing.txt");
        file.setFileUrl("https://bucket.example/attachments/existing.txt");
        return file;
    }

    private static FileInfo ownedFile(Long ownerId, String path, String url) {
        FileInfo file = new FileInfo();
        file.setId(10L);
        file.setUploadUserId(ownerId);
        file.setFilePath(path);
        file.setFileUrl(url);
        return file;
    }

    /** Reflectively invoke {@code extractObjectKeyFromUrl}. */
    private String invokeExtractObjectKeyFromUrl(String url) {
        try {
            java.lang.reflect.Method method = FileUploadServiceImpl.class
                    .getDeclaredMethod("extractObjectKeyFromUrl", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Reflectively invoke {@code getFileExtension}. */
    private String invokeGetFileExtension(String fileName) {
        try {
            java.lang.reflect.Method method = FileUploadServiceImpl.class
                    .getDeclaredMethod("getFileExtension", String.class);
            method.setAccessible(true);
            return (String) method.invoke(service, fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to avoid ambiguity in varargs matchers.
     */
    private static String eq(String value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
