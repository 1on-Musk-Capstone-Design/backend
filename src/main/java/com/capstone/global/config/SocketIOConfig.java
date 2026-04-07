package com.capstone.global.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class SocketIOConfig {

  @Bean
  public SocketIOServer socketIOServer(
      @Value("${socketio.host:0.0.0.0}") String host,
      @Value("${socketio.port:9092}") int port,
      @Value("${socketio.allow-origin:*}") String allowOrigin) {
    com.corundumstudio.socketio.Configuration cfg = new com.corundumstudio.socketio.Configuration();
    cfg.setHostname(host);
    cfg.setPort(port);
    cfg.setOrigin(allowOrigin);
    return new SocketIOServer(cfg);
  }
}
