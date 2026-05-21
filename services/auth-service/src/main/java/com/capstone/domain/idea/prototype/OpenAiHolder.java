package com.capstone.domain.idea.prototype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OpenAI 연동 진입점(하위 호환). 실제 호출은 {@link OpenAiChatService} 가 수행합니다.
 *
 * <p>이전 theokanning 기반 {@code OpenAiService} 는 제거되었으며, REST 직접 호출로 대체되었습니다.
 */
@Component
@Getter
@RequiredArgsConstructor
public class OpenAiHolder {

  private final OpenAiChatService openAiChatService;

  public boolean isEnabled() {
    return openAiChatService.isEnabled();
  }
}
