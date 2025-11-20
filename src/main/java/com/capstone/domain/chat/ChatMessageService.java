package com.capstone.domain.chat;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatMessageService {

  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;

  public ChatMessageService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
    this.chatMessageRepository = chatMessageRepository;
    this.userRepository = userRepository;
  }

  /**
   * 채팅 메시지 저장
   */
  @Transactional
  public ChatMessage saveMessage(Long workspaceId, Long userId, String content) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    
    ChatMessage message = new ChatMessage();
    message.setWorkspaceId(workspaceId);
    message.setUser(user);
    message.setContent(content);
    message.setMessageType("text");

    return chatMessageRepository.save(message);
  }

  /**
   * 파일/이미지 메시지 저장
   */
  @Transactional
  public ChatMessage saveFileMessage(
      Long workspaceId,
      Long userId,
      String content,
      String messageType,
      String fileUrl,
      String fileName,
      String mimeType,
      Long fileSize
  ) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    
    ChatMessage message = new ChatMessage();
    message.setWorkspaceId(workspaceId);
    message.setUser(user);
    message.setContent(content);
    message.setMessageType(messageType);
    message.setFileUrl(fileUrl);
    message.setFileName(fileName);
    message.setMimeType(mimeType);
    message.setFileSize(fileSize);
    return chatMessageRepository.save(message);
  }

  /**
   * 특정 워크스페이스의 모든 메시지 조회 (시간순)
   */
  @Transactional(readOnly = true)
  public List<ChatMessage> getMessagesByWorkspace(Long workspaceId) {
    return chatMessageRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
  }

  /**
   * 특정 워크스페이스의 최근 N개 메시지 조회
   */
  @Transactional(readOnly = true)
  public List<ChatMessage> getRecentMessages(Long workspaceId, int limit) {
    Pageable pageable = PageRequest.of(0, limit);
    return chatMessageRepository.findRecentMessagesByWorkspaceId(workspaceId, pageable);
  }

  /**
   * 특정 사용자의 메시지 조회
   */
  @Transactional(readOnly = true)
  public List<ChatMessage> getMessagesByUser(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    return chatMessageRepository.findByUserOrderByCreatedAtDesc(user);
  }

  /**
   * 특정 워크스페이스의 메시지 개수 조회
   */
  @Transactional(readOnly = true)
  public long getMessageCountByWorkspace(Long workspaceId) {
    return chatMessageRepository.countByWorkspaceId(workspaceId);
  }
}
