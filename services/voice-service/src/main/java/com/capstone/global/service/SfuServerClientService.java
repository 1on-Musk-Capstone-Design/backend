package com.capstone.global.service;

import com.capstone.global.config.AppProperties;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class SfuServerClientService {

  private final RestTemplate restTemplate;
  private final AppProperties appProperties;

  public boolean isEnabled() {
    return appProperties.getWebrtc() != null && appProperties.getWebrtc().isSfuEnabled();
  }

  public boolean healthCheck() {
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(buildUri(getHealthPath()), String.class);
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      log.warn("SFU server health check failed: {}", e.getMessage());
      return false;
    }
  }

  public Map<String, Object> getRouterRtpCapabilities(Long workspaceId, Long sessionId) {
    ResponseEntity<Map> response = restTemplate.getForEntity(
        buildRoomUri(workspaceId, sessionId, "/router-capabilities"),
        Map.class
    );
    return response.getBody();
  }

  public Map<String, Object> createTransport(Long workspaceId, Long sessionId, String peerId) {
    return post(
        buildRoomUri(workspaceId, sessionId, "/transports"),
        requestBody("peerId", peerId),
        Map.class
    );
  }

  public Map<String, Object> connectTransport(Long workspaceId, Long sessionId, String transportId,
      String peerId, Map<String, Object> dtlsParameters) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("peerId", peerId);
    body.put("dtlsParameters", dtlsParameters);
    return post(
        buildRoomUri(workspaceId, sessionId, "/transports/" + transportId + "/connect"),
        body,
        Map.class
    );
  }

  public Map<String, Object> produce(Long workspaceId, Long sessionId, String transportId, String peerId,
      String kind, Map<String, Object> rtpParameters, Map<String, Object> appData) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("peerId", peerId);
    body.put("transportId", transportId);
    body.put("kind", kind);
    body.put("rtpParameters", rtpParameters);
    body.put("appData", appData == null ? Map.of() : appData);
    return post(
        buildRoomUri(workspaceId, sessionId, "/producers"),
        body,
        Map.class
    );
  }

  public Map<String, Object> listProducers(Long workspaceId, Long sessionId) {
    ResponseEntity<Map> response = restTemplate.getForEntity(
        buildRoomUri(workspaceId, sessionId, "/producers"),
        Map.class
    );
    return response.getBody();
  }

  public Map<String, Object> consume(Long workspaceId, Long sessionId, String transportId, String peerId,
      String producerId, Map<String, Object> rtpCapabilities) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("peerId", peerId);
    body.put("transportId", transportId);
    body.put("producerId", producerId);
    body.put("rtpCapabilities", rtpCapabilities);
    return post(
        buildRoomUri(workspaceId, sessionId, "/consumers"),
        body,
        Map.class
    );
  }

  public Map<String, Object> resumeConsumer(Long workspaceId, Long sessionId, String consumerId, String peerId) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("peerId", peerId);
    return post(
        buildRoomUri(workspaceId, sessionId, "/consumers/" + consumerId + "/resume"),
        body,
        Map.class
    );
  }

  public Map<String, Object> closePeer(Long workspaceId, Long sessionId, String peerId) {
    ResponseEntity<Map> response = restTemplate.exchange(
        buildRoomUri(workspaceId, sessionId, "/peers/" + peerId),
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        Map.class
    );
    return response.getBody();
  }

  public Map<String, Object> closeRoom(Long workspaceId, Long sessionId) {
    ResponseEntity<Map> response = restTemplate.exchange(
        buildRoomUri(workspaceId, sessionId, ""),
        HttpMethod.DELETE,
        HttpEntity.EMPTY,
        Map.class
    );
    return response.getBody();
  }

  public String getBaseUrl() {
    return normalizeBaseUrl(appProperties.getWebrtc().getSfuServer().getBaseUrl());
  }

  public String getHealthPath() {
    return appProperties.getWebrtc().getSfuServer().getHealthPath();
  }

  public String getRoomsPath() {
    return appProperties.getWebrtc().getSfuServer().getRoomsPath();
  }

  private <T> T post(URI uri, Object body, Class<T> responseType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<T> response = restTemplate.postForEntity(uri, new HttpEntity<>(body, headers), responseType);
    return response.getBody();
  }

  private URI buildRoomUri(Long workspaceId, Long sessionId, String suffix) {
    return UriComponentsBuilder.fromHttpUrl(getBaseUrl())
        .path(getRoomsPath())
        .pathSegment(String.valueOf(workspaceId), "sessions", String.valueOf(sessionId))
        .path(suffix)
        .build()
        .toUri();
  }

  private URI buildUri(String path) {
    return UriComponentsBuilder.fromHttpUrl(getBaseUrl())
        .path(path)
        .build()
        .toUri();
  }

  private Map<String, Object> requestBody(String key, Object value) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put(key, value);
    return body;
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return "http://localhost:4000";
    }
    return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }
}
