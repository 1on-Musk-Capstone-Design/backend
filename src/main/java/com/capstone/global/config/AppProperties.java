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

  /**
   * 아이디어 → PRD → 프로토타입 배포 파이프라인 (GitHub / Vercel 토큰은 선택).
   */
  private Prototype prototype = new Prototype();

  @Getter
  @Setter
  public static class Prototype {
    /** GitHub personal access token (repo 권한). 비어 있으면 repo push 생략·시뮬 URL 반환 */
    private String githubToken = "";
    /** 저장소 소유자(조직/사용자). 비어 있으면 /user API로 로그인 사용자 사용 */
    private String githubOwner = "";
    /** Vercel 토큰. 비어 있으면 배포 URL 시뮬레이션 */
    private String vercelToken = "";
    /** 팀 배포 시 팀 ID (선택) */
    private String vercelTeamId = "";
    /** 시뮬레이션 모드에서 사용할 기본 URL 접두사 */
    private String simulatedBaseUrl = "https://prototype.example.com";
  }
}

