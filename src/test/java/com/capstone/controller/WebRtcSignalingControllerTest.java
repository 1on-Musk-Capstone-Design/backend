package com.capstone.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.capstone.domain.voicesessionUser.VoiceSessionUserService;
import com.capstone.global.controller.WebRtcSignalingController;
import com.capstone.global.dto.WebRtcSignalMessage;
import com.capstone.global.dto.WebRtcSignalMessage.SignalType;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebRtcSignalingController 단위 테스트")
class WebRtcSignalingControllerTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private VoiceSessionUserService voiceSessionUserService;

  @InjectMocks
  private WebRtcSignalingController webRtcSignalingController;

  @Test
  @DisplayName("성공 - 권한 검증 통과 시 시그널 릴레이")
  void relaySignal_success() {
    // Given
    WebRtcSignalMessage message = WebRtcSignalMessage.builder()
        .type(SignalType.OFFER)
        .workspaceId(1L)
        .sessionId(10L)
        .fromWorkspaceUserId(100L)
        .toWorkspaceUserId(200L)
        .sdp("v=0")
        .clientMessageId("msg-1")
        .build();

    // When
    webRtcSignalingController.relaySignal(message);

    // Then
    assertThat(message.getSentAt()).isNotNull();
    verify(voiceSessionUserService)
        .validateSignalingPermission(1L, 10L, 100L);
    verify(messagingTemplate)
        .convertAndSend("/topic/workspace/1/voice/10/signal", message);
  }

  @Test
  @DisplayName("실패 - 필수 필드 누락 시 BAD_REQUEST")
  void relaySignal_missingField_throwsBadRequest() {
    // Given
    WebRtcSignalMessage message = WebRtcSignalMessage.builder()
        .type(SignalType.ICE_CANDIDATE)
        .workspaceId(1L)
        .sessionId(10L)
        .candidate("candidate:1 1 udp 2122260223 192.168.0.2 54321 typ host")
        .build();

    // When & Then
    assertThatThrownBy(() -> webRtcSignalingController.relaySignal(message))
        .isInstanceOf(CustomException.class)
        .extracting(ex -> ((CustomException) ex).getErrorCode())
        .isEqualTo(ErrorCode.BAD_REQUEST);

    verify(voiceSessionUserService, never())
        .validateSignalingPermission(anyLong(), anyLong(), anyLong());
    verify(messagingTemplate, never())
        .convertAndSend(anyString(), org.mockito.ArgumentMatchers.<Object>any());
  }

  @Test
  @DisplayName("실패 - 권한 검증 실패 시 브로드캐스트하지 않음")
  void relaySignal_permissionDenied_noBroadcast() {
    // Given
    WebRtcSignalMessage message = WebRtcSignalMessage.builder()
        .type(SignalType.ANSWER)
        .workspaceId(1L)
        .sessionId(10L)
        .fromWorkspaceUserId(999L)
        .sdp("v=0")
        .build();

    doThrow(new CustomException(ErrorCode.FORBIDDEN_WORKSPACE_ACCESS))
        .when(voiceSessionUserService)
        .validateSignalingPermission(1L, 10L, 999L);

    // When & Then
    assertThatThrownBy(() -> webRtcSignalingController.relaySignal(message))
        .isInstanceOf(CustomException.class)
        .extracting(ex -> ((CustomException) ex).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN_WORKSPACE_ACCESS);

    verify(voiceSessionUserService)
        .validateSignalingPermission(1L, 10L, 999L);
    verify(messagingTemplate, never())
        .convertAndSend(anyString(), org.mockito.ArgumentMatchers.<Object>any());
  }
}
