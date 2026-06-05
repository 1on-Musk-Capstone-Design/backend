package com.capstone.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발 시 프론트 자동 로그인용 부트스트랩 API on/off.
 * 운영 배포 시 {@code APP_DEV_BOOTSTRAP_AUTH=false} 권장.
 */
@Component
@ConfigurationProperties(prefix = "app.dev-bootstrap-auth")
@Getter
@Setter
public class DevBootstrapAuthProperties {

  /** {@code true}이면 {@code POST /v1/auth/dev/bootstrap} 이 동작합니다. */
  private boolean enabled = false;
}
