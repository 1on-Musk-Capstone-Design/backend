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

import com.capstone.domain.idea.IdeaController;
import com.capstone.domain.idea.IdeaRequest;
import com.capstone.domain.idea.IdeaResponse;
import com.capstone.domain.idea.IdeaService;
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

@WebMvcTest(controllers = IdeaController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
public class IdeaControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private IdeaService ideaService;

  @MockBean
  private JwtProvider jwtProvider;

  private IdeaRequest ideaRequest;
  private IdeaResponse ideaResponse;

  private final String token = "BAD TOKEN";
  private final Long userId = 1L;
  private final Long workspaceId = 5L;
  private final Long canvasId = 3L;
  private final Long ideaId = 1L;

  @BeforeEach
  void setUp() {
    ideaRequest = IdeaRequest.builder()
        .workspaceId(workspaceId)
        .canvasId(canvasId)
        .content("NEW 아이디어")
        .positionX(100.0)
        .positionY(200.0)
        .build();

    ideaResponse = IdeaResponse.builder()
        .id(ideaId)
        .workspaceId(workspaceId)
        .canvasId(canvasId)
        .content(ideaRequest.getContent())
        .positionX(ideaRequest.getPositionX())
        .positionY(ideaRequest.getPositionY())
        .build();
  }

  @Test
  @DisplayName("아이디어 생성 성공")
  void successCreateIdea() throws Exception {
    given(jwtProvider.getUserIdFromAccessToken("BAD TOKEN")).willReturn(userId);
    given(ideaService.createIdea(any(Long.class), any(IdeaRequest.class)))
        .willReturn(ideaResponse);

    mockMvc.perform(post("/v1/ideas")
            .with(csrf())
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ideaRequest)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ideaId))
        .andExpect(jsonPath("$.content").value(ideaRequest.getContent()));
  }

  @Test
  @DisplayName("아이디어 전체 조회 성공")
  void successGetAllIdeas() throws Exception {
    given(ideaService.getAllIdeas(workspaceId)).willReturn(List.of(ideaResponse));

    mockMvc.perform(get("/v1/ideas/workspace/{workspaceId}", workspaceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(ideaId))
        .andExpect(jsonPath("$[0].content").value("NEW 아이디어"));
  }

  @Test
  @DisplayName("아이디어 단건 조회 성공")
  void successGetIdea() throws Exception {
    given(ideaService.getIdea(ideaId)).willReturn(ideaResponse);

    mockMvc.perform(get("/v1/ideas/{ideaId}", ideaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ideaId))
        .andExpect(jsonPath("$.content").value("NEW 아이디어"));
  }

  @Test
  @DisplayName("아이디어 수정 성공")
  void successUpdateIdea() throws Exception {
    IdeaRequest updateRequest = IdeaRequest.builder()
        .workspaceId(workspaceId)
        .canvasId(canvasId)
        .content("수정된 아이디어")
        .positionX(150.0)
        .positionY(250.0)
        .build();

    IdeaResponse updatedResponse = IdeaResponse.builder()
        .id(ideaId)
        .workspaceId(workspaceId)
        .canvasId(canvasId)
        .content("수정된 아이디어")
        .positionX(150.0)
        .positionY(250.0)
        .build();

    given(ideaService.updateIdea(any(Long.class), eq(ideaId), any(IdeaRequest.class)))
        .willReturn(updatedResponse);

    mockMvc.perform(put("/v1/ideas/{ideaId}", ideaId)
            .with(csrf())
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ideaId))
        .andExpect(jsonPath("$.content").value("수정된 아이디어"))
        .andExpect(jsonPath("$.positionX").value(150.0))
        .andExpect(jsonPath("$.positionY").value(250.0));
  }

  @Test
  @DisplayName("아이디어 삭제 성공")
  void successDeleteIdea() throws Exception {
    given(jwtProvider.getUserIdFromAccessToken("BAD TOKEN")).willReturn(userId);
    doNothing().when(ideaService).deleteIdea(userId, ideaId);

    mockMvc.perform(delete("/v1/ideas/{ideaId}", ideaId)
            .with(csrf())
            .header("Authorization", token))
        .andExpect(status().isOk());
  }
}
