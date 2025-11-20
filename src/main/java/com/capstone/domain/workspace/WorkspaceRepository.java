package com.capstone.domain.workspace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

  @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.workspaceId = :id")
  Optional<Workspace> findByIdWithOwner(@Param("id") Long id);
}
