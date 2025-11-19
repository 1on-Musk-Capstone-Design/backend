package com.capstone.domain.workspaceInvite;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, Long> {

  Optional<WorkspaceInvite> findByToken(String token);
}

