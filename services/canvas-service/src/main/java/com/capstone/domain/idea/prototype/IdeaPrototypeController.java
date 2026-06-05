package com.capstone.domain.idea.prototype;

import static com.capstone.global.exception.ErrorCode.INVALID_TOKEN;
import static com.capstone.global.exception.ErrorCode.UNAUTHORIZED_USER;

import com.capstone.domain.idea.prototype.dto.PrototypeJobAcceptedResponse;
import com.capstone.domain.idea.prototype.dto.PrototypeJobSummaryResponse;
import com.capstone.domain.idea.prototype.dto.PrototypePipelineResponse;
import com.capstone.domain.idea.prototype.dto.PrototypeSourceFileResponse;
import com.capstone.global.exception.CustomException;
import com.capstone.global.oauth.JwtProvider;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Idea Prototype", description = "아이디어 → PRD → UI 구조 → React 템플릿 → GitHub/Vercel 파이프라인")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/ideas")
public class IdeaPrototypeController {

  private final IdeaPrototypeService ideaPrototypeService;
  private final JwtProvider jwtProvider;

  @Operation(
      summary = "프로토타입 파이프라인 실행",
      description =
          "sync=false(기본): 202 Accepted 후 백그라운드 실행. sync=true: 동기 완료 시 200.")
  @PostMapping("/{ideaId}/prototype/pipeline")
  public ResponseEntity<?> runPipeline(
      @RequestHeader("Authorization") String token,
      @PathVariable Long ideaId,
      @Parameter(description = "true면 HTTP 응답이 끝날 때까지 파이프라인 완료 대기")
          @RequestParam(defaultValue = "false")
          boolean sync) {
    Long userId = extractUserId(token);
    if (sync) {
      return ResponseEntity.ok(ideaPrototypeService.runPipelineSync(userId, ideaId));
    }
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ideaPrototypeService.startPipelineAsync(userId, ideaId));
  }

  @Operation(summary = "프로토타입 파이프라인 재시도", description = "새 작업을 큐에 넣고 비동기로 실행합니다.")
  @PostMapping("/{ideaId}/prototype/retry")
  public ResponseEntity<PrototypeJobAcceptedResponse> retry(
      @RequestHeader("Authorization") String token, @PathVariable Long ideaId) {
    Long userId = extractUserId(token);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ideaPrototypeService.retryPipelineAsync(userId, ideaId));
  }

  @Operation(summary = "최근 프로토타입 작업 조회")
  @GetMapping("/{ideaId}/prototype")
  public ResponseEntity<PrototypePipelineResponse> getLatest(
      @RequestHeader("Authorization") String token, @PathVariable Long ideaId) {
    Long userId = extractUserId(token);
    return ResponseEntity.ok(ideaPrototypeService.getLatest(userId, ideaId));
  }

  @Operation(summary = "프로토타입 작업 목록")
  @GetMapping("/{ideaId}/prototype/jobs")
  public ResponseEntity<List<PrototypeJobSummaryResponse>> listJobs(
      @RequestHeader("Authorization") String token, @PathVariable Long ideaId) {
    Long userId = extractUserId(token);
    return ResponseEntity.ok(ideaPrototypeService.listJobs(userId, ideaId));
  }

  @Operation(summary = "프로토타입 작업 단건 조회")
  @GetMapping("/{ideaId}/prototype/jobs/{jobId}")
  public ResponseEntity<PrototypePipelineResponse> getJob(
      @RequestHeader("Authorization") String token,
      @PathVariable Long ideaId,
      @PathVariable Long jobId) {
    Long userId = extractUserId(token);
    return ResponseEntity.ok(ideaPrototypeService.getJob(userId, ideaId, jobId));
  }

  @Operation(
      summary = "프로토타입 생성 소스 파일 목록 (AI 생성 코드, 서버 로컬 보관)",
      description = "같은 워크스페이스 권한이 있으면 조회 가능합니다. PRD/코드 뷰어에 사용됩니다.")
  @GetMapping("/{ideaId}/prototype/jobs/{jobId}/source-files")
  public ResponseEntity<List<PrototypeSourceFileResponse>> listSourceFiles(
      @RequestHeader("Authorization") String token,
      @PathVariable Long ideaId,
      @PathVariable Long jobId) {
    Long userId = extractUserId(token);
    return ResponseEntity.ok(ideaPrototypeService.listSourceFiles(userId, ideaId, jobId));
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
