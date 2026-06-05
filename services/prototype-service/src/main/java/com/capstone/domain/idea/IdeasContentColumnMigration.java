package com.capstone.domain.idea;

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
 * 과거 스키마에서 {@code ideas.content} 가 {@code varchar(255)} 인 경우, 통합 PRD 본문이 길어 insert/update 시
 * 22001 오류가 납니다. Hibernate {@code columnDefinition = TEXT} 만으로는 기존 컬럼 타입이 자동 확장되지 않을 수
 * 있어 기동 시 TEXT 로 맞춥니다.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class IdeasContentColumnMigration implements ApplicationRunner {

  private final DataSource dataSource;

  @Override
  public void run(ApplicationArguments args) {
    try (Connection c = dataSource.getConnection();
        Statement st = c.createStatement()) {
      c.setAutoCommit(true);
      st.execute("ALTER TABLE ideas ALTER COLUMN content TYPE TEXT USING content::text");
      log.info("ideas.content 컬럼을 TEXT 로 맞췄습니다.");
    } catch (Exception e) {
      log.warn("ideas.content TEXT 동기화를 건너뜁니다: {}", e.getMessage());
    }
  }
}
