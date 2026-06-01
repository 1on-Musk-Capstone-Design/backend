package com.capstone.domain.voicesessionUser;

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
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    WorkspaceUser workspaceUser = resolveWorkspaceUser(workspaceId, userId);

    VoiceSession session = sessionRepository.findById(sessionId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_SESSION));

    if (!session.getWorkspace().getWorkspaceId().equals(workspaceId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE_SESSION);
    }

    if (session.getEndedAt() != null) {
      throw new CustomException(FORBIDDEN_CLOSED_SESSION);
    }

    Optional<VoiceSessionUser> existing = voiceSessionUserRepository
        .findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUser.getId());

    if (existing.isPresent()) {
      throw new CustomException(FORBIDDEN_CLOSED_SESSION);
    }

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
    WorkspaceUser workspaceUser = resolveWorkspaceUser(workspaceId, userId);

    VoiceSessionUser voiceSessionUser = voiceSessionUserRepository
        .findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(sessionId, workspaceUser.getId())
        .orElseThrow(() -> new CustomException(NOT_FOUND_SESSION_USER));

    if (!voiceSessionUser.getWorkspaceUser().getWorkspace().getWorkspaceId().equals(workspaceId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE_SESSION);
    }

    voiceSessionUser.leave();

    return voiceSessionUserRepository.save(voiceSessionUser);
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

  private WorkspaceUser resolveWorkspaceUser(Long workspaceId, Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    WorkspaceUser workspaceUser = workspaceUserRepository
        .findByWorkspace_WorkspaceIdAndUser_Id(workspaceId, userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE_USER));

    if (!workspaceUser.getWorkspace().getWorkspaceId().equals(workspaceId)
        || !workspaceUser.getUser().getId().equals(user.getId())) {
      throw new CustomException(FORBIDDEN_WORKSPACE_ACCESS);
    }

    return workspaceUser;
  }
}
