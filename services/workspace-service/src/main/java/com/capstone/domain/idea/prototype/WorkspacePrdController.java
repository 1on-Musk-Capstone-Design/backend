package com.capstone.domain.idea.prototype;

import static com.capstone.global.exception.ErrorCode.INVALID_TOKEN;
import static com.capstone.global.exception.ErrorCode.UNAUTHORIZED_USER;

import com.capstone.domain.idea.prototype.dto.PrototypePipelineResponse;
import com.capstone.domain.idea.prototype.dto.PrototypeSourceFileResponse;
import com.capstone.global.exception.CustomException;
import com.capstone.global.oauth.JwtProvider;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace PRD", description = "워크스페이스별 공유 PRD 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspaces")
public class WorkspacePrdController {

  private final IdeaPrototypeService ideaPrototypeService;
  private final JwtProvider jwtProvider;

  @Operation(summary = "워크스페이스 PRD 단건 조회", description = "prdId는 prototype jobId와 동일합니다.")
  @GetMapping("/{workspaceId}/prds/{prdId}")
  public ResponseEntity<PrototypePipelineResponse> getWorkspacePrd(
      @RequestHeader("Authorization") String token,
      @PathVariable Long workspaceId,
      @PathVariable Long prdId) {
    Long userId = extractUserId(token);
    return ResponseEntity.ok(ideaPrototypeService.getWorkspacePrd(userId, workspaceId, prdId));
  }

  @Operation(summary = "워크스페이스 PRD 소스 파일 조회")
  @GetMapping("/{workspaceId}/prds/{prdId}/source-files")
  public ResponseEntity<List<PrototypeSourceFileResponse>> getWorkspacePrdSourceFiles(
      @RequestHeader("Authorization") String token,
      @PathVariable Long workspaceId,
      @PathVariable Long prdId) {
    Long userId = extractUserId(token);
    return ResponseEntity.ok(
        ideaPrototypeService.listWorkspacePrdSourceFiles(userId, workspaceId, prdId));
  }

  @Operation(summary = "워크스페이스 PRD 실행 프리뷰 HTML")
  @GetMapping(
      value = "/{workspaceId}/prds/{prdId}/preview",
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> getWorkspacePrdPreview(
      @RequestHeader("Authorization") String token,
      @PathVariable Long workspaceId,
      @PathVariable Long prdId) {
    Long userId = extractUserId(token);
    return ResponseEntity.ok(ideaPrototypeService.getWorkspacePrdPreviewHtml(userId, workspaceId, prdId));
  }

  private Long extractUserId(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new CustomException(UNAUTHORIZED_USER);
    }
    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new CustomException(INVALID_TOKEN);
    }
    String token = authorizationHeader.substring("Bearer ".length()).trim();
    if (token.isBlank()) {
      throw new CustomException(INVALID_TOKEN);
    }
    try {
      return jwtProvider.getUserIdFromAccessToken(token);
    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException(INVALID_TOKEN);
    }
  }
}
