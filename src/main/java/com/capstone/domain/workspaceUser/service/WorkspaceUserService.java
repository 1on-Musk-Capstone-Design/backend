package com.capstone.domain.workspaceUser.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.entity.WorkspaceUser;
import com.capstone.domain.workspaceUser.repository.WorkspaceUserRepository;
import com.capstone.global.oauth.service.GoogleService;
import com.capstone.global.type.Role;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceUserService {

  private final WorkspaceUserRepository workspaceUserRepository;
  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;

  @Transactional
  public void joinWorkspace(Long workspaceId, Long userId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다."));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

    if (workspaceUserRepository.existsByWorkspaceAndUser(workspace, user)) {
      throw new RuntimeException("이미 워크스페이스에 참여한 유저입니다.");
    }

    WorkspaceUser workspaceUser = WorkspaceUser.builder()
        .workspace(workspace)
        .user(user)
        .role(Role.MEMBER)
        .build();

    workspaceUserRepository.save(workspaceUser);
  }

  @Transactional
  public List<WorkspaceUser> getWorkspaceUsers(Long workspaceId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다."));

    return workspaceUserRepository.findByWorkspace(workspace);
  }
}
