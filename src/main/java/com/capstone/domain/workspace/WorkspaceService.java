package com.capstone.domain.workspace;

import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public Workspace createWorkspace(String name) {
        Workspace workspace = new Workspace();
        workspace.setName(name);
        return workspaceRepository.save(workspace);
    }
}
