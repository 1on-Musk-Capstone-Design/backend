package com.capstone.domain.workspaceUser;

import static com.capstone.global.exception.ErrorCode.ALREADY_JOINED_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE_USER;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.global.exception.CustomException;
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
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    if (workspaceUserRepository.existsByWorkspaceAndUser(workspace, user)) {
      throw new CustomException(ALREADY_JOINED_WORKSPACE);
    }

    WorkspaceUser workspaceUser = WorkspaceUser.builder()
        .workspace(workspace)
        .user(user)
        .role(Role.MEMBER)
        .build();

    workspaceUserRepository.save(workspaceUser);
  }

  @Transactional
  public List<WorkspaceUserResponse> getWorkspaceUsers(Long workspaceId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    return workspaceUserRepository.findByWorkspace(workspace).stream()
        .map(user -> WorkspaceUserResponse.builder()
            .id(user.getUser().getId())
            .email(user.getUser().getEmail())
            .name(user.getUser().getName())
            .profileImage(user.getUser().getProfileImage())
            .role(user.getRole())
            .joinedAt(user.getJoinedAt())
            .build())
        .toList();
  }

  @Transactional
  public void removeUser(Long workspaceId, Long userId, Long requesterId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    User requester = userRepository.findById(requesterId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
    User target = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    WorkspaceUser requesterWorkspaceUser = workspaceUserRepository
        .findByWorkspaceAndUser(workspace, requester)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE_USER));

    if (requesterWorkspaceUser.getRole() != Role.OWNER) {
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    WorkspaceUser targetWorkspaceUser = workspaceUserRepository
        .findByWorkspaceAndUser(workspace, target)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE_USER));

    workspaceUserRepository.delete(targetWorkspaceUser);
  }
}
