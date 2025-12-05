package com.capstone.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.domain.workspace.WorkspaceService;
import com.capstone.domain.workspaceUser.WorkspaceUserResponse;
import com.capstone.domain.workspaceUser.WorkspaceUserService;
import com.capstone.global.service.WebSocketService;
import com.capstone.global.type.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceUserServiceTest {

  @Mock
  private WorkspaceUserRepository workspaceUserRepository;
  @Mock
  private WorkspaceRepository workspaceRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private WorkspaceService workspaceService;
  @Mock
  private WebSocketService webSocketService;

  @InjectMocks
  private WorkspaceUserService workspaceUserService;

  @Test
  @DisplayName("워크스페이스 참여 성공")
  void joinWorkspace() {
    Long userId = 1L;
    Long workspaceId = 1L;

    User user = User.builder().id(userId).name("캡스톤").email("capstone@test.com").build();
    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(workspaceId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
    when(workspaceUserRepository.existsByWorkspaceAndUser(workspace, user)).thenReturn(false);
    when(workspaceUserRepository.save(any())).thenAnswer(
        invocation -> invocation.getArgument(0)); // save 정상 처리
    doNothing().when(webSocketService).notifyUserJoined(anyLong(), anyLong(), anyString());

    workspaceUserService.joinWorkspace(workspaceId, userId);
  }

  @Test
  @DisplayName("워크스페이스 사용자 목록 조회")
  void getWorkspaceUsers() {
    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(1L);

    User user = User.builder().id(1L).name("캡스톤").email("capstone@test.com").build();
    WorkspaceUser workspaceUser = WorkspaceUser.builder()
        .workspace(workspace)
        .user(user)
        .role(Role.MEMBER)
        .build();

    when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
    when(workspaceUserRepository.findByWorkspace(workspace)).thenReturn(List.of(workspaceUser));

    List<WorkspaceUserResponse> users = workspaceUserService.getWorkspaceUsers(1L);
    assertThat(users).hasSize(1);
    assertThat(users.getFirst().getName()).isEqualTo("캡스톤");
  }

  @Test
  @DisplayName("워크스페이스 삭제 권한 없는 경우 예외 발생")
  void removeUser() {
    User requester = User.builder().id(10L).build();
    User target = User.builder().id(20L).build();
    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(1L);

    WorkspaceUser requesterWorkspaceUser = WorkspaceUser.builder()
        .workspace(workspace)
        .user(requester)
        .role(Role.MEMBER)
        .build();

    when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
    when(userRepository.findById(10L)).thenReturn(Optional.of(requester));
    when(userRepository.findById(20L)).thenReturn(Optional.of(target));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, requester))
        .thenReturn(Optional.of(requesterWorkspaceUser));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, target))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> workspaceUserService.removeUser(1L, 20L, 10L))
        .isInstanceOf(RuntimeException.class);
  }
}