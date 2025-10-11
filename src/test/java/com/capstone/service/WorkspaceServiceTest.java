package com.capstone.service;

import com.capstone.domain.user.entity.User;
import com.capstone.domain.user.repository.UserRepository;
import com.capstone.domain.workspace.Workspace;
import com.capstone.domain.workspace.WorkspaceDtos;
import com.capstone.domain.workspace.WorkspaceRepository;
import com.capstone.domain.workspace.WorkspaceService;
import com.capstone.domain.workspaceUser.WorkspaceUser;
import com.capstone.domain.workspaceUser.WorkspaceUserRepository;
import com.capstone.global.type.Role;
import java.util.Arrays;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

  @Mock
  private WorkspaceRepository workspaceRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private WorkspaceUserRepository workspaceUserRepository;

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
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .email("capstone@test.com")
        .name("캡스톤 유저")
        .build();

    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(1L);
    workspace.setName("Old");

    WorkspaceUser workspaceUser = WorkspaceUser.builder()
        .workspace(workspace)
        .user(user)
        .role(Role.OWNER)
        .build();

    when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
    when(userRepository.findById(10L)).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user))
        .thenReturn(Optional.of(workspaceUser));
    when(workspaceRepository.save(workspace)).thenReturn(workspace);

    WorkspaceDtos.ListItem result = workspaceService.updateWorkspaceName(1L, "New", 10L);

    assertThat(result.getName()).isEqualTo("New");
  }

  @Test
  @DisplayName("워크스페이스 삭제 시 OWNER가 아니면 예외가 발생")
  void deleteWorkspace() {
    // Given
    Long userId = 1L;
    User user = User.builder()
        .id(userId)
        .email("capstone@test.com")
        .name("캡스톤 유저")
        .build();

    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(1L);

    WorkspaceUser workspaceUser = WorkspaceUser.builder()
        .workspace(workspace)
        .user(user)
        .role(Role.MEMBER)
        .build();

    when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
    when(userRepository.findById(10L)).thenReturn(Optional.of(user));
    when(workspaceUserRepository.findByWorkspaceAndUser(workspace, user))
        .thenReturn(Optional.of(workspaceUser));

    assertThatThrownBy(() -> workspaceService.deleteWorkspace(1L, 10L))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("워크스페이스 삭제 권한이 없습니다.");
  }
}