package com.capstone.domain.workspaceUser;

import com.capstone.global.oauth.JwtProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}")
public class WorkspaceUserController {

  private final WorkspaceUserService workspaceUserService;
  private final JwtProvider jwtProvider; // GoogleService 대신 JwtProvider 사용

  @PostMapping("/join")
  public ResponseEntity<String> joinWorkspace(
      @PathVariable Long workspaceId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token
  ) {
    String jwt = token.replace("Bearer ", "").trim();
    Long userId = jwtProvider.getUserIdFromAccessToken(jwt);

    workspaceUserService.joinWorkspace(workspaceId, userId);
    return ResponseEntity.ok("워크스페이스 참여 성공");
  }

  @GetMapping("/users")
  public ResponseEntity<List<WorkspaceUserResponse>> getWorkspaceUsers(
      @PathVariable Long workspaceId) {
    List<WorkspaceUserResponse> users = workspaceUserService.getWorkspaceUsers(workspaceId);
    return ResponseEntity.ok(users);
  }

  @DeleteMapping("/users/{userId}")
  public ResponseEntity<String> removeUser(
      @PathVariable Long workspaceId,
      @PathVariable Long userId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

    String jwt = token.replace("Bearer ", "").trim();
    Long requesterId = jwtProvider.getUserIdFromAccessToken(jwt);

    workspaceUserService.removeUser(workspaceId, userId, requesterId);
    return ResponseEntity.ok("유저 삭제 완료");
  }
}
