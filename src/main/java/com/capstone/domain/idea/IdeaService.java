package com.capstone.domain.idea;

import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_ACCESS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_CANVAS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_IDEA;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE;

import com.capstone.domain.canvas.Canvas;
import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspace.WorkspaceService;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.domain.user.UserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.service.WebSocketService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdeaService {

  private final IdeaRepository ideaRepository;
  private final WorkspaceRepository workspaceRepository;
  private final CanvasRepository canvasRepository;
  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final WebSocketService webSocketService;
  private final WorkspaceService workspaceService;

  @Transactional
  public IdeaResponse createIdea(Long userId, IdeaRequest request) {
    Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    workspaceUserRepository.findByWorkspaceAndUser(workspace,
            userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_USER)))
        .orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE_ACCESS));

    Idea idea = Idea.builder()
        .workspace(workspace)
        .content(request.getContent())
        .patchSizeX(request.getPatchSizeX())
        .patchSizeY(request.getPatchSizeY())
        .positionX(request.getPositionX())
        .positionY(request.getPositionY())
        .build();

    if (request.getCanvasId() != null) {
      Canvas canvas = canvasRepository.findById(request.getCanvasId())
          .orElseThrow(() -> new CustomException(NOT_FOUND_CANVAS));
      idea.setCanvas(canvas);
    }

    IdeaResponse response = IdeaResponse.from(ideaRepository.save(idea));

    // WebSocket 브로드캐스트 (실패해도 API 응답은 성공 — STOMP/직렬화 이슈로 500 방지)
    try {
      webSocketService.broadcastIdeaChange(request.getWorkspaceId(), "created", response);
    } catch (Exception e) {
      log.warn("아이디어 생성 브로드캐스트 실패 workspaceId={}: {}", request.getWorkspaceId(), e.getMessage());
    }

    // 썸네일 자동 갱신 (비동기)
    try {
      workspaceService.updateWorkspaceThumbnailIfNeeded(request.getWorkspaceId());
    } catch (Exception e) {
      log.warn("아이디어 생성 후 썸네일 갱신 실패 - workspaceId: {}, error: {}", 
          request.getWorkspaceId(), e.getMessage());
    }

    return response;
  }


  public List<IdeaResponse> getAllIdeas(Long workspaceId) {
    return ideaRepository.findAll().stream()
        .filter(i -> i.getWorkspace().getWorkspaceId().equals(workspaceId))
        .map(IdeaResponse::from)
        .collect(Collectors.toList());
  }

  public IdeaResponse getIdea(Long ideaId) {
    Idea idea = ideaRepository.findById(ideaId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_IDEA));
    return IdeaResponse.from(idea);
  }

  @Transactional
  public IdeaResponse updateIdea(Long userId, Long ideaId, IdeaRequest request) {
    Idea idea = ideaRepository.findById(ideaId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_IDEA));

    workspaceUserRepository.findByWorkspaceAndUser(idea.getWorkspace(),
            userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_USER)))
        .orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE_ACCESS));

    if (request.getCanvasId() != null) {
      Canvas canvas = canvasRepository.findById(request.getCanvasId())
          .orElseThrow(() -> new CustomException(NOT_FOUND_CANVAS));
      idea.setCanvas(canvas);
    }

    // null 필드는 덮어쓰지 않음 — PRD 등에서 content·workspaceId만 보낼 때 좌표/패치가 null로 지워지며 500·데이터 꼬임 방지
    if (request.getContent() != null) {
      idea.setContent(request.getContent());
    }
    if (request.getPatchSizeX() != null) {
      idea.setPatchSizeX(request.getPatchSizeX());
    }
    if (request.getPatchSizeY() != null) {
      idea.setPatchSizeY(request.getPatchSizeY());
    }
    if (request.getPositionX() != null) {
      idea.setPositionX(request.getPositionX());
    }
    if (request.getPositionY() != null) {
      idea.setPositionY(request.getPositionY());
    }

    ideaRepository.save(idea);

    IdeaResponse response = IdeaResponse.from(idea);

    // WebSocket 브로드캐스트 (실패해도 API는 성공 — 직렬화/메시지 크기 등으로 500 방지)
    try {
      webSocketService.broadcastIdeaChange(idea.getWorkspace().getWorkspaceId(), "updated", response);
    } catch (Exception e) {
      log.warn(
          "아이디어 수정 브로드캐스트 실패 workspaceId={}: {}",
          idea.getWorkspace().getWorkspaceId(),
          e.getMessage());
    }

    // 썸네일 자동 갱신 (비동기)
    try {
      workspaceService.updateWorkspaceThumbnailIfNeeded(idea.getWorkspace().getWorkspaceId());
    } catch (Exception e) {
      log.warn("아이디어 수정 후 썸네일 갱신 실패 - workspaceId: {}, error: {}", 
          idea.getWorkspace().getWorkspaceId(), e.getMessage());
    }

    return response;
  }

  @Transactional
  public void deleteIdea(Long userId, Long ideaId) {
    Idea idea = ideaRepository.findById(ideaId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_IDEA));

    workspaceUserRepository.findByWorkspaceAndUser(idea.getWorkspace(),
            userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(NOT_FOUND_USER)))
        .orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE_ACCESS));

    Long workspaceId = idea.getWorkspace().getWorkspaceId();
    Long deletedIdeaId = idea.getId();

    ideaRepository.delete(idea);

    try {
      webSocketService.broadcastIdeaChange(workspaceId, "deleted", java.util.Map.of("id", deletedIdeaId));
    } catch (Exception e) {
      log.warn("아이디어 삭제 브로드캐스트 실패 workspaceId={}: {}", workspaceId, e.getMessage());
    }

    // 썸네일 자동 갱신 (비동기)
    try {
      workspaceService.updateWorkspaceThumbnailIfNeeded(workspaceId);
    } catch (Exception e) {
      log.warn("아이디어 삭제 후 썸네일 갱신 실패 - workspaceId: {}, error: {}", 
          workspaceId, e.getMessage());
    }
  }
}
