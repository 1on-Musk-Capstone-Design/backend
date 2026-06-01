package com.capstone.domain.voicesessionUser;

import static com.capstone.global.exception.ErrorCode.ALREADY_JOINED_SESSION;
import static com.capstone.global.exception.ErrorCode.FORBIDDEN_CLOSED_SESSION;
import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_ACCESS;
import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_SESSION;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_SESSION;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_SESSION_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE_USER;

import com.capstone.domain.user.User;
import com.capstone.domain.user.UserRepository;
import com.capstone.domain.voicesession.VoiceSession;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoiceSessionUserService {

  private final VoiceSessionUserRepository voiceSessionUserRepository;
  private final VoiceSessionRepository sessionRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final UserRepository userRepository;

  /**
   * 세션 참여
   */
  @Transactional
  public VoiceSessionUser joinSession(Long workspaceId, Long sessionId, Long userId) {
    // 1. 세션 조회
    VoiceSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_SESSION));

    // 2. 세션이 해당 워크스페이스에 속하는지 확인
    if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE_SESSION);
    }

    // 3. JWT의 userId 기준으로 실제 워크스페이스 사용자를 해석
    WorkspaceUser workspaceUser = resolveWorkspaceUser(session.getWorkspace(), userId);

    // 4. 세션 종료 여부 확인
    if (session.getEndedAt() != null) {
      throw new CustomException(FORBIDDEN_CLOSED_SESSION);
    }

    // 5. 이미 참여 중인지 확인
    List<VoiceSessionUser> existing = voiceSessionUserRepository
      .findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUser.getId());

    if (!existing.isEmpty()) {
      throw new CustomException(ALREADY_JOINED_SESSION);
    }

    // 6. 참여자 생성
    VoiceSessionUser voiceSessionUser = VoiceSessionUser.builder()
        .session(session)
        .workspaceUser(workspaceUser)
        .build();

    return voiceSessionUserRepository.save(voiceSessionUser);
  }

  /**
   * 세션 퇴장
   */
  @Transactional
  public VoiceSessionUser leaveSession(Long workspaceId, Long sessionId, Long userId) {
    VoiceSession session = sessionRepository.findById(sessionId)
      .orElseThrow(() -> new CustomException(NOT_FOUND_SESSION));

    WorkspaceUser workspaceUser = resolveWorkspaceUser(session.getWorkspace(), userId);

    List<VoiceSessionUser> activeUsers = voiceSessionUserRepository
      .findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUser.getId())
        ;

    if (activeUsers.isEmpty()) {
      throw new CustomException(NOT_FOUND_SESSION_USER);
    }

    VoiceSessionUser voiceSessionUser = activeUsers.get(0);

    // 워크스페이스 일치 확인
    if (!voiceSessionUser.getWorkspaceUser().getWorkspace().getWorkspaceId().equals(workspaceId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE_SESSION);
    }

    // 동일한 사용자/세션의 활성 중복 레코드가 있으면 함께 종료하여 다음 조회를 안전하게 만든다.
    List<VoiceSessionUser> toClose = new ArrayList<>();
    for (VoiceSessionUser candidate : activeUsers) {
      if (candidate.isActive()) {
        candidate.leave();
        toClose.add(candidate);
      }
    }

    voiceSessionUserRepository.saveAll(toClose);
    return voiceSessionUser;
  }

  /**
   * 채널 이동
   */
  @Transactional
  public VoiceSessionUser moveToSession(Long workspaceId, Long fromSessionId, Long toSessionId,
      Long userId) {
    try {
      leaveSession(workspaceId, fromSessionId, userId);
    } catch (IllegalArgumentException e) {
      // 무시
    }
    return joinSession(workspaceId, toSessionId, userId);
  }

  private WorkspaceUser resolveWorkspaceUser(com.capstone.domain.workspace.Workspace workspace, Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    return workspaceUserRepository.findByWorkspaceAndUser(workspace, user)
        .orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE_ACCESS));
  }

  /**
   * 현재 참여자 조회
   */
  public List<VoiceSessionUser> getActiveUsers(Long workspaceId, Long sessionId) {
    VoiceSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_SESSION));

    if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE_SESSION);
    }

    return voiceSessionUserRepository.findBySessionIdAndLeftAtIsNull(sessionId);
  }

  /**
   * 전체 참여자 조회
   */
  public List<VoiceSessionUser> getAllUsers(Long workspaceId, Long sessionId) {
    VoiceSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_SESSION));

    if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE_SESSION);
    }

    return voiceSessionUserRepository.findBySessionId(sessionId);
  }

  /**
   * 현재 참여자 수 조회
   */
  public long getActiveUserCount(Long workspaceId, Long sessionId) {
    VoiceSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_SESSION));

    if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE_SESSION);
    }

    return voiceSessionUserRepository.countBySessionIdAndLeftAtIsNull(sessionId);
  }
}
