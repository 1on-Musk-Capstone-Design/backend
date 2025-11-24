package com.capstone.service;

import com.capstone.domain.chat.ChatMessage;
import com.capstone.domain.chat.ChatMessageRepository;
import com.capstone.domain.chat.ChatMessageService;
import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ChatMessageService chatMessageService;

  private User createMockUser(Long userId) {
    return User.builder()
        .id(userId)
        .email("test@example.com")
        .name("Test User")
        .build();
  }

  @Test
  void saveMessage_shouldReturnSavedMessage() {
    // Given
    Long workspaceId = 1L;
    Long userId = 456L;
    String content = "안녕하세요!";

    User user = createMockUser(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    ChatMessage mockMessage = new ChatMessage();
    mockMessage.setMessageId(1L);
    mockMessage.setWorkspaceId(workspaceId);
    mockMessage.setUser(user);
    mockMessage.setContent(content);
    mockMessage.setCreatedAt(Instant.now());

    when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);

    // When
    ChatMessage result = chatMessageService.saveMessage(workspaceId, userId, content);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getMessageId()).isEqualTo(1L);
    assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
    assertThat(result.getUser().getId()).isEqualTo(userId);
    assertThat(result.getContent()).isEqualTo(content);
  }

  @Test
  void getMessagesByWorkspace_shouldReturnMessagesInOrder() {
    // Given
    Long workspaceId = 1L;

    ChatMessage message1 = new ChatMessage();
    message1.setMessageId(1L);
    message1.setContent("첫 번째 메시지");
    message1.setCreatedAt(Instant.now().minusSeconds(10));

    ChatMessage message2 = new ChatMessage();
    message2.setMessageId(2L);
    message2.setContent("두 번째 메시지");
    message2.setCreatedAt(Instant.now());

    List<ChatMessage> mockMessages = Arrays.asList(message1, message2);
    when(chatMessageRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId)).thenReturn(
        mockMessages);

    // When
    List<ChatMessage> result = chatMessageService.getMessagesByWorkspace(workspaceId);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getContent()).isEqualTo("첫 번째 메시지");
    assertThat(result.get(1).getContent()).isEqualTo("두 번째 메시지");
  }

  @Test
  void getMessageCountByWorkspace_shouldReturnCount() {
    // Given
    Long workspaceId = 1L;
    when(chatMessageRepository.countByWorkspaceId(workspaceId)).thenReturn(5L);

    // When
    long result = chatMessageService.getMessageCountByWorkspace(workspaceId);

    // Then
    assertThat(result).isEqualTo(5L);
  }
}
