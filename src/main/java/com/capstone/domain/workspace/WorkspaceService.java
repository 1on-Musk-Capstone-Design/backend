package com.capstone.domain.workspace;

import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_ACCESS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE_USER;

import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.chat.ChatMessageRepository;
import com.capstone.domain.idea.IdeaRepository;
import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.voicesessionUser.VoiceSessionUserRepository;
import com.capstone.domain.workspaceInvitation.WorkspaceInvitationRepository;
import com.capstone.domain.workspaceInvite.WorkspaceInviteRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.service.WebSocketService;
import com.capstone.global.type.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;
  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final WorkspaceInviteRepository workspaceInviteRepository;
  private final WorkspaceInvitationRepository workspaceInvitationRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final CanvasRepository canvasRepository;
  private final IdeaRepository ideaRepository;
  private final VoiceSessionRepository voiceSessionRepository;
  private final VoiceSessionUserRepository voiceSessionUserRepository;
  private final WebSocketService webSocketService;

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

    // WebSocket 브로드캐스트 (생성자에게만)
    webSocketService.broadcastWorkspaceChange(savedWorkspace.getWorkspaceId(), "created", 
        Map.of("workspaceId", savedWorkspace.getWorkspaceId(), "name", savedWorkspace.getName()));

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
    // owner를 함께 로드하여 조회
    Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    // OWNER만 수정 가능
    if (workspace.getOwner() == null || !workspace.getOwner().getId().equals(userId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    workspace.setName(name);
    Workspace updated = workspaceRepository.save(workspace);

    WorkspaceDtos.ListItem dto = new WorkspaceDtos.ListItem();
    dto.setWorkspaceId(updated.getWorkspaceId());
    dto.setName(updated.getName());

    // WebSocket 브로드캐스트
    webSocketService.broadcastWorkspaceChange(workspaceId, "updated", 
        Map.of("workspaceId", updated.getWorkspaceId(), "name", updated.getName()));

    return dto;
  }

  @Transactional
  public void deleteWorkspace(Long workspaceId, Long userId) {
    log.info("=== 워크스페이스 삭제 시도 시작 ===");
    log.info("workspaceId: {} (타입: {})", workspaceId, workspaceId.getClass().getSimpleName());
    log.info("userId: {} (타입: {})", userId, userId.getClass().getSimpleName());
    
    // owner를 함께 로드하여 조회
    Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
        .orElseThrow(() -> {
          log.error("워크스페이스를 찾을 수 없음 - workspaceId: {}", workspaceId);
          return new CustomException(NOT_FOUND_WORKSPACE);
        });

    log.info("워크스페이스 조회 성공 - workspaceId: {}, name: {}", 
        workspace.getWorkspaceId(), workspace.getName());

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.error("사용자를 찾을 수 없음 - userId: {}", userId);
          return new CustomException(NOT_FOUND_USER);
        });

    log.info("사용자 조회 성공 - userId: {}, email: {}, name: {}", 
        user.getId(), user.getEmail(), user.getName());

    // OWNER 확인
    if (workspace.getOwner() == null) {
      log.error("워크스페이스에 OWNER가 설정되지 않음 - workspaceId: {}", workspaceId);
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    User owner = workspace.getOwner();
    Long ownerId = owner.getId();
    
    log.info("=== OWNER 확인 ===");
    log.info("workspaceId: {}", workspaceId);
    log.info("요청 userId: {} (타입: {})", userId, userId.getClass().getSimpleName());
    log.info("워크스페이스 ownerId: {} (타입: {})", ownerId, ownerId.getClass().getSimpleName());
    log.info("owner email: {}", owner.getEmail());
    log.info("owner name: {}", owner.getName());
    log.info("user email: {}", user.getEmail());
    log.info("user name: {}", user.getName());
    log.info("비교 결과: ownerId.equals(userId) = {}", ownerId.equals(userId));
    log.info("비교 결과: ownerId.longValue() == userId.longValue() = {}", 
        ownerId.longValue() == userId.longValue());
    log.info("비교 결과: Objects.equals(ownerId, userId) = {}", 
        java.util.Objects.equals(ownerId, userId));

    // Long 타입 비교를 안전하게 처리 - Objects.equals 사용
    if (!java.util.Objects.equals(ownerId, userId)) {
      log.error("=== OWNER 권한이 아님 ===");
      log.error("workspaceId: {}", workspaceId);
      log.error("요청 userId: {} (타입: {}), email: {}, name: {}", 
          userId, userId.getClass().getSimpleName(), user.getEmail(), user.getName());
      log.error("워크스페이스 ownerId: {} (타입: {}), email: {}, name: {}", 
          ownerId, ownerId.getClass().getSimpleName(), owner.getEmail(), owner.getName());
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    log.info("=== OWNER 권한 확인 완료, 삭제 진행 ===");
    
    // 워크스페이스 삭제 전 관련 데이터 먼저 삭제
    log.info("워크스페이스 관련 데이터 삭제 시작 - workspaceId: {}", workspaceId);
    
    // WorkspaceUser를 먼저 조회 (VoiceSessionUser 삭제에 필요)
    List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findByWorkspace(workspace);
    
    // 1. VoiceSessionUser 삭제 (workspace_user_id를 통해 간접 참조)
    for (WorkspaceUser workspaceUser : workspaceUsers) {
      List<com.capstone.domain.voicesessionUser.VoiceSessionUser> voiceSessionUsers = 
          voiceSessionUserRepository.findByWorkspaceUser(workspaceUser);
      voiceSessionUserRepository.deleteAll(voiceSessionUsers);
    }
    log.info("VoiceSessionUser 삭제 완료 - {}개", workspaceUsers.size());
    
    // 2. VoiceSession 삭제
    List<com.capstone.domain.voicesession.VoiceSession> voiceSessions = 
        voiceSessionRepository.findByWorkspace_WorkspaceId(workspaceId);
    voiceSessionRepository.deleteAll(voiceSessions);
    log.info("VoiceSession 삭제 완료 - {}개", voiceSessions.size());
    
    // 3. ChatMessage 삭제
    List<com.capstone.domain.chat.ChatMessage> chatMessages = 
        chatMessageRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);
    chatMessageRepository.deleteAll(chatMessages);
    log.info("ChatMessage 삭제 완료 - {}개", chatMessages.size());
    
    // 4. Canvas 삭제
    List<com.capstone.domain.canvas.Canvas> canvases = canvasRepository.findAll().stream()
        .filter(canvas -> canvas.getWorkspace().getWorkspaceId().equals(workspaceId))
        .toList();
    canvasRepository.deleteAll(canvases);
    log.info("Canvas 삭제 완료 - {}개", canvases.size());
    
    // 5. Idea 삭제
    List<com.capstone.domain.idea.Idea> ideas = ideaRepository.findAll().stream()
        .filter(idea -> idea.getWorkspace().getWorkspaceId().equals(workspaceId))
        .toList();
    ideaRepository.deleteAll(ideas);
    log.info("Idea 삭제 완료 - {}개", ideas.size());
    
    // 6. WorkspaceInvitation 삭제
    List<com.capstone.domain.workspaceInvitation.WorkspaceInvitation> invitations = 
        workspaceInvitationRepository.findByWorkspace(workspace);
    workspaceInvitationRepository.deleteAll(invitations);
    log.info("WorkspaceInvitation 삭제 완료 - {}개", invitations.size());
    
    // 7. WorkspaceInvite 삭제
    List<com.capstone.domain.workspaceInvite.WorkspaceInvite> invites = 
        workspaceInviteRepository.findAll().stream()
            .filter(invite -> invite.getWorkspace().getWorkspaceId().equals(workspaceId))
            .toList();
    workspaceInviteRepository.deleteAll(invites);
    log.info("WorkspaceInvite 삭제 완료 - {}개", invites.size());
    
    // 8. WorkspaceUser 삭제 (가장 마지막)
    workspaceUserRepository.deleteAll(workspaceUsers);
    log.info("WorkspaceUser 삭제 완료 - {}개", workspaceUsers.size());
    
    // 9. 마지막으로 Workspace 삭제
    workspaceRepository.delete(workspace);
    log.info("워크스페이스 삭제 완료 - workspaceId: {}, userId: {}", workspaceId, userId);

    // WebSocket 브로드캐스트 (삭제 전에 알림)
    webSocketService.broadcastWorkspaceChange(workspaceId, "deleted", 
        Map.of("workspaceId", workspaceId));
  }
}
