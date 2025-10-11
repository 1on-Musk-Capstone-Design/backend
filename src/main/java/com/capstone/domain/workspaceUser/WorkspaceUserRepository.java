package com.capstone.domain.workspaceUser;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.workspace.Workspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceUserRepository extends JpaRepository<WorkspaceUser, Long> {

  boolean existsByWorkspaceAndUser(Workspace workspace, User user);

  List<WorkspaceUser> findByWorkspace(Workspace workspace);

  Optional<WorkspaceUser> findByWorkspaceAndUser(Workspace workspace, User user);
}