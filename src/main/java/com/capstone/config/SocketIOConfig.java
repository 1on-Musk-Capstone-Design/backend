package com.capstone.config;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration as SpringConfiguration;

@SpringConfiguration
public class SocketIOConfig {
    
    @Value("${socketio.host:localhost}")
    private String host;
    
    @Value("${socketio.port:9092}")
    private int port;
    
    @Value("${socketio.cors.origins:*:*}")
    private String corsOrigins;
    
    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        
        // CORS 설정
        config.setOrigin(corsOrigins);
        
        // 연결 설정
        config.setMaxFramePayloadLength(1024 * 1024); // 1MB
        config.setMaxHttpContentLength(1024 * 1024); // 1MB
        
        // 네임스페이스 설정
        config.setContext("/socket.io/");
        
        return new SocketIOServer(config);
    }
}
