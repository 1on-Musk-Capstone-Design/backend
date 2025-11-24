package com.capstone.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.capstone.domain.canvas.Canvas;
import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.idea.*;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.global.service.WebSocketService;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class IdeaServiceTest {

  @Mock
  private IdeaRepository ideaRepository;

  @Mock
  private WorkspaceRepository workspaceRepository;

  @Mock
  private CanvasRepository canvasRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private WorkspaceUserRepository workspaceUserRepository;

  @Mock
  private WebSocketService webSocketService;

  @InjectMocks
  private IdeaService ideaService;

  private User user;
  private Workspace workspace;
  private Canvas canvas;
  private WorkspaceUser workspaceUser;
  private Timestamp now;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    now = new Timestamp(System.currentTimeMillis());

    user = User.builder()
        .id(1L)
        .name("캡스톤")
        .email("캡스톤@com")
        .createdAt(now)
        .updatedAt(now)
        .build();

    workspace = new Workspace();
    workspace.setWorkspaceId(1L);
    workspace.setName("캡스톤 워크스페이스");

    canvas = Canvas.builder()
        .id(1L)
        .workspace(workspace)
        .title("캔버스1")
        .createdAt(now)
        .updatedAt(now)
        .build();

    workspaceUser = WorkspaceUser.builder()
        .id(1L)
        .user(user)
        .workspace(workspace)
        .joinedAt(now)
        .build();
  }

  @Test
  @DisplayName("아이디어 생성 성공")
  void createIdea() {
    IdeaRequest request = IdeaRequest.builder()
        .workspaceId(workspace.getWorkspaceId())
        .canvasId(canvas.getId())
        .content("캡스톤 아이디어")
        .patchSizeX(2.0)
        .patchSizeY(20.0)
        .positionX(1.0)
        .positionY(2.0)
        .build();

    when(workspaceRepository.findById(workspace.getWorkspaceId())).thenReturn(
        Optional.of(workspace));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user)).thenReturn(
        Optional.of(workspaceUser));
    when(canvasRepository.findById(canvas.getId())).thenReturn(Optional.of(canvas));

    Idea savedIdea = Idea.builder()
        .id(1L)
        .workspace(workspace)
        .canvas(canvas)
        .content(request.getContent())
        .patchSizeX(request.getPatchSizeX())
        .patchSizeY(request.getPatchSizeY())
        .positionX(request.getPositionX())
        .positionY(request.getPositionY())
        .createdAt(now)
        .updatedAt(now)
        .build();

    when(ideaRepository.save(any(Idea.class))).thenReturn(savedIdea);
    doNothing().when(webSocketService).broadcastIdeaChange(any(), any(), any());

    IdeaResponse response = ideaService.createIdea(user.getId(), request);
    assertEquals(request.getContent(), response.getContent());
    assertEquals(request.getPatchSizeX(), savedIdea.getPatchSizeX());
    assertEquals(request.getPatchSizeY(), savedIdea.getPatchSizeY());
    assertEquals(request.getPositionX(), savedIdea.getPositionX());
    assertEquals(request.getPositionY(), savedIdea.getPositionY());
  }

  @Test
  @DisplayName("아이디어 전체 조회 성공")
  void getAllIdeas() {
    Idea idea1 = Idea.builder()
        .id(1L)
        .workspace(workspace)
        .content("Idea 1")
        .createdAt(now)
        .updatedAt(now)
        .build();

    Idea idea2 = Idea.builder()
        .id(2L)
        .workspace(workspace)
        .content("Idea 2")
        .createdAt(now)
        .updatedAt(now)
        .build();

    when(ideaRepository.findAll()).thenReturn(List.of(idea1, idea2));

    List<IdeaResponse> responses = ideaService.getAllIdeas(workspace.getWorkspaceId());

    assertEquals(2, responses.size());
    assertEquals("Idea 1", responses.get(0).getContent());
    assertEquals("Idea 2", responses.get(1).getContent());
  }

  @Test
  @DisplayName("아이디어 단일 조회 성공")
  void getIdea() {
    Idea idea = Idea.builder()
        .id(1L)
        .workspace(workspace)
        .content("Idea 1")
        .createdAt(now)
        .updatedAt(now)
        .build();

    when(ideaRepository.findById(idea.getId())).thenReturn(Optional.of(idea));

    IdeaResponse response = ideaService.getIdea(idea.getId());

    assertEquals("Idea 1", response.getContent());
  }

  @Test
  @DisplayName("아이디어 수정 성공")
  void updateIdea() {
    Idea idea = Idea.builder()
        .id(1L)
        .workspace(workspace)
        .canvas(canvas)
        .content("Old Content")
        .patchSizeX(0.0)
        .patchSizeY(0.0)
        .positionX(0.0)
        .positionY(0.0)
        .createdAt(now)
        .updatedAt(now)
        .build();

    IdeaRequest request = IdeaRequest.builder()
        .workspaceId(workspace.getWorkspaceId())
        .canvasId(canvas.getId())
        .content("Updated Content")
        .patchSizeX(10.0)
        .patchSizeY(20.0)
        .positionX(10.0)
        .positionY(20.0)
        .build();

    when(ideaRepository.findById(idea.getId())).thenReturn(Optional.of(idea));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user)).thenReturn(
        Optional.of(workspaceUser));
    when(canvasRepository.findById(canvas.getId())).thenReturn(Optional.of(canvas));

    IdeaResponse response = ideaService.updateIdea(user.getId(), idea.getId(), request);

    assertEquals("Updated Content", response.getContent());
    assertEquals(10.0, response.getPositionX());
    assertEquals(20.0, response.getPositionY());
  }

  @Test
  @DisplayName("아이디어 삭제 성공")
  void deleteIdea() {
    Idea idea = Idea.builder().id(1L).workspace(workspace).build();

    when(ideaRepository.findById(idea.getId())).thenReturn(Optional.of(idea));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user)).thenReturn(
        Optional.of(workspaceUser));
    doNothing().when(webSocketService).broadcastIdeaChange(any(), any(), any());

    assertDoesNotThrow(() -> ideaService.deleteIdea(user.getId(), idea.getId()));
    verify(ideaRepository, times(1)).delete(idea);
  }
}
