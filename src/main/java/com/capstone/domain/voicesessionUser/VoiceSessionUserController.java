package com.capstone.domain.voicesessionUser;

import com.capstone.global.oauth.JwtProvider;
import com.capstone.global.service.SfuServerClientService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/workspaces/{workspaceId}/voice/{sessionId}/users")
@RequiredArgsConstructor
@Slf4j
public class VoiceSessionUserController {

  private final VoiceSessionUserService service;
  private final SfuServerClientService sfuServerClientService;
  private final JwtProvider jwtProvider;

  @PostMapping
  public ResponseEntity<VoiceSessionUserResponse> joinSession(
      @PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @RequestHeader(value = "Authorization", required = false) String token,
      @Valid @RequestBody VoiceSessionUserRequest request
  ) {
    Long userId = resolveUserId(token, request.getWorkspaceUserId());
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다.");
    }
    VoiceSessionUser user = service.joinSession(workspaceId, sessionId, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(VoiceSessionUserResponse.from(user));
  }

  @DeleteMapping("/{workspaceUserId}")
  public ResponseEntity<VoiceSessionUserResponse> leaveSession(
      @PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @RequestHeader(value = "Authorization", required = false) String token,
      @PathVariable Long workspaceUserId
  ) {
    Long userId = resolveUserId(token, workspaceUserId);
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다.");
    }
    VoiceSessionUser user = service.leaveSession(workspaceId, sessionId, userId);
    closeSfuPeer(workspaceId, sessionId, user.getWorkspaceUser().getId());
    return ResponseEntity.ok(VoiceSessionUserResponse.from(user));
  }

  @PostMapping("/move")
  public ResponseEntity<VoiceSessionUserResponse> moveToSession(
      @PathVariable Long workspaceId,
      @RequestParam Long fromSessionId,
      @RequestParam Long toSessionId,
      @RequestHeader(value = "Authorization", required = false) String token,
      @Valid @RequestBody VoiceSessionUserRequest request
  ) {
    Long userId = resolveUserId(token, request.getWorkspaceUserId());
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다.");
    }
    VoiceSessionUser user = service.moveToSession(workspaceId, fromSessionId, toSessionId, userId);
    return ResponseEntity.ok(VoiceSessionUserResponse.from(user));
  }

  @GetMapping
  public ResponseEntity<List<VoiceSessionUserResponse>> getActiveUsers(
      @PathVariable Long workspaceId,
      @PathVariable Long sessionId
  ) {
    List<VoiceSessionUserResponse> dtos = service.getActiveUsers(workspaceId, sessionId).stream()
        .map(VoiceSessionUserResponse::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/all")
  public ResponseEntity<List<VoiceSessionUserResponse>> getAllUsers(
      @PathVariable Long workspaceId,
      @PathVariable Long sessionId
  ) {
    List<VoiceSessionUserResponse> dtos = service.getAllUsers(workspaceId, sessionId).stream()
        .map(VoiceSessionUserResponse::from)
        .collect(Collectors.toList());
    return ResponseEntity.ok(dtos);
  }

  @GetMapping("/count")
  public ResponseEntity<Long> countActiveUsers(
      @PathVariable Long workspaceId,
      @PathVariable Long sessionId
  ) {
    long count = service.getActiveUserCount(workspaceId, sessionId);
    return ResponseEntity.ok(count);
  }

  private void closeSfuPeer(Long workspaceId, Long sessionId, Long workspaceUserId) {
    if (!sfuServerClientService.isEnabled()) {
      return;
    }

    try {
      sfuServerClientService.closePeer(workspaceId, sessionId, String.valueOf(workspaceUserId));
    } catch (Exception e) {
      log.warn(
          "SFU peer cleanup failed: workspaceId={}, sessionId={}, workspaceUserId={}, error={}",
          workspaceId,
          sessionId,
          workspaceUserId,
          e.getMessage()
      );
    }
  }

  private Long resolveUserId(String token, Long fallbackId) {
    if (token != null && !token.trim().isEmpty()) {
      String jwt = token.replace("Bearer ", "").trim();
      return jwtProvider.getUserIdFromAccessToken(jwt);
    }
    return fallbackId;
  }
}
