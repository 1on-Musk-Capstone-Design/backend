package com.capstone.global.oauth.controller;

import com.capstone.global.config.DevBootstrapAuthProperties;
import com.capstone.global.oauth.dto.DevBootstrapSessionDto;
import com.capstone.global.oauth.service.DevBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth/dev")
@RequiredArgsConstructor
public class DevBootstrapController {

  private final DevBootstrapAuthProperties devBootstrapAuthProperties;
  private final DevBootstrapService devBootstrapService;

  /**
   * 로컬 개발 전용: DB의 첫 번째 사용자(없으면 dev 계정 생성)로 JWT 발급.
   * {@code app.dev-bootstrap-auth.enabled=false} 이면 404.
   */
  @PostMapping("/bootstrap")
  public ResponseEntity<DevBootstrapSessionDto> bootstrap() {
    if (!devBootstrapAuthProperties.isEnabled()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(devBootstrapService.issueSession());
  }
}
