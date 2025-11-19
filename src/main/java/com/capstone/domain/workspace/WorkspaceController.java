package com.capstone.domain.workspace;

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
public class WorkspaceController {

  private final WorkspaceService workspaceService;
  private final JwtProvider jwtProvider;

  @Operation(summary = "워크스페이스 목록 조회", description = "모든 워크스페이스 목록을 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공",
          content = @Content(schema = @Schema(implementation = WorkspaceDtos.ListItem.class)))
  })
  @GetMapping
  public ResponseEntity<List<WorkspaceDtos.ListItem>> getAllWorkspaces() {
    List<Workspace> workspaces = workspaceService.getAllWorkspaces();

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

  @Operation(summary = "워크스페이스 삭제", description = "워크스페이스를 삭제합니다. (개발용: Authorization 선택사항)")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "삭제 성공"),
      @ApiResponse(responseCode = "404", description = "워크스페이스를 찾을 수 없음")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteWorkspace(
      @Parameter(description = "워크스페이스 ID", required = true) @PathVariable Long id,
      @Parameter(description = "JWT 토큰 (개발용: 선택사항)", required = false)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
  ) {
    // 개발/테스트: Authorization 없으면 더미 userId 사용
    Long userId = 1L;
    if (token != null && !token.trim().isEmpty()) {
      String jwt = token.replace("Bearer ", "").trim();
      userId = jwtProvider.getUserIdFromAccessToken(jwt);
    }

    workspaceService.deleteWorkspace(id, userId);

    return ResponseEntity.ok("워크스페이스가 삭제되었습니다.");
  }
}