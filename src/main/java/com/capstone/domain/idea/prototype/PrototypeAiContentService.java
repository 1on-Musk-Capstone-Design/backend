package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OpenAI가 설정된 경우 PRD·UI 구조를 LLM으로 생성하고, 실패 시 템플릿으로 폴백합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrototypeAiContentService {

  private final OpenAiHolder openAiHolder;
  private final PrdPrototypeGenerator templatePrd;
  private final UiStructureGenerator templateUi;
  private final ObjectMapper objectMapper;

  @Value("${openai.prototype-model:gpt-4o-mini}")
  private String prototypeModel;

  public String generatePrd(Idea idea) {
    if (!openAiHolder.isEnabled()) {
      log.warn(
          "PRD: OpenAI 비활성(api-key 없음/형식 불일치) — 템플릿만 사용합니다. "
              + "src/main/resources/application-local.yml 또는 Capstone 루트 local-application.yml, "
              + "환경변수 OPENAI_API_KEY 를 확인하세요.");
      return templatePrd.generate(idea);
    }
    try {
      String raw =
          chat(
              List.of(
                  Map.of(
                      "role",
                      "system",
                      "content",
                      """
                      당신은 제품 기획자입니다. 입력은 한 워크스페이스에서 모은 하나 이상의 아이디어/카드일 수 있습니다.
                      내용이 여러 조각이면 연관성을 파악하고 중복을 정리한 뒤, 하나의 제품·서비스로 통합하여
                      한국어 PRD(제품 요구사항 문서)를 Markdown으로 작성합니다. 표·목록을 활용하고,
                      배경, 문제, 목표, 사용자, 범위(MVP), 비기능 요구사항, 리스크, 성공 지표 섹션을 포함합니다.
                      입력이 키워드 나열 형태면:
                      1) 키워드를 도메인/문제/사용자/기능/제약으로 재분류
                      2) 모호한 키워드는 합리적 가정을 명시
                      3) 단어 나열이 아닌 문장형 요구사항으로 재작성
                      4) 기능마다 사용자 가치와 수용 기준(acceptance criteria)을 구체화하세요.
                      섹션 마지막에는 "불확실성/추가 확인 필요" 항목을 추가하세요."""),
                  Map.of(
                      "role",
                      "user",
                      "content", buildPrdUserPrompt(idea))));
      if (raw != null && !raw.isBlank()) {
        String out = raw.trim();
        log.info("PRD: OpenAI 응답 사용 (모델={}, {}자)", prototypeModel, out.length());
        return out;
      }
      log.warn("PRD: OpenAI 응답이 비어 있어 템플릿 사용");
      return templatePrd.generate(idea);
    } catch (Exception e) {
      log.warn(
          "LLM PRD 생성 실패, 템플릿 사용: {} ({})",
          e.getMessage(),
          e.getClass().getSimpleName());
      if (e.getMessage() != null
          && (e.getMessage().contains("401")
              || e.getMessage().toLowerCase().contains("invalid_api_key")
              || e.getMessage().toLowerCase().contains("incorrect api key"))) {
        log.warn("OpenAI 인증 실패 가능성: OPENAI_API_KEY 또는 local-application.yml 의 openai.api-key 를 확인하세요.");
      }
      return templatePrd.generate(idea);
    }
  }

  public String generateUiJson(Idea idea, String prdMarkdown) {
    if (!openAiHolder.isEnabled()) {
      log.warn("UI JSON: OpenAI 비활성 — 템플릿만 사용합니다.");
      return templateUi.generate(idea);
    }
    try {
      String raw =
          chat(
              List.of(
                  Map.of(
                      "role",
                      "system",
                      "content",
                      """
                      당신은 UX 정보구조 설계자입니다. 아래 아이디어와 PRD를 참고해
                      단일 페이지 랜딩 프로토타입용 UI 정보구조를 JSON만 출력합니다.
                      스키마: {"version":1,"appName":string,"routes":[{"path":"/","title":string,
                      "sections":[{"type":"hero|features|cta","title":string,"bullets":string[]}]}],
                      "ideaSnippet":string}
                      JSON 이외의 텍스트는 출력하지 마세요."""),
                  Map.of(
                      "role",
                      "user",
                      "content",
                      "아이디어:\n"
                          + nullToEmpty(idea.getContent())
                          + "\n\nPRD:\n"
                          + nullToEmpty(prdMarkdown))));
      String cleaned = stripJsonFence(raw);
      JsonNode node = objectMapper.readTree(cleaned);
      if (node != null && node.isObject()) {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        log.info("UI JSON: OpenAI 응답 사용 (모델={}, {}자)", prototypeModel, json.length());
        return json;
      }
      log.warn("UI JSON: OpenAI 응답이 유효한 JSON 객체가 아니어 템플릿 사용");
    } catch (Exception e) {
      log.warn(
          "LLM UI JSON 생성 실패, 템플릿 사용: {} ({})",
          e.getMessage(),
          e.getClass().getSimpleName());
    }
    return templateUi.generate(idea);
  }

  private String chat(List<Map<String, String>> messages) {
    return openAiHolder.getOpenAiChatService().createChatCompletion(prototypeModel, messages);
  }

  private static String buildPrdUserPrompt(Idea idea) {
    String original = nullToEmpty(idea.getContent());
    KeywordAnalysis analysis = analyzeKeywords(original);
    String modeHint =
        analysis.keywordLike
            ? "입력은 키워드 중심 메모입니다. 키워드의 연결관계를 먼저 해석한 뒤 PRD를 작성하세요."
            : "입력은 일반 문장/메모입니다. 중복을 제거하고 핵심을 통합해 PRD를 작성하세요.";

    StringBuilder sb = new StringBuilder();
    sb.append("아래는 아이디어 원문입니다(여러 카드가 구분선으로 이어져 있을 수 있음). 통합·요약한 하나의 PRD를 작성하세요.\n\n");
    sb.append("분석 힌트: ").append(modeHint).append('\n');
    sb.append("원문 토큰 수: ").append(analysis.tokenCount).append(", 평균 토큰 길이: ").append(analysis.avgTokenLength).append('\n');
    if (!analysis.topTokens.isEmpty()) {
      sb.append("핵심 토큰 후보: ").append(String.join(", ", analysis.topTokens)).append('\n');
    }
    sb.append('\n').append("원문:\n").append(original);
    return sb.toString();
  }

  private static KeywordAnalysis analyzeKeywords(String text) {
    if (text == null || text.isBlank()) {
      return new KeywordAnalysis(false, 0, 0, List.of());
    }
    String[] raw = text.split("[\\n,|;/•·]+");
    List<String> tokens = new ArrayList<>();
    int totalLen = 0;
    for (String r : raw) {
      String t = r.trim();
      if (t.isEmpty()) {
        continue;
      }
      tokens.add(t);
      totalLen += t.length();
    }
    int count = tokens.size();
    int avg = count == 0 ? 0 : totalLen / count;
    boolean keywordLike = count >= 6 && avg <= 12;
    List<String> top = tokens.stream().filter(t -> t.length() >= 2).distinct().limit(12).toList();
    return new KeywordAnalysis(keywordLike, count, avg, top);
  }

  private record KeywordAnalysis(
      boolean keywordLike, int tokenCount, int avgTokenLength, List<String> topTokens) {}

  private static String stripJsonFence(String raw) {
    if (raw == null) {
      return "";
    }
    String s = raw.trim();
    if (s.startsWith("```")) {
      int start = s.indexOf('\n');
      int end = s.lastIndexOf("```");
      if (start > 0 && end > start) {
        return s.substring(start + 1, end).trim();
      }
    }
    return s;
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
