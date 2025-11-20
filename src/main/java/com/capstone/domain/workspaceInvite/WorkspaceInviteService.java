package com.capstone.domain.workspaceInvite;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspace.WorkspaceDtos.InviteLinkResponse;
import com.capstone.domain.workspaceInvitation.WorkspaceInvitation;
import com.capstone.domain.workspaceInvitation.WorkspaceInvitationRepository;
import com.capstone.domain.workspaceUser.WorkspaceUserService;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
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
  private final UserRepository userRepository;
  private final WorkspaceUserService workspaceUserService;
  private final WorkspaceInvitationRepository workspaceInvitationRepository;

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

    // OWNER만 초대 링크 생성 가능
    if (!workspace.getOwner().getId().equals(userId)) {
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

    Workspace workspace = invite.getWorkspace();
    User invitedUser = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

    // workspace_invitations 테이블 업데이트 또는 생성
    WorkspaceInvitation invitation = workspaceInvitationRepository
        .findByWorkspaceAndInvitedUser(workspace, invitedUser)
        .orElse(null);

    if (invitation == null) {
      // 초대 기록이 없으면 새로 생성
      invitation = WorkspaceInvitation.builder()
          .workspace(workspace)
          .invitedUser(invitedUser)
          .invitedBy(invite.getCreatedBy())
          .status(WorkspaceInvitation.InvitationStatus.ACCEPTED)
          .expiresAt(invite.getExpiresAt())
          .build();
    } else {
      // 초대 기록이 있으면 상태를 ACCEPTED로 업데이트
      invitation.setStatus(WorkspaceInvitation.InvitationStatus.ACCEPTED);
    }

    workspaceInvitationRepository.save(invitation);

    // 워크스페이스에 사용자 추가
    workspaceUserService.joinWorkspace(workspace.getWorkspaceId(), userId);
  }
}

