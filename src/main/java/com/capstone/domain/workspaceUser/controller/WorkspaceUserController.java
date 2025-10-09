package com.capstone.domain.workspaceUser.controller;

import com.capstone.domain.workspaceUser.service.WorkspaceUserService;
import com.capstone.global.oauth.service.GoogleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceUserController {

  private final WorkspaceUserService workspaceUserService;
  private final GoogleService googleService;

  @PostMapping("/{workspaceId}/join")
  public ResponseEntity<String> joinWorkspace(
      @PathVariable Long workspaceId,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token
  ) {

    Long userId = googleService.getUserIdFromToken(token);
    workspaceUserService.joinWorkspace(workspaceId, userId);

    return ResponseEntity.ok("워크스페이스 참여 성공");
  }
}
