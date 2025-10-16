package com.capstone.domain.canvas;

import com.capstone.global.oauth.JwtProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CanvasController {

  private final CanvasService canvasService;
  private final JwtProvider jwtProvider;

  @PostMapping("/{workspaceId}/canvas")
  public ResponseEntity<CanvasResponse> createCanvas(
      @RequestHeader("Authorization") String token,
      @PathVariable Long workspaceId,
      @RequestBody CanvasRequest request
  ) {
    String jwt = token.replace("Bearer ", "").trim();
    Long userId = jwtProvider.getUserIdFromAccessToken(jwt);
    return ResponseEntity.ok(canvasService.createCanvas(userId, workspaceId, request));
  }

  @GetMapping("/{workspaceId}/canvas")
  public ResponseEntity<List<CanvasResponse>> getAllCanvas(@PathVariable Long workspaceId) {
    return ResponseEntity.ok(canvasService.getAllCanvas(workspaceId));
  }

  @GetMapping("/canvas/{canvasId}")
  public ResponseEntity<CanvasResponse> getCanvas(@PathVariable Long canvasId) {
    return ResponseEntity.ok(canvasService.getCanvas(canvasId));
  }

  @PutMapping("/canvas/{canvasId}")
  public ResponseEntity<CanvasResponse> updateCanvas(
      @RequestHeader("Authorization") String token,
      @PathVariable Long canvasId,
      @RequestBody CanvasRequest request
  ) {
    String jwt = token.replace("Bearer ", "").trim();
    Long userId = jwtProvider.getUserIdFromAccessToken(jwt);
    return ResponseEntity.ok(canvasService.updateCanvas(userId, canvasId, request));
  }

  @DeleteMapping("/canvas/{canvasId}")
  public ResponseEntity<String> deleteCanvas(
      @RequestHeader("Authorization") String token,
      @PathVariable Long canvasId
  ) {
    String jwt = token.replace("Bearer ", "").trim();
    Long userId = jwtProvider.getUserIdFromAccessToken(jwt);
    canvasService.deleteCanvas(userId, canvasId);
    return ResponseEntity.ok("캔버스가 삭제되었습니다.");
  }
}
