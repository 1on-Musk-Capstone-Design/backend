package com.capstone.global.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.capstone.domain.chat.ChatMessage;
import com.capstone.domain.chat.ChatMessageService;
import com.capstone.domain.chat.ChatMessageDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SocketIOService {
    
    @Autowired
    private SocketIOServer socketIOServer;
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 클라이언트 세션 관리
    private final Map<UUID, String> clientSessionMap = new HashMap<>();
    private final Map<String, UUID> sessionClientMap = new HashMap<>();
    
    @PostConstruct
    public void start() {
        try {
            // 연결 이벤트 리스너
            socketIOServer.addConnectListener(new ConnectListener() {
                @Override
                public void onConnect(com.corundumstudio.socketio.SocketIOClient client) {
                    System.out.println("클라이언트 연결됨: " + client.getSessionId());
                    
                    // 연결 확인 메시지 전송
                    client.sendEvent("connected", "서버에 성공적으로 연결되었습니다.");
                }
            });
            
            // 연결 해제 이벤트 리스너
            socketIOServer.addDisconnectListener(new DisconnectListener() {
                @Override
                public void onDisconnect(com.corundumstudio.socketio.SocketIOClient client) {
                    System.out.println("클라이언트 연결 해제됨: " + client.getSessionId());
                    
                    // 세션 정리
                    String sessionId = clientSessionMap.remove(client.getSessionId());
                    if (sessionId != null) {
                        sessionClientMap.remove(sessionId);
                    }
                }
            });
            
            // 채팅 메시지 이벤트
            socketIOServer.addEventListener("chat_message", String.class, (client, data, ackSender) -> {
                try {
                    String workspaceId = clientSessionMap.get(client.getSessionId());
                    if (workspaceId != null) {
                        // JSON 데이터를 ChatMessageDtos.SendRequest로 파싱
                        ChatMessageDtos.SendRequest request = objectMapper.readValue(data, ChatMessageDtos.SendRequest.class);
                        
                        // 메시지를 데이터베이스에 저장
                        ChatMessage savedMessage = chatMessageService.saveMessage(
                            Long.parseLong(workspaceId), 
                            request.getUserId(), 
                            request.getContent()
                        );
                        
                        // 저장된 메시지를 응답 DTO로 변환
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
                        
                        // 해당 워크스페이스의 모든 클라이언트에게 메시지 브로드캐스트
                        String responseJson = objectMapper.writeValueAsString(response);
                        socketIOServer.getRoomOperations(workspaceId).sendEvent("new_message", responseJson);
                    }
                } catch (Exception e) {
                    System.err.println("채팅 메시지 처리 중 오류 발생: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            // 파일/이미지 메타 전송 이벤트
            socketIOServer.addEventListener("file_message", String.class, (client, data, ackSender) -> {
                try {
                    String workspaceId = clientSessionMap.get(client.getSessionId());
                    if (workspaceId != null) {
                        // data: { userId, content?, messageType, fileUrl, fileName, mimeType, fileSize }
                        java.util.Map<String, Object> payload = objectMapper.readValue(data, java.util.Map.class);
                        String userId = (String) payload.get("userId");
                        String content = payload.get("content") == null ? null : payload.get("content").toString();
                        String messageType = (String) payload.getOrDefault("messageType", "file");
                        String fileUrl = (String) payload.get("fileUrl");
                        String fileName = (String) payload.get("fileName");
                        String mimeType = (String) payload.get("mimeType");
                        Long fileSize = payload.get("fileSize") == null ? null : Long.valueOf(payload.get("fileSize").toString());

                        ChatMessage saved = chatMessageService.saveFileMessage(
                                Long.parseLong(workspaceId),
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
                        response.setUserId(saved.getUserId());
                        response.setContent(saved.getContent());
                        response.setMessageType(saved.getMessageType());
                        response.setFileUrl(saved.getFileUrl());
                        response.setFileName(saved.getFileName());
                        response.setMimeType(saved.getMimeType());
                        response.setFileSize(saved.getFileSize());
                        response.setCreatedAt(saved.getCreatedAt());

                        String responseJson = objectMapper.writeValueAsString(response);
                        socketIOServer.getRoomOperations(workspaceId).sendEvent("new_message", responseJson);
                    }
                } catch (Exception e) {
                    System.err.println("파일 메시지 처리 중 오류 발생: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
            // 워크스페이스 참여 이벤트
            socketIOServer.addEventListener("join_workspace", String.class, (client, workspaceId, ackSender) -> {
                client.joinRoom(workspaceId);
                clientSessionMap.put(client.getSessionId(), workspaceId);
                sessionClientMap.put(workspaceId, client.getSessionId());
                
                // 워크스페이스 참여자들에게 새 참여자 알림
                socketIOServer.getRoomOperations(workspaceId).sendEvent("user_joined", 
                    "사용자가 워크스페이스에 참여했습니다.");
                
                client.sendEvent("joined_workspace", "워크스페이스에 성공적으로 참여했습니다.");
            });
            
            // 워크스페이스 나가기 이벤트
            socketIOServer.addEventListener("leave_workspace", String.class, (client, workspaceId, ackSender) -> {
                client.leaveRoom(workspaceId);
                clientSessionMap.remove(client.getSessionId());
                sessionClientMap.remove(workspaceId);
                
                // 워크스페이스 참여자들에게 나가기 알림
                socketIOServer.getRoomOperations(workspaceId).sendEvent("user_left", 
                    "사용자가 워크스페이스를 떠났습니다.");
                
                client.sendEvent("left_workspace", "워크스페이스에서 나갔습니다.");
            });
            
            // 아이디어 박스 업데이트 이벤트
            socketIOServer.addEventListener("idea_update", String.class, (client, data, ackSender) -> {
                String workspaceId = clientSessionMap.get(client.getSessionId());
                if (workspaceId != null) {
                    // 해당 워크스페이스의 모든 클라이언트에게 아이디어 업데이트 브로드캐스트
                    socketIOServer.getRoomOperations(workspaceId).sendEvent("idea_updated", data);
                }
            });
            
            // 음성 채팅 참여 이벤트
            socketIOServer.addEventListener("voice_join", String.class, (client, workspaceId, ackSender) -> {
                socketIOServer.getRoomOperations(workspaceId).sendEvent("voice_user_joined", 
                    "사용자가 음성 채팅에 참여했습니다.");
            });
            
            // 음성 채팅 나가기 이벤트
            socketIOServer.addEventListener("voice_leave", String.class, (client, workspaceId, ackSender) -> {
                socketIOServer.getRoomOperations(workspaceId).sendEvent("voice_user_left", 
                    "사용자가 음성 채팅에서 나갔습니다.");
            });
            
            socketIOServer.start();
            System.out.println("Socket.IO 서버가 시작되었습니다. 포트: " + socketIOServer.getConfiguration().getPort());
            
        } catch (Exception e) {
            System.err.println("Socket.IO 서버 시작 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @PreDestroy
    public void stop() {
        if (socketIOServer != null) {
            socketIOServer.stop();
            System.out.println("Socket.IO 서버가 중지되었습니다.");
        }
    }
    
    // 특정 워크스페이스에 메시지 브로드캐스트
    public void broadcastToWorkspace(String workspaceId, String event, Object data) {
        socketIOServer.getRoomOperations(workspaceId).sendEvent(event, data);
    }
    
    // 특정 클라이언트에게 메시지 전송
    public void sendToClient(UUID clientId, String event, Object data) {
        socketIOServer.getClient(clientId).sendEvent(event, data);
    }
}
