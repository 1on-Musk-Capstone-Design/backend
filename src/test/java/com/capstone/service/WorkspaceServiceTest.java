package com.capstone.service;

import com.capstone.entity.Workspace;
import com.capstone.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_shouldReturnSavedWorkspace() {
        // Given
        String workspaceName = "테스트 워크스페이스";
        Workspace mockWorkspace = new Workspace();
        mockWorkspace.setId(1L);
        mockWorkspace.setName(workspaceName);

        when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);

        // When
        Workspace result = workspaceService.createWorkspace(workspaceName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(workspaceName);
    }

    @Test
    void createWorkspace_shouldTrimWhitespace() {
        // Given
        String workspaceName = "  테스트 워크스페이스  ";
        String expectedName = "테스트 워크스페이스";
        Workspace mockWorkspace = new Workspace();
        mockWorkspace.setId(1L);
        mockWorkspace.setName(expectedName);

        when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);

        // When
        Workspace result = workspaceService.createWorkspace(workspaceName);

        // Then
        assertThat(result.getName()).isEqualTo(expectedName);
    }
}
