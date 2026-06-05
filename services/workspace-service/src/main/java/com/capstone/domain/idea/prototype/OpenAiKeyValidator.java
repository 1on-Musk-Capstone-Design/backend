package com.capstone.domain.idea.prototype;

/**
 * OpenAI API secret 키 형식 검사. 플레이스홀더·오타로 잘못된 호출을 줄입니다.
 */
public final class OpenAiKeyValidator {

  private OpenAiKeyValidator() {}

  /**
   * {@code sk-} 접두 + 충분한 길이의 ASCII 문자열. {@code sk-proj-} 등 프로젝트 키 포함.
   */
  public static boolean looksLikeOpenAiSecretKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    String k = key.trim();
    if (k.length() < 20) {
      return false;
    }
    if (!k.startsWith("sk-")) {
      return false;
    }
    // 프로젝트 키 등: 영숫자, 하이픈, 밑줄, 점(일부 환경)
    if (!k.matches("^sk-[A-Za-z0-9_.-]+$")) {
      return false;
    }
    String lower = k.toLowerCase();
    if (lower.contains("your-openai-api-key")
        || lower.contains("xxxx")
        || lower.contains("example")
        || lower.contains("dummy")
        || lower.contains("paste")
        || lower.contains("here")) {
      return false;
    }
    return true;
  }
}
