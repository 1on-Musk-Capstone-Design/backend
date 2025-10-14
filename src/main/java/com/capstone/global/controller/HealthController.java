package com.capstone.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.corundumstudio.socketio.SocketIOServer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Health Check", description = "서버 상태 확인 API")
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private SocketIOServer socketIOServer;

    @Operation(summary = "헬스 체크", description = "서버 및 Socket.IO 서버의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서버 정상 동작")
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Spring Boot 애플리케이션이 정상적으로 실행 중입니다.");
        
        // Socket.IO 서버 상태 확인
        Map<String, Object> socketStatus = new HashMap<>();
        socketStatus.put("running", socketIOServer != null);
        socketStatus.put("port", socketIOServer.getConfiguration().getPort());
        socketStatus.put("connectedClients", socketIOServer.getAllClients().size());
        response.put("socketIO", socketStatus);
        
        return ResponseEntity.ok(response);
    }
}
