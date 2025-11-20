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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspaces/{workspaceId}")
public class WorkspaceUserController {

  private final WorkspaceUserService workspaceUserService;
  private final JwtProvider jwtProvider; // GoogleService 대신 JwtProvider 사용

  @PostMapping("/join")
  public ResponseEntity<String> joinWorkspace(
      @PathVariable Long workspaceId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token
  ) {
    // 개발/테스트: Authorization 없으면 더미 userId 사용
    Long userId = 1L;
    if (token != null && !token.trim().isEmpty()) {
      String jwt = token.replace("Bearer ", "").trim();
      userId = jwtProvider.getUserIdFromAccessToken(jwt);
    }

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
      @PathVariable("userId") Long targetUserId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Authorization 없이 본인을 삭제할 경우 userId 필수", required = false)
      @RequestBody(required = false) SelfIdentityRequest request,
      @RequestParam(value = "userId", required = false) Long userIdParam) {

    Long requesterId = resolveUserId(token, request != null ? request.getUserId() : null, userIdParam);
    if (requesterId == null) {
      return ResponseEntity.badRequest().body("userId 또는 Authorization 헤더가 필요합니다.");
    }

    workspaceUserService.removeUser(workspaceId, targetUserId, requesterId);
    return ResponseEntity.ok("유저 삭제 완료");
  }

  @DeleteMapping("/leave")
  public ResponseEntity<String> leaveWorkspace(
      @PathVariable Long workspaceId,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String token,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Authorization 헤더가 없을 경우 userId 필수", required = false)
      @RequestBody(required = false) SelfIdentityRequest request,
      @RequestParam(value = "userId", required = false) Long userIdParam) {

    Long userId = resolveUserId(token, request != null ? request.getUserId() : null, userIdParam);
    if (userId == null) {
      return ResponseEntity.badRequest().body("userId 또는 Authorization 헤더가 필요합니다.");
    }

    workspaceUserService.leaveWorkspace(workspaceId, userId);
    return ResponseEntity.ok("워크스페이스를 나갔습니다.");
  }

  private Long resolveUserId(String token, Long bodyUserId, Long paramUserId) {
    if (token != null && !token.trim().isEmpty()) {
      String jwt = token.replace("Bearer ", "").trim();
      return jwtProvider.getUserIdFromAccessToken(jwt);
    }
    if (bodyUserId != null) {
      return bodyUserId;
    }
    return paramUserId;
  }

  public static class SelfIdentityRequest {
    private Long userId;

    public Long getUserId() {
      return userId;
    }

    public void setUserId(Long userId) {
      this.userId = userId;
    }
  }
}
