package com.capstone.controller;

import com.capstone.config.TestSecurityConfig;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceController;
import com.capstone.domain.workspace.WorkspaceDtos;
import com.capstone.domain.workspace.WorkspaceService;
import com.capstone.domain.workspaceInvite.WorkspaceInviteService;
import com.capstone.global.oauth.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceController.class)
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
class WorkspaceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private WorkspaceService workspaceService;

  @MockBean
  private WorkspaceInviteService workspaceInviteService;

  @MockBean
  private JwtProvider jwtProvider;

  @Test
  @DisplayName("워크스페이스 생성 시 정상적으로 반환")
  void createWorkspace() throws Exception {
    // Given
    Workspace mockWorkspace = new Workspace();
    mockWorkspace.setWorkspaceId(1L);
    mockWorkspace.setName("캡스톤 워크스페이스");
    mockWorkspace.setCreatedAt(Instant.now());

    when(jwtProvider.getUserIdFromAccessToken("fake")).thenReturn(10L);
    when(workspaceService.createWorkspace("캡스톤 워크스페이스", 10L))
        .thenReturn(mockWorkspace);

    String requestBody = "{\"name\": \"캡스톤 워크스페이스\"}";

    // When & Then
    mockMvc.perform(post("/v1/workspaces")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .header(HttpHeaders.AUTHORIZATION, "Bearer fake")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workspaceId").value(1))
        .andExpect(jsonPath("$.name").value("캡스톤 워크스페이스"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  @DisplayName("워크스페이스 이름 수정 시 OWNER면 정상")
  void updateWorkspace() throws Exception {
    WorkspaceDtos.ListItem dto = new WorkspaceDtos.ListItem();
    dto.setWorkspaceId(1L);
    dto.setName("캡스톤");

    when(jwtProvider.getUserIdFromAccessToken("fake")).thenReturn(10L);
    when(workspaceService.updateWorkspaceName(1L, "캡스톤", 10L))
        .thenReturn(dto);

    String requestBody = "{\"name\": \"캡스톤\"}";

    mockMvc.perform(put("/v1/workspaces/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .header(HttpHeaders.AUTHORIZATION, "Bearer fake")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workspaceId").value(1))
        .andExpect(jsonPath("$.name").value("캡스톤"));
  }

  @Test
  @DisplayName("워크스페이스 삭제 시 OWNER면 성공 메시지를 반환")
  void deleteWorkspace() throws Exception {
    doNothing().when(workspaceService).deleteWorkspace(1L, 10L);
    when(jwtProvider.getUserIdFromAccessToken("fake")).thenReturn(10L);

    mockMvc.perform(delete("/v1/workspaces/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer fake")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("워크스페이스가 삭제되었습니다."));
  }
}