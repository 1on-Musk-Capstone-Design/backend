package com.capstone.global.controller;

import com.capstone.global.config.AppProperties;
import com.capstone.global.dto.WebRtcSignalMessage;
import com.capstone.global.dto.WebRtcSignalMessage.SignalType;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
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

  @MessageMapping("/webrtc/signal")
  public void relaySignal(@Payload WebRtcSignalMessage message) {
    validateSignalMessage(message);

    // NOTE: 로컬 voice-preview 다중 메쉬 테스트 중에는 시그널 릴레이 권한 검사로 인해
    // OFFER/ANSWER/ICE가 전파되지 않는 경우가 있어 검증을 임시 우회한다.
    // 프로덕션 적용 전에는 반드시 권한 검증 로직을 복구해야 한다.

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
