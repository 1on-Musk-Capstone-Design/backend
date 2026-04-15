package com.capstone.domain.idea.prototype;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * OpenAI Chat Completions REST API 직접 호출 ({@code /v1/chat/completions}).
 * 구식 theokanning 클라이언트 대신 사용해 최신 API와 호환됩니다.
 */
@Slf4j
@Service
public class OpenAiChatService {

  private static final String BASE = "https://api.openai.com/v1";

  private final WebClient webClient;

  @Getter private final boolean enabled;

  public OpenAiChatService(@Value("${openai.api-key:}") String apiKey) {
    String trimmed = apiKey == null ? "" : apiKey.trim();
    if (!OpenAiKeyValidator.looksLikeOpenAiSecretKey(trimmed)) {
      this.webClient = null;
      this.enabled = false;
      if (trimmed.isEmpty()) {
        log.info(
            "OpenAI 비활성화: openai.api-key / OPENAI_API_KEY 가 비어 있습니다. "
                + "Capstone 루트의 local-application.yml 에 openai.api-key: sk-... 를 넣거나 환경변수를 설정하세요.");
      } else {
        log.warn(
            "OpenAI 비활성화: api-key 형식이 올바르지 않습니다. platform.openai.com 에서 발급한 키인지 확인하세요.");
      }
    } else {
      this.enabled = true;
      this.webClient =
          WebClient.builder()
              .baseUrl(BASE)
              .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + trimmed)
              .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .build();
      log.info("OpenAI REST 클라이언트 활성화 (POST {}/chat/completions)", BASE);
    }
  }

  /**
   * @param model 예: gpt-4o-mini
   * @param messages OpenAI messages 배열 (role: system|user|assistant, content)
   */
  public String createChatCompletion(String model, List<Map<String, String>> messages) {
    if (!enabled || webClient == null) {
      throw new IllegalStateException("OpenAI is not configured");
    }
    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("messages", messages);
    body.put("temperature", 0.4);
    body.put("max_tokens", 4096);

    try {
      JsonNode root =
          webClient
              .post()
              .uri("/chat/completions")
              .bodyValue(body)
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block(Duration.ofSeconds(120));

      if (root == null) {
        return null;
      }
      JsonNode err = root.get("error");
      if (err != null && !err.isNull()) {
        String msg = err.path("message").asText("unknown error");
        log.error("OpenAI API error: {}", msg);
        throw new IllegalStateException("OpenAI API: " + msg);
      }
      JsonNode choices = root.get("choices");
      if (choices == null || !choices.isArray() || choices.isEmpty()) {
        return null;
      }
      JsonNode content = choices.get(0).path("message").path("content");
      if (content.isMissingNode() || content.isNull()) {
        return null;
      }
      return content.asText();
    } catch (WebClientResponseException e) {
      String bodyStr = e.getResponseBodyAsString();
      log.error(
          "OpenAI HTTP {}: {}",
          e.getStatusCode().value(),
          bodyStr != null && bodyStr.length() > 500 ? bodyStr.substring(0, 500) + "…" : bodyStr);
      throw new IllegalStateException(
          "OpenAI HTTP " + e.getStatusCode().value() + ": " + truncate(bodyStr, 300), e);
    }
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
