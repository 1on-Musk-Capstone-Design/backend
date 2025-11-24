package com.capstone.domain.canvas;

import static com.capstone.global.exception.ErrorCode.FORBIDDEN_WORKSPACE_ACCESS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_CANVAS;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_USER;
import static com.capstone.global.exception.ErrorCode.NOT_FOUND_WORKSPACE;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.exception.CustomException;
import com.capstone.global.service.WebSocketService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CanvasService {

  private final CanvasRepository canvasRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final UserRepository userRepository;
  private final WebSocketService webSocketService;

  public CanvasResponse createCanvas(Long userId, Long workspaceId, CanvasRequest request) {
    Workspace workspace = workspaceRepository.findById(workspaceId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_WORKSPACE));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_USER));

    WorkspaceUser workspaceUser = workspaceUserRepository.findByWorkspaceAndUser(workspace, user)
        .orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE_ACCESS));

    Canvas canvas = Canvas.builder()
        .workspace(workspace)
        .workspaceUser(workspaceUser)
        .title(request.getTitle())
        .build();
    Canvas newCanvas = canvasRepository.save(canvas);

    CanvasResponse response = CanvasResponse.builder()
        .id(newCanvas.getId())
        .title(newCanvas.getTitle())
        .createdAt(newCanvas.getCreatedAt().toString())
        .updatedAt(newCanvas.getUpdatedAt().toString())
        .build();

    // WebSocket 브로드캐스트
    webSocketService.broadcastCanvasChange(workspaceId, "created", response);

    return response;
  }

  public List<CanvasResponse> getAllCanvas(Long workspaceId) {
    return canvasRepository.findAll().stream()
        .filter(c -> c.getWorkspace().getWorkspaceId().equals(workspaceId))
        .map(c -> CanvasResponse.builder()
            .id(c.getId())
            .title(c.getTitle())
            .createdAt(c.getCreatedAt().toString())
            .updatedAt(c.getUpdatedAt().toString())
            .build())
        .collect(Collectors.toList());
  }

  public CanvasResponse getCanvas(Long canvasId) {
    Canvas updateCanvas = canvasRepository.findById(canvasId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_CANVAS));

    return CanvasResponse.builder()
        .id(updateCanvas.getId())
        .title(updateCanvas.getTitle())
        .createdAt(updateCanvas.getCreatedAt().toString())
        .updatedAt(updateCanvas.getUpdatedAt().toString())
        .build();
  }

  @Transactional
  public CanvasResponse updateCanvas(Long userId, Long canvasId, CanvasRequest request) {
    Canvas canvas = canvasRepository.findById(canvasId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_CANVAS));

    workspaceUserRepository.findByWorkspaceAndUser(
        canvas.getWorkspace(),
        userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(NOT_FOUND_USER))
    ).orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE_ACCESS));

    canvas.setTitle(request.getTitle());
    CanvasResponse response = CanvasResponse.builder()
        .id(canvas.getId())
        .title(canvas.getTitle())
        .createdAt(canvas.getCreatedAt().toString())
        .updatedAt(canvas.getUpdatedAt().toString())
        .build();

    // WebSocket 브로드캐스트
    webSocketService.broadcastCanvasChange(canvas.getWorkspace().getWorkspaceId(), "updated", response);

    return response;
  }

  public void deleteCanvas(Long userId, Long canvasId) {
    Canvas deleteCanvas = canvasRepository.findById(canvasId)
        .orElseThrow(() -> new CustomException(NOT_FOUND_CANVAS));

    workspaceUserRepository.findByWorkspaceAndUser(
        deleteCanvas.getWorkspace(),
        userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(NOT_FOUND_USER))
    ).orElseThrow(() -> new CustomException(FORBIDDEN_WORKSPACE_ACCESS));

    Long workspaceId = deleteCanvas.getWorkspace().getWorkspaceId();
    Long deletedCanvasId = deleteCanvas.getId();

    canvasRepository.delete(deleteCanvas);

    // WebSocket 브로드캐스트
    webSocketService.broadcastCanvasChange(workspaceId, "deleted", Map.of("id", deletedCanvasId));
  }
}