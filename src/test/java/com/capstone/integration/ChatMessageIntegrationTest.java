package com.capstone.integration;

import com.capstone.domain.chat.ChatMessage;
import com.capstone.domain.chat.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChatMessageIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Test
  void saveChatMessage_shouldPersistAndRetrieve() {
    // Given
    ChatMessage message = new ChatMessage();
    message.setWorkspaceId(1L);
    message.setUserId("user-456");
    message.setContent("통합 테스트 메시지");
    message.setCreatedAt(Instant.now());

    // When
    ChatMessage savedMessage = chatMessageRepository.save(message);
    entityManager.flush();
    entityManager.clear();

    // Then
    assertThat(savedMessage.getMessageId()).isNotNull();
    assertThat(savedMessage.getWorkspaceId()).isEqualTo(1L);
    assertThat(savedMessage.getUserId()).isEqualTo("user-456");
    assertThat(savedMessage.getContent()).isEqualTo("통합 테스트 메시지");
    assertThat(savedMessage.getCreatedAt()).isNotNull();
  }

  @Test
  void findByWorkspaceId_shouldReturnMessagesInOrder() {
    // Given
    ChatMessage message1 = new ChatMessage();
    message1.setWorkspaceId(1L);
    message1.setUserId("user-1");
    message1.setContent("첫 번째 메시지");
    message1.setCreatedAt(Instant.now().minusSeconds(10));

    ChatMessage message2 = new ChatMessage();
    message2.setWorkspaceId(1L);
    message2.setUserId("user-2");
    message2.setContent("두 번째 메시지");
    message2.setCreatedAt(Instant.now());

    entityManager.persistAndFlush(message1);
    entityManager.persistAndFlush(message2);

    // When
    var result = chatMessageRepository.findByWorkspaceIdOrderByCreatedAtAsc(1L);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getContent()).isEqualTo("첫 번째 메시지");
    assertThat(result.get(1).getContent()).isEqualTo("두 번째 메시지");
  }
}
