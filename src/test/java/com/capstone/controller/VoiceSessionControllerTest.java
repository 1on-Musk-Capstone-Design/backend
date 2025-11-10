package com.capstone.controller;

import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.voicesession.VoiceSessionController;
import com.capstone.domain.voicesession.VoiceSessionService;
import com.capstone.domain.workspace.Workspace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoiceSessionController.class)
@WithMockUser
class VoiceSessionControllerTest {

  private static final Long WORKSPACE_ID = 1L;
  private static final Long SESSION_ID = 10L;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private VoiceSessionService voiceSessionService;

  @Nested
  @DisplayName("세션 생성 테스트")
  class StartSession {

    @Test
    @DisplayName("세션 생성 성공")
    void startSession_created() throws Exception {
      // Given
      VoiceSession session = new VoiceSession(workspace(WORKSPACE_ID), LocalDateTime.now());
      setId(session, SESSION_ID);
      safeSetEndedAt(session, null);
      when(voiceSessionService.startSession(WORKSPACE_ID)).thenReturn(session);

      // When
      var result = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/voice", WORKSPACE_ID)
          .with(csrf()));

      // Then
      result.andDo(print())
          .andExpect(status().isCreated())
          .andExpect(
              header().string(HttpHeaders.LOCATION, "/api/v1/workspaces/1/voice/" + SESSION_ID))
          .andExpect(jsonPath("$.id").value(SESSION_ID))
          .andExpect(jsonPath("$.workspaceId").value(WORKSPACE_ID))
          .andExpect(jsonPath("$.startedAt").exists())
          .andExpect(endedAtAbsentOrNull());
    }
  }

  @Nested
  @DisplayName("세션 종료 테스트")
  class EndSession {

    @Test
    @DisplayName("세션 종료 성공")
    void endSession_ok() throws Exception {
      // Given
      VoiceSession ended = new VoiceSession(workspace(WORKSPACE_ID),
          LocalDateTime.now().minusMinutes(5));
      setId(ended, SESSION_ID);
      safeSetEndedAt(ended, LocalDateTime.now());
      when(voiceSessionService.endSession(SESSION_ID)).thenReturn(ended);

      // When
      var result = mockMvc.perform(
          patch("/api/v1/workspaces/{workspaceId}/voice/{sessionId}", WORKSPACE_ID, SESSION_ID)
              .with(csrf()));

      // Then
      result.andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(SESSION_ID))
          .andExpect(jsonPath("$.workspaceId").value(WORKSPACE_ID))
          .andExpect(jsonPath("$.endedAt").exists());
    }

    @Test
    @DisplayName("세션 종료 실패")
    void endSession_workspaceMismatch_badRequest() throws Exception {
      // Given
      VoiceSession other = new VoiceSession(workspace(2L), LocalDateTime.now().minusMinutes(2));
      setId(other, SESSION_ID);
      safeSetEndedAt(other, LocalDateTime.now());
      when(voiceSessionService.endSession(SESSION_ID)).thenReturn(other);

      // When
      var result = mockMvc.perform(
          patch("/api/v1/workspaces/{workspaceId}/voice/{sessionId}", WORKSPACE_ID, SESSION_ID)
              .with(csrf()));

      // Then
      result.andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  // ===== 헬퍼 메서드 =====

  private Workspace workspace(Long id) {
    Workspace w = new Workspace();
    if (!tryInvoke(w, "setWorkspaceId", id) &&
        !tryInvoke(w, "setId", id)) {
      ReflectionTestUtils.setField(w, "workspaceId", id);
      ReflectionTestUtils.setField(w, "id", id);
    }
    return w;
  }

  private void setId(VoiceSession session, Long id) {
      if (tryInvoke(session, "setId", id)) {
          return;
      }
      if (tryInvoke(session, "setVoiceSessionId", id)) {
          return;
      }
    ReflectionTestUtils.setField(session, "id", id);
    ReflectionTestUtils.setField(session, "voiceSessionId", id);
  }

  private void safeSetEndedAt(VoiceSession session, LocalDateTime value) {
      if (tryInvoke(session, "setEndedAt", value)) {
          return;
      }
    ReflectionTestUtils.setField(session, "endedAt", value);
  }

  private boolean tryInvoke(Object target, String method, Object arg) {
    try {
      Class<?> argType = (arg == null) ? LocalDateTime.class : arg.getClass();
      target.getClass().getMethod(method, argType).invoke(target, arg);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private org.springframework.test.web.servlet.ResultMatcher endedAtAbsentOrNull() {
    return result -> {
      String content = result.getResponse().getContentAsString();
      if (content.contains("\"endedAt\"")) {
        jsonPath("$.endedAt", nullValue()).match(result);
      }
    };
  }
}