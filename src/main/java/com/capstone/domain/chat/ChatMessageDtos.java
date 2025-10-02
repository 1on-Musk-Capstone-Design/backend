package com.capstone.domain.chat;

import java.time.Instant;

public class ChatMessageDtos {
    
    /**
     * 채팅 메시지 전송 요청 DTO
     */
    public static class SendRequest {
        private Long workspaceId;
        private String userId;
        private String content;
        
        public Long getWorkspaceId() {
            return workspaceId;
        }
        
        public void setWorkspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
    
    /**
     * 채팅 메시지 응답 DTO
     */
    public static class Response {
        private Long messageId;
        private Long workspaceId;
        private String userId;
        private String content;
        private Instant createdAt;
        
        public Long getMessageId() {
            return messageId;
        }
        
        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }
        
        public Long getWorkspaceId() {
            return workspaceId;
        }
        
        public void setWorkspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    /**
     * 채팅 메시지 목록 조회 응답 DTO
     */
    public static class ListResponse {
        private Long messageId;
        private String userId;
        private String content;
        private Instant createdAt;
        
        public Long getMessageId() {
            return messageId;
        }
        
        public void setMessageId(Long messageId) {
            this.messageId = messageId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Instant getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    /**
     * 채팅 메시지 개수 조회 응답 DTO
     */
    public static class CountResponse {
        private Long workspaceId;
        private long messageCount;
        
        public Long getWorkspaceId() {
            return workspaceId;
        }
        
        public void setWorkspaceId(Long workspaceId) {
            this.workspaceId = workspaceId;
        }
        
        public long getMessageCount() {
            return messageCount;
        }
        
        public void setMessageCount(long messageCount) {
            this.messageCount = messageCount;
        }
    }
}
