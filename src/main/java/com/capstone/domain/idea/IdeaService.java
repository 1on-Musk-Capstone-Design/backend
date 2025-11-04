package com.capstone.domain.idea;

import com.capstone.domain.canvas.Canvas;
import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.domain.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdeaService {

  private final IdeaRepository ideaRepository;
  private final WorkspaceRepository workspaceRepository;
  private final CanvasRepository canvasRepository;
  private final UserRepository userRepository;
  private final WorkspaceUserRepository workspaceUserRepository;

  @Transactional
  public IdeaResponse createIdea(Long userId, IdeaRequest request) {
    Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
        .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다."));

    workspaceUserRepository.findByWorkspaceAndUser(workspace,
            userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")))
        .orElseThrow(() -> new IllegalArgumentException("워크스페이스 소속 사용자가 아닙니다."));

    Idea idea = Idea.builder()
        .workspace(workspace)
        .content(request.getContent())
        .positionX(request.getPositionX())
        .positionY(request.getPositionY())
        .build();

    if (request.getCanvasId() != null) {
      Canvas canvas = canvasRepository.findById(request.getCanvasId())
          .orElseThrow(() -> new IllegalArgumentException("캔버스를 찾을 수 없습니다."));
      idea.setCanvas(canvas);
    }

    return IdeaResponse.from(ideaRepository.save(idea));
  }


  public List<IdeaResponse> getAllIdeas(Long workspaceId) {
    return ideaRepository.findAll().stream()
        .filter(i -> i.getWorkspace().getWorkspaceId().equals(workspaceId))
        .map(IdeaResponse::from)
        .collect(Collectors.toList());
  }

  public IdeaResponse getIdea(Long ideaId) {
    Idea idea = ideaRepository.findById(ideaId)
        .orElseThrow(() -> new IllegalArgumentException("아이디어를 찾을 수 없습니다."));
    return IdeaResponse.from(idea);
  }

  @Transactional
  public IdeaResponse updateIdea(Long userId, Long ideaId, IdeaRequest request) {
    Idea idea = ideaRepository.findById(ideaId)
        .orElseThrow(() -> new IllegalArgumentException("아이디어를 찾을 수 없습니다."));

    workspaceUserRepository.findByWorkspaceAndUser(idea.getWorkspace(),
            userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")))
        .orElseThrow(() -> new IllegalArgumentException("워크스페이스 소속 사용자가 아닙니다."));

    if (request.getCanvasId() != null) {
      Canvas canvas = canvasRepository.findById(request.getCanvasId())
          .orElseThrow(() -> new IllegalArgumentException("캔버스를 찾을 수 없습니다."));
      idea.setCanvas(canvas);
    }

    idea.setContent(request.getContent());
    idea.setPositionX(request.getPositionX());
    idea.setPositionY(request.getPositionY());

    return IdeaResponse.from(idea);
  }

  @Transactional
  public void deleteIdea(Long userId, Long ideaId) {
    Idea idea = ideaRepository.findById(ideaId)
        .orElseThrow(() -> new IllegalArgumentException("아이디어를 찾을 수 없습니다."));

    workspaceUserRepository.findByWorkspaceAndUser(idea.getWorkspace(),
            userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")))
        .orElseThrow(() -> new IllegalArgumentException("워크스페이스 소속 사용자가 아닙니다."));

    ideaRepository.delete(idea);
  }
}
