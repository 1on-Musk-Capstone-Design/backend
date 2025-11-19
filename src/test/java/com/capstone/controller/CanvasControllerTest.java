package com.capstone.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.domain.canvas.CanvasController;
import com.capstone.domain.canvas.CanvasRequest;
import com.capstone.domain.canvas.CanvasResponse;
import com.capstone.domain.canvas.CanvasService;
import com.capstone.global.oauth.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CanvasController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
public class CanvasControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CanvasService canvasService;

  @MockBean
  private JwtProvider jwtProvider;

  private CanvasRequest canvasRequest;
  private CanvasResponse canvasResponse;

  private final String token = "wrong token";
  private final Long userId = 1L;
  private final Long workspaceId = 1L;
  private final Long canvasId = 1L;

  @BeforeEach
  void setUp() {
    canvasRequest = CanvasRequest.builder()
        .title("새로운 캔버스")
        .build();

    canvasResponse = CanvasResponse.builder()
        .id(canvasId)
        .title(canvasRequest.getTitle())
        .createdAt("2025-10-15")
        .updatedAt("2025-10-15")
        .build();
  }

  @Test
  @DisplayName("캔버스 생성 성공")
  void successCreateCanvas() throws Exception {
    given(jwtProvider.getUserIdFromAccessToken("wrong token")).willReturn(userId);

    given(canvasService.createCanvas(any(Long.class), any(Long.class), any(CanvasRequest.class)))
        .willReturn(canvasResponse);

    mockMvc.perform(post("/api/v1/{workspaceId}/canvas", workspaceId)
            .with(csrf())
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(canvasRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(canvasId))
        .andExpect(jsonPath("$.title").value(canvasRequest.getTitle()));
  }

  @Test
  @DisplayName("캔버스 전체 조회 성공")
  void successGetAllCanvas() throws Exception {
    given(canvasService.getAllCanvas(workspaceId)).willReturn(List.of(canvasResponse));

    mockMvc.perform(get("/api/v1/{workspaceId}/canvas", workspaceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(canvasId))
        .andExpect(jsonPath("$[0].title").value("새로운 캔버스"));
  }

  @Test
  @DisplayName("캔버스 단건 조회 성공")
  void successGetCanvas() throws Exception {
    given(canvasService.getCanvas(canvasId)).willReturn(canvasResponse);

    mockMvc.perform(get("/api/v1/canvas/{canvasId}", canvasId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(canvasId))
        .andExpect(jsonPath("$.title").value("새로운 캔버스"));
  }

  @Test
  @DisplayName("캔버스 수정 성공")
  void successUpdateCanvas() throws Exception {

    CanvasRequest updateRequest = CanvasRequest.builder()
        .title("수정된 캔버스")
        .build();

    CanvasResponse updatedResponse = CanvasResponse.builder()
        .id(canvasId)
        .title("수정된 캔버스")
        .createdAt(canvasResponse.getCreatedAt())
        .updatedAt("2025-10-15")
        .build();

    given(canvasService.updateCanvas(any(Long.class), eq(canvasId), any(CanvasRequest.class)))
        .willReturn(updatedResponse);

    mockMvc.perform(put("/api/v1/canvas/{canvasId}", canvasId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest))
            .header("Authorization", "Bearer wrong token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(canvasId))
        .andExpect(jsonPath("$.title").value("수정된 캔버스"))
        .andExpect(jsonPath("$.updatedAt").value("2025-10-15"));
  }

  @Test
  @DisplayName("캔버스 삭제 성공")
  void successDeleteCanvas() throws Exception {
    given(jwtProvider.getUserIdFromAccessToken("wrong token")).willReturn(userId);
    doNothing().when(canvasService).deleteCanvas(userId, canvasId);

    mockMvc.perform(delete("/api/v1/canvas/{canvasId}", canvasId)
            .with(csrf())
            .header("Authorization", token))
        .andExpect(status().isOk());
  }
}
