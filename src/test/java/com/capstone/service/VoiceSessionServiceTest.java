package com.capstone.service;

import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.voicesession.VoiceSessionService;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoiceSessionService 단위 테스트")
class VoiceSessionServiceTest {

  @Mock
  private VoiceSessionRepository voiceSessionRepository;

  @Mock
  private WorkspaceRepository workspaceRepository;

  @InjectMocks
  private VoiceSessionService voiceSessionService;

  @Nested
  @DisplayName("세션 생성")
  class StartSession {

    @Test
    @DisplayName("성공 - 워크스페이스가 존재하면 세션 생성 성공")
    void startSession_success() {
      // Given
      Long workspaceId = 1L;
      Workspace workspace = new Workspace();
      workspace.setWorkspaceId(workspaceId);

      VoiceSession session = new VoiceSession(workspace, LocalDateTime.now());

      when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
      when(voiceSessionRepository.save(any(VoiceSession.class))).thenReturn(session);

      // When
      VoiceSession result = voiceSessionService.startSession(workspaceId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getWorkspace()).isEqualTo(workspace);
      assertThat(result.getStartedAt()).isNotNull();
      assertThat(result.getEndedAt()).isNull();

      verify(workspaceRepository, times(1)).findById(workspaceId);
      verify(voiceSessionRepository, times(1)).save(any(VoiceSession.class));
    }

    @Test
    @DisplayName("실패 - 워크스페이스가 존재하지 않으면 예외 발생")
    void startSession_workspaceNotFound_throwsException() {
      // Given
      Long workspaceId = 999L;
      when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> voiceSessionService.startSession(workspaceId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("해당 워크스페이스를 찾을 수 없습니다: " + (workspaceId));

      verify(voiceSessionRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("세션 종료")
  class EndSession {

    @Test
    @DisplayName("성공 - 세션이 존재하면 종료 시간 설정")
    void endSession_success() {
      // Given
      Long sessionId = 10L;
      VoiceSession session = new VoiceSession(new Workspace(), LocalDateTime.now().minusMinutes(5));

      when(voiceSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
      when(voiceSessionRepository.save(any(VoiceSession.class))).thenReturn(session);

      // When
      VoiceSession result = voiceSessionService.endSession(sessionId);

      // Then
      assertThat(result.getEndedAt()).isNotNull();
      verify(voiceSessionRepository, times(1)).findById(sessionId);
      verify(voiceSessionRepository, times(1)).save(session);
    }

    @Test
    @DisplayName("실패 - 세션이 존재하지 않으면 예외 발생")
    void endSession_sessionNotFound_throwsException() {
      // Given
      Long sessionId = 999L;
      when(voiceSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> voiceSessionService.endSession(sessionId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("해당 음성 세션을 찾을 수 없습니다: " + (sessionId));
    }
  }

  @Nested
  @DisplayName("세션 조회")
  class GetSessions {

    @Test
    @DisplayName("성공 - 워크스페이스의 모든 세션 조회")
    void getAllSessions_success() {
      // Given
      Long workspaceId = 1L;
      Workspace workspace = new Workspace();
      workspace.setWorkspaceId(workspaceId);

      List<VoiceSession> sessions = Arrays.asList(
          new VoiceSession(workspace, LocalDateTime.now().minusHours(2)),
          new VoiceSession(workspace, LocalDateTime.now().minusHours(1))
      );

      when(voiceSessionRepository.findByWorkspace_WorkspaceId(workspaceId)).thenReturn(sessions);

      // When
      List<VoiceSession> result = voiceSessionService.getAllSessions(workspaceId);

      // Then
      assertThat(result).hasSize(2);
      assertThat(result).isEqualTo(sessions);
      verify(voiceSessionRepository, times(1)).findByWorkspace_WorkspaceId(workspaceId);
    }
  }
}