package com.capstone.domain.workspace;

import com.capstone.domain.workspaceInvite.WorkspaceInviteService;
import com.capstone.global.exception.CustomException;
import com.capstone.global.oauth.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Workspace", description = "워크스페이스 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspaces")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@Slf4j
public class WorkspaceController {

  private final WorkspaceService workspaceService;
  private final WorkspaceInviteService workspaceInviteService;
  private final JwtProvider jwtProvider;

  @Operation(summary = "워크스페이스 목록 조회", description = "본인이 속한 워크스페이스 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공",
          content = @Content(schema = @Schema(implementation = WorkspaceDtos.ListItem.class)))
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @GetMapping
  public ResponseEntity<List<WorkspaceDtos.ListItem>> getAllWorkspaces(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token) {
    
    // Authorization 헤더가 없으면 빈 리스트 반환
    if (token == null || token.trim().isEmpty()) {
      return ResponseEntity.ok(List.of());
    }
    
    String jwt = token.replace("Bearer ", "").trim();
    Long userId = jwtProvider.getUserIdFromAccessToken(jwt);
    
    List<Workspace> workspaces = workspaceService.getWorkspacesByUserId(userId);

    List<WorkspaceDtos.ListItem> response = workspaces.stream()
        .map(workspace -> {
          WorkspaceDtos.ListItem item = new WorkspaceDtos.ListItem();
          item.setWorkspaceId(workspace.getWorkspaceId());
          item.setName(workspace.getName());
          return item;
        })
        .collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "워크스페이스 초대 링크 생성", description = "OWNER가 초대 링크를 생성합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "생성 성공")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @PostMapping("/{id}/invite-link")
  public ResponseEntity<WorkspaceDtos.InviteLinkResponse> createInviteLink(
      @Parameter(description = "워크스페이스 ID", required = true) @PathVariable Long id,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
  ) {
    if (token == null || token.trim().isEmpty()) {
      return ResponseEntity.status(401).build();
    }

    String jwt = token.replace("Bearer ", "").trim();
    Long userId = jwtProvider.getUserIdFromAccessToken(jwt);

    WorkspaceDtos.InviteLinkResponse response = workspaceInviteService.createInvite(id, userId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "초대 링크 수락", description = "초대 링크를 통해 워크스페이스에 참여합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "참여 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 필요 (userId 미제공 시)")
  })
  @PostMapping("/invite/{token}/accept")
  public ResponseEntity<String> acceptInvite(
      @Parameter(description = "초대 토큰", required = true) @PathVariable String token,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "초대 링크 수락 요청 (Authorization 없을 경우 userId 필수)", required = false)
      @RequestBody(required = false) WorkspaceDtos.InviteAcceptRequest request
  ) {
    Long userId = null;

    if (authHeader != null && !authHeader.trim().isEmpty()) {
      String jwt = authHeader.replace("Bearer ", "").trim();
      userId = jwtProvider.getUserIdFromAccessToken(jwt);
    } else if (request != null && request.getUserId() != null) {
      userId = request.getUserId();
    }

    if (userId == null) {
      log.warn("초대 수락 요청에 사용자 정보가 없음 - token: {}", token);
      return ResponseEntity.badRequest().body("userId 또는 Authorization 헤더가 필요합니다.");
    }

    workspaceInviteService.acceptInvite(token, userId);
    return ResponseEntity.ok("워크스페이스 참여 성공");
  }

  @Operation(summary = "워크스페이스 상세 조회", description = "특정 워크스페이스의 상세 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
  })
  @GetMapping("/{id}")
  public ResponseEntity<WorkspaceDtos.ListItem> getWorkspaceById(
      @Parameter(description = "워크스페이스 ID", required = true) @PathVariable Long id) {
    try {
      Workspace workspace = workspaceService.getWorkspaceById(id);

      WorkspaceDtos.ListItem response = new WorkspaceDtos.ListItem();
      response.setWorkspaceId(workspace.getWorkspaceId());
      response.setName(workspace.getName());

      return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
      return ResponseEntity.notFound().build();
    }
  }


  @Operation(summary = "워크스페이스 이름 수정", description = "워크스페이스의 이름을 수정합니다. (개발용: Authorization 선택사항)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @PutMapping("/{id}")
  public ResponseEntity<WorkspaceDtos.ListItem> updateWorkspace(
      @Parameter(description = "워크스페이스 ID", required = true) @PathVariable Long id,
      @Parameter(description = "JWT 토큰 (개발용: 선택사항)", required = false)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "수정할 워크스페이스 정보", required = true)
      @RequestBody WorkspaceDtos.UpdateRequest request) {

    // 개발/테스트: Authorization 없으면 더미 userId 사용
    Long userId = 1L;
    if (token != null && !token.trim().isEmpty()) {
      String jwt = token.replace("Bearer ", "").trim();
      userId = jwtProvider.getUserIdFromAccessToken(jwt);
    }

    return ResponseEntity.ok(
        workspaceService.updateWorkspaceName(id, request.getName().trim(), userId));
  }

  @Operation(summary = "워크스페이스 생성", description = "새로운 워크스페이스를 생성합니다. (개발용: Authorization 선택사항)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "생성 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (이름이 비어있음)")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @PostMapping
  public ResponseEntity<WorkspaceDtos.Response> create(
      @Parameter(description = "JWT 토큰 (개발용: 선택사항)", required = false)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "생성할 워크스페이스 정보", required = true)
      @RequestBody WorkspaceDtos.CreateRequest request) {

    if (request.getName() == null || request.getName().trim().isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    // 개발/테스트: Authorization 없으면 더미 userId 사용
    Long userId = 1L;
    if (token != null && !token.trim().isEmpty()) {
      String jwt = token.replace("Bearer ", "").trim();
      userId = jwtProvider.getUserIdFromAccessToken(jwt);
    }

    Workspace saved = workspaceService.createWorkspace(request.getName().trim(), userId);

    WorkspaceDtos.Response resp = new WorkspaceDtos.Response();
    resp.setWorkspaceId(saved.getWorkspaceId());
    resp.setName(saved.getName());
    resp.setCreatedAt(saved.getCreatedAt());

    return ResponseEntity.ok(resp);
  }

  @Operation(summary = "워크스페이스 삭제", description = "워크스페이스를 삭제합니다. (OWNER만 가능)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "삭제 성공"),
      @ApiResponse(responseCode = "401", description = "인증 토큰이 필요하거나 유효하지 않음"),
      @ApiResponse(responseCode = "403", description = "워크스페이스 OWNER 권한이 없음"),
      @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteWorkspace(
      @Parameter(description = "워크스페이스 ID", required = true) @PathVariable Long id,
      @Parameter(description = "JWT 토큰", required = false)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
  ) {
    log.info("=== 워크스페이스 삭제 요청 ===");
    log.info("workspaceId: {}", id);
    log.info("Authorization header present: {}", token != null && !token.trim().isEmpty());
    
    Long userId;
    if (token != null && !token.trim().isEmpty()) {
      try {
        String jwt = token.replace("Bearer ", "").trim();
        userId = jwtProvider.getUserIdFromAccessToken(jwt);
        log.info("JWT에서 파싱한 userId: {} (타입: {})", userId, userId.getClass().getSimpleName());
      } catch (Exception e) {
        log.error("JWT 파싱 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
      }
    } else {
      log.warn("인증 토큰이 없음");
      return ResponseEntity.status(401).body("인증 토큰이 필요합니다.");
    }

    try {
      workspaceService.deleteWorkspace(id, userId);
      log.info("워크스페이스 삭제 성공 - workspaceId: {}, userId: {}", id, userId);
      return ResponseEntity.ok("워크스페이스가 삭제되었습니다.");
    } catch (CustomException e) {
      log.error("워크스페이스 삭제 실패 - workspaceId: {}, userId: {}, error: {}", 
          id, userId, e.getMessage());
      return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(e.getMessage());
    } catch (Exception e) {
      log.error("워크스페이스 삭제 중 예상치 못한 오류 - workspaceId: {}, userId: {}", 
          id, userId, e.getMessage(), e);
      return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
    }
  }
}