package com.capstone.global.oauth.controller;

import com.capstone.global.oauth.AppleLoginRequest;
import com.capstone.global.oauth.TokenDto;
import com.capstone.global.oauth.service.AppleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/auth-apple")
@RequiredArgsConstructor
public class AppleController {

  private final AppleService appleService;

  @PostMapping
  public ResponseEntity<TokenDto> loginOrJoin(@RequestBody @Valid AppleLoginRequest req) {

    log.info("Apple 로그인 요청 수신");
    return ResponseEntity.ok(appleService.loginOrJoin(req.identityToken(), req.fullName()));
  }
}