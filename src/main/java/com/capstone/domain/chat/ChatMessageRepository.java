package com.capstone.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    /**
     * 특정 워크스페이스의 모든 메시지를 시간순으로 조회
     */
    List<ChatMessage> findByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);
    
    /**
     * 특정 워크스페이스의 최근 N개 메시지 조회
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.workspaceId = :workspaceId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByWorkspaceId(@Param("workspaceId") Long workspaceId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * 특정 사용자의 메시지 조회
     */
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * 특정 워크스페이스의 메시지 개수 조회
     */
    long countByWorkspaceId(Long workspaceId);
}
