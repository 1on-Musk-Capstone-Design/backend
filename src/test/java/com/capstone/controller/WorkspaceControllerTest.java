package com.capstone.controller;

import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.capstone.domain.workspace.WorkspaceController.class)
@org.springframework.context.annotation.Import(com.capstone.config.TestSecurityConfig.class)
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

    @Test
    void getAllWorkspaces_shouldReturnWorkspaceList() throws Exception {
        // Given
        Workspace workspace1 = new Workspace();
        workspace1.setId(1L);
        workspace1.setName("팀 프로젝트");
        workspace1.setCreatedAt(Instant.now());

        Workspace workspace2 = new Workspace();
        workspace2.setId(2L);
        workspace2.setName("개인 프로젝트");
        workspace2.setCreatedAt(Instant.now());

        List<Workspace> mockWorkspaces = Arrays.asList(workspace1, workspace2);
        when(workspaceService.getAllWorkspaces()).thenReturn(mockWorkspaces);

        // When & Then
        mockMvc.perform(get("/api/v1/workspaces")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].workspaceId").value(1))
                .andExpect(jsonPath("$[0].name").value("팀 프로젝트"))
                .andExpect(jsonPath("$[1].workspaceId").value(2))
                .andExpect(jsonPath("$[1].name").value("개인 프로젝트"));
    }

    @Test
    void getAllWorkspaces_withEmptyList_shouldReturnEmptyArray() throws Exception {
        // Given
        when(workspaceService.getAllWorkspaces()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/v1/workspaces")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getWorkspaceById_shouldReturnWorkspace() throws Exception {
        // Given
        Workspace mockWorkspace = new Workspace();
        mockWorkspace.setId(1L);
        mockWorkspace.setName("팀 프로젝트");
        mockWorkspace.setCreatedAt(Instant.now());

        when(workspaceService.getWorkspaceById(1L)).thenReturn(mockWorkspace);

        // When & Then
        mockMvc.perform(get("/api/v1/workspaces/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(1))
                .andExpect(jsonPath("$.name").value("팀 프로젝트"));
    }

    @Test
    void getWorkspaceById_withNonExistentId_shouldReturnNotFound() throws Exception {
        // Given
        when(workspaceService.getWorkspaceById(999L))
                .thenThrow(new RuntimeException("워크스페이스를 찾을 수 없습니다. ID: 999"));

        // When & Then
        mockMvc.perform(get("/api/v1/workspaces/999")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }
}
