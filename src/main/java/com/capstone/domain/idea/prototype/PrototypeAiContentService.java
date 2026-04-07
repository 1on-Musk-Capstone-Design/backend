package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.List;
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
      return templatePrd.generate(idea);
    }
    try {
      String raw = chat(
          List.of(
              new ChatMessage(
                  ChatMessageRole.SYSTEM.value(),
                  """
                  당신은 제품 기획자입니다. 사용자의 아이디어를 바탕으로 한국어 PRD(제품 요구사항 문서)를
                  Markdown으로 작성합니다. 표·목록을 활용하고, 배경, 문제, 목표, 사용자, 범위(MVP),
                  비기능 요구사항, 리스크, 성공 지표 섹션을 포함합니다."""),
              new ChatMessage(
                  ChatMessageRole.USER.value(),
                  "아이디어 원문:\n" + nullToEmpty(idea.getContent()))));
      return raw != null && !raw.isBlank() ? raw.trim() : templatePrd.generate(idea);
    } catch (Exception e) {
      log.warn("LLM PRD 생성 실패, 템플릿 사용: {}", e.getMessage());
      return templatePrd.generate(idea);
    }
  }

  public String generateUiJson(Idea idea, String prdMarkdown) {
    if (!openAiHolder.isEnabled()) {
      return templateUi.generate(idea);
    }
    try {
      String raw =
          chat(
              List.of(
                  new ChatMessage(
                      ChatMessageRole.SYSTEM.value(),
                      """
                      당신은 UX 정보구조 설계자입니다. 아래 아이디어와 PRD를 참고해
                      단일 페이지 랜딩 프로토타입용 UI 정보구조를 JSON만 출력합니다.
                      스키마: {"version":1,"appName":string,"routes":[{"path":"/","title":string,
                      "sections":[{"type":"hero|features|cta","title":string,"bullets":string[]}]}],
                      "ideaSnippet":string}
                      JSON 이외의 텍스트는 출력하지 마세요."""),
                  new ChatMessage(
                      ChatMessageRole.USER.value(),
                      "아이디어:\n"
                          + nullToEmpty(idea.getContent())
                          + "\n\nPRD:\n"
                          + nullToEmpty(prdMarkdown))));
      String cleaned = stripJsonFence(raw);
      JsonNode node = objectMapper.readTree(cleaned);
      if (node != null && node.isObject()) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
      }
    } catch (Exception e) {
      log.warn("LLM UI JSON 생성 실패, 템플릿 사용: {}", e.getMessage());
    }
    return templateUi.generate(idea);
  }

  private String chat(List<ChatMessage> messages) {
    ChatCompletionRequest req =
        ChatCompletionRequest.builder()
            .model(prototypeModel)
            .messages(messages)
            .temperature(0.4)
            .maxTokens(4096)
            .build();
    ChatCompletionResult result = openAiHolder.getOpenAiService().createChatCompletion(req);
    if (result.getChoices() == null || result.getChoices().isEmpty()) {
      return null;
    }
    return result.getChoices().get(0).getMessage().getContent();
  }

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
