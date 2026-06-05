package com.capstone.domain.idea.prototype;

import com.capstone.global.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Vercel REST API로 생성 파일을 직접 배포하고, 실패 시 시뮬 URL로 폴백합니다. */
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
   * @param files 생성된 파일 맵 (경로 -> 내용)
   * @param jobId 작업 ID (시뮬 URL 경로용)
   * @param projectSlug Vercel 프로젝트/배포 이름 (소문자·하이픈)
   */
  public VercelResolution resolveUrls(Map<String, String> files, long jobId, String projectSlug) {
    String token = appProperties.getPrototype().getVercelToken();
    String teamId = appProperties.getPrototype().getVercelTeamId();
    String base = appProperties.getPrototype().getSimulatedBaseUrl();

    if (token != null && !token.isBlank()) {
      Optional<VercelResolution> fromApi =
          tryCreateDeploymentFromFiles(token, teamId, files, projectSlug);
      if (fromApi.isPresent()) {
        log.info("Vercel API 배포 성공: {}", fromApi.get().previewUrl());
        return fromApi.get();
      }
      log.warn("Vercel API 직접 배포 실패로 시뮬레이션 URL 폴백 (jobId={})", jobId);
    }

    String simulated = base.replaceAll("/$", "") + "/job/" + jobId;
    log.warn("Vercel 토큰/계정 연결이 없어 시뮬레이션 URL 반환 (jobId={})", jobId);
    return new VercelResolution(simulated + "/preview", simulated + "/production", true, false);
  }

  private Optional<VercelResolution> tryCreateDeploymentFromFiles(
      String token, String teamId, Map<String, String> files, String projectSlug) {
    if (files == null || files.isEmpty()) {
      log.warn("Vercel direct deploy skipped: files empty");
      return Optional.empty();
    }
    String name = projectSlug != null && !projectSlug.isBlank() ? projectSlug : "idea-prototype";

    try {
      var body = objectMapper.createObjectNode().put("name", name).put("target", "production");
      var projectSettings = body.putObject("projectSettings");
      projectSettings.put("framework", "vite");
      projectSettings.put("buildCommand", "npm run build");
      projectSettings.put("installCommand", "npm install");
      projectSettings.put("outputDirectory", "dist");
      var filesNode = body.putArray("files");
      files.forEach(
          (path, content) ->
              filesNode.add(
                  objectMapper
                      .createObjectNode()
                      .put("file", normalizePath(path))
                      .put("data", content == null ? "" : content)));

      StringBuilder url = new StringBuilder("https://api.vercel.com/v13/deployments");
      if (teamId != null && !teamId.isBlank()) {
        url.append("?teamId=").append(teamId.trim());
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
      return Optional.of(new VercelResolution(deploymentUrl, production, false, true));
    } catch (Exception e) {
      log.warn("Vercel deployment API error: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private static String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "index.html";
    }
    String p = path.replace("\\", "/");
    return p.startsWith("/") ? p.substring(1) : p;
  }

  private static String truncate(String s) {
    if (s == null) {
      return "";
    }
    return s.length() > 800 ? s.substring(0, 800) + "…" : s;
  }
}
