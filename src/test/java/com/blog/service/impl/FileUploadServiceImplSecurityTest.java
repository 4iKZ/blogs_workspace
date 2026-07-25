package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.FileInfo;
import com.blog.exception.BusinessException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceImplSecurityTest {

    @Mock
    private FileInfoMapper fileInfoMapper;

    @Mock
    private TOSService tosService;

    @InjectMocks
    private FileUploadServiceImpl service;

    @Test
    void getFileList_regularUser_shouldFilterByOwner() {
        when(fileInfoMapper.selectPage(any(), any())).thenReturn(new Page<FileInfo>(1, 10));

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            service.getFileList(1, 10, null);
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<FileInfo>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(fileInfoMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getSqlSegment())
                .containsIgnoringCase("upload_user_id");
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs()).containsValue(7L);
    }

    @Test
    void getFileById_unrelatedUser_shouldReject() {
        FileInfo file = ownedFile(1L);
        when(fileInfoMapper.selectById(10L)).thenReturn(file);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            assertThatThrownBy(() -> service.getFileById(10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("权限");
        }
    }

    @Test
    void deleteFile_unrelatedUser_shouldRejectBeforeDeletingAnything() {
        FileInfo file = ownedFile(1L);
        when(fileInfoMapper.selectById(10L)).thenReturn(file);

        try (MockedStatic<AuthUtils> auth = Mockito.mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(7L);
            auth.when(AuthUtils::isAdmin).thenReturn(false);

            assertThatThrownBy(() -> service.deleteFile(10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("权限");
        }

        verify(fileInfoMapper, never()).deleteById(anyLong());
        verify(tosService, never()).deleteFile(anyString());
    }

    private static FileInfo ownedFile(Long ownerId) {
        FileInfo file = new FileInfo();
        file.setId(10L);
        file.setUploadUserId(ownerId);
        file.setFilePath("attachments/test.txt");
        return file;
    }
}
