package com.capstone.global.controller;

import com.capstone.domain.idea.prototype.OpenAiChatService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAI 관련 REST API. {@code GET /v1/openai/test} 는 Chat Completions 로 실제 연결을 검증합니다.
 */
@RestController
@RequestMapping("/v1/openai")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class OpenAIController {

  private final OpenAiChatService openAiChatService;

  @Value("${openai.prototype-model:gpt-4o-mini}")
  private String prototypeModel;

  /**
   * 아이디어 클러스터링 API POST /openai/cluster
   */
  @PostMapping("/cluster")
  public ResponseEntity<?> clusterIdeas(@RequestBody Map<String, List<String>> request) {
    try {
      List<String> ideas = request.get("ideas");
      if (ideas == null || ideas.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "아이디어 목록이 비어있습니다."));
      }

      String result = "OpenAI 서비스가 비활성화되어 있습니다.";
      return ResponseEntity.ok(Map.of("clusters", result));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "클러스터링 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 아이디어 피드백 생성 API POST /openai/feedback
   */
  @PostMapping("/feedback")
  public ResponseEntity<?> generateFeedback(@RequestBody Map<String, String> request) {
    try {
      String idea = request.get("idea");
      if (idea == null || idea.trim().isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "아이디어가 비어있습니다."));
      }

      String feedback = "OpenAI 서비스가 비활성화되어 있습니다.";
      return ResponseEntity.ok(Map.of("feedback", feedback));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "피드백 생성 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 세션 요약 생성 API POST /openai/summary
   */
  @PostMapping("/summary")
  public ResponseEntity<?> generateSessionSummary(@RequestBody Map<String, Object> request) {
    try {
      @SuppressWarnings("unchecked")
      List<String> ideas = (List<String>) request.get("ideas");
      Integer duration = (Integer) request.get("duration");

      if (ideas == null || ideas.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "아이디어 목록이 비어있습니다."));
      }

      if (duration == null || duration <= 0) {
        duration = 30;
      }

      String summary = "OpenAI 서비스가 비활성화되어 있습니다.";
      return ResponseEntity.ok(Map.of("summary", summary));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "요약 생성 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 텍스트 임베딩 생성 API POST /openai/embedding
   */
  @PostMapping("/embedding")
  public ResponseEntity<?> getEmbedding(@RequestBody Map<String, String> request) {
    try {
      String text = request.get("text");
      if (text == null || text.trim().isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "텍스트가 비어있습니다."));
      }

      List<Double> embedding = List.of(0.0, 0.0, 0.0);
      return ResponseEntity.ok(Map.of("embedding", embedding));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "임베딩 생성 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * 여러 텍스트의 임베딩 일괄 생성 API POST /openai/embeddings
   */
  @PostMapping("/embeddings")
  public ResponseEntity<?> getEmbeddings(@RequestBody Map<String, List<String>> request) {
    try {
      List<String> texts = request.get("texts");
      if (texts == null || texts.isEmpty()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "텍스트 목록이 비어있습니다."));
      }

      List<List<Double>> embeddings = texts.stream()
          .map(text -> List.of(0.0, 0.0, 0.0))
          .toList();
      return ResponseEntity.ok(Map.of("embeddings", embeddings));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(Map.of("error", "일괄 임베딩 생성 중 오류가 발생했습니다: " + e.getMessage()));
    }
  }

  /**
   * OpenAI Chat Completions 실호출 연결 테스트 GET /v1/openai/test
   */
  @GetMapping("/test")
  public ResponseEntity<?> testConnection() {
    if (!openAiChatService.isEnabled()) {
      return ResponseEntity.status(503)
          .body(
              Map.of(
                  "status",
                  "disabled",
                  "message",
                  "OPENAI_API_KEY 또는 local-application.yml 의 openai.api-key 가 없거나 형식이 올바르지 않습니다."));
    }
    try {
      String reply =
          openAiChatService.createChatCompletion(
              prototypeModel,
              List.of(Map.of("role", "user", "content", "Reply with exactly: OK")));
      boolean ok = reply != null && reply.toLowerCase().contains("ok");
      String preview = reply == null ? "" : reply.substring(0, Math.min(120, reply.length()));
      return ResponseEntity.ok(
          Map.of(
              "status",
              ok ? "success" : "partial",
              "message",
              ok ? "OpenAI Chat Completions 호출 성공" : "응답을 받았으나 예상과 다를 수 있음",
              "model",
              prototypeModel,
              "replyPreview",
              preview));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(
              Map.of(
                  "status",
                  "error",
                  "message",
                  e.getMessage() != null ? e.getMessage() : "unknown"));
    }
  }
}
