package com.capstone.global.oauth.controller;

import com.capstone.global.oauth.TokenDto;
import com.capstone.global.oauth.service.GoogleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/v1/auth-google")
@RequiredArgsConstructor
public class GoogleController {

  private final GoogleService googleService;

  @Value("${oauth2.google.client-id}")
  private String clientId;

  @Value("${oauth2.google.redirect-uri}")
  private String redirectUri;

  @Value("${oauth2.google.login-uri}")
  private String loginUriBase;

  @PostMapping
  public ResponseEntity<TokenDto> loginOrJoin(@RequestParam String code) {
    log.info("OAuth 콜백 요청 수신 - code: {}", code != null ? code.substring(0, Math.min(10, code.length())) + "..." : "null");
    try {
      TokenDto tokenDto = googleService.loginOrJoin(code);
      log.info("OAuth 로그인 성공 - 토큰 발급 완료");
      return ResponseEntity.ok(tokenDto);
    } catch (Exception e) {
      log.error("OAuth 로그인 처리 실패", e);
      throw e;
    }
  }

  @GetMapping("/login-uri")
  public ResponseEntity<String> getLoginUri() {
    // 동적으로 Google OAuth 로그인 URL 생성
    String scope = "openid email profile";
    String responseType = "code";
    
    String loginUri = String.format(
      "%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s",
      loginUriBase,
      URLEncoder.encode(clientId, StandardCharsets.UTF_8),
      URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
      responseType,
      URLEncoder.encode(scope, StandardCharsets.UTF_8)
    );
    
    return ResponseEntity.ok(loginUri);
  }
}
