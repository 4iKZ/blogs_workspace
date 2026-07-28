package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.BackupInfoDTO;
import com.blog.service.DataBackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DatabaseRestoreEndpointRemovalTest {

    private MockMvc mockMvc;

    private DataBackupService dataBackupService;

    @BeforeEach
    void setUp() {
        dataBackupService = mock(DataBackupService.class);
        DataBackupController controller = new DataBackupController();
        ReflectionTestUtils.setField(controller, "dataBackupService", dataBackupService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void restoreEndpoint_shouldReturn404() throws Exception {
        mockMvc.perform(post("/api/system/backup/restore/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void backupList_shouldRemainAvailable() throws Exception {
        when(dataBackupService.getBackupList()).thenReturn(Result.success(List.<BackupInfoDTO>of()));

        mockMvc.perform(get("/api/system/backup/list"))
                .andExpect(status().isOk());

        verify(dataBackupService).getBackupList();
    }
}
