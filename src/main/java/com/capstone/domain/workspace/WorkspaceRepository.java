package com.capstone.domain.workspace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

  @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.workspaceId = :id")
  Optional<Workspace> findByIdWithOwner(@Param("id") Long id);

  @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.workspaceId = :id AND w.deletedAt IS NULL")
  Optional<Workspace> findByIdWithOwnerAndNotDeleted(@Param("id") Long id);

  @Query("SELECT w FROM Workspace w JOIN FETCH w.owner WHERE w.deletedAt IS NOT NULL AND EXISTS (SELECT 1 FROM WorkspaceUser wu WHERE wu.workspace = w AND wu.user.id = :userId)")
  List<Workspace> findDeletedWorkspacesByUserId(@Param("userId") Long userId);
}
