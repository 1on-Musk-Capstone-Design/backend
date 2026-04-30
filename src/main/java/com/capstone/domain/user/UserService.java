package com.capstone.domain.user;

import com.capstone.domain.canvas.Canvas;
import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.idea.IdeaRepository;
import com.capstone.domain.voicesessionUser.VoiceSessionUserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceInvitation.WorkspaceInvitationRepository;
import com.capstone.domain.workspaceInvite.WorkspaceInviteRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.exception.ErrorCode;
import com.capstone.global.oauth.JwtProvider;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final WorkspaceInviteRepository workspaceInviteRepository;
  private final WorkspaceInvitationRepository workspaceInvitationRepository;
  private final VoiceSessionUserRepository voiceSessionUserRepository;
  private final WorkspaceRepository workspaceRepository;
  private final IdeaRepository ideaRepository;
  private final CanvasRepository canvasRepository;
  private final JwtProvider jwtProvider;

  public UserResponse getCurrentUser(String accessToken) {
    try {
      Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

      return UserResponse.from(user);
    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }
  }

  @Transactional
  public UserResponse updateUser(String accessToken, UserUpdateRequest request) {
    try {
      Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

      // name이 제공된 경우에만 업데이트
      if (request.getName() != null && !request.getName().trim().isEmpty()) {
        user.setName(request.getName().trim());
      }

      // profileImage가 제공된 경우 업데이트 (null도 허용하여 프로필 이미지 제거 가능)
      if (request.getProfileImage() != null) {
        user.setProfileImage(request.getProfileImage());
      }

      User updatedUser = userRepository.saveAndFlush(user);
      return UserResponse.from(updatedUser);
    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException(ErrorCode.INVALID_TOKEN);
    }
  }

  @Transactional
  public void deleteUser(String accessToken) {
    try {
      Long userId = jwtProvider.getUserIdFromAccessToken(accessToken);
      User user = userRepository.findById(userId)
          .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

      List<WorkspaceUser> memberships = deleteAllUserRelatedData(user);

      workspaceUserRepository.deleteAll(memberships);

      user.setRefreshToken(null);
      userRepository.delete(user);
      log.info("유저({}) 탈퇴 처리 완료", userId);

    } catch (Exception e) {
      log.error("유저 삭제 중 오류 발생: {}", e.getMessage());
      throw e;
    }
  }

  //@NotNull
  private List<WorkspaceUser> deleteAllUserRelatedData(User user) {
    workspaceInvitationRepository.deleteAll(
        workspaceInvitationRepository.findByInvitedUserOrInvitedBy(user, user));
    workspaceInviteRepository.deleteAll(workspaceInviteRepository.findByCreatedBy(user));

    List<Workspace> ownedWorkspaces = workspaceRepository.findByOwner(user);
    for (Workspace ws : ownedWorkspaces) {
      canvasRepository.deleteAll(canvasRepository.findByWorkspace(ws));
      ideaRepository.deleteAll(ideaRepository.findByWorkspace(ws));

      List<WorkspaceUser> allMembersOfWorkspace = workspaceUserRepository.findByWorkspace(ws);
      workspaceUserRepository.deleteAll(allMembersOfWorkspace);

      log.info("워크스페이스({}) 내 컨텐츠 및 멤버십 삭제 완료", ws.getWorkspaceId());

      workspaceRepository.delete(ws);
      log.info("Owner인 워크스페이스({}) 삭제 완료", ws.getWorkspaceId());
    }

    List<WorkspaceUser> memberships = workspaceUserRepository.findByUser(user);
    for (WorkspaceUser member : memberships) {
      if (!workspaceRepository.existsById(member.getWorkspace().getWorkspaceId())) {
        continue;
      }

      for (Canvas canvas : canvasRepository.findByWorkspaceUser(member)) {
        canvas.setWorkspaceUser(null);
        canvasRepository.save(canvas);
      }

      voiceSessionUserRepository.deleteAll(voiceSessionUserRepository.findByWorkspaceUser(member));
    }

    return workspaceUserRepository.findByUser(user);
  }
}
