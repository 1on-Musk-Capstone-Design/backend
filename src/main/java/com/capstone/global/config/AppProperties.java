package com.capstone.global.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  /**
   * CORS 및 WebSocket에서 허용할 Origin 패턴 목록.
   */
  private List<String> allowedOrigins = new ArrayList<>();

  /**
   * Origin 별 OAuth redirect URI 매핑.
   */
  private Map<String, String> oauthRedirectMap = new HashMap<>();

  /**
   * 매핑되지 않은 Origin에서 사용할 기본 redirect URI.
   */
  private String defaultRedirectUri = "http://localhost:3000/auth/callback";
}

