package com.capstone.domain.user.controller;

import com.capstone.domain.user.dto.UserResponse;
import com.capstone.domain.user.service.UserService;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
    }

    String accessToken = authorizationHeader.substring(7);
    UserResponse response = userService.getCurrentUser(accessToken);
    return ResponseEntity.ok(response);
  }
}

