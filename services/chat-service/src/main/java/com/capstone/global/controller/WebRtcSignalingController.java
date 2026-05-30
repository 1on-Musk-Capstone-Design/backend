package com.capstone.global.controller;

import com.capstone.global.config.AppProperties;
import com.capstone.global.dto.WebRtcSignalMessage;
import com.capstone.global.dto.WebRtcSignalMessage.SignalType;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.service.WebRtcSignalAuthorizationService;
import java.security.Principal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRtcSignalingController {

  private final AppProperties appProperties;
  private final SimpMessagingTemplate messagingTemplate;
  private final WebRtcSignalAuthorizationService webRtcSignalAuthorizationService;

  @MessageMapping("/webrtc/signal")
  public void relaySignal(@Payload WebRtcSignalMessage message, Principal principal) {
    validateSignalMessage(message);

    if (appProperties.getWebrtc().isSignalAuthSoftValidationEnabled()) {
      webRtcSignalAuthorizationService.validateAndLog(principal, message);
    }

    message.setSentAt(Instant.now());

    String destination = appProperties.getWebrtc().getSignalTopicPrefix() + "/"
        + message.getWorkspaceId()
        + "/" + appProperties.getWebrtc().getVoiceSessionSegment()
        + "/" + message.getSessionId()
        + "/" + appProperties.getWebrtc().getSignalSegment();

    messagingTemplate.convertAndSend(destination, message);

    log.debug(
        "WebRTC signal relayed: type={}, workspaceId={}, sessionId={}, from={}, to={}",
        message.getType(),
        message.getWorkspaceId(),
        message.getSessionId(),
        message.getFromWorkspaceUserId(),
        message.getToWorkspaceUserId()
    );
  }

  private void validateSignalMessage(WebRtcSignalMessage message) {
    if (message == null) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    if (message.getType() == null
        || message.getWorkspaceId() == null
        || message.getSessionId() == null
        || message.getFromWorkspaceUserId() == null) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    SignalType type = message.getType();

    if ((type == SignalType.OFFER || type == SignalType.ANSWER)
        && isBlank(message.getSdp())) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    if (type == SignalType.ICE_CANDIDATE && isBlank(message.getCandidate())) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    if (isSfuSignalType(type) && (message.getPayload() == null || message.getPayload().isEmpty())) {
      throw new CustomException(ErrorCode.BAD_REQUEST);
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private boolean isSfuSignalType(SignalType type) {
    return type == SignalType.GET_ROUTER_RTP_CAPABILITIES
        || type == SignalType.CREATE_TRANSPORT
        || type == SignalType.CONNECT_TRANSPORT
        || type == SignalType.PRODUCE
        || type == SignalType.CONSUME;
  }
}
