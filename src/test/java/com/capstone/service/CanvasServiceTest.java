package com.capstone.service;

import com.capstone.domain.canvas.Canvas;
import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.canvas.CanvasRequest;
import com.capstone.domain.canvas.CanvasResponse;
import com.capstone.domain.canvas.CanvasService;
import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CanvasServiceTest {

  @Mock
  private CanvasRepository canvasRepository;

  @Mock
  private WorkspaceRepository workspaceRepository;

  @Mock
  private WorkspaceUserRepository workspaceUserRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private CanvasService canvasService;

  private User user;
  private Workspace workspace;
  private WorkspaceUser workspaceUser;
  private Canvas canvas;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    user = User.builder()
        .id(1L)
        .email("캡스톤@com")
        .name("캡스톤")
        .profileImage("profile.png")
        .createdAt(new Timestamp(System.currentTimeMillis()))
        .updatedAt(new Timestamp(System.currentTimeMillis()))
        .build();

    workspace = new Workspace();
    workspace.setWorkspaceId(1L);
    workspace.setName("캡스톤 워크스페이스");

    workspaceUser = WorkspaceUser.builder()
        .id(1L)
        .workspace(workspace)
        .user(user)
        .role(null)
        .joinedAt(new Timestamp(System.currentTimeMillis()))
        .build();

    canvas = Canvas.builder()
        .id(1L)
        .workspace(workspace)
        .workspaceUser(workspaceUser)
        .title("캡스톤 캔버스")
        .createdAt(new Timestamp(System.currentTimeMillis()))
        .updatedAt(new Timestamp(System.currentTimeMillis()))
        .build();
  }

  @Test
  @DisplayName("캔버스 생성 성공")
  void createCanvasSuccess() {
    CanvasRequest request = new CanvasRequest("새 캔버스");

    when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user))
        .thenReturn(Optional.of(workspaceUser));
    when(canvasRepository.save(any(Canvas.class))).thenReturn(canvas);

    CanvasResponse response = canvasService.createCanvas(1L, 1L, request);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(canvas.getId());
    assertThat(response.getTitle()).isEqualTo(canvas.getTitle());

    verify(canvasRepository, times(1)).save(any(Canvas.class));
  }

  @Test
  @DisplayName("캔버스 전체 조회 성공")
  void getAllCanvasSuccess() {
    when(canvasRepository.findAll()).thenReturn(List.of(canvas));

    List<CanvasResponse> responses = canvasService.getAllCanvas(1L);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().getId()).isEqualTo(canvas.getId());
  }

  @Test
  @DisplayName("캔버스 단건 조회 성공")
  void getCanvasSuccess() {
    when(canvasRepository.findById(1L)).thenReturn(Optional.of(canvas));

    CanvasResponse response = canvasService.getCanvas(1L);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(canvas.getId());
  }

  @Test
  @DisplayName("캔버스 수정 성공")
  void updateCanvasSuccess() {
    CanvasRequest request = new CanvasRequest("수정된 제목");

    when(canvasRepository.findById(1L)).thenReturn(Optional.of(canvas));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user))
        .thenReturn(Optional.of(workspaceUser));

    CanvasResponse response = canvasService.updateCanvas(1L, 1L, request);

    assertThat(response.getTitle()).isEqualTo("수정된 제목");
  }

  @Test
  @DisplayName("캔버스 삭제 성공")
  void deleteCanvasSuccess() {
    when(canvasRepository.findById(1L)).thenReturn(Optional.of(canvas));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user))
        .thenReturn(Optional.of(workspaceUser));

    canvasService.deleteCanvas(1L, 1L);

    verify(canvasRepository, times(1)).delete(canvas);
  }
}
