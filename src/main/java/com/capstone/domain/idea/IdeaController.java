package com.capstone.domain.idea;

import com.capstone.global.oauth.JwtProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ideas")
public class IdeaController {

  private final IdeaService ideaService;
  private final JwtProvider jwtProvider;

  @PostMapping
  public ResponseEntity<IdeaResponse> createIdea(
      @RequestHeader("Authorization") String token,
      @RequestBody IdeaRequest request) {

    Long userId = jwtProvider.getUserIdFromAccessToken(token.replace("Bearer ", "").trim());
    return ResponseEntity.ok(ideaService.createIdea(userId, request));
  }

  @GetMapping("/workspace/{workspaceId}")
  public ResponseEntity<List<IdeaResponse>> getAllIdeas(@PathVariable Long workspaceId) {
    return ResponseEntity.ok(ideaService.getAllIdeas(workspaceId));
  }

  @GetMapping("/{ideaId}")
  public ResponseEntity<IdeaResponse> getIdea(@PathVariable Long ideaId) {
    return ResponseEntity.ok(ideaService.getIdea(ideaId));
  }

  @PutMapping("/{ideaId}")
  public ResponseEntity<IdeaResponse> updateIdea(
      @RequestHeader("Authorization") String token,
      @PathVariable Long ideaId,
      @RequestBody IdeaRequest request) {

    Long userId = jwtProvider.getUserIdFromAccessToken(token.replace("Bearer ", "").trim());
    return ResponseEntity.ok(ideaService.updateIdea(userId, ideaId, request));
  }

  @DeleteMapping("/{ideaId}")
  public ResponseEntity<String> deleteIdea(
      @RequestHeader("Authorization") String token,
      @PathVariable Long ideaId) {

    Long userId = jwtProvider.getUserIdFromAccessToken(token.replace("Bearer ", "").trim());
    ideaService.deleteIdea(userId, ideaId);
    return ResponseEntity.ok("아이디어가 삭제되었습니다.");
  }
}
