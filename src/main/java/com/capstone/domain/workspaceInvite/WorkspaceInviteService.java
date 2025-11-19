package com.capstone.domain.workspaceInvite;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspace.WorkspaceDtos.InviteLinkResponse;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.domain.workspaceUser.WorkspaceUserService;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.type.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceInviteService {

  private final WorkspaceInviteRepository workspaceInviteRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final UserRepository userRepository;
  private final WorkspaceUserService workspaceUserService;

  @Value("${workspace.invite.base-url:http://localhost:3000/invite}")
  private String inviteBaseUrl;

  @Value("${workspace.invite.expire-hours:168}")
  private long inviteExpireHours;

  @Transactional
  public InviteLinkResponse createInvite(Long workspaceId, Long userId) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WORKSPACE));

    User creator = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

    WorkspaceUser workspaceUser = workspaceUserRepository.findByWorkspaceAndUser(workspace, creator)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WORKSPACE_USER));

    if (workspaceUser.getRole() != Role.OWNER) {
      throw new CustomException(ErrorCode.FORBIDDEN_WORKSPACE);
    }

    String token = UUID.randomUUID().toString().replace("-", "");
    Instant expiresAt = Instant.now().plus(Duration.ofHours(inviteExpireHours));

    WorkspaceInvite invite = WorkspaceInvite.builder()
        .workspace(workspace)
        .createdBy(creator)
        .token(token)
        .expiresAt(expiresAt)
        .build();

    workspaceInviteRepository.save(invite);

    String inviteUrl = String.format("%s?token=%s", inviteBaseUrl, token);
    InviteLinkResponse response = new InviteLinkResponse();
    response.setToken(token);
    response.setInviteUrl(inviteUrl);
    response.setExpiresAt(expiresAt);
    return response;
  }

  @Transactional
  public void acceptInvite(String token, Long userId) {
    WorkspaceInvite invite = workspaceInviteRepository.findByToken(token)
        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INVITE_TOKEN));

    if (invite.getExpiresAt().isBefore(Instant.now())) {
      throw new CustomException(ErrorCode.EXPIRED_INVITE_TOKEN);
    }

    workspaceUserService.joinWorkspace(invite.getWorkspace().getWorkspaceId(), userId);
  }
}

