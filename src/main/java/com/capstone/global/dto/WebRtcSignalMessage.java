package com.capstone.global.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebRtcSignalMessage {

  private SignalType type;

  // 라우팅 키
  private Long workspaceId;
  private Long sessionId;

  // 송신/수신자 (수신자는 null이면 세션 브로드캐스트)
  private Long fromWorkspaceUserId;
  private Long toWorkspaceUserId;

  // SDP payload
  private String sdp;

  // ICE payload
  private String candidate;
  private String sdpMid;
  private Integer sdpMLineIndex;

  // 클라이언트 중복 처리용 식별자
  private String clientMessageId;

  private Instant sentAt;

  public enum SignalType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    LEAVE
  }
}
