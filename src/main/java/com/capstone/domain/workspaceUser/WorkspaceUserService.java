package com.capstone.domain.workspaceUser;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
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
  public List<WorkspaceUserResponse> getWorkspaceUsers(Long workspaceId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다."));

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
        .orElseThrow(() -> new RuntimeException("워크스페이스를 찾을 수 없습니다."));

    User requester = userRepository.findById(requesterId)
        .orElseThrow(() -> new RuntimeException("요청자를 찾을 수 없습니다."));
    User target = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("삭제할 유저를 찾을 수 없습니다."));

    WorkspaceUser requesterWorkspaceUser = workspaceUserRepository
        .findByWorkspaceAndUser(workspace, requester)
        .orElseThrow(() -> new RuntimeException("요청자가 워크스페이스에 없습니다."));

    if (requesterWorkspaceUser.getRole() != Role.OWNER) {
      throw new RuntimeException("삭제 권한이 없습니다.");
    }

    WorkspaceUser targetWorkspaceUser = workspaceUserRepository
        .findByWorkspaceAndUser(workspace, target)
        .orElseThrow(() -> new RuntimeException("삭제할 유저가 워크스페이스에 없습니다."));

    workspaceUserRepository.delete(targetWorkspaceUser);
  }
}
