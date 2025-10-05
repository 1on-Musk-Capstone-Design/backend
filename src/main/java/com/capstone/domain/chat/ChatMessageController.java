package com.capstone.domain.chat;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * 파일 업로드 (로컬 스토리지 예시)
     * POST /api/v1/chat/messages/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ChatMessageDtos.Response> uploadFile(
            @RequestParam("workspaceId") Long workspaceId,
            @RequestParam("userId") String userId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        if (workspaceId == null || userId == null || file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 실제 서비스에서는 S3 등 외부 스토리지 사용 권장. 여기서는 간단히 URL만 구성
        String fileUrl = "/files/" + file.getOriginalFilename();

        ChatMessage saved = chatMessageService.saveFileMessage(
                workspaceId,
                userId,
                content,
                file.getContentType() != null && file.getContentType().startsWith("image/") ? "image" : "file",
                fileUrl,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );

        ChatMessageDtos.Response resp = new ChatMessageDtos.Response();
        resp.setMessageId(saved.getMessageId());
        resp.setWorkspaceId(saved.getWorkspaceId());
        resp.setUserId(saved.getUserId());
        resp.setContent(saved.getContent());
        resp.setMessageType(saved.getMessageType());
        resp.setFileUrl(saved.getFileUrl());
        resp.setFileName(saved.getFileName());
        resp.setMimeType(saved.getMimeType());
        resp.setFileSize(saved.getFileSize());
        resp.setCreatedAt(saved.getCreatedAt());

        return ResponseEntity.ok(resp);
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
        response.setMessageType(savedMessage.getMessageType());
        response.setFileUrl(savedMessage.getFileUrl());
        response.setFileName(savedMessage.getFileName());
        response.setMimeType(savedMessage.getMimeType());
        response.setFileSize(savedMessage.getFileSize());
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
