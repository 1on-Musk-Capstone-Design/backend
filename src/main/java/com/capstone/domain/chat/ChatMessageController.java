package com.capstone.domain.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat/messages")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    /**
     * 채팅 메시지 전송
     * POST /api/v1/chat/messages
     */
    @PostMapping
    public ResponseEntity<ChatMessageDtos.Response> sendMessage(@RequestBody ChatMessageDtos.SendRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getWorkspaceId() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }

        ChatMessage savedMessage = chatMessageService.saveMessage(
            request.getWorkspaceId(),
            request.getUserId(),
            request.getContent().trim()
        );

        ChatMessageDtos.Response response = new ChatMessageDtos.Response();
        response.setMessageId(savedMessage.getMessageId());
        response.setWorkspaceId(savedMessage.getWorkspaceId());
        response.setUserId(savedMessage.getUserId());
        response.setContent(savedMessage.getContent());
        response.setCreatedAt(savedMessage.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 워크스페이스의 채팅 메시지 목록 조회
     * GET /api/v1/chat/messages/workspace/{workspaceId}
     */
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<ChatMessageDtos.Response>> getMessagesByWorkspace(@PathVariable Long workspaceId) {
        List<ChatMessage> messages = chatMessageService.getMessagesByWorkspace(workspaceId);
        
        List<ChatMessageDtos.Response> response = messages.stream()
                .map(message -> {
                    ChatMessageDtos.Response dto = new ChatMessageDtos.Response();
                    dto.setMessageId(message.getMessageId());
                    dto.setWorkspaceId(message.getWorkspaceId());
                    dto.setUserId(message.getUserId());
                    dto.setContent(message.getContent());
                    dto.setCreatedAt(message.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 워크스페이스의 최근 채팅 메시지 조회
     * GET /api/v1/chat/messages/workspace/{workspaceId}/recent?limit=10
     */
    @GetMapping("/workspace/{workspaceId}/recent")
    public ResponseEntity<List<ChatMessageDtos.Response>> getRecentMessages(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<ChatMessage> messages = chatMessageService.getRecentMessages(workspaceId, limit);
        
        List<ChatMessageDtos.Response> response = messages.stream()
                .map(message -> {
                    ChatMessageDtos.Response dto = new ChatMessageDtos.Response();
                    dto.setMessageId(message.getMessageId());
                    dto.setWorkspaceId(message.getWorkspaceId());
                    dto.setUserId(message.getUserId());
                    dto.setContent(message.getContent());
                    dto.setCreatedAt(message.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 사용자의 채팅 메시지 목록 조회
     * GET /api/v1/chat/messages/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatMessageDtos.Response>> getMessagesByUser(@PathVariable String userId) {
        List<ChatMessage> messages = chatMessageService.getMessagesByUser(userId);
        
        List<ChatMessageDtos.Response> response = messages.stream()
                .map(message -> {
                    ChatMessageDtos.Response dto = new ChatMessageDtos.Response();
                    dto.setMessageId(message.getMessageId());
                    dto.setWorkspaceId(message.getWorkspaceId());
                    dto.setUserId(message.getUserId());
                    dto.setContent(message.getContent());
                    dto.setCreatedAt(message.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 워크스페이스의 채팅 메시지 개수 조회
     * GET /api/v1/chat/messages/workspace/{workspaceId}/count
     */
    @GetMapping("/workspace/{workspaceId}/count")
    public ResponseEntity<ChatMessageDtos.CountResponse> getMessageCount(@PathVariable Long workspaceId) {
        long count = chatMessageService.getMessageCountByWorkspace(workspaceId);
        
        ChatMessageDtos.CountResponse response = new ChatMessageDtos.CountResponse();
        response.setWorkspaceId(workspaceId);
        response.setMessageCount(count);

        return ResponseEntity.ok(response);
    }
}
