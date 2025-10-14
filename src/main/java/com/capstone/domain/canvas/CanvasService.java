package com.capstone.domain.canvas;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CanvasService {

  private final CanvasRepository canvasRepository;
  private final WorkspaceRepository workspaceRepository;
  private final WorkspaceUserRepository workspaceUserRepository;
  private final UserRepository userRepository;

  public CanvasResponse createCanvas(Long userId, CanvasRequest request) {
    Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
        .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다."));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

    WorkspaceUser workspaceUser = workspaceUserRepository.findByWorkspaceAndUser(workspace, user)
        .orElseThrow(() -> new IllegalArgumentException("워크스페이스 소속 사용자가 아닙니다."));

    Canvas canvas = Canvas.builder()
        .workspace(workspace)
        .workspaceUser(workspaceUser)
        .title(request.getTitle())
        .build();

    Canvas newCanvas = canvasRepository.save(canvas);

    return CanvasResponse.builder()
        .id(newCanvas.getId())
        .title(newCanvas.getTitle())
        .createdAt(newCanvas.getCreatedAt().toString())
        .updatedAt(newCanvas.getUpdatedAt().toString())
        .build();
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

  public CanvasResponse getCanvas(Long id) {
    Canvas updateCanvas = canvasRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("캔버스를 찾을 수 없습니다."));

    return CanvasResponse.builder()
        .id(updateCanvas.getId())
        .title(updateCanvas.getTitle())
        .createdAt(updateCanvas.getCreatedAt().toString())
        .updatedAt(updateCanvas.getUpdatedAt().toString())
        .build();
  }

  public void deleteCanvas(Long userId, Long canvasId) {
    Canvas deleteCanvas = canvasRepository.findById(canvasId)
        .orElseThrow(() -> new IllegalArgumentException("캔버스를 찾을 수 없습니다."));

    workspaceUserRepository.findByWorkspaceAndUser(
        deleteCanvas.getWorkspace(),
        userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."))
    ).orElseThrow(() -> new IllegalArgumentException("워크스페이스 소속 사용자가 아닙니다."));

    canvasRepository.delete(deleteCanvas);
  }
}