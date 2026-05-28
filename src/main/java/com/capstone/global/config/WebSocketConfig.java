package com.capstone.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private static final String[] DEV_ALLOWED_ORIGIN_PATTERNS = {
      "http://localhost:*",
      "https://localhost:*",
      "http://127.0.0.1:*",
      "https://127.0.0.1:*",
      "http://18.234.217.130:*",
      "https://18.234.217.130:*",
      "https://*.ngrok-free.app",
      "http://*.ngrok-free.app",
      "https://*.ngrok.app",
      "http://*.ngrok.app",
      "https://*.ngrok.io",
      "http://*.ngrok.io",
      "https://*.ngrok-free.dev",
      "http://*.ngrok-free.dev",
      "https://*.ngrok.dev",
      "http://*.ngrok.dev"
  };

  private final AppProperties appProperties;

  public WebSocketConfig(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // 클라이언트가 구독할 수 있는 destination prefix
    config.enableSimpleBroker("/topic", "/queue");
    // 클라이언트가 메시지를 보낼 때 사용할 destination prefix
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // WebSocket 연결 엔드포인트
    // 로컬 개발 환경과 프로덕션 환경 모두 허용
    List<String> allowedOriginPatterns = new ArrayList<>(appProperties.getAllowedOrigins());
    allowedOriginPatterns.addAll(List.of(DEV_ALLOWED_ORIGIN_PATTERNS));

    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns(allowedOriginPatterns.toArray(new String[0]))
        .withSockJS();
  }
}

