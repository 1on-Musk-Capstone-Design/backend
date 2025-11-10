package com.capstone.integration;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.voicesessionUser.VoiceSessionUser;
import com.capstone.domain.voicesessionUser.VoiceSessionUserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.type.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("VoiceSessionUser 통합 테스트")
class VoiceSessionUserIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private VoiceSessionUserRepository voiceSessionUserRepository;

  @Autowired
  private VoiceSessionRepository voiceSessionRepository;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Autowired
  private WorkspaceUserRepository workspaceUserRepository;

  // ----------- Helper Methods -----------

  private Workspace createWorkspace(String name) {
    Workspace workspace = new Workspace();
    workspace.setName(name);
    workspace.setCreatedAt(java.time.Instant.now());
    entityManager.persistAndFlush(workspace);
    return workspace;
  }

  private User createUser(String userName) {
    User user = User.builder()
        .name(userName)
        .build();
    entityManager.persistAndFlush(user);
    return user;
  }

  private WorkspaceUser createWorkspaceUser(String userName, Workspace workspace) {
    User user = createUser(userName);
    WorkspaceUser workspaceUser = WorkspaceUser.builder()
        .workspace(workspace)
        .user(user)
        .build();
    entityManager.persistAndFlush(workspaceUser);
    return workspaceUser;
  }

  private VoiceSession createSession(Workspace workspace) {
    VoiceSession session = new VoiceSession();
    session.setWorkspace(workspace);
    session.setStartedAt(LocalDateTime.now());
    entityManager.persistAndFlush(session);
    return session;
  }

  private VoiceSessionUser joinSession(VoiceSession session, WorkspaceUser workspaceUser) {
    VoiceSessionUser vsUser = VoiceSessionUser.builder()
        .session(session)
        .workspaceUser(workspaceUser)
        .joinedAt(LocalDateTime.now())
        .build();
    entityManager.persistAndFlush(vsUser);
    return vsUser;
  }

  // ----------- Tests -----------

  @Test
  @DisplayName("세션 참여 테스트")
  void joinSession_shouldPersistAndRetrieve() {
    Workspace workspace = createWorkspace("테스트 워크스페이스");
    WorkspaceUser workspaceUser = createWorkspaceUser("홍길동", workspace);
    VoiceSession session = createSession(workspace);

    VoiceSessionUser vsUser = joinSession(session, workspaceUser);

    VoiceSessionUser found = voiceSessionUserRepository.findById(vsUser.getId()).orElseThrow();
    assertThat(found.getWorkspaceUser().getId()).isEqualTo(workspaceUser.getId());
    assertThat(found.getSession().getId()).isEqualTo(session.getId());
    assertThat(found.getJoinedAt()).isNotNull();
    assertThat(found.getLeftAt()).isNull();
  }

  @Test
  @DisplayName("세션 퇴장 테스트")
  void leaveSession_shouldUpdateLeftAt() {
    Workspace workspace = createWorkspace("워크스페이스 A");
    WorkspaceUser workspaceUser = createWorkspaceUser("김철수", workspace);
    VoiceSession session = createSession(workspace);
    VoiceSessionUser vsUser = joinSession(session, workspaceUser);

    // 세션 퇴장 처리
    vsUser.leave();
    entityManager.persistAndFlush(vsUser);

    VoiceSessionUser updated = voiceSessionUserRepository.findById(vsUser.getId()).orElseThrow();
    assertThat(updated.getLeftAt()).isNotNull();
  }

  @Test
  @DisplayName("세션 내 활성 참여자 조회 테스트")
  void findActiveUsers_shouldReturnOnlyActive() {
    Workspace workspace = createWorkspace("워크스페이스 B");
    VoiceSession session = createSession(workspace);

    WorkspaceUser user1 = createWorkspaceUser("유저1", workspace);
    WorkspaceUser user2 = createWorkspaceUser("유저2", workspace);

    joinSession(session, user1);
    VoiceSessionUser vsUser2 = joinSession(session, user2);
    vsUser2.leave();
    entityManager.persistAndFlush(vsUser2);

    List<VoiceSessionUser> activeUsers = voiceSessionUserRepository.findBySessionIdAndLeftAtIsNull(
        session.getId());
    assertThat(activeUsers).hasSize(1);
    assertThat(activeUsers.get(0).getWorkspaceUser().getId()).isEqualTo(user1.getId());
  }

  @Test
  @DisplayName("채널 이동 (세션 이동) 테스트")
  void moveSession_shouldUpdateLeftAtAndCreateNewSessionUser() {
    Workspace workspace = createWorkspace("워크스페이스 이동 테스트");
    WorkspaceUser workspaceUser = createWorkspaceUser("이동 유저", workspace);
    VoiceSession oldSession = createSession(workspace);
    VoiceSession newSession = createSession(workspace);

    VoiceSessionUser oldVsUser = joinSession(oldSession, workspaceUser);

    // 이전 세션 퇴장 처리
    oldVsUser.leave();
    entityManager.persistAndFlush(oldVsUser);

    VoiceSessionUser newVsUser = joinSession(newSession, workspaceUser);

    List<VoiceSessionUser> oldSessionActiveUsers = voiceSessionUserRepository.findBySessionIdAndLeftAtIsNull(
        oldSession.getId());
    List<VoiceSessionUser> newSessionActiveUsers = voiceSessionUserRepository.findBySessionIdAndLeftAtIsNull(
        newSession.getId());

    assertThat(oldSessionActiveUsers).isEmpty();
    assertThat(newSessionActiveUsers).hasSize(1);
    assertThat(newSessionActiveUsers.get(0).getWorkspaceUser().getId()).isEqualTo(
        workspaceUser.getId());
  }
}