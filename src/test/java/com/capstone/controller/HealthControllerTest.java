package com.capstone.controller;

import com.corundumstudio.socketio.SocketIOServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.capstone.global.controller.HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SocketIOServer socketIOServer;

    @Test
    void health_shouldReturnStatusUp() throws Exception {
        // Given
        when(socketIOServer.getConfiguration()).thenReturn(new com.corundumstudio.socketio.Configuration());
        when(socketIOServer.getAllClients()).thenReturn(java.util.Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/health")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Spring Boot 애플리케이션이 정상적으로 실행 중입니다."))
                .andExpect(jsonPath("$.socketIO.running").exists())
                .andExpect(jsonPath("$.socketIO.port").exists())
                .andExpect(jsonPath("$.socketIO.connectedClients").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
