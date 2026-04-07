package com.capstone.domain.idea.prototype;

import com.capstone.global.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Vercel REST API로 GitHub 연동 배포를 시도하고, 실패 시 Import URL 또는 시뮬 URL로 폴백합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VercelPrototypeDeployService {

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

  private final AppProperties appProperties;
  private final ObjectMapper objectMapper;

  public record VercelResolution(
      String previewUrl, String productionUrl, boolean simulated, boolean deploymentApiUsed) {}

  /**
   * @param githubRepoUrl GitHub 저장소 HTTPS URL (없으면 시뮬)
   * @param jobId 작업 ID (시뮬 URL 경로용)
   * @param projectSlug Vercel 프로젝트/배포 이름 (소문자·하이픈)
   */
  public VercelResolution resolveUrls(String githubRepoUrl, long jobId, String projectSlug) {
    String token = appProperties.getPrototype().getVercelToken();
    String teamId = appProperties.getPrototype().getVercelTeamId();
    String base = appProperties.getPrototype().getSimulatedBaseUrl();

    if (githubRepoUrl != null
        && !githubRepoUrl.isBlank()
        && token != null
        && !token.isBlank()) {
      Optional<VercelResolution> fromApi =
          tryCreateDeployment(token, teamId, githubRepoUrl.trim(), projectSlug);
      if (fromApi.isPresent()) {
        return fromApi.get();
      }
    }

    if (githubRepoUrl != null && !githubRepoUrl.isBlank()) {
      String repo = githubRepoUrl.trim();
      String importUrl =
          "https://vercel.com/new/clone?repository-url="
              + URLEncoder.encode(repo, StandardCharsets.UTF_8);
      return new VercelResolution(importUrl, importUrl, false, false);
    }

    if (token != null && !token.isBlank()) {
      String simulated = base.replaceAll("/$", "") + "/job/" + jobId;
      return new VercelResolution(simulated + "/preview", simulated + "/production", true, false);
    }

    String simulated = base.replaceAll("/$", "") + "/job/" + jobId;
    return new VercelResolution(simulated + "/preview", simulated + "/production", true, false);
  }

  private Optional<VercelResolution> tryCreateDeployment(
      String token, String teamId, String githubRepoUrl, String projectSlug) {
    Optional<GithubRepoRef.Record> ref = GithubRepoRef.parse(githubRepoUrl);
    if (ref.isEmpty()) {
      return Optional.empty();
    }
    GithubRepoRef.Record r = ref.get();
    String name = projectSlug != null && !projectSlug.isBlank() ? projectSlug : r.repo();

    try {
      var body =
          objectMapper
              .createObjectNode()
              .put("name", name)
              .put("project", name)
              .put("target", "production")
              .set(
                  "gitSource",
                  objectMapper
                      .createObjectNode()
                      .put("type", "github")
                      .put("ref", "main")
                      .put("org", r.org())
                      .put("repo", r.repo()));

      StringBuilder url = new StringBuilder("https://api.vercel.com/v13/deployments");
      if (teamId != null && !teamId.isBlank()) {
        url.append("?teamId=").append(URLEncoder.encode(teamId.trim(), StandardCharsets.UTF_8));
      }

      HttpRequest req =
          HttpRequest.newBuilder()
              .uri(URI.create(url.toString()))
              .header("Authorization", "Bearer " + token.trim())
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
              .timeout(Duration.ofMinutes(2))
              .build();

      HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() < 200 || res.statusCode() >= 300) {
        log.warn(
            "Vercel deployment API failed status={} body={}", res.statusCode(), truncate(res.body()));
        return Optional.empty();
      }

      JsonNode root = objectMapper.readTree(res.body());
      String deploymentUrl = root.path("url").asText(null);
      if (deploymentUrl == null || deploymentUrl.isBlank()) {
        log.warn("Vercel response missing url: {}", truncate(res.body()));
        return Optional.empty();
      }
      if (!deploymentUrl.startsWith("http")) {
        deploymentUrl = "https://" + deploymentUrl;
      }
      String alias =
          root.path("alias").isArray() && root.path("alias").size() > 0
              ? root.path("alias").get(0).asText(null)
              : null;
      String production = alias != null && alias.startsWith("http") ? alias : deploymentUrl;
      return Optional.of(
          new VercelResolution(deploymentUrl, production, false, true));
    } catch (Exception e) {
      log.warn("Vercel deployment API error: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private static String truncate(String s) {
    if (s == null) {
      return "";
    }
    return s.length() > 800 ? s.substring(0, 800) + "…" : s;
  }
}
