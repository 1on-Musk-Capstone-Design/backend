package com.capstone.global.dto;

import java.time.Instant;
import java.util.Map;
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
  private Long workspaceId;
  private Long sessionId;
  private Long fromWorkspaceUserId;
  private Long toWorkspaceUserId;
  private String sdp;
  private String candidate;
  private String sdpMid;
  private Integer sdpMLineIndex;
  private String clientMessageId;
  private Map<String, Object> payload;
  private Instant sentAt;

  public enum SignalType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    LEAVE,
    GET_ROUTER_RTP_CAPABILITIES,
    CREATE_TRANSPORT,
    CONNECT_TRANSPORT,
    PRODUCE,
    CONSUME
  }
}
