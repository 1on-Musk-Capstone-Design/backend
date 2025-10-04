package com.capstone.global.oauth.controller;

import com.capstone.global.oauth.TokenDto;
import com.capstone.global.oauth.service.GoogleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth-google")
@RequiredArgsConstructor
public class GoogleController {

  @Value("${oauth2.google.login-uri}")
  private String loginUri;

  private final GoogleService googleService;

  @PostMapping
  public ResponseEntity<TokenDto> loginOrJoin(@RequestParam String code) {
    return ResponseEntity.ok(googleService.loginOrJoin(code));
  }

  @GetMapping("/login-uri")
  public ResponseEntity<String> getLoginUri() {
    return ResponseEntity.ok(loginUri);
  }
}
