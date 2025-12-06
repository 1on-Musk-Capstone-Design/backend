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
import com.capstone.domain.user.repository.UserRepository;
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

    // WebSocket 브로드캐스트
    webSocketService.broadcastIdeaChange(request.getWorkspaceId(), "created", response);

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

    idea.setContent(request.getContent());
    idea.setPatchSizeX(request.getPatchSizeX());
    idea.setPatchSizeY(request.getPatchSizeY());
    idea.setPositionX(request.getPositionX());
    idea.setPositionY(request.getPositionY());

    IdeaResponse response = IdeaResponse.from(idea);

    // WebSocket 브로드캐스트
    webSocketService.broadcastIdeaChange(idea.getWorkspace().getWorkspaceId(), "updated", response);

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

    // WebSocket 브로드캐스트
    webSocketService.broadcastIdeaChange(workspaceId, "deleted", java.util.Map.of("id", deletedIdeaId));

    // 썸네일 자동 갱신 (비동기)
    try {
      workspaceService.updateWorkspaceThumbnailIfNeeded(workspaceId);
    } catch (Exception e) {
      log.warn("아이디어 삭제 후 썸네일 갱신 실패 - workspaceId: {}, error: {}", 
          workspaceId, e.getMessage());
    }
  }
}
