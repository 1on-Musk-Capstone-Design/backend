package com.capstone.domain.chat;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

public class ChatMessageDtos {

  /**
   * 채팅 메시지 전송 요청 DTO
   */
  @Setter
  @Getter
  public static class SendRequest {

    private Long workspaceId;
    private Long userId;
    private String content;

  }

  /**
   * 채팅 메시지 응답 DTO
   */
  @Setter
  @Getter
  public static class Response {

    private Long messageId;
    private Long workspaceId;
    private Long userId;
    private String content;
    private String messageType; // text | image | file
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private Instant createdAt;

  }

  /**
   * 파일/이미지 메시지 JSON 요청 DTO
   */
  @Setter
  @Getter
  public static class FileMessageRequest {

    private Long workspaceId;
    private Long userId;
    private String content; // optional
    private String messageType; // image | file
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Long fileSize;

  }

  /**
   * 채팅 메시지 목록 조회 응답 DTO
   */
  @Setter
  @Getter
  public static class ListResponse {

    private Long messageId;
    private Long userId;
    private String content;
    private Instant createdAt;

  }

  /**
   * 채팅 메시지 개수 조회 응답 DTO
   */
  @Setter
  @Getter
  public static class CountResponse {

    private Long workspaceId;
    private long messageCount;

  }
}
