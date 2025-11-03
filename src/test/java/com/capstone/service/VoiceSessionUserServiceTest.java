package com.capstone.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.voicesessionUser.VoiceSessionUser;
import com.capstone.domain.voicesessionUser.VoiceSessionUserRepository;
import com.capstone.domain.voicesessionUser.VoiceSessionUserService;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VoiceSessionUserService 단위 테스트")
class VoiceSessionUserServiceTest {

    @Mock
    private VoiceSessionUserRepository voiceSessionUserRepository;
    @Mock
    private VoiceSessionRepository sessionRepository;
    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private VoiceSessionUserService service;

    @Test
    @DisplayName("세션 참여 성공")
    void joinSession_success() {
        // Given
        Long workspaceId = 1L, sessionId = 10L, workspaceUserId = 100L;
        VoiceSession session = createSession(workspaceId, sessionId);
        WorkspaceUser workspaceUser = createWorkspaceUser(workspaceUserId, workspaceId, "홍길동");
        VoiceSessionUser expected = createVoiceSessionUser(session, workspaceUser, 1L);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(workspaceUserRepository.findById(workspaceUserId)).thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.existsByWorkspaceAndUser(workspaceUser.getWorkspace(), workspaceUser.getUser())).thenReturn(true);
        when(voiceSessionUserRepository.findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUserId)).thenReturn(Optional.empty());
        when(voiceSessionUserRepository.save(any(VoiceSessionUser.class))).thenReturn(expected);

        // When
        VoiceSessionUser result = service.joinSession(workspaceId, sessionId, workspaceUserId);

        // Then
        assertThat(result.getSession().getId()).isEqualTo(sessionId);
        assertThat(result.getWorkspaceUser().getId()).isEqualTo(workspaceUserId);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("이미 참여 중인 경우 예외")
    void joinSession_alreadyJoined_throwsException() {
        // Given
        Long workspaceId = 1L, sessionId = 10L, workspaceUserId = 100L;
        VoiceSession session = createSession(workspaceId, sessionId);
        WorkspaceUser workspaceUser = createWorkspaceUser(workspaceUserId, workspaceId, "홍길동");
        VoiceSessionUser existing = createVoiceSessionUser(session, workspaceUser, 1L);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(workspaceUserRepository.findById(workspaceUserId)).thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.existsByWorkspaceAndUser(workspaceUser.getWorkspace(), workspaceUser.getUser())).thenReturn(true);
        when(voiceSessionUserRepository.findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUserId)).thenReturn(Optional.of(existing));

        // When & Then
        assertThatThrownBy(() -> service.joinSession(workspaceId, sessionId, workspaceUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 이 세션에 참여 중입니다.");
    }

    @Test
    @DisplayName("세션 이동 성공")
    void moveToSession_success() {
        // Given
        Long workspaceId = 1L, fromSessionId = 10L, toSessionId = 20L, workspaceUserId = 100L;
        VoiceSession fromSession = createSession(workspaceId, fromSessionId);
        VoiceSession toSession = createSession(workspaceId, toSessionId);
        WorkspaceUser workspaceUser = createWorkspaceUser(workspaceUserId, workspaceId, "홍길동");
        VoiceSessionUser existing = createVoiceSessionUser(fromSession, workspaceUser, 1L);

        VoiceSessionUser moved = VoiceSessionUser.builder()
                .session(toSession)
                .workspaceUser(workspaceUser)
                .joinedAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findById(fromSessionId)).thenReturn(Optional.of(fromSession));
        when(sessionRepository.findById(toSessionId)).thenReturn(Optional.of(toSession));
        when(workspaceUserRepository.findById(workspaceUserId)).thenReturn(Optional.of(workspaceUser));
        when(workspaceUserRepository.existsByWorkspaceAndUser(any(Workspace.class), any(User.class))).thenReturn(true);
        when(voiceSessionUserRepository.findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(fromSessionId, workspaceUserId)).thenReturn(Optional.of(existing));
        when(voiceSessionUserRepository.findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(toSessionId, workspaceUserId)).thenReturn(Optional.empty());
        when(voiceSessionUserRepository.save(any(VoiceSessionUser.class))).thenReturn(moved);

        // When
        VoiceSessionUser result = service.moveToSession(workspaceId, fromSessionId, toSessionId, workspaceUserId);

        // Then
        assertThat(result.getSession().getId()).isEqualTo(toSessionId);
        assertThat(result.getWorkspaceUser().getId()).isEqualTo(workspaceUserId);
    }

    @Test
    @DisplayName("활성 참여자 조회")
    void getActiveUsers_success() {
        // Given
        Long workspaceId = 1L, sessionId = 10L;
        WorkspaceUser wsUser1 = createWorkspaceUser(100L, workspaceId, "홍길동");
        WorkspaceUser wsUser2 = createWorkspaceUser(101L, workspaceId, "김철수");
        VoiceSession session = createSession(workspaceId, sessionId);
        List<VoiceSessionUser> activeUsers = Arrays.asList(
                createVoiceSessionUser(session, wsUser1, 1L),
                createVoiceSessionUser(session, wsUser2, 2L)
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(voiceSessionUserRepository.findBySessionIdAndLeftAtIsNull(sessionId)).thenReturn(activeUsers);

        // When
        List<VoiceSessionUser> result = service.getActiveUsers(workspaceId, sessionId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("활성 참여자 수 조회")
    void getActiveUserCount_success() {
        // Given
        Long workspaceId = 1L, sessionId = 10L;

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(createSession(workspaceId, sessionId)));
        when(voiceSessionUserRepository.countBySessionIdAndLeftAtIsNull(sessionId)).thenReturn(5L);

        // When
        long result = service.getActiveUserCount(workspaceId, sessionId);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    // 헬퍼 메서드
    private VoiceSession createSession(Long workspaceId, Long sessionId) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);

        VoiceSession session = new VoiceSession();
        session.setId(sessionId);
        session.setWorkspace(workspace);
        session.setStartedAt(LocalDateTime.now());
        return session;
    }

    private WorkspaceUser createWorkspaceUser(Long workspaceUserId, Long workspaceId, String userName) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(workspaceId);

        User user = User.builder()
                .id(workspaceUserId)
                .name(userName)
                .build();

        WorkspaceUser workspaceUser = WorkspaceUser.builder()
                .id(workspaceUserId)
                .workspace(workspace)
                .user(user)
                .build();

        return workspaceUser;
    }

    private VoiceSessionUser createVoiceSessionUser(VoiceSession session, WorkspaceUser workspaceUser, Long voiceSessionUserId) {
        return VoiceSessionUser.builder()
                .id(voiceSessionUserId)
                .session(session)
                .workspaceUser(workspaceUser)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}