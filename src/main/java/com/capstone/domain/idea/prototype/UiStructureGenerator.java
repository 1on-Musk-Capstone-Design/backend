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
    sections.addObject().put("type", "hero").put("title", "Hero");
    sections.addObject().put("type", "features").put("title", "핵심 가치");
    sections.addObject().put("type", "cta").put("title", "다음 단계");
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
