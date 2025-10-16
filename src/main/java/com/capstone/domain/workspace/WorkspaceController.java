package com.capstone.domain.workspace;

import com.capstone.global.oauth.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspaces")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class WorkspaceController {

  private final WorkspaceService workspaceService;
  private final JwtProvider jwtProvider;

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

  @GetMapping("/{id}")
  public ResponseEntity<WorkspaceDtos.ListItem> getWorkspaceById(@PathVariable Long id) {
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


  @PutMapping("/{id}")
  public ResponseEntity<WorkspaceDtos.ListItem> updateWorkspace(
      @PathVariable Long id,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token,
      @RequestBody WorkspaceDtos.UpdateRequest request) {

    // 개발/테스트: Authorization 없으면 더미 userId 사용
    Long userId = 1L;
    if (token != null && !token.trim().isEmpty()) {
      String jwt = token.replace("Bearer ", "").trim();
      userId = jwtProvider.getUserIdFromAccessToken(jwt);
    }

    return ResponseEntity.ok(workspaceService.updateWorkspaceName(id, request.getName().trim(), userId));
  }

  @PostMapping
  public ResponseEntity<WorkspaceDtos.Response> create(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token,
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

  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteWorkspace(
      @PathVariable Long id,
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