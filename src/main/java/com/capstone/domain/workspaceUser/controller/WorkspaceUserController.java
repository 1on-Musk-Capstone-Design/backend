package com.capstone.domain.workspaceUser.controller;

import com.capstone.domain.workspaceUser.entity.WorkspaceUser;
import com.capstone.domain.workspaceUser.service.WorkspaceUserService;
import com.capstone.global.oauth.service.GoogleService;
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
  private final GoogleService googleService;

  @PostMapping("/join")
  public ResponseEntity<String> joinWorkspace(
      @PathVariable Long workspaceId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token
  ) {

    Long userId = googleService.getUserIdFromToken(token);
    workspaceUserService.joinWorkspace(workspaceId, userId);

    return ResponseEntity.ok("워크스페이스 참여 성공");
  }

  @GetMapping("/users")
  public ResponseEntity<List<WorkspaceUser>> getWorkspaceUsers(@PathVariable Long workspaceId) {
    List<WorkspaceUser> users = workspaceUserService.getWorkspaceUsers(workspaceId);
    return ResponseEntity.ok(users);
  }

  @DeleteMapping("/users/{userId}")
  public ResponseEntity<String> removeUser(
      @PathVariable Long workspaceId,
      @PathVariable Long userId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

    Long requesterId = googleService.getUserIdFromToken(token);
    workspaceUserService.removeUser(workspaceId, userId, requesterId);

    return ResponseEntity.ok("유저 삭제 완료");
  }
}
