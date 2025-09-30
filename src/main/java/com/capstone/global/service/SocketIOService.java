package com.capstone.global.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
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
                String sessionId = clientSessionMap.get(client.getSessionId());
                if (sessionId != null) {
                    // 해당 세션의 모든 클라이언트에게 메시지 브로드캐스트
                    socketIOServer.getRoomOperations(sessionId).sendEvent("new_message", data);
                }
            });
            
            // 세션 참여 이벤트
            socketIOServer.addEventListener("join_session", String.class, (client, sessionId, ackSender) -> {
                client.joinRoom(sessionId);
                clientSessionMap.put(client.getSessionId(), sessionId);
                sessionClientMap.put(sessionId, client.getSessionId());
                
                // 세션 참여자들에게 새 참여자 알림
                socketIOServer.getRoomOperations(sessionId).sendEvent("user_joined", 
                    "사용자가 세션에 참여했습니다.");
                
                client.sendEvent("joined_session", "세션에 성공적으로 참여했습니다.");
            });
            
            // 세션 나가기 이벤트
            socketIOServer.addEventListener("leave_session", String.class, (client, sessionId, ackSender) -> {
                client.leaveRoom(sessionId);
                clientSessionMap.remove(client.getSessionId());
                sessionClientMap.remove(sessionId);
                
                // 세션 참여자들에게 나가기 알림
                socketIOServer.getRoomOperations(sessionId).sendEvent("user_left", 
                    "사용자가 세션을 떠났습니다.");
                
                client.sendEvent("left_session", "세션에서 나갔습니다.");
            });
            
            // 아이디어 박스 업데이트 이벤트
            socketIOServer.addEventListener("idea_update", String.class, (client, data, ackSender) -> {
                String sessionId = clientSessionMap.get(client.getSessionId());
                if (sessionId != null) {
                    // 해당 세션의 모든 클라이언트에게 아이디어 업데이트 브로드캐스트
                    socketIOServer.getRoomOperations(sessionId).sendEvent("idea_updated", data);
                }
            });
            
            // 음성 채팅 참여 이벤트
            socketIOServer.addEventListener("voice_join", String.class, (client, sessionId, ackSender) -> {
                socketIOServer.getRoomOperations(sessionId).sendEvent("voice_user_joined", 
                    "사용자가 음성 채팅에 참여했습니다.");
            });
            
            // 음성 채팅 나가기 이벤트
            socketIOServer.addEventListener("voice_leave", String.class, (client, sessionId, ackSender) -> {
                socketIOServer.getRoomOperations(sessionId).sendEvent("voice_user_left", 
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
    
    // 특정 세션에 메시지 브로드캐스트
    public void broadcastToSession(String sessionId, String event, Object data) {
        socketIOServer.getRoomOperations(sessionId).sendEvent(event, data);
    }
    
    // 특정 클라이언트에게 메시지 전송
    public void sendToClient(UUID clientId, String event, Object data) {
        socketIOServer.getClient(clientId).sendEvent(event, data);
    }
}
