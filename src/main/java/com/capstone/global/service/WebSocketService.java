package com.capstone.global.service;

import com.capstone.domain.chat.ChatMessage;
import com.capstone.domain.chat.ChatMessageService;
import com.capstone.domain.chat.ChatMessageDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class WebSocketService {

  private final ChatMessageService chatMessageService;
  private final SimpMessagingTemplate messagingTemplate;

  // 채팅 메시지 수신 및 브로드캐스트
  @MessageMapping("/chat/message")
  public void handleChatMessage(@Payload ChatMessageDtos.SendRequest request) {
    log.info("=== 채팅 메시지 수신 시작 ===");
    log.info("요청 데이터: workspaceId={}, userId={}, content={}", 
        request != null ? request.getWorkspaceId() : "null",
        request != null ? request.getUserId() : "null",
        request != null ? request.getContent() : "null");
    
    try {
      if (request == null) {
        log.error("요청 데이터가 null입니다.");
        return;
      }
      
      if (request.getWorkspaceId() == null) {
        log.error("workspaceId가 null입니다.");
        return;
      }
      
      if (request.getUserId() == null) {
        log.error("userId가 null입니다.");
        return;
      }
      
      log.info("메시지 저장 시작 - workspaceId: {}, userId: {}", 
          request.getWorkspaceId(), request.getUserId());
      
      // 메시지를 데이터베이스에 저장
      ChatMessage savedMessage = chatMessageService.saveMessage(
          request.getWorkspaceId(),
          request.getUserId(),
          request.getContent()
      );

      log.info("메시지 저장 완료 - messageId: {}", savedMessage.getMessageId());

      // 응답 DTO 생성
      ChatMessageDtos.Response response = new ChatMessageDtos.Response();
      response.setMessageId(savedMessage.getMessageId());
      response.setWorkspaceId(savedMessage.getWorkspaceId());
      response.setUserId(savedMessage.getUser().getId());
      response.setContent(savedMessage.getContent());
      response.setMessageType(savedMessage.getMessageType());
      response.setFileUrl(savedMessage.getFileUrl());
      response.setFileName(savedMessage.getFileName());
      response.setMimeType(savedMessage.getMimeType());
      response.setFileSize(savedMessage.getFileSize());
      response.setCreatedAt(savedMessage.getCreatedAt());

      String broadcastPath = "/topic/workspace/" + request.getWorkspaceId() + "/messages";
      log.info("브로드캐스트 시작 - 경로: {}", broadcastPath);
      log.info("브로드캐스트 데이터: {}", response);

      // 해당 워크스페이스의 모든 클라이언트에게 브로드캐스트
      messagingTemplate.convertAndSend(broadcastPath, response);
      
      log.info("브로드캐스트 완료 - 경로: {}", broadcastPath);
      log.info("=== 채팅 메시지 처리 완료 ===");
    } catch (Exception e) {
      log.error("채팅 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
      log.error("스택 트레이스:", e);
    }
  }

  // 파일 메시지 수신 및 브로드캐스트
  @MessageMapping("/chat/file")
  public void handleFileMessage(@Payload Map<String, Object> payload) {
    try {
      Long workspaceId = payload.get("workspaceId") == null ? null
          : Long.valueOf(payload.get("workspaceId").toString());
      
      if (workspaceId != null) {
        Long userId = payload.get("userId") == null ? null
            : Long.valueOf(payload.get("userId").toString());
        String content = payload.get("content") == null ? null : payload.get("content").toString();
        String messageType = (String) payload.getOrDefault("messageType", "file");
        String fileUrl = (String) payload.get("fileUrl");
        String fileName = (String) payload.get("fileName");
        String mimeType = (String) payload.get("mimeType");
        Long fileSize = payload.get("fileSize") == null ? null
            : Long.valueOf(payload.get("fileSize").toString());

        ChatMessage saved = chatMessageService.saveFileMessage(
            workspaceId,
            userId,
            content,
            messageType,
            fileUrl,
            fileName,
            mimeType,
            fileSize
        );

        ChatMessageDtos.Response response = new ChatMessageDtos.Response();
        response.setMessageId(saved.getMessageId());
        response.setWorkspaceId(saved.getWorkspaceId());
        response.setUserId(saved.getUser().getId());
        response.setContent(saved.getContent());
        response.setMessageType(saved.getMessageType());
        response.setFileUrl(saved.getFileUrl());
        response.setFileName(saved.getFileName());
        response.setMimeType(saved.getMimeType());
        response.setFileSize(saved.getFileSize());
        response.setCreatedAt(saved.getCreatedAt());

        // 해당 워크스페이스의 모든 클라이언트에게 브로드캐스트
        messagingTemplate.convertAndSend(
            "/topic/workspace/" + workspaceId + "/messages",
            response
        );
      }
    } catch (Exception e) {
      log.error("파일 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  // 워크스페이스 참여 알림
  public void notifyUserJoined(Long workspaceId, String message) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/users",
        message
    );
  }

  // 워크스페이스 나가기 알림
  public void notifyUserLeft(Long workspaceId, String message) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/users",
        message
    );
  }

  // 아이디어 업데이트 브로드캐스트
  public void broadcastIdeaUpdate(Long workspaceId, Object data) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/ideas",
        data
    );
  }

  // 음성 채팅 참여 알림
  public void notifyVoiceUserJoined(Long workspaceId, String message) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/voice",
        message
    );
  }

  // 음성 채팅 나가기 알림
  public void notifyVoiceUserLeft(Long workspaceId, String message) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/voice",
        message
    );
  }

  // 워크스페이스 참여 이벤트 (클라이언트에서 직접 전송)
  @MessageMapping("/workspace/join")
  public void handleWorkspaceJoin(@Payload Map<String, Object> payload) {
    try {
      Long workspaceId = payload.get("workspaceId") == null ? null
          : Long.valueOf(payload.get("workspaceId").toString());
      if (workspaceId != null) {
        messagingTemplate.convertAndSend(
            "/topic/workspace/" + workspaceId + "/users",
            Map.of("type", "user_joined", "message", "사용자가 워크스페이스에 참여했습니다.")
        );
      }
    } catch (Exception e) {
      log.error("워크스페이스 참여 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  // 워크스페이스 나가기 이벤트 (클라이언트에서 직접 전송)
  @MessageMapping("/workspace/leave")
  public void handleWorkspaceLeave(@Payload Map<String, Object> payload) {
    try {
      Long workspaceId = payload.get("workspaceId") == null ? null
          : Long.valueOf(payload.get("workspaceId").toString());
      if (workspaceId != null) {
        messagingTemplate.convertAndSend(
            "/topic/workspace/" + workspaceId + "/users",
            Map.of("type", "user_left", "message", "사용자가 워크스페이스를 떠났습니다.")
        );
      }
    } catch (Exception e) {
      log.error("워크스페이스 나가기 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  // 아이디어 업데이트 이벤트 (클라이언트에서 직접 전송)
  @MessageMapping("/idea/update")
  public void handleIdeaUpdate(@Payload Map<String, Object> payload) {
    try {
      Long workspaceId = payload.get("workspaceId") == null ? null
          : Long.valueOf(payload.get("workspaceId").toString());
      if (workspaceId != null) {
        messagingTemplate.convertAndSend(
            "/topic/workspace/" + workspaceId + "/ideas",
            payload
        );
      }
    } catch (Exception e) {
      log.error("아이디어 업데이트 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  // 음성 채팅 참여 이벤트 (클라이언트에서 직접 전송)
  @MessageMapping("/voice/join")
  public void handleVoiceJoin(@Payload Map<String, Object> payload) {
    try {
      Long workspaceId = payload.get("workspaceId") == null ? null
          : Long.valueOf(payload.get("workspaceId").toString());
      if (workspaceId != null) {
        messagingTemplate.convertAndSend(
            "/topic/workspace/" + workspaceId + "/voice",
            Map.of("type", "voice_user_joined", "message", "사용자가 음성 채팅에 참여했습니다.")
        );
      }
    } catch (Exception e) {
      log.error("음성 채팅 참여 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  // 음성 채팅 나가기 이벤트 (클라이언트에서 직접 전송)
  @MessageMapping("/voice/leave")
  public void handleVoiceLeave(@Payload Map<String, Object> payload) {
    try {
      Long workspaceId = payload.get("workspaceId") == null ? null
          : Long.valueOf(payload.get("workspaceId").toString());
      if (workspaceId != null) {
        messagingTemplate.convertAndSend(
            "/topic/workspace/" + workspaceId + "/voice",
            Map.of("type", "voice_user_left", "message", "사용자가 음성 채팅에서 나갔습니다.")
        );
      }
    } catch (Exception e) {
      log.error("음성 채팅 나가기 처리 중 오류 발생: {}", e.getMessage(), e);
    }
  }

  // 캔버스 생성/수정/삭제 브로드캐스트
  public void broadcastCanvasChange(Long workspaceId, String action, Object data) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/canvas",
        Map.of("action", action, "data", data)
    );
  }

  // 아이디어 생성/수정/삭제 브로드캐스트
  public void broadcastIdeaChange(Long workspaceId, String action, Object data) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/ideas",
        Map.of("action", action, "data", data)
    );
  }

  // 워크스페이스 변경 브로드캐스트
  public void broadcastWorkspaceChange(Long workspaceId, String action, Object data) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/workspace",
        Map.of("action", action, "data", data)
    );
  }

  // 특정 워크스페이스에 메시지 브로드캐스트
  public void broadcastToWorkspace(Long workspaceId, String destination, Object data) {
    messagingTemplate.convertAndSend(
        "/topic/workspace/" + workspaceId + "/" + destination,
        data
    );
  }
}

