package com.capstone.global.oauth.controller;

import com.capstone.global.config.AppProperties;
import com.capstone.global.config.DevBootstrapAuthProperties;
import com.capstone.global.oauth.TokenDto;
import com.capstone.global.oauth.service.DevBootstrapService;
import com.capstone.global.oauth.service.GoogleService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/v1/auth-google")
@RequiredArgsConstructor
public class GoogleController {

  private static final String LOCAL_DEV_CODE = "local-dev";

  private final GoogleService googleService;
  private final DevBootstrapAuthProperties devBootstrapAuthProperties;
  private final DevBootstrapService devBootstrapService;
  private final AppProperties appProperties;

  @Value("${oauth2.google.web.client-id}")
  private String clientId;

  @Value("${oauth2.google.ios.client-id}")
  private String iosClientId;

  @Value("${oauth2.google.login-uri:https://accounts.google.com/o/oauth2/v2/auth}")
  private String loginUriBase;

  @PostMapping
  public ResponseEntity<TokenDto> loginOrJoin(
      @RequestParam String code,
      @RequestParam(value = "redirect_uri", required = false) String redirectUriParam,
      @RequestParam(value = "platform", required = false) String platform,
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
      if (isLocalDevLogin(code, dynamicRedirectUri)) {
        log.info("로컬 개발자 로그인 처리 - redirect_uri: {}", dynamicRedirectUri);
        return ResponseEntity.ok(devBootstrapService.issueToken());
      }
      boolean isIos = "ios".equalsIgnoreCase(platform);
      TokenDto tokenDto = googleService.loginOrJoin(code, dynamicRedirectUri, isIos);
      log.info("OAuth 로그인 성공 - 리다이렉트: {}", dynamicRedirectUri);
      return ResponseEntity.ok(tokenDto);
    } catch (Exception e) {
      log.error("OAuth 로그인 처리 실패", e);
      throw e;
    }
  }

  @GetMapping("/login-uri")
  public ResponseEntity<String> getLoginUri(
      @RequestParam(value = "redirect_uri", required = false) String redirectUriParam,
      @RequestParam(value = "platform", required = false) String platform,
      HttpServletResponse response) {
    boolean isIos = "ios".equalsIgnoreCase(platform);

    if (redirectUriParam == null || redirectUriParam.trim().isEmpty()) {
      throw new IllegalArgumentException("redirect_uri는 필수입니다.");
    }
    response.setHeader("ngrok-skip-browser-warning", "any-value");
    String dynamicRedirectUri = redirectUriParam;

    // platform & redirect_uri 검증
    if (isIos) {
      if (!dynamicRedirectUri.startsWith("com.googleusercontent.apps")) {
        throw new IllegalArgumentException("iOS redirect_uri 형식이 아닙니다.");
      }
    } else {
      if (!dynamicRedirectUri.startsWith("http")) {
        throw new IllegalArgumentException("웹 redirect_uri 형식이 아닙니다.");
      }
    }

    log.info("OAuth 로그인 URI 생성 - platform: {}, redirect_uri: {}", platform, dynamicRedirectUri);

    if (!isIos && isLocalRedirectUri(dynamicRedirectUri) && devBootstrapAuthProperties.isEnabled()) {
      String localDevCallbackUri = UriComponentsBuilder.fromHttpUrl(dynamicRedirectUri)
          .queryParam("code", LOCAL_DEV_CODE)
          .build()
          .toUriString();
      log.info("로컬 개발자 로그인 URI 생성: {}", localDevCallbackUri);
      return ResponseEntity.ok(localDevCallbackUri);
    }

    String selectedClientId = isIos ? iosClientId : clientId;

    String scope = "openid email profile";

    String loginUri = UriComponentsBuilder.fromHttpUrl(loginUriBase)
        .queryParam("client_id", selectedClientId)
        .queryParam("redirect_uri", dynamicRedirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", scope)
        .build()
        .toUriString();

    log.info("생성된 최종 Login URI: {}", loginUri);
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

  private boolean isLocalDevLogin(String code, String redirectUri) {
    return devBootstrapAuthProperties.isEnabled()
        && LOCAL_DEV_CODE.equals(code)
        && isLocalRedirectUri(redirectUri);
  }

  private boolean isLocalRedirectUri(String redirectUri) {
    return redirectUri != null
        && (redirectUri.startsWith("http://localhost:")
        || redirectUri.startsWith("http://127.0.0.1:"));
  }
}
