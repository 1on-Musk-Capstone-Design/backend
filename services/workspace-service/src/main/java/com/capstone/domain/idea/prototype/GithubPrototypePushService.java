package com.capstone.domain.idea.prototype;

import com.capstone.global.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubPrototypePushService {

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

  private final AppProperties appProperties;
  private final ObjectMapper objectMapper;

  /**
   * GitHub에 저장소를 만들고 파일을 푸시합니다. 실패 시 null을 반환합니다.
   */
  public String pushFiles(String repoName, Map<String, String> files) {
    AppProperties.Prototype p = appProperties.getPrototype();
    String token = p.getGithubToken();
    if (token == null || token.isBlank()) {
      return null;
    }
    try {
      String owner = resolveOwner(token);
      if (owner == null) {
        return null;
      }
      if (!createRepository(token, repoName)) {
        return null;
      }
      for (Map.Entry<String, String> e : files.entrySet()) {
        putFile(token, owner, repoName, e.getKey(), e.getValue());
      }
      return "https://github.com/%s/%s".formatted(owner, repoName);
    } catch (Exception e) {
      log.warn("GitHub push failed: {}", e.getMessage());
      return null;
    }
  }

  private String resolveOwner(String token) throws Exception {
    String configured = appProperties.getPrototype().getGithubOwner();
    if (configured != null && !configured.isBlank()) {
      return configured.trim();
    }
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/user"))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build();
    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() >= 300) {
      log.warn("GitHub /user status {}", res.statusCode());
      return null;
    }
    JsonNode n = objectMapper.readTree(res.body());
    return n.path("login").asText(null);
  }

  private boolean createRepository(String token, String name) throws Exception {
    String body =
        objectMapper
            .createObjectNode()
            .put("name", name)
            .put("private", false)
            .put("auto_init", false)
            .toString();
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/user/repos"))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() == 422) {
      // 이미 존재하면 성공으로 간주
      return true;
    }
    if (res.statusCode() >= 300) {
      log.warn("GitHub create repo status {} body {}", res.statusCode(), res.body());
      return false;
    }
    return true;
  }

  private void putFile(String token, String owner, String repo, String path, String content)
      throws Exception {
    String b64 = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    String body =
        objectMapper
            .createObjectNode()
            .put("message", "Add " + path)
            .put("content", b64)
            .toString();
    String url =
        "https://api.github.com/repos/%s/%s/contents/%s"
            .formatted(owner, repo, path.replace(" ", "%20"));
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .build();
    HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() >= 300) {
      log.warn("GitHub put file {} status {} {}", path, res.statusCode(), res.body());
    }
  }
}
