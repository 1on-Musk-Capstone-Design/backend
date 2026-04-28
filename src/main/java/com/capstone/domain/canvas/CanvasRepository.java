package com.capstone.domain.canvas;

import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CanvasRepository extends JpaRepository<Canvas, Long> {

  List<Canvas> findByWorkspace(Workspace workspace);
  List<Canvas> findByWorkspaceUser(WorkspaceUser workspaceUser);
}