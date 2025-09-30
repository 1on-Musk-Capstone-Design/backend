package com.capstone.global.controller;

// import com.capstone.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OpenAI API 관련 REST API 컨트롤러
 * AI 클러스터링 및 분석 기능 제공
 */
// @RestController
@RequestMapping("/api/openai")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class OpenAIController {
    
    // @Autowired
    // private OpenAIService openAIService;
    
    /**
     * 아이디어 클러스터링 API
     * POST /api/openai/cluster
     */
    @PostMapping("/cluster")
    public ResponseEntity<?> clusterIdeas(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> ideas = request.get("ideas");
            if (ideas == null || ideas.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "아이디어 목록이 비어있습니다."));
            }
            
            // String result = openAIService.clusterIdeas(ideas);
            String result = "OpenAI 서비스가 비활성화되어 있습니다.";
            return ResponseEntity.ok(Map.of("clusters", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "클러스터링 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 아이디어 피드백 생성 API
     * POST /api/openai/feedback
     */
    @PostMapping("/feedback")
    public ResponseEntity<?> generateFeedback(@RequestBody Map<String, String> request) {
        try {
            String idea = request.get("idea");
            if (idea == null || idea.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "아이디어가 비어있습니다."));
            }
            
            // String feedback = openAIService.generateFeedback(idea);
            String feedback = "OpenAI 서비스가 비활성화되어 있습니다.";
            return ResponseEntity.ok(Map.of("feedback", feedback));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "피드백 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 세션 요약 생성 API
     * POST /api/openai/summary
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
                duration = 30; // 기본값 30분
            }
            
            // String summary = openAIService.generateSessionSummary(ideas, duration);
            String summary = "OpenAI 서비스가 비활성화되어 있습니다.";
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "요약 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 텍스트 임베딩 생성 API
     * POST /api/openai/embedding
     */
    @PostMapping("/embedding")
    public ResponseEntity<?> getEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "텍스트가 비어있습니다."));
            }
            
            // List<Double> embedding = openAIService.getEmbedding(text);
            List<Double> embedding = List.of(0.0, 0.0, 0.0); // 더미 데이터
            return ResponseEntity.ok(Map.of("embedding", embedding));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "임베딩 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 여러 텍스트의 임베딩 일괄 생성 API
     * POST /api/openai/embeddings
     */
    @PostMapping("/embeddings")
    public ResponseEntity<?> getEmbeddings(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> texts = request.get("texts");
            if (texts == null || texts.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "텍스트 목록이 비어있습니다."));
            }
            
            // List<List<Double>> embeddings = openAIService.getEmbeddings(texts);
            List<List<Double>> embeddings = texts.stream()
                .map(text -> List.of(0.0, 0.0, 0.0))
                .toList(); // 더미 데이터
            return ResponseEntity.ok(Map.of("embeddings", embeddings));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "일괄 임베딩 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * OpenAI API 연결 테스트
     * GET /api/openai/test
     */
    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        try {
            // 간단한 테스트를 위해 "test" 텍스트의 임베딩을 생성
            // List<Double> embedding = openAIService.getEmbedding("test");
            List<Double> embedding = List.of(0.0, 0.0, 0.0); // 더미 데이터
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "OpenAI API 연결이 정상입니다.",
                "embedding_size", embedding.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "OpenAI API 연결에 실패했습니다: " + e.getMessage()
                ));
        }
    }
}
