package com.capstone.controller;

import com.capstone.domain.chat.ChatMessage;
import com.capstone.domain.chat.ChatMessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.capstone.domain.chat.ChatMessageController.class)
@org.springframework.context.annotation.Import(com.capstone.config.TestSecurityConfig.class)
class ChatMessageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ChatMessageService chatMessageService;

  @Test
  void sendMessage_shouldReturnCreatedMessage() throws Exception {
    // Given
    ChatMessage mockMessage = new ChatMessage();
    mockMessage.setMessageId(1L);
    mockMessage.setWorkspaceId(1L);
    mockMessage.setUserId("user-123");
    mockMessage.setContent("안녕하세요!");
    mockMessage.setCreatedAt(Instant.now());

    when(chatMessageService.saveMessage(1L, "user-123", "안녕하세요!")).thenReturn(mockMessage);

    String requestBody = "{\"workspaceId\": 1, \"userId\": \"user-123\", \"content\": \"안녕하세요!\"}";

    // When & Then
    mockMvc.perform(post("/api/v1/chat/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messageId").value(1))
        .andExpect(jsonPath("$.workspaceId").value(1))
        .andExpect(jsonPath("$.userId").value("user-123"))
        .andExpect(jsonPath("$.content").value("안녕하세요!"))
        .andExpect(jsonPath("$.createdAt").exists());
  }

  @Test
  void createFileMessage_shouldReturnResponse() throws Exception {
    // Given
    ChatMessage saved = new ChatMessage();
    saved.setMessageId(10L);
    saved.setWorkspaceId(1L);
    saved.setUserId("user-123");
    saved.setContent("이미지 설명");
    saved.setMessageType("image");
    saved.setFileUrl("https://cdn.example.com/path/img.png");
    saved.setFileName("img.png");
    saved.setMimeType("image/png");
    saved.setFileSize(123456L);
    saved.setCreatedAt(Instant.now());

    org.mockito.Mockito.when(chatMessageService.saveFileMessage(
        1L, "user-123", "이미지 설명", "image",
        "https://cdn.example.com/path/img.png", "img.png", "image/png", 123456L
    )).thenReturn(saved);

    String body = "{\n  \"workspaceId\": 1,\n  \"userId\": \"user-123\",\n  \"content\": \"이미지 설명\",\n  \"messageType\": \"image\",\n  \"fileUrl\": \"https://cdn.example.com/path/img.png\",\n  \"fileName\": \"img.png\",\n  \"mimeType\": \"image/png\",\n  \"fileSize\": 123456\n}";

    // When & Then
    mockMvc.perform(post("/api/v1/chat/messages/file")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messageId").value(10))
        .andExpect(jsonPath("$.messageType").value("image"))
        .andExpect(jsonPath("$.fileName").value("img.png"))
        .andExpect(jsonPath("$.mimeType").value("image/png"))
        .andExpect(jsonPath("$.fileSize").value(123456));
  }

  @Test
  void sendMessage_withEmptyContent_shouldReturnBadRequest() throws Exception {
    // Given
    String requestBody = "{\"workspaceId\": 1, \"userId\": \"user-123\", \"content\": \"\"}";

    // When & Then
    mockMvc.perform(post("/api/v1/chat/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sendMessage_withNullContent_shouldReturnBadRequest() throws Exception {
    // Given
    String requestBody = "{\"workspaceId\": 1, \"userId\": \"user-123\", \"content\": null}";

    // When & Then
    mockMvc.perform(post("/api/v1/chat/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sendMessage_withNullWorkspaceId_shouldReturnBadRequest() throws Exception {
    // Given
    String requestBody = "{\"workspaceId\": null, \"userId\": \"user-123\", \"content\": \"안녕하세요!\"}";

    // When & Then
    mockMvc.perform(post("/api/v1/chat/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getMessagesByWorkspace_shouldReturnMessageList() throws Exception {
    // Given
    ChatMessage message1 = new ChatMessage();
    message1.setMessageId(1L);
    message1.setWorkspaceId(1L);
    message1.setUserId("user-123");
    message1.setContent("첫 번째 메시지");
    message1.setCreatedAt(Instant.now().minusSeconds(10));

    ChatMessage message2 = new ChatMessage();
    message2.setMessageId(2L);
    message2.setWorkspaceId(1L);
    message2.setUserId("user-456");
    message2.setContent("두 번째 메시지");
    message2.setCreatedAt(Instant.now());

    List<ChatMessage> mockMessages = Arrays.asList(message1, message2);
    when(chatMessageService.getMessagesByWorkspace(1L)).thenReturn(mockMessages);

    // When & Then
    mockMvc.perform(get("/api/v1/chat/messages/workspace/1")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].messageId").value(1))
        .andExpect(jsonPath("$[0].content").value("첫 번째 메시지"))
        .andExpect(jsonPath("$[1].messageId").value(2))
        .andExpect(jsonPath("$[1].content").value("두 번째 메시지"));
  }

  @Test
  void getMessagesByWorkspace_withEmptyList_shouldReturnEmptyArray() throws Exception {
    // Given
    when(chatMessageService.getMessagesByWorkspace(1L)).thenReturn(Arrays.asList());

    // When & Then
    mockMvc.perform(get("/api/v1/chat/messages/workspace/1")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getRecentMessages_shouldReturnLimitedMessages() throws Exception {
    // Given
    ChatMessage message1 = new ChatMessage();
    message1.setMessageId(2L);
    message1.setWorkspaceId(1L);
    message1.setUserId("user-456");
    message1.setContent("최근 메시지");
    message1.setCreatedAt(Instant.now());

    List<ChatMessage> mockMessages = Arrays.asList(message1);
    when(chatMessageService.getRecentMessages(1L, 1)).thenReturn(mockMessages);

    // When & Then
    mockMvc.perform(get("/api/v1/chat/messages/workspace/1/recent?limit=1")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].messageId").value(2))
        .andExpect(jsonPath("$[0].content").value("최근 메시지"));
  }

  @Test
  void getMessagesByUser_shouldReturnUserMessages() throws Exception {
    // Given
    ChatMessage message1 = new ChatMessage();
    message1.setMessageId(1L);
    message1.setWorkspaceId(1L);
    message1.setUserId("user-123");
    message1.setContent("사용자 메시지");
    message1.setCreatedAt(Instant.now());

    List<ChatMessage> mockMessages = Arrays.asList(message1);
    when(chatMessageService.getMessagesByUser("user-123")).thenReturn(mockMessages);

    // When & Then
    mockMvc.perform(get("/api/v1/chat/messages/user/user-123")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].messageId").value(1))
        .andExpect(jsonPath("$[0].userId").value("user-123"))
        .andExpect(jsonPath("$[0].content").value("사용자 메시지"));
  }

  @Test
  void getMessageCount_shouldReturnCount() throws Exception {
    // Given
    when(chatMessageService.getMessageCountByWorkspace(1L)).thenReturn(5L);

    // When & Then
    mockMvc.perform(get("/api/v1/chat/messages/workspace/1/count")
            .with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workspaceId").value(1))
        .andExpect(jsonPath("$.messageCount").value(5));
  }
}
