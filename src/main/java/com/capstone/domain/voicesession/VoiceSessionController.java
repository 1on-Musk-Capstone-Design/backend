package com.capstone.domain.voicesession;

import com.capstone.global.service.SfuServerClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/workspaces/{workspaceId}/voice")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@Slf4j
public class VoiceSessionController {

  private final VoiceSessionService service;
  private final SfuServerClientService sfuServerClientService;

  public VoiceSessionController(VoiceSessionService service,
      SfuServerClientService sfuServerClientService) {
    this.service = service;
    this.sfuServerClientService = sfuServerClientService;
  }

  @PostMapping
  public ResponseEntity<VoiceSessionDto> startSession(@PathVariable Long workspaceId) {
    VoiceSession session = service.startSession(workspaceId);
    Long sessionId = session.getId(); // null 체크 가능
    if (sessionId == null) {
      throw new IllegalStateException("세션 ID가 생성되지 않았습니다.");
    }
    return ResponseEntity
        .created(URI.create("/v1/workspaces/" + workspaceId + "/voice/" + sessionId))
        .body(VoiceSessionDto.from(session));
  }

  @PatchMapping("/{sessionId}")
  public ResponseEntity<VoiceSessionDto> endSession(@PathVariable Long workspaceId,
      @PathVariable Long sessionId) {
    VoiceSession session = service.endSession(sessionId);
    if (session.getWorkspace() == null || !session.getWorkspace().getWorkspaceId()
        .equals(workspaceId)) {
      return ResponseEntity.badRequest().build();
    }
    closeSfuRoom(workspaceId, sessionId);
    return ResponseEntity.ok(VoiceSessionDto.from(session));
  }

  @GetMapping
  public ResponseEntity<List<VoiceSessionDto>> getSessions(@PathVariable Long workspaceId) {
    List<VoiceSessionDto> list = service.getAllSessions(workspaceId)
        .stream()
        .map(VoiceSessionDto::from)
        .toList();
    return ResponseEntity.ok(list);
  }

  private void closeSfuRoom(Long workspaceId, Long sessionId) {
    if (!sfuServerClientService.isEnabled()) {
      return;
    }

    try {
      sfuServerClientService.closeRoom(workspaceId, sessionId);
    } catch (Exception e) {
      log.warn(
          "SFU room cleanup failed: workspaceId={}, sessionId={}, error={}",
          workspaceId,
          sessionId,
          e.getMessage()
      );
    }
  }
}
