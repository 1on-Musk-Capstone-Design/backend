package com.capstone.global.service;

import com.capstone.domain.user.User;
import com.capstone.domain.user.UserRepository;
import com.capstone.domain.voicesessionUser.VoiceSessionUserRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.dto.WebRtcSignalMessage;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebRtcSignalAuthorizationService {

  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final VoiceSessionUserRepository voiceSessionUserRepository;

  /**
   * 운영 전 soft validation 용도.
   * 현재는 차단하지 않고, principal / workspace / session / fromWorkspaceUserId 정합성만 점검해 로그를 남긴다.
   */
  public void validateAndLog(Principal principal, WebRtcSignalMessage message) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      log.warn(
          "WebRTC signal soft validation skipped: missing principal, workspaceId={}, sessionId={}, fromWorkspaceUserId={}",
          message.getWorkspaceId(),
          message.getSessionId(),
          message.getFromWorkspaceUserId()
      );
      return;
    }

    Optional<User> userOptional = userRepository.findByEmail(principal.getName());
    if (userOptional.isEmpty()) {
      log.warn(
          "WebRTC signal soft validation failed: user not found for principal={}, workspaceId={}, sessionId={}, fromWorkspaceUserId={}",
          principal.getName(),
          message.getWorkspaceId(),
          message.getSessionId(),
          message.getFromWorkspaceUserId()
      );
      return;
    }

    User user = userOptional.get();
    List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findByUser(user);

    Optional<WorkspaceUser> matchingWorkspaceUser = workspaceUsers.stream()
        .filter(workspaceUser -> workspaceUser.getId().equals(message.getFromWorkspaceUserId()))
        .findFirst();

    if (matchingWorkspaceUser.isEmpty()) {
      log.warn(
          "WebRTC signal soft validation failed: principal user does not own fromWorkspaceUserId, principal={}, userId={}, workspaceId={}, sessionId={}, fromWorkspaceUserId={}",
          principal.getName(),
          user.getId(),
          message.getWorkspaceId(),
          message.getSessionId(),
          message.getFromWorkspaceUserId()
      );
      return;
    }

    WorkspaceUser workspaceUser = matchingWorkspaceUser.get();
    if (!workspaceUser.getWorkspace().getWorkspaceId().equals(message.getWorkspaceId())) {
      log.warn(
          "WebRTC signal soft validation failed: workspace mismatch, principal={}, userId={}, actualWorkspaceId={}, messageWorkspaceId={}, sessionId={}, fromWorkspaceUserId={}",
          principal.getName(),
          user.getId(),
          workspaceUser.getWorkspace().getWorkspaceId(),
          message.getWorkspaceId(),
          message.getSessionId(),
          message.getFromWorkspaceUserId()
      );
      return;
    }

    boolean activeInSession = !voiceSessionUserRepository
        .findBySessionIdAndWorkspaceUserIdAndLeftAtIsNull(message.getSessionId(), workspaceUser.getId())
        .isEmpty();

    if (!activeInSession) {
      log.warn(
          "WebRTC signal soft validation failed: sender not active in session, principal={}, userId={}, workspaceId={}, sessionId={}, fromWorkspaceUserId={}",
          principal.getName(),
          user.getId(),
          message.getWorkspaceId(),
          message.getSessionId(),
          message.getFromWorkspaceUserId()
      );
      return;
    }

    log.debug(
        "WebRTC signal soft validation passed: principal={}, userId={}, workspaceId={}, sessionId={}, fromWorkspaceUserId={}",
        principal.getName(),
        user.getId(),
        message.getWorkspaceId(),
        message.getSessionId(),
        message.getFromWorkspaceUserId()
    );
  }
}
