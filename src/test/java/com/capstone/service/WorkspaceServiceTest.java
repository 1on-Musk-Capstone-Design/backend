package com.capstone.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceDtos;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspace.WorkspaceService;
import com.capstone.domain.canvas.CanvasRepository;
import com.capstone.domain.chat.ChatMessageRepository;
import com.capstone.domain.idea.IdeaRepository;
import com.capstone.domain.voicesession.VoiceSessionRepository;
import com.capstone.domain.voicesessionUser.VoiceSessionUserRepository;
import com.capstone.domain.workspaceInvitation.WorkspaceInvitationRepository;
import com.capstone.domain.workspaceInvite.WorkspaceInviteRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.service.WebSocketService;
import com.capstone.global.type.Role;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

  @Mock
  private WorkspaceRepository workspaceRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private WorkspaceUserRepository workspaceUserRepository;
  @Mock
  private WorkspaceInviteRepository workspaceInviteRepository;
  @Mock
  private WorkspaceInvitationRepository workspaceInvitationRepository;
  @Mock
  private ChatMessageRepository chatMessageRepository;
  @Mock
  private CanvasRepository canvasRepository;
  @Mock
  private IdeaRepository ideaRepository;
  @Mock
  private VoiceSessionRepository voiceSessionRepository;
  @Mock
  private VoiceSessionUserRepository voiceSessionUserRepository;
  @Mock
  private WebSocketService webSocketService;

  @InjectMocks
  private WorkspaceService workspaceService;

  @Test
  @DisplayName("워크스페이스 생성 시 정상적으로 생성")
  void createWorkspace() {
    Long userId = 10L;
    String workspaceName = "캡스톤 워크스페이스";

    User user = User.builder().id(userId).email("캡스톤@user.com").name("캡스톤").build();
    Workspace savedWorkspace = new Workspace();
    savedWorkspace.setWorkspaceId(1L);
    savedWorkspace.setName(workspaceName);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(workspaceRepository.save(org.mockito.ArgumentMatchers.any(Workspace.class)))
        .thenReturn(savedWorkspace);
    when(workspaceUserRepository.save(any(WorkspaceUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    doNothing().when(webSocketService).broadcastWorkspaceChange(any(), any(), any());

    Workspace result = workspaceService.createWorkspace(workspaceName, userId);

    assertThat(result.getWorkspaceId()).isEqualTo(1L);
    assertThat(result.getName()).isEqualTo("캡스톤 워크스페이스");
  }

  @Test
  @DisplayName("모든 워크스페이스 조회 시 전체 리스트를 반환")
  void getAllWorkspaces() {
    Workspace w1 = new Workspace();
    w1.setWorkspaceId(1L);
    w1.setName("워크스페이스1");
    Workspace w2 = new Workspace();
    w2.setWorkspaceId(2L);
    w2.setName("워크스페이스2");

    when(workspaceRepository.findAll()).thenReturn(Arrays.asList(w1, w2));

    List<Workspace> result = workspaceService.getAllWorkspaces();

    assertThat(result).hasSize(2);
    assertThat(result.getFirst().getName()).isEqualTo("워크스페이스1");
  }

  @Test
  @DisplayName("워크스페이스 이름 수정 시 OWNER면 정상적으로 수정")
  void updateWorkspaceName() {
    // Given
    Long userId = 10L;
    User user = User.builder()
        .id(userId)
        .email("capstone@test.com")
        .name("캡스톤 유저")
        .build();

    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(1L);
    workspace.setName("Old");
    workspace.setOwner(user);

    when(workspaceRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(workspace));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(workspaceRepository.save(workspace)).thenReturn(workspace);
    doNothing().when(webSocketService).broadcastWorkspaceChange(any(), any(), any());

    WorkspaceDtos.ListItem result = workspaceService.updateWorkspaceName(1L, "New", userId);

    assertThat(result.getName()).isEqualTo("New");
  }

  @Test
  @DisplayName("워크스페이스 삭제 시 OWNER가 아니면 예외가 발생")
  void deleteWorkspace() {
    // Given
    Long userId = 10L;
    User user = User.builder()
        .id(userId)
        .email("capstone@test.com")
        .name("캡스톤 유저")
        .build();

    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(1L);
    User owner = User.builder().id(20L).email("owner@test.com").name("Owner").build();
    workspace.setOwner(owner);

    when(workspaceRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(workspace));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> workspaceService.deleteWorkspace(1L, userId))
        .isInstanceOf(com.capstone.global.exception.CustomException.class);
  }
}