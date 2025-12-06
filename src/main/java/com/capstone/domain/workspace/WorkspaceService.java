package com.capstone.domain.workspace;

import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_ACCESS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE_USER;

import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.chat.ChatMessageRepository;
import com.capstone.domain.idea.Idea;
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
import com.capstone.global.service.FileStorageService;
import com.capstone.global.service.WebSocketService;
import com.capstone.global.type.Role;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
  private final FileStorageService fileStorageService;

  @Transactional
  public Workspace createWorkspace(String name, Long userId) {
    User owner = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    Workspace workspace = new Workspace();
    workspace.setName(name);
    workspace.setOwner(owner);
    Workspace savedWorkspace = workspaceRepository.save(workspace);

    // 기본 썸네일 자동 생성
    try {
      String thumbnailUrl = fileStorageService.generateDefaultThumbnail(name, savedWorkspace.getWorkspaceId());
      savedWorkspace.setThumbnailUrl(thumbnailUrl);
      savedWorkspace = workspaceRepository.saveAndFlush(savedWorkspace);
      log.info("워크스페이스 생성 시 기본 썸네일 자동 생성 완료 - workspaceId: {}, thumbnailUrl: {}", 
          savedWorkspace.getWorkspaceId(), thumbnailUrl);
    } catch (Exception e) {
      log.error("워크스페이스 생성 시 기본 썸네일 생성 실패 - workspaceId: {}, error: {}", 
          savedWorkspace.getWorkspaceId(), e.getMessage(), e);
      // 썸네일 생성 실패해도 워크스페이스 생성은 계속 진행
    }

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

  @Transactional
  public List<Workspace> getWorkspacesByUserId(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(com.capstone.global.exception.ErrorCode.NOT_FOUND_USER));
    
    List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findByUser(user);
    List<Workspace> workspaces = workspaceUsers.stream()
        .map(WorkspaceUser::getWorkspace)
        .filter(w -> w.getDeletedAt() == null) // 삭제되지 않은 워크스페이스만
        .toList();
    
    // 썸네일이 없거나 기본 썸네일인 경우 내용 기반 썸네일로 업데이트
    for (Workspace workspace : workspaces) {
      String currentThumbnailUrl = workspace.getThumbnailUrl();
      boolean needsUpdate = currentThumbnailUrl == null || 
                           currentThumbnailUrl.trim().isEmpty() ||
                           currentThumbnailUrl.contains("-default.png");
      
      if (needsUpdate) {
        try {
          // 워크스페이스의 아이디어 가져오기
          List<Idea> ideas = ideaRepository.findByWorkspace(workspace);
          
          String thumbnailUrl;
          if (ideas != null && !ideas.isEmpty()) {
            // 아이디어가 있으면 내용 기반 썸네일 생성
            thumbnailUrl = fileStorageService.generateWorkspaceContentThumbnail(
                workspace.getName(), workspace.getWorkspaceId(), ideas);
          } else {
            // 아이디어가 없으면 기본 썸네일 생성
            thumbnailUrl = fileStorageService.generateDefaultThumbnail(
                workspace.getName(), workspace.getWorkspaceId());
          }
          
          // 영속성 컨텍스트에 연결된 엔티티로 다시 조회하여 저장
          Workspace managedWorkspace = workspaceRepository.findById(workspace.getWorkspaceId())
              .orElse(workspace);
          managedWorkspace.setThumbnailUrl(thumbnailUrl);
          workspaceRepository.saveAndFlush(managedWorkspace);
          
          // 반환 리스트의 엔티티도 업데이트
          workspace.setThumbnailUrl(thumbnailUrl);
          
          log.info("워크스페이스 썸네일 업데이트 완료 - workspaceId: {}, thumbnailUrl: {}, 아이디어 수: {}", 
              workspace.getWorkspaceId(), thumbnailUrl, ideas != null ? ideas.size() : 0);
        } catch (Exception e) {
          log.error("워크스페이스 썸네일 생성 실패 - workspaceId: {}, error: {}", 
              workspace.getWorkspaceId(), e.getMessage(), e);
          // 썸네일 생성 실패해도 계속 진행
        }
      }
    }
    
    return workspaces;
  }

  @Transactional(readOnly = true)
  public Workspace getWorkspaceById(Long id) {
    return workspaceRepository.findByIdWithOwnerAndNotDeleted(id)
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
    Workspace updated = workspaceRepository.saveAndFlush(workspace);

    WorkspaceDtos.ListItem dto = new WorkspaceDtos.ListItem();
    dto.setWorkspaceId(updated.getWorkspaceId());
    dto.setName(updated.getName());
    dto.setCreatedAt(updated.getCreatedAt());
    dto.setThumbnailUrl(updated.getThumbnailUrl());

    // WebSocket 브로드캐스트
    webSocketService.broadcastWorkspaceChange(workspaceId, "updated", 
        Map.of("workspaceId", updated.getWorkspaceId(), "name", updated.getName()));

    return dto;
  }

  @Transactional
  public void deleteWorkspace(Long workspaceId, Long userId) {
    log.info("=== 워크스페이스 Soft Delete 시도 시작 ===");
    log.info("workspaceId: {} (타입: {})", workspaceId, workspaceId.getClass().getSimpleName());
    log.info("userId: {} (타입: {})", userId, userId.getClass().getSimpleName());
    
    // owner를 함께 로드하여 조회 (삭제되지 않은 것만)
    Workspace workspace = workspaceRepository.findByIdWithOwnerAndNotDeleted(workspaceId)
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

    log.info("=== OWNER 권한 확인 완료, Soft Delete 진행 ===");
    
    // Soft Delete: deletedAt 필드만 설정
    workspace.setDeletedAt(Instant.now());
    workspaceRepository.saveAndFlush(workspace);
    log.info("워크스페이스 Soft Delete 완료 - workspaceId: {}, userId: {}, deletedAt: {}", 
        workspaceId, userId, workspace.getDeletedAt());

    // WebSocket 브로드캐스트
    webSocketService.broadcastWorkspaceChange(workspaceId, "deleted", 
        Map.of("workspaceId", workspaceId));
  }

  @Transactional
  public void restoreWorkspace(Long workspaceId, Long userId) {
    log.info("=== 워크스페이스 복원 시도 시작 ===");
    log.info("workspaceId: {}, userId: {}", workspaceId, userId);
    
    // 삭제된 워크스페이스 조회 (deletedAt이 있는 것)
    Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
        .orElseThrow(() -> {
          log.error("워크스페이스를 찾을 수 없음 - workspaceId: {}", workspaceId);
          return new CustomException(NOT_FOUND_WORKSPACE);
        });

    if (workspace.getDeletedAt() == null) {
      log.warn("워크스페이스가 이미 복원되어 있음 - workspaceId: {}", workspaceId);
      throw new CustomException(com.capstone.global.exception.ErrorCode.BAD_REQUEST);
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    // OWNER 확인
    if (workspace.getOwner() == null || !java.util.Objects.equals(workspace.getOwner().getId(), userId)) {
      log.error("OWNER 권한이 아님 - workspaceId: {}, userId: {}", workspaceId, userId);
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    // 복원: deletedAt을 null로 설정
    workspace.setDeletedAt(null);
    workspaceRepository.saveAndFlush(workspace);
    log.info("워크스페이스 복원 완료 - workspaceId: {}, userId: {}", workspaceId, userId);

    // WebSocket 브로드캐스트
    webSocketService.broadcastWorkspaceChange(workspaceId, "restored", 
        Map.of("workspaceId", workspaceId));
  }

  @Transactional
  public void permanentDeleteWorkspace(Long workspaceId, Long userId) {
    log.info("=== 워크스페이스 영구 삭제 시도 시작 ===");
    log.info("workspaceId: {}, userId: {}", workspaceId, userId);
    
    // 삭제된 워크스페이스 조회
    Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
        .orElseThrow(() -> {
          log.error("워크스페이스를 찾을 수 없음 - workspaceId: {}", workspaceId);
          return new CustomException(NOT_FOUND_WORKSPACE);
        });

    if (workspace.getDeletedAt() == null) {
      log.warn("워크스페이스가 삭제되지 않음 - workspaceId: {}", workspaceId);
      throw new CustomException(com.capstone.global.exception.ErrorCode.BAD_REQUEST);
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    // OWNER 확인
    if (workspace.getOwner() == null || !java.util.Objects.equals(workspace.getOwner().getId(), userId)) {
      log.error("OWNER 권한이 아님 - workspaceId: {}, userId: {}", workspaceId, userId);
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    log.info("=== OWNER 권한 확인 완료, 영구 삭제 진행 ===");
    
    // 워크스페이스 삭제 전 관련 데이터 먼저 삭제
    log.info("워크스페이스 관련 데이터 삭제 시작 - workspaceId: {}", workspaceId);
    
    // WorkspaceUser를 먼저 조회 (VoiceSessionUser 삭제에 필요)
    List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findByWorkspace(workspace);
    
    // 1. VoiceSessionUser 삭제
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
    
    // 9. 마지막으로 Workspace 영구 삭제
    workspaceRepository.delete(workspace);
    log.info("워크스페이스 영구 삭제 완료 - workspaceId: {}, userId: {}", workspaceId, userId);

    // WebSocket 브로드캐스트
    webSocketService.broadcastWorkspaceChange(workspaceId, "permanently_deleted", 
        Map.of("workspaceId", workspaceId));
  }

  @Transactional(readOnly = true)
  public List<Workspace> getDeletedWorkspacesByUserId(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));
    
    return workspaceRepository.findDeletedWorkspacesByUserId(userId);
  }

  /**
   * 워크스페이스 내용이 변경되었을 때 썸네일을 자동으로 갱신합니다.
   * 아이디어 생성/수정/삭제 시 호출됩니다.
   */
  @Transactional
  public void updateWorkspaceThumbnailIfNeeded(Long workspaceId) {
    try {
      Workspace workspace = workspaceRepository.findById(workspaceId)
          .orElse(null);
      
      if (workspace == null) {
        return;
      }

      // 워크스페이스의 아이디어 가져오기
      List<Idea> ideas = ideaRepository.findByWorkspace(workspace);
      
      String thumbnailUrl;
      if (ideas != null && !ideas.isEmpty()) {
        // 아이디어가 있으면 내용 기반 썸네일 생성
        thumbnailUrl = fileStorageService.generateWorkspaceContentThumbnail(
            workspace.getName(), workspace.getWorkspaceId(), ideas);
      } else {
        // 아이디어가 없으면 기본 썸네일 생성
        thumbnailUrl = fileStorageService.generateDefaultThumbnail(
            workspace.getName(), workspace.getWorkspaceId());
      }
      
      // 썸네일 URL 업데이트
      workspace.setThumbnailUrl(thumbnailUrl);
      workspaceRepository.saveAndFlush(workspace);
      
      log.info("워크스페이스 썸네일 자동 갱신 완료 - workspaceId: {}, thumbnailUrl: {}, 아이디어 수: {}", 
          workspaceId, thumbnailUrl, ideas != null ? ideas.size() : 0);
      
      // WebSocket 브로드캐스트
      webSocketService.broadcastWorkspaceChange(workspaceId, "thumbnail_updated", 
          java.util.Map.of("workspaceId", workspaceId, "thumbnailUrl", thumbnailUrl));
    } catch (Exception e) {
      log.error("워크스페이스 썸네일 자동 갱신 실패 - workspaceId: {}, error: {}", 
          workspaceId, e.getMessage(), e);
      // 썸네일 갱신 실패해도 아이디어 작업은 계속 진행
    }
  }

  @Transactional
  public String updateThumbnail(Long workspaceId, MultipartFile file, Long userId) throws java.io.IOException {
    // owner를 함께 로드하여 조회
    Workspace workspace = workspaceRepository.findByIdWithOwner(workspaceId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    // OWNER만 수정 가능
    if (workspace.getOwner() == null || !workspace.getOwner().getId().equals(userId)) {
      throw new CustomException(FORBIDDEN_WORKSPACE);
    }

    // 기존 썸네일 삭제
    if (workspace.getThumbnailUrl() != null) {
      fileStorageService.deleteThumbnail(workspace.getThumbnailUrl());
    }

    // 새 썸네일 저장
    String thumbnailUrl = fileStorageService.saveThumbnail(file, workspaceId);
    workspace.setThumbnailUrl(thumbnailUrl);
    workspaceRepository.saveAndFlush(workspace);

    // WebSocket 브로드캐스트
    webSocketService.broadcastWorkspaceChange(workspaceId, "thumbnail_updated", 
        Map.of("workspaceId", workspaceId, "thumbnailUrl", thumbnailUrl));

    return thumbnailUrl;
  }
}
