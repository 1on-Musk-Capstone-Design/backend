package com.capstone.domain.user.controller;

import com.capstone.domain.user.dto.UserResponse;
import com.capstone.domain.user.dto.UserUpdateRequest;
import com.capstone.domain.user.service.UserService;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "현재 사용자 정보 조회", description = "JWT 토큰을 기반으로 현재 로그인한 사용자 정보를 조회합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "조회 성공",
          content = @Content(schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 실패")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
    }

    String accessToken = authorizationHeader.substring(7);
    UserResponse response = userService.getCurrentUser(accessToken);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "사용자 정보 수정", description = "현재 로그인한 사용자의 이름 및 프로필 이미지를 수정합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "수정 성공",
          content = @Content(schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "401", description = "인증 실패"),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
  })
  @SecurityRequirement(name = "Bearer Authentication")
  @PatchMapping("/me")
  public ResponseEntity<UserResponse> updateCurrentUser(
      @Parameter(description = "JWT 토큰", required = true)
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "수정할 사용자 정보", required = true)
      @RequestBody UserUpdateRequest request) {

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
    }

    String accessToken = authorizationHeader.substring(7);
    UserResponse response = userService.updateUser(accessToken, request);
    return ResponseEntity.ok(response);
  }
}

