package com.capstone.domain.idea.prototype;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기존 DB에 남아 있던 {@code idea_prototype_jobs_status_check} 가 PENDING/DEPLOYED/FAILED 정도만 허용하는 경우,
 * Java {@link PrototypeJobStatus} 의 RUNNING·PRD_GENERATED 등으로 갱신 시 23514 제약 위반이 발생합니다.
 *
 * <p>기동 시 제약을 최신 ENUM 목록에 맞게 교체합니다(멱등).
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class PrototypeJobStatusConstraintMigration implements ApplicationRunner {

  private static final String CONSTRAINT_NAME = "idea_prototype_jobs_status_check";

  private final DataSource dataSource;

  @Override
  public void run(ApplicationArguments args) {
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement()) {
      c.setAutoCommit(true);
      st.execute("ALTER TABLE idea_prototype_jobs DROP CONSTRAINT IF EXISTS " + CONSTRAINT_NAME);
      st.execute(
          "ALTER TABLE idea_prototype_jobs ADD CONSTRAINT "
              + CONSTRAINT_NAME
              + " CHECK (status IN ("
              + "'PENDING', 'RUNNING', 'PRD_GENERATED', 'UI_GENERATED', 'CODE_GENERATED', "
              + "'GITHUB_PUSHED', 'DEPLOYED', 'FAILED'))");
      log.info("DB 제약 {} 을 PrototypeJobStatus 전체 값과 동기화했습니다.", CONSTRAINT_NAME);
    } catch (Exception e) {
      log.warn(
          "idea_prototype_jobs 상태 제약 동기화를 건너뜁니다(비 Postgres 또는 테이블 없음): {}",
          e.getMessage());
    }
  }
}
