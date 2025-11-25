package com.capstone.global.oauth.controller;

import com.capstone.global.config.AppProperties;
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
  private final AppProperties appProperties;

  @Value("${oauth2.google.client-id}")
  private String clientId;

  @Value("${oauth2.google.login-uri}")
  private String loginUriBase;

  @PostMapping
  public ResponseEntity<TokenDto> loginOrJoin(
      @RequestParam String code,
      @RequestParam(value = "redirect_uri", required = false) String redirectUriParam,
      @RequestHeader(value = "Origin", required = false) String origin) {
    log.info("OAuth 콜백 요청 수신 - code: {}, origin: {}, redirect_uri: {}",
        code != null ? code.substring(0, Math.min(10, code.length())) + "..." : "null",
        origin,
        redirectUriParam);
    try {
      // redirect_uri 파라미터가 있으면 우선 사용, 없으면 Origin 헤더 기반으로 결정
      String dynamicRedirectUri;
      if (redirectUriParam != null && !redirectUriParam.trim().isEmpty()) {
        dynamicRedirectUri = redirectUriParam;
        log.info("OAuth 로그인 처리 - redirect_uri 파라미터 사용: {}", dynamicRedirectUri);
      } else {
        dynamicRedirectUri = resolveRedirectUri(origin);
        log.info("OAuth 로그인 처리 - Origin 헤더 기반: {}, Redirect URI: {}", origin, dynamicRedirectUri);
      }
      TokenDto tokenDto = googleService.loginOrJoin(code, dynamicRedirectUri);
      log.info("OAuth 로그인 성공 - 리다이렉트: {}", dynamicRedirectUri);
      return ResponseEntity.ok(tokenDto);
    } catch (Exception e) {
      log.error("OAuth 로그인 처리 실패", e);
      throw e;
    }
  }

  @GetMapping("/login-uri")
  public ResponseEntity<String> getLoginUri(
      @RequestHeader(value = "Origin", required = false) String origin,
      @RequestParam(value = "redirect_uri", required = false) String redirectUriParam) {
    
    // redirect_uri 파라미터가 있으면 우선 사용, 없으면 Origin 헤더 기반으로 결정
    String dynamicRedirectUri;
    if (redirectUriParam != null && !redirectUriParam.trim().isEmpty()) {
      dynamicRedirectUri = redirectUriParam;
      log.info("OAuth 로그인 URI 생성 - redirect_uri 파라미터 사용: {}", dynamicRedirectUri);
    } else {
      dynamicRedirectUri = resolveRedirectUri(origin);
      log.info("OAuth 로그인 URI 생성 - Origin 헤더 기반: {}, Redirect URI: {}", origin, dynamicRedirectUri);
    }

    // 동적으로 Google OAuth 로그인 URL 생성
    String scope = "openid email profile";
    String responseType = "code";
    
    String loginUri = String.format(
      "%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s",
      loginUriBase,
      URLEncoder.encode(clientId, StandardCharsets.UTF_8),
      URLEncoder.encode(dynamicRedirectUri, StandardCharsets.UTF_8),
      responseType,
      URLEncoder.encode(scope, StandardCharsets.UTF_8)
    );
    
    return ResponseEntity.ok(loginUri);
  }

  private String resolveRedirectUri(String origin) {
    if (origin != null) {
      for (var entry : appProperties.getOauthRedirectMap().entrySet()) {
        if (origin.contains(entry.getKey())) {
          return entry.getValue();
        }
      }
    }
    return appProperties.getDefaultRedirectUri();
  }
}
