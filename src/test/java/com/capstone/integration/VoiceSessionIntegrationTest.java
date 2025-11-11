package com.capstone.integration;

import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("VoiceSession 통합 테스트")
class VoiceSessionIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private VoiceSessionRepository voiceSessionRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Test
  @DisplayName("세션 저장 및 조회 테스트")
  void saveVoiceSession_shouldPersistAndRetrieve() {
    // Given
    Workspace workspace = new Workspace();
    workspace.setName("테스트 워크스페이스");
    workspace.setCreatedAt(Instant.now());
    entityManager.persistAndFlush(workspace);

    VoiceSession session = new VoiceSession(workspace, LocalDateTime.now());

    // When
    VoiceSession savedSession = voiceSessionRepository.save(session);
    entityManager.flush();
    entityManager.clear();

    // Then
    VoiceSession found = voiceSessionRepository.findById(savedSession.getId()).orElseThrow();
    assertThat(found.getId()).isNotNull();
    assertThat(found.getWorkspace().getWorkspaceId()).isEqualTo(workspace.getWorkspaceId());
    assertThat(found.getStartedAt()).isNotNull();
    assertThat(found.getEndedAt()).isNull();
  }

  @Test
  @DisplayName("워크스페이스별 세션 조회 테스트")
  void findByWorkspaceId_shouldReturnSessionsInOrder() {
    // Given
    Workspace workspace = new Workspace();
    workspace.setName("테스트 워크스페이스");
    workspace.setCreatedAt(Instant.now());
    entityManager.persistAndFlush(workspace);

    VoiceSession session1 = new VoiceSession(workspace, LocalDateTime.now().minusHours(2));
    VoiceSession session2 = new VoiceSession(workspace, LocalDateTime.now().minusHours(1));
    VoiceSession session3 = new VoiceSession(workspace, LocalDateTime.now());

    entityManager.persistAndFlush(session1);
    entityManager.persistAndFlush(session2);
    entityManager.persistAndFlush(session3);
    entityManager.clear();

    // When
    List<VoiceSession> result = voiceSessionRepository.findByWorkspace_WorkspaceId(
        workspace.getWorkspaceId());

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getStartedAt()).isBefore(result.get(1).getStartedAt());
    assertThat(result.get(1).getStartedAt()).isBefore(result.get(2).getStartedAt());
  }

  @Test
  @DisplayName("세션 종료 테스트")
  void endSession_shouldUpdateEndedAt() {
    // Given
    Workspace workspace = new Workspace();
    workspace.setName("테스트 워크스페이스");
    workspace.setCreatedAt(Instant.now());
    entityManager.persistAndFlush(workspace);

    VoiceSession session = new VoiceSession(workspace, LocalDateTime.now().minusMinutes(10));
    entityManager.persistAndFlush(session);
    Long sessionId = session.getId();
    entityManager.clear();

    // When
    VoiceSession found = voiceSessionRepository.findById(sessionId).orElseThrow();
    found.setEndedAt(LocalDateTime.now());
    voiceSessionRepository.save(found);
    entityManager.flush();
    entityManager.clear();

    // Then
    VoiceSession updated = voiceSessionRepository.findById(sessionId).orElseThrow();
    assertThat(updated.getEndedAt()).isNotNull();
    assertThat(updated.getEndedAt()).isAfter(updated.getStartedAt());
  }

  @Test
  @DisplayName("특정 워크스페이스 세션 조회")
  void findByWorkspaceId_shouldNotReturnOtherWorkspaceSessions() {
    // Given - 두 개의 워크스페이스와 세션 생성
    Workspace workspace1 = new Workspace();
    workspace1.setName("워크스페이스 1");
    workspace1.setCreatedAt(Instant.now());
    entityManager.persistAndFlush(workspace1);

    Workspace workspace2 = new Workspace();
    workspace2.setName("워크스페이스 2");
    workspace2.setCreatedAt(Instant.now());
    entityManager.persistAndFlush(workspace2);

    VoiceSession session1 = new VoiceSession(workspace1, LocalDateTime.now());
    VoiceSession session2 = new VoiceSession(workspace2, LocalDateTime.now());

    entityManager.persistAndFlush(session1);
    entityManager.persistAndFlush(session2);
    entityManager.clear();

    // When - workspace1의 세션만 조회
    List<VoiceSession> result = voiceSessionRepository.findByWorkspace_WorkspaceId(
        workspace1.getWorkspaceId());

    // Then - workspace1의 세션만 반환
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getWorkspace().getWorkspaceId()).isEqualTo(
        workspace1.getWorkspaceId());
  }
}