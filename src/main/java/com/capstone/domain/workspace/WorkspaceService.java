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
    // 개발/테스트: User가 없으면 자동으로 생성
    User owner = userRepository.findById(userId)
        .orElseGet(() -> {
          User newUser = User.builder()
              .email("test@example.com")
              .name("테스트 사용자")
              .profileImage(null)
              .build();
          return userRepository.save(newUser);
        });

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
  public WorkspaceDtos.ListItem updateWorkspaceName(Long workspaceId, String name, Long userId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다."));

    // 개발/테스트: User가 없으면 자동으로 생성
    User user = userRepository.findById(userId)
        .orElseGet(() -> {
          User newUser = User.builder()
              .email("test@example.com")
              .name("테스트 사용자")
              .profileImage(null)
              .build();
          return userRepository.save(newUser);
        });

    WorkspaceUser workspaceUser = workspaceUserRepository.findByWorkspaceAndUser(workspace, user)
        .orElseThrow(() -> new RuntimeException("워크스페이스 멤버가 아닙니다."));

    if (workspaceUser.getRole() != Role.OWNER) {
      throw new SecurityException("워크스페이스 이름은 OWNER만 수정할 수 있습니다.");
    }

    workspace.setName(name);
    Workspace updated = workspaceRepository.save(workspace);

    WorkspaceDtos.ListItem dto = new WorkspaceDtos.ListItem();
    dto.setWorkspaceId(updated.getWorkspaceId());
    dto.setName(updated.getName());
    return dto;
  }

  @Transactional
  public void deleteWorkspace(Long workspaceId, Long userId) {
    Workspace workspace = getWorkspaceById(workspaceId);

    // 개발/테스트: User가 없으면 자동으로 생성
    User user = userRepository.findById(userId)
        .orElseGet(() -> {
          User newUser = User.builder()
              .email("test@example.com")
              .name("테스트 사용자")
              .profileImage(null)
              .build();
          return userRepository.save(newUser);
        });

    WorkspaceUser workspaceUser = workspaceUserRepository.findByWorkspaceAndUser(workspace, user)
        .orElseThrow(() -> new RuntimeException("워크스페이스 참여 이력이 없습니다."));

    if (workspaceUser.getRole() != Role.OWNER) {
      throw new RuntimeException("워크스페이스 삭제 권한이 없습니다.");
    }

    workspaceRepository.delete(workspace);
  }
}
