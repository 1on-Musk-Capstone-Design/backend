package com.capstone.integration;

import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WorkspaceIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private WorkspaceRepository workspaceRepository;

  @Test
  void saveWorkspace_shouldPersistToDatabase() {
    // Given
    Workspace workspace = new Workspace();
    workspace.setName("통합 테스트 워크스페이스");
    workspace.setCreatedAt(Instant.now());

    // When
    Workspace savedWorkspace = workspaceRepository.save(workspace);
    entityManager.flush();

    // Then
    assertThat(savedWorkspace.getWorkspaceId()).isNotNull();
    assertThat(savedWorkspace.getName()).isEqualTo("통합 테스트 워크스페이스");
    assertThat(savedWorkspace.getCreatedAt()).isNotNull();
  }

  @Test
  void findWorkspaceById_shouldReturnWorkspace() {
    // Given
    Workspace workspace = new Workspace();
    workspace.setName("조회 테스트 워크스페이스");
    workspace.setCreatedAt(Instant.now());

    Workspace savedWorkspace = entityManager.persistAndFlush(workspace);

    // When
    Workspace foundWorkspace = workspaceRepository.findById(savedWorkspace.getWorkspaceId())
        .orElse(null);

    // Then
    assertThat(foundWorkspace).isNotNull();
    assertThat(foundWorkspace.getName()).isEqualTo("조회 테스트 워크스페이스");
  }
}
