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
import com.capstone.domain.workspace.WorkspaceService;
import com.capstone.global.exception.CustomException;
import com.capstone.global.service.WebSocketService;
import com.capstone.global.type.Role;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceUserService {

  private final WorkspaceUserRepository workspaceUserRepository;
  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;
  private final WorkspaceService workspaceService;
  private final WebSocketService webSocketService;

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

    try {
      workspaceUserRepository.save(workspaceUser);
      
      // WebSocket 브로드캐스트
      webSocketService.notifyUserJoined(workspaceId, 
          String.format("사용자 %s가 워크스페이스에 참여했습니다.", user.getName()));
    } catch (DataIntegrityViolationException e) {
      log.warn("중복 워크스페이스 참여 시도 감지 - workspaceId: {}, userId: {}", workspaceId, userId);
      throw new CustomException(ALREADY_JOINED_WORKSPACE);
    }
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

    WorkspaceUser targetWorkspaceUser = workspaceUserRepository
        .findByWorkspaceAndUser(workspace, target)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE_USER));

    // 권한 확인: OWNER이거나 자신을 나가는 경우 허용
    boolean isOwner = requesterWorkspaceUser.getRole() == Role.OWNER;
    boolean isSelf = requesterId.equals(userId);

    if (!isOwner && !isSelf) {
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    if (targetWorkspaceUser.getRole() == Role.OWNER) {
      boolean deleted = handleOwnerLeaving(workspace, targetWorkspaceUser, isSelf, requesterId);
      if (deleted) {
        log.info("마지막 OWNER가 워크스페이스를 떠나며 삭제되었습니다. workspaceId={}, userId={}", workspaceId,
            requesterId);
        return;
      }
    }

    workspaceUserRepository.delete(targetWorkspaceUser);

    // WebSocket 브로드캐스트
    webSocketService.notifyUserLeft(workspaceId,
        String.format("사용자 %s가 워크스페이스에서 제거되었습니다.", target.getName()));
  }

  @Transactional
  public void leaveWorkspace(Long workspaceId, Long userId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    WorkspaceUser workspaceUser = workspaceUserRepository
        .findByWorkspaceAndUser(workspace, user)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE_USER));

    if (workspaceUser.getRole() == Role.OWNER) {
      boolean deleted = handleOwnerLeaving(workspace, workspaceUser, true, userId);
      if (deleted) {
        log.info("마지막 OWNER가 워크스페이스를 떠나며 삭제되었습니다. workspaceId={}, userId={}", workspaceId, userId);
        return;
      }
    }

    workspaceUserRepository.delete(workspaceUser);

    // WebSocket 브로드캐스트
    webSocketService.notifyUserLeft(workspaceId,
        String.format("사용자 %s가 워크스페이스를 떠났습니다.", user.getName()));
  }

  private boolean handleOwnerLeaving(Workspace workspace, WorkspaceUser leavingUser, boolean isSelf,
      Long actingUserId) {
    List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findByWorkspace(workspace);

    long ownerCount = workspaceUsers.stream()
        .filter(wu -> wu.getRole() == Role.OWNER)
        .count();

    if (ownerCount <= 1) {
      if (isSelf) {
        log.info("마지막 OWNER가 워크스페이스를 떠납니다. workspaceId={}, userId={}", workspace.getWorkspaceId(),
            actingUserId);
        workspaceService.deleteWorkspace(workspace.getWorkspaceId(), actingUserId);
        return true;
      }

      log.warn("다른 사용자가 마지막 OWNER를 제거하려고 시도했습니다. workspaceId={}, actingUser={}", workspace.getWorkspaceId(),
          actingUserId);
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    WorkspaceUser nextOwner = workspaceUsers.stream()
        .filter(wu -> !wu.getId().equals(leavingUser.getId()))
        .findFirst()
        .orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE));

    nextOwner.updateRole(Role.OWNER);
    workspace.setOwner(nextOwner.getUser());
    workspaceRepository.save(workspace);
    workspaceUserRepository.save(nextOwner);

    log.info("워크스페이스 OWNER 변경 - workspaceId: {}, newOwnerId: {}", workspace.getWorkspaceId(),
        nextOwner.getUser().getId());
    return false;
  }
}
