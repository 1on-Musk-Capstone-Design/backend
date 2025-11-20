package com.capstone.domain.workspaceInvitation;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.workspace.Workspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

  Optional<WorkspaceInvitation> findByWorkspaceAndInvitedUser(Workspace workspace, User invitedUser);

  List<WorkspaceInvitation> findByInvitedUser(User invitedUser);

  List<WorkspaceInvitation> findByWorkspace(Workspace workspace);

  List<WorkspaceInvitation> findByWorkspaceAndStatus(Workspace workspace, WorkspaceInvitation.InvitationStatus status);
}

