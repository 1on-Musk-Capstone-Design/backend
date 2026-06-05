package com.capstone.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.capstone.global.controller.HealthController.class)
@org.springframework.context.annotation.Import(com.capstone.config.TestSecurityConfig.class)
class HealthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void health_shouldReturnStatusUp() throws Exception {
    // When & Then
    mockMvc.perform(get("/v1/health")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.message").value("Spring Boot 애플리케이션이 정상적으로 실행 중입니다."))
        .andExpect(jsonPath("$.websocket.enabled").value(true))
        .andExpect(jsonPath("$.websocket.endpoint").value("/ws"))
        .andExpect(jsonPath("$.websocket.protocol").value("STOMP"))
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
