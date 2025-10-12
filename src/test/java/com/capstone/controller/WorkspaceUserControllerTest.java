package com.capstone.controller;

import com.capstone.config.TestSecurityConfig;
import com.capstone.domain.workspaceUser.WorkspaceUserController;
import com.capstone.domain.workspaceUser.WorkspaceUserResponse;
import com.capstone.domain.workspaceUser.WorkspaceUserService;
import com.capstone.global.oauth.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceUserController.class)
@org.springframework.context.annotation.Import(TestSecurityConfig.class)
class WorkspaceUserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private WorkspaceUserService workspaceUserService;

  @MockBean
  private JwtProvider jwtProvider;

  @Test
  @DisplayName("워크스페이스 참여 성공")
  void joinWorkspace() throws Exception {
    Long workspaceId = 1L;
    Long userId = 10L;
    String token = "Bearer fake";

    when(jwtProvider.getUserIdFromAccessToken("fake")).thenReturn(userId);
    doNothing().when(workspaceUserService).joinWorkspace(workspaceId, userId);

    mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/join", workspaceId)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("워크스페이스 참여 성공"));
  }

  @Test
  @DisplayName("워크스페이스 사용자 목록 조회")
  void getWorkspaceUsers() throws Exception {
    Long workspaceId = 1L;

    WorkspaceUserResponse user1 = WorkspaceUserResponse.builder()
        .id(10L)
        .email("캡스톤@user.com")
        .name("캡스톤 유저")
        .role(com.capstone.global.type.Role.MEMBER)
        .build();

    when(workspaceUserService.getWorkspaceUsers(workspaceId)).thenReturn(List.of(user1));

    mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/users", workspaceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10))
        .andExpect(jsonPath("$[0].name").value("캡스톤 유저"));
  }

  @Test
  @DisplayName("워크스페이스 사용자 삭제 성공")
  void removeUser() throws Exception {
    Long workspaceId = 1L;
    Long targetUserId = 20L;
    Long requesterId = 10L;
    String token = "Bearer fake";

    when(jwtProvider.getUserIdFromAccessToken("fake")).thenReturn(requesterId);
    doNothing().when(workspaceUserService).removeUser(workspaceId, targetUserId, requesterId);

    mockMvc.perform(delete("/api/v1/workspaces/{workspaceId}/users/{userId}", workspaceId, targetUserId)
            .header(HttpHeaders.AUTHORIZATION, token)
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string("유저 삭제 완료"));
  }
}
