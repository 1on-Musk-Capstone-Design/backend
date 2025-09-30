package com.capstone.controller;

import com.capstone.dto.WorkspaceDtos;
import com.capstone.entity.Workspace;
import com.capstone.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceController.class)
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_shouldReturnCreatedWorkspace() throws Exception {
        // Given
        Workspace mockWorkspace = new Workspace();
        mockWorkspace.setId(1L);
        mockWorkspace.setName("테스트 워크스페이스");
        mockWorkspace.setCreatedAt(Instant.now());

        when(workspaceService.createWorkspace(any(String.class))).thenReturn(mockWorkspace);

        String requestBody = "{\"name\": \"테스트 워크스페이스\"}";

        // When & Then
        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(1))
                .andExpect(jsonPath("$.name").value("테스트 워크스페이스"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createWorkspace_withEmptyName_shouldReturnBadRequest() throws Exception {
        // Given
        String requestBody = "{\"name\": \"\"}";

        // When & Then
        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWorkspace_withNullName_shouldReturnBadRequest() throws Exception {
        // Given
        String requestBody = "{\"name\": null}";

        // When & Then
        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest());
    }
}
