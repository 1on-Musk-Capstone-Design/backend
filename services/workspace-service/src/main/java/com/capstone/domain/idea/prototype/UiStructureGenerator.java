package com.capstone.domain.idea.prototype;

import com.capstone.domain.idea.Idea;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UiStructureGenerator {

  private final ObjectMapper objectMapper;

  public String generate(Idea idea) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("version", 1);
    root.put("appName", "IdeaPrototype");
    ArrayNode routes = root.putArray("routes");
    ObjectNode home = routes.addObject();
    home.put("path", "/");
    home.put("title", "Home");
    ArrayNode sections = home.putArray("sections");
    sections
        .addObject()
        .put("type", "hero")
        .put("title", "제품 랜딩")
        .put("subtitle", "OpenAI 키가 있으면 PRD·이 화면이 자동으로 채워집니다.")
        .putArray("bullets")
        .add("핵심 가치·기능·CTA는 PRD와 IA JSON에서 옵니다.");
    sections
        .addObject()
        .put("type", "features")
        .put("title", "핵심 가치")
        .putArray("bullets")
        .add("사용자 문제 정의")
        .add("제안 솔루션")
        .add("다음 액션");
    sections
        .addObject()
        .put("type", "cta")
        .put("title", "다음 단계")
        .putArray("bullets")
        .add("시작하기")
        .add("문의·협의가 필요하면 팀에 공유하세요.");
    root.put("ideaSnippet", truncate(idea.getContent(), 200));
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    } catch (JsonProcessingException e) {
      return root.toString();
    }
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
