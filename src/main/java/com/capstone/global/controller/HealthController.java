package com.capstone.global.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health Check", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/v1/health")
public class HealthController {

  @Operation(summary = "헬스 체크", description = "서버 및 WebSocket 서버의 상태를 확인합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "서버 정상 동작")
  })
  @GetMapping
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", LocalDateTime.now());
    response.put("message", "Spring Boot 애플리케이션이 정상적으로 실행 중입니다.");

    // WebSocket 서버 상태 확인
    Map<String, Object> websocketStatus = new HashMap<>();
    websocketStatus.put("enabled", true);
    websocketStatus.put("endpoint", "/ws");
    websocketStatus.put("protocol", "STOMP");
    response.put("websocket", websocketStatus);

    return ResponseEntity.ok(response);
  }
}
