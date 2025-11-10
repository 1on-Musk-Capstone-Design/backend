package com.capstone.domain.voicesession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/v1/workspaces/{workspaceId}/voice")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class VoiceSessionController {

  private final VoiceSessionService service;

  public VoiceSessionController(VoiceSessionService service) {
    this.service = service;
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
}