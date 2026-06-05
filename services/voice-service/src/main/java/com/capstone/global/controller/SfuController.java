package com.capstone.global.controller;

import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.service.SfuServerClientService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/webrtc/sfu")
public class SfuController {

  private final SfuServerClientService sfuServerClientService;

  @GetMapping("/health")
  public Map<String, Object> health() {
    ensureEnabled();
    return Map.of("enabled", sfuServerClientService.healthCheck());
  }

  @GetMapping("/workspaces/{workspaceId}/sessions/{sessionId}/router-capabilities")
  public Map<String, Object> getRouterCapabilities(@PathVariable Long workspaceId,
      @PathVariable Long sessionId) {
    ensureEnabled();
    return sfuServerClientService.getRouterRtpCapabilities(workspaceId, sessionId);
  }

  @PostMapping("/workspaces/{workspaceId}/sessions/{sessionId}/transports")
  public Map<String, Object> createTransport(@PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @RequestBody Map<String, Object> body) {
    ensureEnabled();
    return sfuServerClientService.createTransport(workspaceId, sessionId, requireString(body, "peerId"));
  }

  @PostMapping("/workspaces/{workspaceId}/sessions/{sessionId}/transports/{transportId}/connect")
  public Map<String, Object> connectTransport(@PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @PathVariable String transportId,
      @RequestBody Map<String, Object> body) {
    ensureEnabled();
    return sfuServerClientService.connectTransport(
        workspaceId,
        sessionId,
        transportId,
        requireString(body, "peerId"),
        requireMap(body, "dtlsParameters")
    );
  }

  @PostMapping("/workspaces/{workspaceId}/sessions/{sessionId}/producers")
  public Map<String, Object> produce(@PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @RequestBody Map<String, Object> body) {
    ensureEnabled();
    return sfuServerClientService.produce(
        workspaceId,
        sessionId,
        requireString(body, "transportId"),
        requireString(body, "peerId"),
        requireString(body, "kind"),
        requireMap(body, "rtpParameters"),
        requireOptionalMap(body, "appData")
    );
  }

  @GetMapping("/workspaces/{workspaceId}/sessions/{sessionId}/producers")
  public Map<String, Object> listProducers(@PathVariable Long workspaceId,
      @PathVariable Long sessionId) {
    ensureEnabled();
    return sfuServerClientService.listProducers(workspaceId, sessionId);
  }

  @PostMapping("/workspaces/{workspaceId}/sessions/{sessionId}/consumers")
  public Map<String, Object> consume(@PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @RequestBody Map<String, Object> body) {
    ensureEnabled();
    return sfuServerClientService.consume(
        workspaceId,
        sessionId,
        requireString(body, "transportId"),
        requireString(body, "peerId"),
        requireString(body, "producerId"),
        requireMap(body, "rtpCapabilities")
    );
  }

  @PostMapping("/workspaces/{workspaceId}/sessions/{sessionId}/consumers/{consumerId}/resume")
  public Map<String, Object> resumeConsumer(@PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @PathVariable String consumerId,
      @RequestBody Map<String, Object> body) {
    ensureEnabled();
    return sfuServerClientService.resumeConsumer(
        workspaceId,
        sessionId,
        consumerId,
        requireString(body, "peerId")
    );
  }

  @DeleteMapping("/workspaces/{workspaceId}/sessions/{sessionId}/peers/{peerId}")
  public Map<String, Object> closePeer(@PathVariable Long workspaceId,
      @PathVariable Long sessionId,
      @PathVariable String peerId) {
    ensureEnabled();
    return sfuServerClientService.closePeer(workspaceId, sessionId, peerId);
  }

  @DeleteMapping("/workspaces/{workspaceId}/sessions/{sessionId}")
  public Map<String, Object> closeRoom(@PathVariable Long workspaceId,
      @PathVariable Long sessionId) {
    ensureEnabled();
    return sfuServerClientService.closeRoom(workspaceId, sessionId);
  }

  private void ensureEnabled() {
    if (!sfuServerClientService.isEnabled()) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }
  }

  private String requireString(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }
    return String.valueOf(value);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> requireMap(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (!(value instanceof Map)) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> requireOptionalMap(Map<String, Object> body, String key) {
    Object value = body.get(key);
    if (value == null) {
      return Map.of();
    }
    if (!(value instanceof Map)) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }
    return (Map<String, Object>) value;
  }
}
