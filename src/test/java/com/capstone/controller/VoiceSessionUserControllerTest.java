package com.capstone.controller;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.voicesessionUser.VoiceSessionUser;
import com.capstone.domain.voicesessionUser.VoiceSessionUserController;
import com.capstone.domain.voicesessionUser.VoiceSessionUserService;
import com.capstone.domain.voicesessionUser.VoiceSessionUserRequest;
import com.capstone.domain.voicesessionUser.VoiceSessionUserResponse;
import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspace.Workspace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoiceSessionUserController.class)
@WithMockUser
public class VoiceSessionUserControllerTest {

  private static final Long WORKSPACE_ID = 1L;
  private static final Long SESSION_ID = 10L;
  private static final Long USER_ID = 100L;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private VoiceSessionUserService voiceSessionUserService;

  @Nested
  @DisplayName("세션 참여 테스트")
  public class JoinSession {

    @Test
    @DisplayName("세션 참여 성공")
    void joinSession_created() throws Exception {
      VoiceSessionUser joined = createVoiceSessionUser(SESSION_ID, USER_ID, "홍길동", 1L);
      when(voiceSessionUserService.joinSession(WORKSPACE_ID, SESSION_ID, USER_ID)).thenReturn(
          joined);

      String requestBody = "{\"workspaceUserId\": " + USER_ID + "}";

      mockMvc.perform(
              post("/v1/workspaces/{workspaceId}/voice/{sessionId}/users", WORKSPACE_ID, SESSION_ID)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestBody)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.sessionId").value(SESSION_ID))
          .andExpect(jsonPath("$.workspaceUserId").value(USER_ID))
          .andExpect(jsonPath("$.active").value(true));
    }

    // @Test
    // @DisplayName("실패 - 잘못된 요청 (userId 누락) - 400")
    // void joinSession_badRequest() throws Exception {
    //     mockMvc.perform(post("/v1/workspaces/{workspaceId}/voice/{sessionId}/users", WORKSPACE_ID, SESSION_ID)
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content("{}")
    //             .with(csrf()))
    //             .andDo(print())
    //             .andExpect(status().isBadRequest());
    // }
  }

  @Nested
  @DisplayName("세션 퇴장 테스트")
  public class LeaveSession {

    @Test
    @DisplayName("세션 퇴장 성공")
    void leaveSession_ok() throws Exception {
      VoiceSessionUser left = createVoiceSessionUser(SESSION_ID, USER_ID, "홍길동", 1L);
      left.leave();
      when(voiceSessionUserService.leaveSession(WORKSPACE_ID, SESSION_ID, USER_ID)).thenReturn(
          left);

      mockMvc.perform(
              delete("/v1/workspaces/{workspaceId}/voice/{sessionId}/users/{userId}", WORKSPACE_ID,
                  SESSION_ID, USER_ID)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.active").value(false));
    }
  }

  @Nested
  @DisplayName("채널 이동 테스트")
  public class MoveToSession {

    @Test
    @DisplayName("채널 이동 성공")
    void moveToSession_ok() throws Exception {
      Long toSessionId = 20L;
      VoiceSessionUser moved = createVoiceSessionUser(toSessionId, USER_ID, "홍길동", 1L);
      when(voiceSessionUserService.moveToSession(eq(WORKSPACE_ID), eq(SESSION_ID), eq(toSessionId),
          eq(USER_ID))).thenReturn(moved);

      String requestBody = "{\"workspaceUserId\": " + USER_ID + "}";

      mockMvc.perform(
              post("/v1/workspaces/{workspaceId}/voice/{sessionId}/users/move", WORKSPACE_ID,
                  SESSION_ID)
                  .param("fromSessionId", SESSION_ID.toString())
                  .param("toSessionId", toSessionId.toString())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestBody)
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.sessionId").value(toSessionId));
    }
  }

  @Nested
  @DisplayName("활성 참여자 조회")
  public class GetActiveUsers {

    @Test
    @DisplayName("활성화 된 참여자 조회 성공")
    void getActiveUsers_ok() throws Exception {
      List<VoiceSessionUser> activeUsers = Arrays.asList(
          createVoiceSessionUser(SESSION_ID, USER_ID, "홍길동", 1L),
          createVoiceSessionUser(SESSION_ID, 101L, "김철수", 2L)
      );
      when(voiceSessionUserService.getActiveUsers(WORKSPACE_ID, SESSION_ID)).thenReturn(
          activeUsers);

      mockMvc.perform(
              get("/v1/workspaces/{workspaceId}/voice/{sessionId}/users", WORKSPACE_ID, SESSION_ID))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].active").value(true));
    }
  }

  @Nested
  @DisplayName("활성 참여자 수 조회")
  public class CountActiveUsers {

    @Test
    @DisplayName("활성화 된 참여자 수 조회 성공")
    void countActiveUsers_ok() throws Exception {
      when(voiceSessionUserService.getActiveUserCount(WORKSPACE_ID, SESSION_ID)).thenReturn(5L);

      mockMvc.perform(
              get("/v1/workspaces/{workspaceId}/voice/{sessionId}/users/count", WORKSPACE_ID,
                  SESSION_ID))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(content().string("5"));
    }
  }

  private VoiceSessionUser createVoiceSessionUser(Long sessionId, Long workspaceUserId,
      String userName, Long voiceSessionUserId) {
    // 세션
    VoiceSession session = new VoiceSession();
    session.setId(sessionId);

    // 워크스페이스
    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(WORKSPACE_ID);
    session.setWorkspace(workspace);

    // 테스트용 유저 생성
    User user = User.builder()
        .id(workspaceUserId)
        .name(userName)
        .build();

    // 워크스페이스 유저
    WorkspaceUser workspaceUser = WorkspaceUser.builder()
        .id(workspaceUserId)
        .user(user)
        .build();

    // 보이스세션유저
    VoiceSessionUser voiceSessionUser = VoiceSessionUser.builder()
        .id(voiceSessionUserId)
        .session(session)
        .workspaceUser(workspaceUser)
        .joinedAt(LocalDateTime.now())
        .leftAt(null)
        .build();

    return voiceSessionUser;
  }
}