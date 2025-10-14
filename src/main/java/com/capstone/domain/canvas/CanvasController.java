package com.capstone.domain.canvas;

import com.capstone.global.oauth.JwtProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/canvas")
public class CanvasController {

  private final CanvasService canvasService;
  private final JwtProvider jwtProvider;

  @PostMapping
  public ResponseEntity<CanvasResponse> createCanvas(
      @RequestHeader("Authorization") String token,
      @RequestBody CanvasRequest request
  ) {
    Long userId = jwtProvider.getUserIdFromAccessToken(token);
    return ResponseEntity.ok(canvasService.createCanvas(userId, request));
  }

  @GetMapping("/{workspaceId}")
  public ResponseEntity<List<CanvasResponse>> getAllCanvas(@PathVariable Long workspaceId) {
    return ResponseEntity.ok(canvasService.getAllCanvas(workspaceId));
  }

  @GetMapping("/detail/{id}")
  public ResponseEntity<CanvasResponse> getCanvas(@PathVariable Long id) {
    return ResponseEntity.ok(canvasService.getCanvas(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCanvas(
      @RequestHeader("Authorization") String token,
      @PathVariable Long id
  ) {
    Long userId = jwtProvider.getUserIdFromAccessToken(token);
    canvasService.deleteCanvas(userId, id);
    return ResponseEntity.noContent().build();
  }
}
