package com.capstone.domain.idea.prototype;

import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code openai.api-key}가 유효할 때만 {@link OpenAiService}를 생성합니다.
 */
@Component
@Getter
public class OpenAiHolder {

  private final OpenAiService openAiService;

  public OpenAiHolder(@Value("${openai.api-key:}") String apiKey) {
    if (apiKey == null
        || apiKey.isBlank()
        || apiKey.contains("your-openai-api-key")
        || apiKey.equals("your-openai-api-key-here")) {
      this.openAiService = null;
    } else {
      this.openAiService = new OpenAiService(apiKey.trim(), Duration.ofSeconds(120));
    }
  }

  public boolean isEnabled() {
    return openAiService != null;
  }
}
