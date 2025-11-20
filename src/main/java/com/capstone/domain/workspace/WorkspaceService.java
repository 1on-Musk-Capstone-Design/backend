package com.capstone.domain.workspace;

import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_ACCESS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE_USER;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.type.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;

  @Transactional
  public Workspace createWorkspace(String name, Long userId) {
    User owner = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    Workspace workspace = new Workspace();
    workspace.setName(name);
    workspace.setOwner(owner);
    Workspace savedWorkspace = workspaceRepository.save(workspace);

    // OWNER도 workspace_users에 추가 (멤버십 관리)
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
  public List<Workspace> getWorkspacesByUserId(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(com.capstone.global.exception.ErrorCode.NOT_FOUND_USER));
    
    List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findByUser(user);
    return workspaceUsers.stream()
        .map(WorkspaceUser::getWorkspace)
        .toList();
  }

  @Transactional(readOnly = true)
  public Workspace getWorkspaceById(Long id) {
    return workspaceRepository.findById(id)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));
  }

  @Transactional
  public WorkspaceDtos.ListItem updateWorkspaceName(Long workspaceId, String name, Long userId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    // OWNER만 수정 가능
    if (!workspace.getOwner().getId().equals(userId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE);
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
    log.info("워크스페이스 삭제 시도 - workspaceId: {}, userId: {}", workspaceId, userId);
    
    Workspace workspace = getWorkspaceById(workspaceId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.error("사용자를 찾을 수 없음 - userId: {}", userId);
          return new CustomException(NOT_FOUND_USER);
        });

    // OWNER만 삭제 가능
    if (!workspace.getOwner().getId().equals(userId)) {
      log.error("OWNER 권한이 아님 - workspaceId: {}, userId: {}, ownerId: {}", 
          workspaceId, userId, workspace.getOwner().getId());
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    log.info("워크스페이스 삭제 진행 - workspaceId: {}, userId: {}", workspaceId, userId);
    workspaceRepository.delete(workspace);
  }
}
