package com.capstone.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capstone.domain.idea.prototype.IdeaPrototypeController;
import com.capstone.domain.idea.prototype.IdeaPrototypeService;
import com.capstone.domain.idea.prototype.PrototypeJobStatus;
import com.capstone.domain.idea.prototype.dto.PrototypeJobAcceptedResponse;
import com.capstone.domain.idea.prototype.dto.PrototypePipelineResponse;
import com.capstone.global.oauth.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = IdeaPrototypeController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
class IdeaPrototypeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private IdeaPrototypeService ideaPrototypeService;
  @MockBean private JwtProvider jwtProvider;

  private final String token = "Bearer test-token";
  private final Long userId = 1L;
  private final Long ideaId = 10L;

  private static PrototypePipelineResponse sampleFullResponse(Long ideaId) {
    return PrototypePipelineResponse.builder()
        .workspaceId(1L)
        .prdViewPath("/prd/workspaces/1/prds/99")
        .prdViewUrl("http://localhost:3000/prd/workspaces/1/prds/99")
        .jobId(99L)
        .ideaId(ideaId)
        .status(PrototypeJobStatus.DEPLOYED)
        .prdMarkdown("# PRD")
        .uiStructureJson("{}")
        .githubRepoUrl("https://github.com/u/r")
        .vercelPreviewUrl("https://vercel.app/preview")
        .vercelProductionUrl("https://vercel.app/prod")
        .simulated(false)
        .vercelDeploymentApiUsed(true)
        .message("ok")
        .createdAt(null)
        .updatedAt(null)
        .build();
  }

  @Test
  @DisplayName("프로토타입 파이프라인 동기 실행 성공")
  void runPipelineSyncOk() throws Exception {
    given(jwtProvider.getUserIdFromAccessToken("test-token")).willReturn(userId);
    given(ideaPrototypeService.runPipelineSync(eq(userId), eq(ideaId)))
        .willReturn(sampleFullResponse(ideaId));

    mockMvc
        .perform(
            post("/v1/ideas/{ideaId}/prototype/pipeline", ideaId)
                .param("sync", "true")
                .with(csrf())
                .header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jobId").value(99))
        .andExpect(jsonPath("$.ideaId").value(ideaId))
        .andExpect(jsonPath("$.status").value("DEPLOYED"))
        .andExpect(jsonPath("$.message").value("ok"));
  }

  @Test
  @DisplayName("프로토타입 파이프라인 비동기 큐잉 202")
  void runPipelineAsyncAccepted() throws Exception {
    given(jwtProvider.getUserIdFromAccessToken("test-token")).willReturn(userId);
    given(ideaPrototypeService.startPipelineAsync(eq(userId), eq(ideaId)))
        .willReturn(
            PrototypeJobAcceptedResponse.builder()
                .jobId(5L)
                .ideaId(ideaId)
                .status(PrototypeJobStatus.PENDING)
                .message("queued")
                .build());

    mockMvc
        .perform(
            post("/v1/ideas/{ideaId}/prototype/pipeline", ideaId)
                .with(csrf())
                .header("Authorization", token))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.jobId").value(5))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  @DisplayName("최근 프로토타입 조회 성공")
  void getLatestOk() throws Exception {
    given(jwtProvider.getUserIdFromAccessToken("test-token")).willReturn(userId);
    PrototypePipelineResponse body =
        PrototypePipelineResponse.builder()
            .workspaceId(1L)
            .prdViewPath("/prd/workspaces/1/prds/1")
            .prdViewUrl("http://localhost:3000/prd/workspaces/1/prds/1")
            .jobId(1L)
            .ideaId(ideaId)
            .status(PrototypeJobStatus.DEPLOYED)
            .prdMarkdown("# x")
            .uiStructureJson("{}")
            .githubRepoUrl(null)
            .vercelPreviewUrl("https://p")
            .vercelProductionUrl("https://prod")
            .simulated(true)
            .vercelDeploymentApiUsed(false)
            .message("")
            .createdAt(null)
            .updatedAt(null)
            .build();
    given(ideaPrototypeService.getLatest(eq(userId), eq(ideaId))).willReturn(body);

    mockMvc
        .perform(get("/v1/ideas/{ideaId}/prototype", ideaId).header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.simulated").value(true));
  }
}
