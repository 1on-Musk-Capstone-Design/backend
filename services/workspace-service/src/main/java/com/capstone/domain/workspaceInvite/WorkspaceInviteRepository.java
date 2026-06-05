package com.capstone.domain.workspaceInvite;

import com.capstone.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, Long> {

  Optional<WorkspaceInvite> findByToken(String token);

  List<WorkspaceInvite> findByCreatedBy(User createdBy);
}

