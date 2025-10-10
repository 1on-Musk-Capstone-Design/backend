package com.capstone.domain.workspace;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.type.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceUserRepository workspaceUserRepository;

    @Transactional
    public Workspace createWorkspace(String name, Long userId) {
        User owner = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));

        Workspace workspace = new Workspace();
        workspace.setName(name);
        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceUser workspaceUser = WorkspaceUser.builder()
            .workspace(savedWorkspace)
            .user(owner)
            .role(Role.OWNER)
            .build();

        workspaceUserRepository.save(workspaceUser);

        return savedWorkspace;
    }
    @Transactional(readOnly = true)
    public List<Workspace> getAllWorkspaces() {
        return workspaceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspaceById(Long id) {
        return workspaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다. ID: " + id));
    }

    @Transactional
    public Workspace updateWorkspaceName(Long id, String name) {
        Workspace workspace = getWorkspaceById(id);
        workspace.setName(name);
        return workspaceRepository.save(workspace);
    }

    @Transactional
    public void deleteWorkspace(Long id) {
        Workspace workspace = getWorkspaceById(id);
        workspaceRepository.delete(workspace);
    }
}
