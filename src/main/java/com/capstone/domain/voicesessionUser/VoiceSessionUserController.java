package com.capstone.domain.voicesessionUser;

import com.capstone.domain.workspaceUser.WorkspaceUser;
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
public class VoiceSessionUserController {

  private final VoiceSessionUserService service;

  @PostMapping
  public ResponseEntity<VoiceSessionUserResponse> joinSession(
      @PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @Valid @RequestBody VoiceSessionUserRequest request
  ) {
    if (request.getWorkspaceUserId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다.");
    }
    VoiceSessionUser user = service.joinSession(workspaceId, sessionId,
        request.getWorkspaceUserId());
    return ResponseEntity.status(HttpStatus.CREATED).body(VoiceSessionUserResponse.from(user));
  }

  @DeleteMapping("/{workspaceUserId}")
  public ResponseEntity<VoiceSessionUserResponse> leaveSession(
      @PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @PathVariable Long workspaceUserId
  ) {
    VoiceSessionUser user = service.leaveSession(workspaceId, sessionId, workspaceUserId);
    return ResponseEntity.ok(VoiceSessionUserResponse.from(user));
  }

  @PostMapping("/move")
  public ResponseEntity<VoiceSessionUserResponse> moveToSession(
      @PathVariable Long workspaceId,
      @RequestParam Long fromSessionId,
      @RequestParam Long toSessionId,
      @Valid @RequestBody VoiceSessionUserRequest request
  ) {
    if (request.getWorkspaceUserId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다.");
    }
    VoiceSessionUser user = service.moveToSession(workspaceId, fromSessionId, toSessionId,
        request.getWorkspaceUserId());
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
}