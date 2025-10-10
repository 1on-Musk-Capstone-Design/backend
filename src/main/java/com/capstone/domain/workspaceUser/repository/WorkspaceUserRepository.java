package com.capstone.domain.workspaceUser.repository;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspaceUser.entity.WorkspaceUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceUserRepository extends JpaRepository<WorkspaceUser, Long> {

  boolean existsByWorkspaceAndUser(Workspace workspace, User user);

  List<WorkspaceUser> findByWorkspace(Workspace workspace);
}
