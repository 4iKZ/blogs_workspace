package com.blog.service.impl;

import com.blog.entity.FileCleanupTask;
import com.blog.entity.FileInfo;
import com.blog.mapper.FileCleanupTaskMapper;
import com.blog.mapper.FileInfoMapper;
import com.blog.service.TOSService;
import com.blog.utils.AuthUtils;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadDeduplicationTest {

    private static final String HELLO_SHA256 =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private FileCleanupTaskMapper cleanupTaskMapper;

    @Mock
    private TOSService tosService;

    @InjectMocks
    private FileUploadServiceImpl service;

    @Test
    void sameContentDifferentFilename_sameUser_shouldReturnExistingWithoutUploading() {
        FileInfo existing = existingFile(7L);
        when(fileInfoMapper.selectOne(any())).thenReturn(existing);
        MockMultipartFile file = file("renamed.txt");

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);

            var result = service.uploadFile(file);

            assertThat(result.getData().getId()).isEqualTo(existing.getId());
        }

        verify(tosService, never()).uploadFile(any(), anyString());
    }

    @Test
    void sameContent_differentUsers_shouldNotShareTheExistingRecord() {
        when(fileInfoMapper.selectOne(any()))
                .thenReturn(existingFile(7L))
                .thenReturn(null);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/new.txt");
        when(fileInfoMapper.insert(any())).thenReturn(1);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L, 8L);

            service.uploadFile(file("first.txt"));
            service.uploadFile(file("second.txt"));
        }

        ArgumentCaptor<FileInfo> inserted = ArgumentCaptor.forClass(FileInfo.class);
        verify(fileInfoMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getUploadUserId()).isEqualTo(8L);
        assertThat(inserted.getValue().getContentHash()).isEqualTo(HELLO_SHA256);
        verify(tosService).uploadFile(any(), anyString());
    }

    @Test
    void concurrentUniqueConflict_shouldDeleteOwnObjectAndReturnWinner() {
        FileInfo winner = existingFile(7L);
        when(fileInfoMapper.selectOne(any())).thenReturn(null, winner);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/race.txt");
        when(fileInfoMapper.insert(any()))
                .thenThrow(new DuplicateKeyException("uk_file_info_user_hash"));
        when(tosService.deleteFile("attachments/race.txt")).thenReturn(true);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            var result = service.uploadFile(file("race.txt"));
            assertThat(result.getData().getId()).isEqualTo(winner.getId());
        }

        verify(tosService).deleteFile("attachments/race.txt");
    }

    @Test
    void databaseFailure_shouldImmediatelyDeleteTheUploadedObject() {
        when(fileInfoMapper.selectOne(any())).thenReturn(null);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/orphan.txt");
        when(fileInfoMapper.insert(any())).thenThrow(new IllegalStateException("database unavailable"));
        when(tosService.deleteFile("attachments/orphan.txt")).thenReturn(true);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            assertThat(service.uploadFile(file("orphan.txt")).getCode()).isNotEqualTo(200);
        }

        verify(tosService).deleteFile("attachments/orphan.txt");
        verify(cleanupTaskMapper, never()).insert(any());
    }

    @Test
    void compensationDeleteFailure_shouldCreateCleanupTask() {
        when(fileInfoMapper.selectOne(any())).thenReturn(null);
        when(tosService.uploadFile(any(), anyString()))
                .thenReturn("https://bucket.example/attachments/retry.txt");
        when(fileInfoMapper.insert(any())).thenThrow(new IllegalStateException("database unavailable"));
        when(tosService.deleteFile("attachments/retry.txt")).thenReturn(false);
        when(cleanupTaskMapper.insert(any())).thenReturn(1);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            service.uploadFile(file("retry.txt"));
        }

        ArgumentCaptor<FileCleanupTask> task = ArgumentCaptor.forClass(FileCleanupTask.class);
        verify(cleanupTaskMapper).insert(task.capture());
        assertThat(task.getValue().getObjectKey()).isEqualTo("attachments/retry.txt");
        assertThat(task.getValue().getStatus()).isEqualTo("pending");
        assertThat(task.getValue().getRetryCount()).isZero();
    }

    private static MockMultipartFile file(String filename) {
        return new MockMultipartFile("file", filename, "text/plain", "hello".getBytes());
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
}
